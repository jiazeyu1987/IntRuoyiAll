<template>
  <ContentWrap>
    <div class="dcc-print-template-page">
      <div class="dcc-print-template-page__header">
        <div>
          <div class="text-20px font-700">模板配置</div>
          <div class="mt-6px text-13px text-[var(--el-text-color-secondary)]">
            配置 DCC 流程打印与 Word 导出的 .docx 模板
          </div>
          <div
            class="dcc-print-template-page__required-context"
            data-testid="dcc-print-template-required-context"
          >
            {{ requiredPlaceholderContextText }}
          </div>
        </div>
        <el-button :loading="loading" @click="loadActiveTemplate">
          <Icon icon="ep:refresh" class="mr-5px" />
          刷新
        </el-button>
      </div>

      <el-descriptions
        v-if="activeTemplate"
        class="mt-18px"
        :column="2"
        border
      >
        <el-descriptions-item label="当前模板">
          {{ activeTemplate.templateFileName }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag type="success">已启用</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模板文件ID">
          {{ activeTemplate.templateFileId }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTimeValue(activeTemplate.updateTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ activeTemplate.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="dcc-print-template-page__body">
        <div class="dcc-print-template-page__panel">
          <div class="dcc-print-template-page__panel-title">模板文件</div>
          <el-upload
            ref="uploadRef"
            v-model:file-list="fileList"
            drag
            accept=".docx"
            :limit="1"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <Icon icon="ep:upload-filled" class="dcc-print-template-page__upload-icon" />
            <div class="el-upload__text">拖入 .docx 文件，或点击选择</div>
            <template #tip>
              <div class="el-upload__tip">
                后端会校验 Word 包格式与必填占位符，校验失败不会启用模板。
              </div>
            </template>
          </el-upload>
        </div>

        <div class="dcc-print-template-page__panel">
          <div class="dcc-print-template-page__panel-heading">
            <div>
              <div class="dcc-print-template-page__panel-title">占位符</div>
              <div
                class="dcc-print-template-page__placeholder-summary"
                data-testid="dcc-print-template-required-summary"
              >
                必填 {{ requiredPlaceholderRows.length }} 个 / 全部 {{ placeholderRows.length }} 个
              </div>
            </div>
            <el-radio-group
              v-model="placeholderViewMode"
              class="dcc-print-template-page__view-mode"
              data-testid="dcc-print-template-placeholder-view-mode"
            >
              <el-radio-button label="required">必填占位符</el-radio-button>
              <el-radio-button label="all">全部占位符</el-radio-button>
            </el-radio-group>
          </div>
          <el-table :data="displayedPlaceholderRows" border data-testid="dcc-print-template-placeholder-table">
            <el-table-column prop="name" label="占位符" min-width="180" />
            <el-table-column prop="required" label="必填" width="90">
              <template #default="{ row }">
                <el-tag :type="row.required ? 'danger' : 'info'">
                  {{ row.required ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="label" label="说明" min-width="180" />
          </el-table>
        </div>
      </div>

      <el-form class="mt-18px" label-width="72px">
        <el-form-item label="备注">
          <el-input
            v-model="remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="记录模板适用范围或版本说明"
          />
        </el-form-item>
      </el-form>

      <el-alert
        v-if="inlineError"
        class="mt-12px"
        type="error"
        :closable="false"
        show-icon
        :title="inlineError"
      />

      <div class="dcc-print-template-page__actions">
        <el-button type="primary" :loading="saving" @click="saveTemplate">
          <Icon icon="ep:check" class="mr-5px" />
          保存并启用
        </el-button>
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { UploadFile, UploadUserFile } from 'element-plus'
import { updateFile } from '@/api/infra/file'
import { formatDateTimeValue } from '@/utils/formatTime'
import {
  getActiveApprovalPrintTemplate,
  saveActiveApprovalPrintTemplate,
  type ApprovalPrintTemplateVO
} from '@/api/dcc/controlledFile/approvalPrintTemplate'

defineOptions({ name: 'DccApprovalPrintTemplate' })

const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const activeTemplate = ref<ApprovalPrintTemplateVO | null>(null)
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File>()
const remark = ref('')
const inlineError = ref('')

type PlaceholderViewMode = 'required' | 'all'

interface PlaceholderRow {
  name: string
  required: boolean
  label: string
}

const placeholderViewMode = ref<PlaceholderViewMode>('required')

const placeholderRows: PlaceholderRow[] = [
  { name: '{{fileNumber}}', required: true, label: '受控文件编号' },
  { name: '{{fileName}}', required: true, label: '文件名称' },
  { name: '{{versionNo}}', required: true, label: '版本号' },
  { name: '{{approvalRecords}}', required: true, label: '审批路线与签核记录' },
  { name: '{{title}}', required: false, label: '标题' },
  { name: '{{productCode}}', required: false, label: '产品编号' },
  { name: '{{effectiveDate}}', required: false, label: '生效日期' },
  { name: '{{processInstanceId}}', required: false, label: '流程实例编号' },
  { name: '{{processContent}}', required: false, label: '审批内容明细' }
]

const requiredPlaceholderRows = computed(() => placeholderRows.filter((item) => item.required))
const requiredPlaceholderContextText = computed(() => {
  const requiredNames = requiredPlaceholderRows.value.map((item) => item.name).join('、')
  return `必填 ${requiredPlaceholderRows.value.length} 个：${requiredNames} · 后端保存时校验 Word 包和必填占位符`
})
const isAllPlaceholderView = computed(() => placeholderViewMode.value === 'all')
const displayedPlaceholderRows = computed(() =>
  isAllPlaceholderView.value ? placeholderRows : requiredPlaceholderRows.value
)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return fallback
}

const resolveUploadedTemplateFile = (result: unknown) => {
  const data = (result as { data?: unknown })?.data ?? result
  const url = typeof data === 'string' ? data : (data as { url?: string })?.url
  const match = url ? String(url).match(/[?&]id=(\d+)/) : null
  return {
    templateFileId: match ? Number(match[1]) : undefined,
    templateFileUrl: url ? String(url) : undefined
  }
}

const loadActiveTemplate = async () => {
  loading.value = true
  inlineError.value = ''
  try {
    activeTemplate.value = await getActiveApprovalPrintTemplate()
    remark.value = activeTemplate.value?.remark || ''
  } catch (error) {
    inlineError.value = resolveErrorMessage(error, '读取审批打印模板失败。')
    message.error(inlineError.value)
  } finally {
    loading.value = false
  }
}

const handleFileChange = (uploadFile: UploadFile) => {
  inlineError.value = ''
  selectedFile.value = uploadFile.raw
}

const handleFileRemove = () => {
  selectedFile.value = undefined
}

const resolveSelectedTemplateFile = () => {
  return selectedFile.value || (fileList.value[0] as UploadUserFile & { raw?: File })?.raw
}

const saveTemplate = async () => {
  const templateFile = resolveSelectedTemplateFile()
  if (!templateFile) {
    inlineError.value = '请先选择 .docx 模板文件。'
    message.error(inlineError.value)
    return
  }
  if (!templateFile.name.toLowerCase().endsWith('.docx')) {
    inlineError.value = '审批打印模板必须是 .docx 文件。'
    message.error(inlineError.value)
    return
  }

  saving.value = true
  inlineError.value = ''
  try {
    const formData = new FormData()
    formData.append('file', templateFile)
    const uploadResult = await updateFile(formData)
    const uploadedTemplateFile = resolveUploadedTemplateFile(uploadResult)
    if (!uploadedTemplateFile.templateFileId && !uploadedTemplateFile.templateFileUrl) {
      throw new Error('文件上传成功但未返回模板文件地址。')
    }
    activeTemplate.value = await saveActiveApprovalPrintTemplate({
      ...uploadedTemplateFile,
      remark: remark.value
    })
    fileList.value = []
    selectedFile.value = undefined
    message.success('审批打印模板已启用')
  } catch (error) {
    inlineError.value = resolveErrorMessage(error, '保存审批打印模板失败。')
    message.error(inlineError.value)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadActiveTemplate()
})
</script>

<style scoped>
.dcc-print-template-page__header,
.dcc-print-template-page__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dcc-print-template-page__header {
  align-items: flex-start;
}

.dcc-print-template-page__required-context {
  margin-top: 8px;
  max-width: 780px;
  color: #4b5563;
  font-size: 13px;
  line-height: 20px;
}

.dcc-print-template-page__body {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(420px, 1fr);
  gap: 16px;
  margin-top: 18px;
}

.dcc-print-template-page__panel {
  min-width: 0;
}

.dcc-print-template-page__panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.dcc-print-template-page__panel-title {
  font-size: 14px;
  font-weight: 700;
}

.dcc-print-template-page__placeholder-summary {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.dcc-print-template-page__view-mode {
  flex-shrink: 0;
}

.dcc-print-template-page__upload-icon {
  display: block;
  margin: 0 auto 8px;
  font-size: 34px;
  color: var(--el-color-primary);
}

.dcc-print-template-page__actions {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 960px) {
  .dcc-print-template-page__body {
    grid-template-columns: 1fr;
  }

  .dcc-print-template-page__panel-heading {
    flex-direction: column;
  }
}
</style>
