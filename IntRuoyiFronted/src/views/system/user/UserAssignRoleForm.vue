<template>
  <Dialog v-model="dialogVisible" title="分配角色">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" label-width="80px">
      <el-form-item label="用户名称">
        <el-input v-model="formData.username" :disabled="true" />
      </el-form-item>
      <el-form-item label="用户昵称">
        <el-input v-model="formData.nickname" :disabled="true" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="formData.roleIds" :disabled="formLoading || !roleOptionsReady" multiple placeholder="请选择角色">
          <el-option v-for="item in roleList" :key="item.id" :label="roleOptionLabel(item, roleList)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item
        label="变更原因"
        prop="reason"
        :rules="[{ required: true, message: '请输入角色变更原因', trigger: 'blur' }]"
      >
        <el-input
          v-model="formData.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入本次用户角色变更原因"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading || !roleOptionsReady" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as PermissionApi from '@/api/system/permission'
import * as UserApi from '@/api/system/user'
import * as RoleApi from '@/api/system/role'
import { confirmUserChange } from './user-change-confirmation'
import { roleOptionLabel } from './role-option-label'

defineOptions({ name: 'SystemUserAssignRoleForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
let generation = 0
let originalRoles: number[] = []
watch(dialogVisible, (visible) => { if (!visible) generation++ }, { flush: 'sync' })
const roleOptionsReady = ref(false)
const formData = ref({
  id: -1,
  nickname: '',
  username: '',
  roleIds: [],
  reason: ''
})
const formRef = ref() // 表单 Ref
const roleList = ref([] as RoleApi.RoleVO[]) // 角色的列表

/** 打开弹窗 */
const open = async (row: UserApi.UserVO) => {
  const current = ++generation
  dialogVisible.value = true
  resetForm()
  // 设置数据
  formData.value.id = row.id
  formData.value.username = row.username
  formData.value.nickname = row.nickname
  // 获得角色拥有的菜单集合
  formLoading.value = true
  try {
    const [roleIds, availableRoles] = await Promise.all([
      PermissionApi.getUserRoleList(row.id),
      RoleApi.getSimpleRoleList()
    ])
    if (current !== generation || !dialogVisible.value) return
    originalRoles = [...roleIds]
    roleList.value = availableRoles
    formData.value.roleIds = roleIds
    roleOptionsReady.value = true
  } finally {
    if (current === generation) formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  if (formLoading.value || !dialogVisible.value) return
  if (!roleOptionsReady.value) throw new Error('角色资料尚未加载完成，请重新打开后再分配')
  if (!formRef.value) throw new Error('角色表单尚未就绪')
  const current = generation
  formLoading.value = true
  try {
    const valid = await formRef.value.validate()
    if (!valid || current !== generation || !dialogVisible.value) return
    const data = {
      userId: formData.value.id,
      roleIds: [...formData.value.roleIds],
      reason: formData.value.reason.trim()
    }
    if (!data.reason) { message.error('请输入角色变更原因'); return }
    const labels = (ids: number[]) => ids.map((id) => {
      const role = roleList.value.find((item) => item.id === id)
      return role ? `${roleOptionLabel(role, roleList.value)}（ID:${id}）` : `角色（ID:${id}）`
    }).join('、') || '无'
    const content = `目标账号：${formData.value.username}（ID:${data.userId}）\n角色：${labels(originalRoles)} → ${labels(data.roleIds)}\n变更原因：${data.reason}`
    if (!(await confirmUserChange(message.confirm, content))) return
    if (current !== generation || !dialogVisible.value) return
    await PermissionApi.assignUserRole({ ...data, idempotencyKey: createPermissionAuditIdempotencyKey() })
    if (current !== generation || !dialogVisible.value) return
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success', true)
  } finally {
    if (current === generation) formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  roleOptionsReady.value = false
  roleList.value = []
  formData.value = {
    id: -1,
    nickname: '',
    username: '',
    roleIds: [],
    reason: ''
  }
  formRef.value?.resetFields()
}

const createPermissionAuditIdempotencyKey = () => {
  if (typeof crypto !== 'object' || typeof crypto.randomUUID !== 'function') {
    throw new Error('当前浏览器不支持 crypto.randomUUID，无法生成权限审计幂等键')
  }
  return `SYSTEM-PERM-USER-ROLE-${formData.value.id}-${crypto.randomUUID()}`
}
</script>
