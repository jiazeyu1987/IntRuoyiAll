<template>
  <Dialog v-model="dialogVisible" title="导入工艺路线 Markdown" width="560">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-form-item label="工序状态" prop="processStatus">
        <el-select
          v-model="formData.processStatus"
          placeholder="请选择导入后工序状态"
          clearable
          class="!w-full"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="检验工序映射" prop="checkProcessCodesByRouteCodeJson">
        <el-input
          v-model="formData.checkProcessCodesByRouteCodeJson"
          type="textarea"
          :rows="5"
          resize="none"
          placeholder='可选，示例：{"ROUTE-YXN.044.02.1020":["W030"]}'
        />
      </el-form-item>
    </el-form>

    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      action="#"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      :on-remove="handleFileRemove"
      :on-change="handleFileChange"
      :on-exceed="handleExceed"
      accept=".md,.markdown,text/markdown"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将 Markdown 文件拖到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <span>仅允许导入 .md、.markdown 格式文件。</span>
        </div>
      </template>
    </el-upload>

    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { ProRouteApi } from '@/api/mes/pro/route'
import type { FormInstance, FormRules, UploadFile, UploadFiles, UploadUserFile } from 'element-plus'
import { resolveRouteOperationErrorMessage } from './routeError'

defineOptions({ name: 'RouteMarkdownImportForm' })

const message = useMessage()

type FormDataType = {
  processStatus: number | undefined
  checkProcessCodesByRouteCodeJson: string
}

const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const formData = reactive<FormDataType>({
  processStatus: undefined,
  checkProcessCodesByRouteCodeJson: ''
})

const validateCheckProcessJson = (_rule: any, value: string, callback: (error?: Error) => void) => {
  if (!value || !value.trim()) {
    callback()
    return
  }
  try {
    const parsed = JSON.parse(value)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      callback(new Error('请输入 JSON 对象'))
      return
    }
    callback()
  } catch {
    callback(new Error('请输入合法 JSON'))
  }
}

const formRules: FormRules<FormDataType> = {
  processStatus: [{ required: true, message: '请选择导入后工序状态', trigger: 'change' }],
  checkProcessCodesByRouteCodeJson: [{ validator: validateCheckProcessJson, trigger: 'blur' }]
}

const open = () => {
  dialogVisible.value = true
  resetForm()
}
defineExpose({ open })

const emits = defineEmits(['success'])

const submitForm = async () => {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!selectedFile.value) {
    message.error('请上传 Markdown 文件')
    return
  }
  try {
    formLoading.value = true
    const uploadFormData = new FormData()
    uploadFormData.append('file', selectedFile.value)
    uploadFormData.append('processStatus', String(formData.processStatus))
    if (formData.checkProcessCodesByRouteCodeJson.trim()) {
      uploadFormData.append(
        'checkProcessCodesByRouteCodeJson',
        formData.checkProcessCodesByRouteCodeJson.trim()
      )
    }

    const result = await ProRouteApi.importIntGyMarkdown(uploadFormData)
    const routeCodesText = result.routeCodes?.length ? result.routeCodes.join('、') : '无'
    message.alert(
      `导入完成；路线数：${result.routeCount}；新建工序数：${result.processCreatedCount}；复用工序数：${result.processReusedCount}；路线工序数：${result.routeProcessCount}；路线编码：${routeCodesText}`
    )
    dialogVisible.value = false
    emits('success')
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '导入 Markdown 失败，请查看后端返回错误'))
  } finally {
    formLoading.value = false
  }
}

const handleFileChange = (file: UploadFile, files: UploadFiles) => {
  if (file.name && !/\.md$|\.markdown$/i.test(file.name)) {
    message.error('仅允许上传 .md 或 .markdown 文件')
    selectedFile.value = null
    fileList.value = []
    nextTick(() => uploadRef.value?.clearFiles())
    return
  }
  fileList.value = files.slice(-1) as UploadUserFile[]
  selectedFile.value = (file.raw as File) || null
}

const handleFileRemove = () => {
  selectedFile.value = null
}

const handleExceed = () => {
  message.error('最多只能上传一个文件！')
}

const resetForm = async () => {
  formLoading.value = false
  formData.processStatus = undefined
  formData.checkProcessCodesByRouteCodeJson = ''
  selectedFile.value = null
  fileList.value = []
  await nextTick()
  formRef.value?.clearValidate()
  uploadRef.value?.clearFiles()
}
</script>
