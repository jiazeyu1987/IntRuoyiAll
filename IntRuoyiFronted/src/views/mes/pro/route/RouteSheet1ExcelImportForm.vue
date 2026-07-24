<template>
  <Dialog v-model="dialogVisible" title="导入 Sheet1 Excel" width="560">
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
    </el-form>

    <div
      class="mb-16px rounded-6px border border-[#dbe3ef] bg-[#fafcff] px-12px py-10px text-13px leading-22px text-[#4b5563]"
    >
      <div>仅读取 Sheet1。</div>
      <div>重复设备行会按工序首出现顺序去重。</div>
      <div>设备、数量、日产能、人工等资源列本次不导入。</div>
    </div>

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
      accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <span>仅允许导入 .xlsx 格式文件。</span>
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

defineOptions({ name: 'RouteSheet1ExcelImportForm' })

const message = useMessage()

type FormDataType = {
  processStatus: number | undefined
}

const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const formData = reactive<FormDataType>({
  processStatus: undefined
})

const formRules: FormRules<FormDataType> = {
  processStatus: [{ required: true, message: '请选择导入后工序状态', trigger: 'change' }]
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
    message.error('请上传 Excel 文件')
    return
  }
  try {
    formLoading.value = true
    const uploadFormData = new FormData()
    uploadFormData.append('file', selectedFile.value)
    uploadFormData.append('processStatus', String(formData.processStatus))

    const result = await ProRouteApi.importSheet1Excel(uploadFormData)
    const routeCodesText = result.routeCodes?.length ? result.routeCodes.join('、') : '无'
    message.alert(
      `导入完成；路线数：${result.routeCount}；新建工序数：${result.processCreatedCount}；复用工序数：${result.processReusedCount}；路线工序数：${result.routeProcessCount}；路线编码：${routeCodesText}`
    )
    dialogVisible.value = false
    emits('success')
  } catch (error) {
    message.error(resolveRouteOperationErrorMessage(error, '导入 Sheet1 Excel 失败，请查看后端返回错误'))
  } finally {
    formLoading.value = false
  }
}

const handleFileChange = (file: UploadFile, files: UploadFiles) => {
  if (file.name && !/\.xlsx$/i.test(file.name)) {
    message.error('仅允许上传 .xlsx 文件')
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
  selectedFile.value = null
  fileList.value = []
  await nextTick()
  formRef.value?.clearValidate()
  uploadRef.value?.clearFiles()
}
</script>
