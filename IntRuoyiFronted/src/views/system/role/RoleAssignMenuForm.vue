<template>
  <Dialog v-model="dialogVisible" title="权限角色菜单权限">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" label-width="80px">
      <el-form-item label="权限角色名称">
        <el-tag>{{ formData.name }}</el-tag>
      </el-form-item>
      <el-form-item
        label="变更原因"
        prop="reason"
        :rules="[{ required: true, message: '请输入权限变更原因', trigger: 'blur' }]"
      >
        <el-input
          v-model="formData.reason"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入本次菜单权限变更原因"
        />
      </el-form-item>
      <el-form-item label="菜单权限">
        <el-checkbox v-model="checkStrictly" @change="preserveSelectedMenus">
          父子独立选择（可仅授权目录）
        </el-checkbox>
        <el-card class="w-full h-400px !overflow-y-scroll" shadow="never">
          <el-tree
            ref="treeRef"
            :check-strictly="checkStrictly"
            :default-expand-all="false"
            :data="menuOptions"
            :props="defaultProps"
            empty-text="加载中，请稍候"
            node-key="id"
            show-checkbox
          />
        </el-card>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { defaultProps, handleTree } from '@/utils/tree'
import * as RoleApi from '@/api/system/role'
import * as MenuApi from '@/api/system/menu'
import * as PermissionApi from '@/api/system/permission'

defineOptions({ name: 'SystemRoleAssignMenuForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

interface RoleAssignMenuFormData {
  id?: number
  name: string
  menuIds: number[]
  reason: string
}

const createDefaultFormData = (): RoleAssignMenuFormData => ({
  id: undefined,
  name: '',
  menuIds: [],
  reason: ''
})

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formData = reactive<RoleAssignMenuFormData>(createDefaultFormData())
const formRef = ref() // 表单 Ref
const menuOptions = ref<any[]>([]) // 菜单树形结构
const treeRef = ref() // 菜单树组件 Ref
const checkStrictly = ref(false)

const preserveSelectedMenus = async () => {
  if (!checkStrictly.value) return
  const selected = [...treeRef.value.getCheckedKeys(false), ...treeRef.value.getHalfCheckedKeys()]
  await nextTick()
  treeRef.value.setCheckedKeys(selected)
}

/** 打开弹窗 */
const open = async (row: RoleApi.RoleVO) => {
  dialogVisible.value = true
  resetForm()
  // 加载 Menu 列表。注意，必须放在前面，不然下面 setChecked 没数据节点
  menuOptions.value = handleTree(await MenuApi.getSimpleMenusList())
  // 设置数据
  formData.id = row.id
  formData.name = row.name
  formLoading.value = true
  try {
    formData.menuIds = await PermissionApi.getRoleMenuList(row.id)
    // 设置选中
    formData.menuIds.forEach((menuId: number) => {
      treeRef.value.setChecked(menuId, true, false)
    })
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = {
      roleId: formData.id!,
      menuIds: [
        ...(treeRef.value.getCheckedKeys(false) as unknown as Array<number>), // 获得当前选中节点
        ...(treeRef.value.getHalfCheckedKeys() as unknown as Array<number>) // 获得半选中的父节点
      ],
      reason: formData.reason.trim(),
      idempotencyKey: createPermissionAuditIdempotencyKey()
    } as PermissionApi.PermissionAssignRoleMenuReqVO
    await PermissionApi.assignRoleMenu(data)
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  // 重置表单
  checkStrictly.value = false
  Object.assign(formData, createDefaultFormData())
  treeRef.value?.setCheckedNodes([])
  formRef.value?.resetFields()
}

const createPermissionAuditIdempotencyKey = () => {
  if (typeof crypto !== 'object' || typeof crypto.randomUUID !== 'function') {
    throw new Error('当前浏览器不支持 crypto.randomUUID，无法生成权限审计幂等键')
  }
  return `SYSTEM-PERM-ROLE-MENU-${formData.id}-${crypto.randomUUID()}`
}
</script>
