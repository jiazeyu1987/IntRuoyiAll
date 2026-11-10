<template>
  <div class="dcc-admin-page">
    <section class="dcc-admin-page__panel">
      <div class="dcc-admin-page__toolbar">
        <el-button
          plain
          :loading="exporting"
          @click="handleExportAdminConfigPackage"
        >
          <Icon icon="ep:download" class="mr-5px" />
          导出数据包
        </el-button>
        <el-button
          plain
          :loading="dmrSheetExporting"
          @click="handleExportDmrSheet"
        >
          <Icon icon="ep:document" class="mr-5px" />
          DMR-sheet
        </el-button>
        <el-button
          type="primary"
          plain
          :loading="importing"
          @click="openAdminConfigImport"
        >
          <Icon icon="ep:upload" class="mr-5px" />
          导入数据包
        </el-button>
        <el-tag class="dcc-admin-page__format" type="info" effect="plain">JSON 单文件</el-tag>
      </div>

      <div class="dcc-admin-page__content">
        <div class="dcc-admin-page__note">
          导入将按业务键覆盖目标租户配置；引用解析失败会阻止导入。
        </div>

        <div class="dcc-admin-page__summary">
          <div class="dcc-admin-page__summary-head">
            <h2>最近一次导入摘要</h2>
            <span>{{ summaryUpdatedAt || '尚未导入' }}</span>
          </div>

          <el-empty
            v-if="!importSummary"
            description="暂无导入结果"
            :image-size="48"
          />

          <div v-else class="dcc-admin-page__summary-grid">
            <article
              v-for="item in summaryItems"
              :key="item.label"
              class="dcc-admin-page__summary-card"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </div>
      </div>
    </section>

    <input
      ref="adminConfigInputRef"
      type="file"
      class="dcc-admin-page__hidden-input"
      accept=".json,application/json"
      @change="handleAdminConfigFileChange"
    />
  </div>
</template>

<script lang="ts" setup>
import dayjs from 'dayjs'
import download from '@/utils/download'
import {
  exportAdminConfigPackage,
  exportDmrSheetWorkbook,
  importAdminConfigPackage,
  type DccAdminFullConfigPackageImportRespVO
} from '@/api/dcc/controlledFile/workflow'

defineOptions({ name: 'DccControlledFileAdmin' })

type SummaryItem = {
  label: string
  value: number
}

const message = useMessage()
const exporting = ref(false)
const dmrSheetExporting = ref(false)
const importing = ref(false)
const adminConfigInputRef = ref<HTMLInputElement>()
const importSummary = ref<DccAdminFullConfigPackageImportRespVO>()
const summaryUpdatedAt = ref('')

const summaryItems = computed<SummaryItem[]>(() => {
  const summary = importSummary.value
  if (!summary) {
    return []
  }
  return [
    { label: '审批岗位', value: summary.approvalPositionCount },
    { label: '目录', value: summary.directoryCount },
    { label: '目录授权', value: summary.directoryAccessRuleCount },
    { label: '类别', value: summary.categoryCount },
    { label: '权限规则', value: summary.permissionRuleCount },
    { label: '审批矩阵', value: summary.approvalMatrixRuleCount },
    { label: '查看矩阵', value: summary.viewMatrixRuleCount },
    { label: '分发规则', value: summary.distributionRuleCount },
    { label: '培训规则', value: summary.trainingRuleCount },
    { label: '移除岗位', value: summary.removedApprovalPositionCount },
    { label: '移除目录', value: summary.removedDirectoryCount },
    { label: '移除类别', value: summary.removedCategoryCount }
  ]
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return fallback
}

const unwrapDownloadedBlob = (payload: unknown): Blob => {
  if (payload instanceof Blob) {
    return payload
  }
  if (
    payload &&
    typeof payload === 'object' &&
    'data' in payload &&
    (payload as { data?: unknown }).data instanceof Blob
  ) {
    return (payload as { data: Blob }).data
  }
  if (payload && typeof payload === 'object') {
    return new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  }
  throw new Error('导出文控管理员全量配置包返回的下载数据不是 Blob')
}

const unwrapExcelBlob = (payload: unknown): Blob => {
  if (payload instanceof Blob) {
    return payload
  }
  if (
    payload &&
    typeof payload === 'object' &&
    'data' in payload &&
    (payload as { data?: unknown }).data instanceof Blob
  ) {
    return (payload as { data: Blob }).data
  }
  throw new Error('导出 DMR-sheet 返回的下载数据不是 Blob')
}

const handleExportAdminConfigPackage = async () => {
  exporting.value = true
  try {
    const data = await exportAdminConfigPackage()
    const downloadBlob = unwrapDownloadedBlob(data)
    download.json(downloadBlob, '文控管理员全量配置包.json')
    message.success('文控管理员全量配置包已导出')
  } catch (error) {
    message.error(resolveErrorMessage(error, '导出文控管理员全量配置包失败'))
    throw error
  } finally {
    exporting.value = false
  }
}

const handleExportDmrSheet = async () => {
  dmrSheetExporting.value = true
  try {
    const data = await exportDmrSheetWorkbook()
    const downloadBlob = unwrapExcelBlob(data)
    download.excel(downloadBlob, 'DMR-sheet.xlsx')
    message.success('DMR-sheet 已导出')
  } catch (error) {
    message.error(resolveErrorMessage(error, '导出 DMR-sheet 失败'))
  } finally {
    dmrSheetExporting.value = false
  }
}

const openAdminConfigImport = () => {
  adminConfigInputRef.value?.click()
}

const handleAdminConfigFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  importing.value = true
  try {
    const result = await importAdminConfigPackage(formData)
    importSummary.value = result
    summaryUpdatedAt.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
    message.success(
      `导入完成；审批岗位 ${result.approvalPositionCount} 条；目录 ${result.directoryCount} 条；类别 ${result.categoryCount} 条；分发规则 ${result.distributionRuleCount} 条；培训规则 ${result.trainingRuleCount} 条`
    )
  } catch (error) {
    message.error(resolveErrorMessage(error, '导入文控管理员全量配置包失败'))
    throw error
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.dcc-admin-page {
  min-width: 0;
  --el-color-primary: #008f86;
  --el-color-primary-light-3: #4db1aa;
  --el-color-primary-light-5: #80c7c2;
  --el-color-primary-light-7: #b3ddd9;
  --el-color-primary-light-8: #cce9e7;
  --el-color-primary-light-9: #e6f5f2;
  --el-color-primary-dark-2: #00726b;
}

.dcc-admin-page__panel {
  border: 1px solid #dbe3e7;
  border-radius: 6px;
  background: #fff;
  padding: 12px;
}

.dcc-admin-page__toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid #dbe3e7;
}

.dcc-admin-page__toolbar :deep(.el-button) {
  height: 32px;
  margin-left: 0;
  padding: 0 12px;
  border-radius: 5px;
  font-size: 13px;
}

.dcc-admin-page__format {
  margin-left: auto;
}

.dcc-admin-page__content {
  display: grid;
  gap: 8px;
  padding-top: 8px;
}

.dcc-admin-page__note {
  color: #b77913;
  font-size: 12px;
  line-height: 20px;
}

.dcc-admin-page__summary h2 {
  margin: 0;
  color: #24343d;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.dcc-admin-page__summary-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.dcc-admin-page__summary-head span {
  color: #4b5563;
  font-size: 12px;
}

.dcc-admin-page__summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.dcc-admin-page__summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  border: 1px solid #dbe3e7;
  border-radius: 5px;
  background: #f5f8f9;
  padding: 14px 12px;
}

.dcc-admin-page__summary-card span {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.dcc-admin-page__summary-card strong {
  color: #008f86;
  font-size: 22px;
  overflow-wrap: anywhere;
  font-variant-numeric: tabular-nums;
  line-height: 26px;
}

.dcc-admin-page__hidden-input {
  display: none;
}

@media (max-width: 768px) {
  .dcc-admin-page__summary-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .dcc-admin-page__summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dcc-admin-page__format {
    margin-left: 0;
  }
}
</style>
