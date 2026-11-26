<template>
  <el-tooltip :content="disabledReason || '按列表当前生效的筛选和排序导出'">
    <span>
      <el-dropdown :disabled="!!disabledReason" @command="open">
        <el-button :disabled="!!disabledReason">批量导出 Excel<Icon icon="ep:arrow-down" class="ml-5px" /></el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="SELECTED" :disabled="recordIds.length === 0">导出已勾选（{{ recordIds.length }} 条）</el-dropdown-item>
            <el-dropdown-item command="FILTERED">{{ hasFilters ? '导出全部筛选结果' : '导出当前列表全部结果' }}（{{ total }} 条，含其他页）</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </span>
  </el-tooltip>
  <el-dialog v-model="visible" title="批量导出 Excel" width="680px" :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy" :before-close="close">
    <div v-loading="previewing">
      <p>导出模式：{{ preview?.mode === 'SELECTED' ? '已勾选记录' : '全部筛选结果（含其他页）' }}；格式：Excel（.xlsx）</p>
      <p v-if="preview">命中 {{ preview.count }} 条源记录；{{ typeSummary }}。同步上限 {{ preview.maxRecords }} 条。</p>
      <template v-if="preview?.mode === 'FILTERED'">
        <p>已生效筛选：{{ filterSummary }}</p>
        <el-alert v-if="hasDraft" title="筛选面板有未提交的修改，本次按已生效条件导出" type="warning" :closable="false" />
      </template>
      <p>工作簿固定保留国内、CE、FDA三张表。有明确来源的字段按系统数据填写，缺失字段留空，不因单列为空跳过记录。</p>
      <el-alert v-for="notice in (status?.structureNotices || preview?.structureNotices || [])" :key="notice" :title="notice" type="warning" :closable="false" class="mb-8px" />
      <p>生成后统一保留5分钟，重试不会延长窗口；重取会再次核验权限。</p>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <p v-if="status?.expiresAt">文件取件截止：{{ new Date(status.expiresAt).toLocaleString() }}</p>
    </div>
    <template #footer>
      <el-button :disabled="busy" @click="visible = false">关闭</el-button>
      <el-button v-if="!submitted" type="primary" :disabled="!preview?.allowed || previewing" :loading="busy" @click="generate">确认导出</el-button>
      <el-button v-else :disabled="busy || terminal" :loading="busy" type="primary" @click="retry">重试获取</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import download from '@/utils/download'
import { previewExcelExport, fetchExcelExport, getExcelExportStatus, type ExportPreview, type ExportStatus } from '@/api/dcc/registrationCertificate/excelExport'
import type { DccRegistrationCertificatePageReqVO } from '@/api/dcc/registrationCertificate'
import { cloneExportQuery, exportQueryKey } from './exportScope'
const props = defineProps<{ query: DccRegistrationCertificatePageReqVO; draft: DccRegistrationCertificatePageReqVO; recordIds: (number | string)[]; total: number; disabledReason: string }>()
const visible = ref(false), busy = ref(false), previewing = ref(false), submitted = ref(false)
const preview = ref<ExportPreview>(), status = ref<ExportStatus>(), error = ref('')
const confirmedQuery = ref<DccRegistrationCertificatePageReqVO>({})
const hasFilters = computed(() => exportQueryKey(props.query) !== '{}')
const hasDraft = computed(() => exportQueryKey(props.draft, true) !== exportQueryKey(confirmedQuery.value, true))
const typeNames: Record<string,string> = { DOMESTIC: '国内注册证', CE: 'CE证书', FDA: 'FDA证书' }
const typeSummary = computed(() => Object.entries(preview.value?.types || {}).map(([key, count]) => `${typeNames[key] || key} ${count}条`).join('，'))
const filterNames: Record<string,string> = { certificateType: '注册证类型', productName: '产品', classification: '分类', firstObtainedStart: '首次获证起', firstObtainedEnd: '首次获证止', approvalStart: '批准日期起', approvalEnd: '批准日期止', effectiveStart: '生效日期起', effectiveEnd: '生效日期止', expiryStart: '有效期起', expiryEnd: '有效期止', ownerCompanyName: '所属公司', listFilters: '快捷筛选', fieldFilters: '分类组合条件' }
const filterSummary = computed(() => Object.entries(JSON.parse(exportQueryKey(confirmedQuery.value))).map(([key,value]) => `${filterNames[key] || key}：${typeof value === 'object' ? JSON.stringify(value) : typeNames[String(value)] || value}`).join('；') || '未设置筛选，导出当前列表全部结果')
const terminal = computed(() => status.value && ['FAILED', 'EXPIRED', 'INVALID'].includes(status.value.state))
const close = (done: () => void) => { if (!busy.value) done() }
const reason = (e: unknown) => e instanceof Error ? e.message : String(e)
const open = async (mode: ExportPreview['mode']) => {
  // An unknown or running request keeps its identity; a completed export permits a new explicit scope.
  if (submitted.value && (!status.value || status.value.state === 'GENERATING')) { visible.value = true; return }
  confirmedQuery.value = cloneExportQuery(props.query)
  visible.value = true; error.value = ''; status.value = undefined; preview.value = undefined; submitted.value = false; previewing.value = true
  try {
    preview.value = await previewExcelExport(mode, confirmedQuery.value, mode === 'SELECTED' ? props.recordIds : [])
    if (!preview.value.allowed) error.value = preview.value.reason
  } catch (e) { error.value = reason(e) }
  finally { previewing.value = false }
}
const receive = async (retrieval: boolean) => {
  if (!preview.value || busy.value) return
  busy.value = true; error.value = ''
  try {
    const result = await fetchExcelExport(preview.value, retrieval)
    download.excel(result.blob, result.fileName)
    ElMessage.success(`本次导出 ${result.exported} 条，跳过 ${result.skipped} 条`)
    status.value = await getExcelExportStatus(preview.value.requestId)
  } catch (e) { error.value = `${reason(e)}。可沿用原请求重试获取，或关闭后稍后重取。` }
  finally { busy.value = false }
}
const generate = async () => { submitted.value = true; await receive(false) }
const retry = async () => {
  if (!preview.value || busy.value) return
  busy.value = true; error.value = ''
  try {
    status.value = await getExcelExportStatus(preview.value.requestId)
    if (status.value.state === 'GENERATING') { error.value = 'Excel正在生成，请稍后重试获取'; return }
    if (status.value.state === 'CONFIRMED') { submitted.value = false; error.value = '请求尚未受理，请重新确认导出'; return }
    if (terminal.value) { error.value = status.value.reason; return }
  } catch (e) { error.value = reason(e); return }
  finally { busy.value = false }
  await receive(true)
}
</script>
