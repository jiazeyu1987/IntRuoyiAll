<template>
  <Dialog v-model="dialogVisible" title="钉钉导入" width="420">
    <div
      v-if="formLoading"
      class="flex flex-col items-center justify-center gap-16px py-40px"
      role="status"
      aria-live="polite"
      aria-busy="true"
    >
      <Icon icon="ep:loading" class="animate-spin text-32px text-primary" />
      <span>正在处理导入，请稍候…</span>
    </div>
    <el-upload
      v-show="!formLoading"
      ref="uploadRef"
      v-model:file-list="fileList"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      :on-exceed="handleExceed"
      accept=".xlsx, .xls"
      action="none"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <span>支持钉钉在职员工 xls、xlsx 文件，含员工UserID、姓名、1至7级部门及部门主管；邮箱、工号、主部门ID可不提供。缺少主部门ID时按完整部门路径识别。</span>
        </div>
      </template>
    </el-upload>
    <template v-if="!formLoading" #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { showImportResult } from '@/utils/importResult'
import type { UploadUserFile } from 'element-plus'
import { confirmUserImport } from './userImportConfirmation'
import * as UserApi from '@/api/system/user'

defineOptions({ name: 'SystemUserDingTalkImportForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])

const emits = defineEmits(['success'])

/** 打开弹窗 */
const open = async () => {
  if (formLoading.value) return
  dialogVisible.value = true
  await resetForm()
}
defineExpose({ open })

let dialogGeneration = 0
watch(dialogVisible, () => { dialogGeneration++ }, { flush: 'sync' })

/** 提交表单 */
const submitForm = async () => {
  if (formLoading.value) return
  if (fileList.value.length === 0) {
    message.error('请上传文件')
    return
  }
  const file = fileList.value[0]
  if (!file.raw) {
    message.error('文件内容不可用，请重新选择文件')
    return
  }
  const visitTenant = getVisitTenantId()
  const tenant = getTenantId()
  const generation = dialogGeneration
  const token = getAccessToken()
  formLoading.value = true
  try {
    const previewData = new FormData()
    previewData.append('file', file.raw)
    await UserApi.importDingTalkUserPreview(previewData)
    if (!dialogVisible.value || generation !== dialogGeneration || fileList.value[0] !== file ||
        getTenantId() !== tenant || getVisitTenantId() !== visitTenant || getAccessToken() !== token) {
      formLoading.value = false
      return
    }
    const confirmed = await confirmUserImport(file.raw)
    if (!confirmed || !dialogVisible.value || generation !== dialogGeneration ||
        fileList.value[0] !== file || getTenantId() !== tenant || getVisitTenantId() !== visitTenant || getAccessToken() !== token) {
      formLoading.value = false
      return
    }
  } catch (error) {
    formLoading.value = false
    throw error
  }
  formLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file.raw as Blob)
    const response = await UserApi.importDingTalkUsers(formData)
    submitFormSuccess(response)
  } catch (error) {
    message.error(error instanceof Error ? error.message : String(error))
  } finally {
    formLoading.value = false
  }
}

/** 文件上传成功 */
const submitFormSuccess = (response: any) => {
  if (response.code !== 0) {
    message.error(response.msg)
    return
  }
  const data: UserApi.UserDingTalkImportRespVO = response.data
  if (!data || !Array.isArray(data.createUsernames) || !Array.isArray(data.existingUsernames) ||
      !Array.isArray(data.authorizedUsernames)) {
    throw new Error('钉钉导入结果缺少电子签名授权信息，前后端版本不一致。请管理员更新并重启后端后再核查本次导入结果。')
  }
  const failureCount = Object.keys(data.failureUsernames).length
  showImportResult({
    title: failureCount ? '钉钉导入存在失败记录' : '钉钉导入完成',
    summary: `已创建 ${data.createUsernames.length} 个用户，匹配 ${data.existingUsernames.length} 个已有用户，${data.authorizedUsernames.length} 个用户已获得电子签名授权，${failureCount} 个用户导入失败。`,
    warning: failureCount > 0 || Object.keys(data.leaderSkippedDeptPaths).length > 0,
    stats: [
      { label: '创建用户', value: data.createUsernames.length },
      { label: '匹配已有用户', value: data.existingUsernames.length },
      { label: '电子签名已授权', value: data.authorizedUsernames.length },
      { label: '用户导入失败', value: failureCount },
      { label: '新建部门', value: data.createdDeptPaths.length },
      { label: '启用部门', value: data.enabledDeptPaths.length },
      { label: '已设置部门负责人', value: Object.keys(data.leaderAssignedDeptPaths).length },
      { label: '未设置部门负责人', value: Object.keys(data.leaderSkippedDeptPaths).length }
    ],
    guidance: failureCount || Object.keys(data.leaderSkippedDeptPaths).length
      ? '请查看下方未完成的记录及原因，核对用户资料和部门负责人设置。'
      : undefined,
    sections: [
      { title: '导入失败的用户', columns: ['用户账号', '失败原因'], rows: Object.entries(data.failureUsernames), expanded: true },
      { title: '未设置负责人的部门', columns: ['部门', '原因'], rows: Object.entries(data.leaderSkippedDeptPaths), expanded: true },
      { title: '已创建的用户', columns: ['用户账号'], rows: data.createUsernames.map((name: string) => [name]) },
      { title: '匹配的已有用户', columns: ['用户账号'], rows: data.existingUsernames.map((name: string) => [name]) },
      { title: '电子签名已授权的用户', columns: ['用户账号'], rows: data.authorizedUsernames.map((name: string) => [name]) },
      { title: '新建的部门', columns: ['部门'], rows: data.createdDeptPaths.map((name: string) => [name]) },
      { title: '已启用的部门', columns: ['部门'], rows: data.enabledDeptPaths.map((name: string) => [name]) },
      { title: '已设置的部门负责人', columns: ['部门', '负责人'], rows: Object.entries(data.leaderAssignedDeptPaths) }
    ]
  })
  dialogVisible.value = false
  emits('success')
}

/** 重置表单 */
const resetForm = async () => {
  fileList.value = []
  await nextTick()
  uploadRef.value?.clearFiles()
}

/** 文件数超出提示 */
const handleExceed = () => {
  message.error('最多只能上传一个文件！')
}
</script>
