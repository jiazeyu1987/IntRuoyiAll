<template>
  <Dialog v-model="dialogVisible" title="用户导入" width="400">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :action="importUrl + '?updateSupport=' + updateSupport"
      :auto-upload="false"
      :disabled="formLoading"
      :headers="uploadHeaders"
      :limit="1"
      :on-error="submitFormError"
      :on-exceed="handleExceed"
      :on-success="submitFormSuccess"
      accept=".xlsx, .xls"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <div class="el-upload__tip">
            <el-checkbox v-model="updateSupport" :disabled="formLoading" />
            是否更新已经存在的用户数据
          </div>
          <span>仅允许导入 xls、xlsx 格式文件。</span>
          <el-link
            :underline="false"
            style="font-size: 12px; vertical-align: baseline"
            type="primary"
            @click="importTemplate"
          >
            下载模板
          </el-link>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import type { UploadUserFile } from 'element-plus'
import { showRecordImportResult } from '@/utils/importResult'
import { confirmUserImport } from './userImportConfirmation'
import * as UserApi from '@/api/system/user'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import download from '@/utils/download'

defineOptions({ name: 'SystemUserImportForm' })

const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中
const uploadRef = ref()
const importUrl =
  import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL + '/system/user/import'
const uploadHeaders = ref() // 上传 Header 头
const fileList = ref<UploadUserFile[]>([]) // 文件列表
const updateSupport = ref(0) // 是否更新已经存在的用户数据

/** 打开弹窗 */
const open = () => {
  if (formLoading.value) return
  dialogVisible.value = true
  updateSupport.value = 0
  fileList.value = []
  resetForm()
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

let dialogGeneration = 0
watch(dialogVisible, () => { dialogGeneration++ }, { flush: 'sync' })

/** 提交表单 */
const submitForm = async () => {
  if (formLoading.value) return
  if (fileList.value.length == 0) {
    message.error('请上传文件')
    return
  }
  const overwrite = updateSupport.value
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
    const preview = await UserApi.importUserPreview(previewData)
    if (!dialogVisible.value || generation !== dialogGeneration || fileList.value[0] !== file ||
        getTenantId() !== tenant || getVisitTenantId() !== visitTenant || getAccessToken() !== token || updateSupport.value !== overwrite) {
      formLoading.value = false
      return
    }
    const confirmed = await confirmUserImport(file.name, overwrite ? 'update' : 'create', visitTenant || tenant, preview)
    if (!confirmed || !dialogVisible.value || generation !== dialogGeneration ||
        fileList.value[0] !== file || updateSupport.value !== overwrite || getTenantId() !== tenant || getVisitTenantId() !== visitTenant || getAccessToken() !== token) {
      formLoading.value = false
      return
    }
  } catch (error) {
    formLoading.value = false
    throw error
  }
  // 提交请求
  uploadHeaders.value = {
    Authorization: 'Bearer ' + token,
    'tenant-id': tenant,
    ...(visitTenant ? { 'visit-tenant-id': visitTenant } : {})
  }
  formLoading.value = true
  await nextTick()
  if (!dialogVisible.value || generation !== dialogGeneration || fileList.value[0] !== file ||
      updateSupport.value !== overwrite || getTenantId() !== tenant || getVisitTenantId() !== visitTenant || getAccessToken() !== token) {
    formLoading.value = false
    return
  }
  uploadRef.value!.submit()
}

/** 文件上传成功 */
const emits = defineEmits(['success'])
const submitFormSuccess = (response: any) => {
  if (response.code !== 0) {
    message.error(response.msg)
    resetForm()
    return
  }
  const data = response.data
  showRecordImportResult({
    entity: '用户账号',
    created: data.createUsernames,
    updated: data.updateUsernames,
    failures: data.failureUsernames,
  })
  formLoading.value = false
  dialogVisible.value = false
  // 发送操作成功的事件
  emits('success')
}

/** 上传错误提示 */
const submitFormError = (): void => {
  message.error('上传失败，请您重新上传！')
  formLoading.value = false
}

/** 重置表单 */
const resetForm = async (): Promise<void> => {
  // 重置上传状态和文件
  formLoading.value = false
  await nextTick()
  uploadRef.value?.clearFiles()
}

/** 文件数超出提示 */
const handleExceed = (): void => {
  message.error('最多只能上传一个文件！')
}

/** 下载模板操作 */
const importTemplate = async () => {
  const res = await UserApi.importUserTemplate()
  download.excel(res, '用户导入模版.xls')
}
</script>
