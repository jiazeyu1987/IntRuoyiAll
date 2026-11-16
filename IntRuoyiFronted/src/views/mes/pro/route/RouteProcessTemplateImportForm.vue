<template>
  <Dialog v-model="dialogVisible" title="工序模板" width="620px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="路线">
        {{ selectedRoute?.code || '-' }} {{ selectedRoute?.name || '' }}
      </el-descriptions-item>
      <el-descriptions-item label="模板字段">
        工序名称、产能、设备编号、是否关键工序
      </el-descriptions-item>
    </el-descriptions>

    <el-alert
      class="mt-12px"
      type="info"
      :closable="false"
      title="模板不包含工序负责人、工序编号、质量控制要求、工序开始配置、批记录表单和表单槽位。批记录表单请使用独立批记录导入。"
    />

    <el-form class="mt-16px" label-width="90px">
      <el-form-item label="导入方式">
        <el-radio-group v-model="importMode">
          <el-radio value="UPGRADE">升版</el-radio>
          <el-radio value="REBUILD">重建</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="importMode === 'UPGRADE'" label="升版说明">
        <span class="text-13px text-[var(--el-text-color-secondary)]">
          生成或更新当前路线的草稿候选，当前生效版本不变。
        </span>
      </el-form-item>
      <el-form-item v-else label="重建说明">
        <span class="text-13px text-[var(--el-text-color-secondary)]">
          直接重建当前路线工序；路线已启用时，后端会拒绝本次导入。
        </span>
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
      accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击选择</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">仅允许导入 .xlsx 格式文件。</div>
      </template>
    </el-upload>

    <template #footer>
      <el-button
        type="success"
        plain
        :loading="downloadLoading"
        :disabled="formLoading"
        @click="downloadTemplate"
      >
        <Icon icon="ep:download" class="mr-5px" /> 下载模板
      </el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">
        <Icon icon="ep:upload" class="mr-5px" /> 导入
      </el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { showImportResult } from '@/utils/importResult'
import { ProRouteApi } from '@/api/mes/pro/route'
import type {
  ProRouteProcessTemplateImportMode,
  ProRouteVO
} from '@/api/mes/pro/route'
import download from '@/utils/download'
import type { UploadFile, UploadFiles, UploadUserFile } from 'element-plus'

defineOptions({ name: 'RouteProcessTemplateImportForm' })

const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const downloadLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const selectedRoute = ref<ProRouteVO>()
const importMode = ref<ProRouteProcessTemplateImportMode>('UPGRADE')

const emits = defineEmits(['success'])

const open = (route: ProRouteVO) => {
  if (!route.id) {
    throw new Error('打开工序模板失败：缺少路线编号')
  }
  selectedRoute.value = route
  dialogVisible.value = true
  resetForm()
}
defineExpose({ open })

const downloadTemplate = async () => {
  if (!selectedRoute.value?.id) {
    message.error('下载模板失败：缺少路线编号')
    return
  }
  downloadLoading.value = true
  try {
    const data = await ProRouteApi.exportRouteProcessTemplate(selectedRoute.value.id)
    download.excel(data, `${selectedRoute.value.code}-工序模板.xlsx`)
  } catch (error) {
    message.error(getErrorMessage(error, '下载工序模板失败，请根据错误提示处理；仍无法下载时，请联系管理员。'))
  } finally {
    downloadLoading.value = false
  }
}

const submitForm = async () => {
  if (!selectedFile.value) {
    message.error('请上传 Excel 文件')
    return
  }
  try {
    formLoading.value = true
    const uploadFormData = new FormData()
    uploadFormData.append('file', selectedFile.value)
    uploadFormData.append('importMode', importMode.value)

    const result = await ProRouteApi.importRouteProcessTemplate(uploadFormData)
    showImportResult({
      title: '工序模板导入完成',
      summary: result.importMode === 'UPGRADE'
        ? `已生成或更新路线版本草稿，共处理 ${result.routeProcessCount} 道工序。当前生效版本未变更。`
        : `已重建路线工序，共处理 ${result.routeProcessCount} 道工序。`,
      stats: [{ label: '工序数量', value: result.routeProcessCount }],
      sections: [
        { title: '版本信息', columns: ['版本'], rows: result.routeVersionNo ? [[String(result.routeVersionNo)]] : [] },
        { title: '工序明细', columns: ['工序名称'], rows: (result.processNames ?? []).map((name: string) => [name]) }
      ]
    })
    dialogVisible.value = false
    emits('success')
  } catch (error) {
    message.error(getErrorMessage(error, '工序模板导入失败，请根据错误提示检查并修改 Excel 文件'))
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
  downloadLoading.value = false
  importMode.value = 'UPGRADE'
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
