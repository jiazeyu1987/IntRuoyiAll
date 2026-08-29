const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const pageFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue')
const apiFile = resolve(process.cwd(), 'src/api/mes/pro/batchrecordcelllink/index.ts')

const page = readFileSync(pageFile, 'utf-8')
const api = readFileSync(apiFile, 'utf-8')

const processPoolOptionStart = page.indexOf('label="报工数据"')
assert.notEqual(processPoolOptionStart, -1, 'source dropdown must keep 报工数据 option')
const processPoolOptionEnd = page.indexOf('/>', processPoolOptionStart)
assert.notEqual(processPoolOptionEnd, -1, '报工数据 option must be a complete option node')
const processPoolOption = page.slice(processPoolOptionStart, processPoolOptionEnd)
assert.ok(!processPoolOption.includes(':disabled='), '报工数据 must be selectable before route process context exists')

for (const token of [
  'DCC_PROJECT_CODE_STATUS_ENABLE',
  'getProjectCodePage',
  'type DccProjectCodeRespVO',
  'const processPoolDccProjectCodeOptions = ref<DccProjectCodeRespVO[]>([])',
  'const selectedProcessPoolDccProjectCodeId = ref<number>()',
  'const processPoolRouteProcesses = ref<BatchRecordCellLinkRouteProcessVO[]>([])',
  'const selectedProcessPoolRouteProcessId = ref<number>()',
  'data-process-pool-context-selector',
  'data-process-pool-dcc-project-select',
  'data-process-pool-route-process-select',
  'loadProcessPoolDccProjectCodeOptions',
  'handleProcessPoolDccProjectCodeChange',
  'handleProcessPoolRouteProcessChange',
  'routeConfigured: true',
  'mainBatchRecordConfigured: true',
  'dccProjectCodeId: isProcessPoolReportSelected.value ? selectedProcessPoolDccProjectCodeId.value : undefined',
  'routeProcessId: routeProcessIdForContext',
  '请选择DCC项目代码',
  '请选择工序',
  '当前工序暂无正式一线生产字段'
]) {
  assert.ok(page.includes(token), `cell-link page misses DCC project/process contract token: ${token}`)
}

for (const token of [
  'export interface BatchRecordCellLinkRouteProcessVO',
  'processId?: number',
  'processCode?: string',
  'processName?: string',
  'batchRecordReportId?: string',
  'dccProjectCodeId?: number',
  'routeProcesses?: BatchRecordCellLinkRouteProcessVO[]'
]) {
  assert.ok(api.includes(token), `cell-link API misses DCC project/process contract token: ${token}`)
}

console.log('batch-record-cell-link-dcc-project-process-static: PASS')
