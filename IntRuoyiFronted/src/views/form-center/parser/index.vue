<template>
  <ContentWrap class="form-parser-page">
    <div class="form-parser-page__head">
      <h2>表单解析</h2>
      <div class="form-parser-page__actions">
        <el-upload
          ref="uploadRef"
          v-model:file-list="fileList"
          :auto-upload="false"
          :limit="1"
          :on-change="handleProductionFileChange"
          :on-exceed="handleExceed"
          :show-file-list="false"
          accept=".doc,.docx"
        >
          <template #trigger>
            <el-button
              v-hasPermi="['form:parser:production-batch-record']"
              class="scheme-d-btn scheme-d-btn--primary"
              :loading="productionLoading"
              type="primary"
              @click="handleProductionBatchRecord"
            >
              <Icon icon="ep:upload-filled" />
              生产批记录
            </el-button>
          </template>
        </el-upload>
        <el-button
          class="scheme-d-btn scheme-d-btn--neutral"
          :disabled="productionLoading"
          @click="handleUnsupportedParseType('QA检验规程')"
        >
          <Icon icon="ep:document" />
          QA检验规程
        </el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--neutral"
          :disabled="productionLoading"
          @click="handleUnsupportedParseType('过程检验记录')"
        >
          <Icon icon="ep:document-checked" />
          过程检验记录
        </el-button>
      </div>
    </div>

    <el-empty v-if="!lastResult" description="请选择生产批记录 Word 文件" />

    <div v-else class="form-parser-result">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="解析类型">{{ lastResult.parseTypeName }}</el-descriptions-item>
        <el-descriptions-item label="源文件">{{ lastResult.sourceFileName }}</el-descriptions-item>
        <el-descriptions-item label="识别字段数">
          {{ lastResult.recognizedFields?.length || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="下载文件">{{ lastDownloadName }}</el-descriptions-item>
      </el-descriptions>
      <el-table
        class="form-parser-result__table"
        :data="lastResult.recognizedFields || []"
        border
        stripe
      >
        <el-table-column label="字段编码" min-width="180" prop="fieldCode" />
        <el-table-column label="字段名称" min-width="180" prop="label" />
        <el-table-column label="字段类型" min-width="140" prop="fieldType" />
        <el-table-column label="必填" width="90">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'">
              {{ row.required ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { UploadFile, UploadFiles, UploadInstance, UploadUserFile } from 'element-plus'
import download from '@/utils/download'
import * as TemplateApi from '@/api/form-center/template'
import type { FormTemplateParseJsonRespVO } from '@/api/form-center/template'

defineOptions({ name: 'FormCenterParser' })

const message = useMessage()
const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const productionLoading = ref(false)
const lastResult = ref<FormTemplateParseJsonRespVO>()
const lastDownloadName = ref('')
const JSON_EXTENSION = '.json'

const handleProductionBatchRecord = () => {
  if (productionLoading.value) {
    return
  }
}

const handleUnsupportedParseType = (label: string) => {
  message.warning(`${label}解析暂未开放`)
}

const handleExceed = (_files: File[], uploadFiles: UploadUserFile[]) => {
  uploadFiles.splice(0, uploadFiles.length)
  message.error('最多只能上传一个 doc/docx 文件')
}

const handleProductionFileChange = async (uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  fileList.value = uploadFiles.slice(-1)
  const rawFile = uploadFile.raw
  if (!rawFile) {
    message.error('文件读取失败')
    return
  }
  if (!isWordFile(rawFile.name)) {
    uploadRef.value?.clearFiles()
    fileList.value = []
    message.error('请上传 doc/docx 文件')
    return
  }
  await parseAndDownloadProductionBatchRecord(rawFile)
}

const parseAndDownloadProductionBatchRecord = async (file: File) => {
  const payload = new FormData()
  payload.append('file', file)
  productionLoading.value = true
  try {
    const result = await TemplateApi.parseProductionBatchRecordJson(payload)
    const downloadName = buildJsonDownloadName(result.sourceFileName)
    const jsonBlob = new Blob([JSON.stringify(result, null, 2)], {
      type: 'application/json;charset=utf-8'
    })
    download.json(jsonBlob, downloadName)
    lastResult.value = result
    lastDownloadName.value = downloadName
    message.success('解析完成，已下载 JSON 文件')
  } catch (error) {
    message.error(resolveParseErrorMessage(error, '解析失败，请检查 Word 文件内容'))
    throw error
  } finally {
    productionLoading.value = false
    uploadRef.value?.clearFiles()
    fileList.value = []
  }
}

const isWordFile = (filename: string) => /\.(doc|docx)$/i.test(filename)

const buildJsonDownloadName = (sourceFileName?: string) => {
  const baseName = (sourceFileName || '生产批记录解析')
    .replace(/\.(doc|docx)$/i, '')
    .replace(/[\\/:*?"<>|]/g, '_')
    .trim()
  return `${baseName || '生产批记录解析'}${JSON_EXTENSION}`
}

const resolveParseErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as { response?: { data?: { msg?: string } } })?.response?.data?.msg
  const directMessage = (error as { message?: string })?.message
  return responseMessage || directMessage || fallback
}
</script>

<style lang="scss" scoped>
.form-parser-page {
  min-height: calc(100vh - 160px);

  :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }
}

.form-parser-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 0;
    color: #1f2937;
    font-size: 20px;
    font-weight: 700;
    line-height: 28px;
  }
}

.form-parser-page__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;

  :deep(.el-upload) {
    display: inline-flex;
  }

  :deep(.el-button) {
    min-width: 136px;
  }

  :deep(.iconify) {
    margin-right: 6px;
  }
}

.form-parser-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-parser-result__table {
  width: 100%;
}

@media (max-width: 720px) {
  .form-parser-page__head {
    align-items: stretch;
    flex-direction: column;
  }

  .form-parser-page__actions {
    justify-content: flex-start;

    :deep(.el-button) {
      width: 100%;
      min-width: 0;
    }

    :deep(.el-upload) {
      width: 100%;
    }
  }
}
</style>
