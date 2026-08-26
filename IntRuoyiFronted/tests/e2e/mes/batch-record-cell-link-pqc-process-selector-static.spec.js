const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const page = readFileSync(resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue'), 'utf-8')

for (const token of [
  'type BatchRecordCellLinkPqcProcessVO',
  'const pqcProcesses = ref<BatchRecordCellLinkPqcProcessVO[]>([])',
  'const selectedPqcQaProcessId = ref<number>()',
  'const selectedPqcQaProcess = computed(() =>',
  'data-pqc-process-selector',
  'class="batch-record-cell-link__pqc-process-select"',
  'placeholder="选择工序"',
  'v-model="selectedPqcQaProcessId"',
  'selectedPqcQaProcess?.sort',
  'selectedPqcQaProcess?.processName',
  'async function loadWorkbenchContext()',
  'const handlePqcProcessChange = async',
  'qaProcessId: selectedPqcQaProcessId.value',
  'const targetQaProcessId = selectedPqcQaProcessId.value',
  'Number(field.qaProcessId) === Number(targetQaProcessId)',
  "sourceReportId: sourceReportId.value || String(route.query.sourceReportId || '')",
  '请选择工序后查看一线PQC字段',
  'sourceSheetEmptyText',
  '正在加载当前工序的一线PQC字段',
  '当前工序暂无正式一线PQC字段',
  '填写时间',
  '复核时间',
  '填写人签名',
  '复核人签名',
  'function resolveSourceFieldDisplayName'
]) {
  assert.ok(page.includes(token), `PQC process selector page misses: ${token}`)
}

assert.ok(page.includes('pqcProcesses.value = (data.pqcProcesses || [])'), 'PQC selector must use QA process catalog')
assert.ok(page.includes('cellKey: field.sourceCellKey || field.fieldCode'), 'PQC source identity must retain qaProcessId source key')

console.log('batch-record-cell-link-pqc-process-selector-static: PASS')
