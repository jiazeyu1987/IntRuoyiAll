<template>
  <Dialog v-model="dialogVisible" title="导入路线 Excel" width="560">
    <div
      class="mb-16px rounded-6px border border-[#dbe3ef] bg-[#fafcff] px-12px py-10px text-13px leading-22px text-[#4b5563]"
    >
      <div>仅支持由“导出”生成的多 Sheet .xlsx 文件。</div>
      <div>重复路线编码、表头不一致或主数据缺失会导入失败。</div>
      <div>不会自动创建产品、物料、工序、工作站或设备主数据。</div>
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
import { ProRouteApi } from '@/api/mes/pro/route'
import type { UploadFile, UploadFiles, UploadUserFile } from 'element-plus'

defineOptions({ name: 'RouteWorkbookExcelImportForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)

const open = () => {
  dialogVisible.value = true
  resetForm()
}
defineExpose({ open })

const emits = defineEmits(['success'])

const submitForm = async () => {
  if (!selectedFile.value) {
    message.error('请上传 Excel 文件')
    return
  }
  try {
    formLoading.value = true
    const uploadFormData = new FormData()
    uploadFormData.append('file', selectedFile.value)

    const result = await ProRouteApi.importRouteWorkbookExcel(uploadFormData)
    const routeCodesText = result.routeCodes?.length ? result.routeCodes.join('、') : '无'
    message.alert(
      `导入完成；路线数：${result.routeCount}；路线工序数：${result.routeProcessCount}；产品绑定数：${result.routeProductCount}；工序BOM数：${result.routeProductBomCount}；路线编码：${routeCodesText}`
    )
    dialogVisible.value = false
    emits('success')
  } catch (error) {
    message.error(getErrorMessage(error, '导入失败，请根据后端提示修正 Excel'))
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
  selectedFile.value = null
  fileList.value = []
  await nextTick()
  uploadRef.value?.clearFiles()
}

const getErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string' && error) {
    return error
  }
  return defaultMessage
}
</script>
