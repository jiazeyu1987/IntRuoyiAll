<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle">
    <el-alert v-if="formError" :title="formError" type="error" :closable="false" />
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      :disabled="formLoading || !postSelectionReady"
      label-width="80px"
    >
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户昵称" prop="nickname">
            <el-input v-model="formData.nickname" placeholder="请输入用户昵称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="归属部门" prop="deptId">
            <el-tree-select
              v-model="formData.deptId"
              :data="deptList"
              :props="defaultProps"
              check-strictly
              node-key="id"
              placeholder="请选择归属部门"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="手机号码" prop="mobile">
            <el-input v-model="formData.mobile" maxlength="11" placeholder="请输入手机号码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" maxlength="50" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item v-if="formData.id === undefined" label="用户名称" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item v-if="formData.id === undefined" label="用户密码" prop="password">
            <el-input
              v-model="formData.password"
              placeholder="请输入用户密码"
              show-password
              type="password"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="用户性别">
            <el-select v-model="formData.sex" placeholder="请选择">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_USER_SEX)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="岗位">
            <el-select
              :model-value="formData.postIds"
              :disabled="!postSelectionReady || formLoading"
              multiple
              placeholder="请选择"
              @update:model-value="updatePostSelection"
            >
              <el-option
                v-for="item in visiblePostList"
                :key="item.id"
                :label="item.label"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="formData.remark" placeholder="请输入内容" type="textarea" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button
        :disabled="formLoading || !postSelectionReady"
        type="primary"
        @click="submitForm"
      >
        确 定
      </el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { defaultProps, handleTree } from '@/utils/tree'
import * as PostApi from '@/api/system/post'
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import { FormRules } from 'element-plus'
import { systemPasswordRule } from './systemPasswordPolicy'

defineOptions({ name: 'SystemUserForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const postSelectionReady = ref(false)
const formError = ref('')
let openGeneration = 0
type UserFormData = Partial<Omit<UserApi.UserVO, 'deptId' | 'postIds'>> & {
  deptId: number | ''
  postIds: string[]
  password?: string
  roleIds?: number[]
}
const formData = ref<UserFormData>({
  nickname: '',
  deptId: '',
  mobile: '',
  email: '',
  id: undefined,
  username: '',
  password: '',
  sex: undefined,
  postIds: [] as string[],
  remark: '',
  status: CommonStatusEnum.ENABLE,
  roleIds: [] as number[]
})
const formRules = reactive<FormRules>({
  username: [{ required: true, message: '用户名称不能为空', trigger: 'blur' }],
  nickname: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  password: [
    { required: true, message: '用户密码不能为空', trigger: 'blur' },
    systemPasswordRule
  ],
  email: [
    {
      type: 'email',
      message: '请输入正确的邮箱地址',
      trigger: ['blur', 'change']
    }
  ],
  mobile: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号码',
      trigger: 'blur'
    }
  ]
})
const formRef = ref() // 表单 Ref
const deptList = ref<Tree[]>([]) // 树形结构
interface PostOption {
  id: string
  name: string
  label: string
}
const postList = ref<PostOption[]>([]) // 正式启用候选
const assignedDisabledPosts = ref<PostOption[]>([])
const retainedDisabledPostIds = ref<string[]>([])
// 停用项保持可移除；一旦移除，候选随之消失，本次编辑不能重新添加。
const visiblePostList = computed(() => [
  ...postList.value,
  ...assignedDisabledPosts.value.filter((post) => retainedDisabledPostIds.value.includes(post.id))
])

const postIntegrityError = () => new Error('用户岗位资料不完整或已变化，请重新打开；持续失败请联系管理员')
const exactPostId = (value: unknown): string => {
  if (typeof value === 'number' && Number.isSafeInteger(value) && value > 0) {
    return String(value)
  }
  if (typeof value === 'string' && /^[1-9]\d*$/.test(value) && BigInt(value) <= 9223372036854775807n) {
    return value
  }
  throw postIntegrityError()
}
const exactPostIds = (value: unknown): string[] => {
  if (!Array.isArray(value)) throw postIntegrityError()
  const ids = value.map(exactPostId)
  if (new Set(ids).size !== ids.length) throw postIntegrityError()
  return ids
}
const validPostName = (value: unknown): string => {
  if (typeof value !== 'string' || !value.trim()) throw postIntegrityError()
  return value
}
const validateCandidates = (value: unknown): PostOption[] => {
  if (!Array.isArray(value)) throw postIntegrityError()
  const candidates = value.map((post) => {
    if (!post || typeof post !== 'object') throw postIntegrityError()
    const name = validPostName(post.name)
    return { id: exactPostId(post.id), name, label: name }
  })
  exactPostIds(candidates.map((post) => post.id))
  return candidates
}
const validateAssignedPosts = (user: UserApi.UserEditVO, candidates: PostOption[]) => {
  if (!user || typeof user !== 'object' || !Array.isArray(user.assignedPosts)) {
    throw postIntegrityError()
  }
  const ids = exactPostIds(user.postIds)
  const assigned = user.assignedPosts.map((post) => {
    if (!post || typeof post !== 'object') throw postIntegrityError()
    const id = exactPostId(post.id)
    const name = validPostName(post.name)
    if (post.status !== CommonStatusEnum.ENABLE && post.status !== CommonStatusEnum.DISABLE) {
      throw postIntegrityError()
    }
    const candidate = candidates.find((item) => item.id === id)
    if (
      (post.status === CommonStatusEnum.ENABLE && (!candidate || candidate.name !== name)) ||
      (post.status === CommonStatusEnum.DISABLE && candidate)
    ) {
      throw postIntegrityError()
    }
    return { id, name, status: post.status, label: name + '（已停用）' }
  })
  const assignedIds = exactPostIds(assigned.map((post) => post.id))
  if (ids.length !== assignedIds.length || ids.some((id) => !assignedIds.includes(id))) {
    throw postIntegrityError()
  }
  return { ids, disabled: assigned.filter((post) => post.status === CommonStatusEnum.DISABLE) }
}
const validateSelection = (value: unknown): string[] => {
  const ids = exactPostIds(value)
  const allowedIds = new Set([
    ...postList.value.map((post) => post.id),
    ...retainedDisabledPostIds.value
  ])
  if (ids.some((id) => !allowedIds.has(id))) throw postIntegrityError()
  return ids
}
const updatePostSelection = (value: unknown) => {
  if (!postSelectionReady.value || formLoading.value) return
  const ids = validateSelection(value)
  retainedDisabledPostIds.value = retainedDisabledPostIds.value.filter((id) => ids.includes(id))
  formData.value.postIds = ids
}
watch(dialogVisible, (visible) => {
  if (!visible) {
    openGeneration++
    postSelectionReady.value = false
    formLoading.value = false
  }
}, { flush: 'sync' })

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  const generation = ++openGeneration
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  formLoading.value = true
  try {
    if (type !== 'create' && type !== 'update') throw new Error('用户表单操作类型无效')
    if (type === 'update' && (!Number.isSafeInteger(id) || !id || id <= 0)) {
      throw new Error('缺少有效用户编号')
    }
    const [user, departments, posts] = await Promise.all([
      type === 'update' ? UserApi.getUserForUpdate(id!) : Promise.resolve(undefined),
      DeptApi.getSimpleDeptList({ ignoreErrorMessage: true }),
      PostApi.getSimplePostList({ ignoreErrorMessage: true })
    ])
    if (generation !== openGeneration || !dialogVisible.value) return
    if (!Array.isArray(departments)) throw new Error('部门资料不完整，请重新打开')
    const candidates = validateCandidates(posts)
    const assigned = type === 'update' ? validateAssignedPosts(user!, candidates) : undefined
    if (user && user.id !== id) throw new Error('用户编辑详情与当前用户不一致，请重新打开')
    const departmentTree = handleTree(departments)
    // 所有正式来源通过校验后，一次激活完整表单，失败不留下可提交的半成品。
    if (user && assigned) {
      const { assignedPosts: _assignedPosts, ...details } = user
      formData.value = { ...details, postIds: assigned.ids }
    }
    deptList.value = departmentTree
    postList.value = candidates
    assignedDisabledPosts.value = assigned?.disabled ?? []
    retainedDisabledPostIds.value = assignedDisabledPosts.value.map((post) => post.id)
    postSelectionReady.value = true
  } catch (error) {
    if (generation !== openGeneration || !dialogVisible.value) return
    formError.value = error instanceof Error && error.message
      ? error.message
      : typeof error === 'string' && error
        ? error
        : '用户编辑资料加载失败，请重新打开'
    return false
  } finally {
    if (generation === openGeneration) formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  if (!dialogVisible.value || !postSelectionReady.value || formLoading.value) return
  if (!formRef.value) throw new Error('用户表单尚未就绪')
  const generation = openGeneration
  const type = formType.value
  formLoading.value = true
  try {
    const valid = await formRef.value.validate()
    if (!valid || generation !== openGeneration || !dialogVisible.value) return
    const data = {
      ...formData.value,
      postIds: validateSelection(formData.value.postIds)
    } as UserApi.UserVO
    if (type === 'create') {
      await UserApi.createUser(data, { ignoreErrorMessage: true })
    } else {
      await UserApi.updateUser(data, { ignoreErrorMessage: true })
    }
    if (generation !== openGeneration || !dialogVisible.value) return
    message.success(t(type === 'create' ? 'common.createSuccess' : 'common.updateSuccess'))
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } catch (error) {
    if (generation !== openGeneration || !dialogVisible.value) return
    formError.value = error instanceof Error && error.message
      ? error.message
      : typeof error === 'string' && error
        ? error
        : '用户保存失败，请检查正式错误信息'
    return false
  } finally {
    if (generation === openGeneration) formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  postSelectionReady.value = false
  formError.value = ''
  deptList.value = []
  postList.value = []
  assignedDisabledPosts.value = []
  retainedDisabledPostIds.value = []
  formData.value = {
    nickname: '',
    deptId: '',
    mobile: '',
    email: '',
    id: undefined,
    username: '',
    password: '',
    sex: undefined,
    postIds: [],
    remark: '',
    status: CommonStatusEnum.ENABLE,
    roleIds: []
  }
  formRef.value?.resetFields()
}
</script>
