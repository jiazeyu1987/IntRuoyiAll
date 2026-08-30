const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const formListFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordformlist/index.vue')
const cellLinkFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue')
const reportApiFile = resolve(process.cwd(), 'src/api/mes/pro/batchrecordreport/index.ts')
const respVoFile = resolve(
  process.cwd(),
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecordreport/vo/BatchRecordReportRespVO.java'
)
const viewFile = resolve(
  process.cwd(),
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportView.java'
)
const serviceFile = resolve(
  process.cwd(),
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java'
)

const formList = readFileSync(formListFile, 'utf-8')
const cellLink = readFileSync(cellLinkFile, 'utf-8')
const reportApi = readFileSync(reportApiFile, 'utf-8')
const respVo = readFileSync(respVoFile, 'utf-8')
const view = readFileSync(viewFile, 'utf-8')
const service = readFileSync(serviceFile, 'utf-8')

for (const token of [
  'dccProjectCodeId?: number',
  'projectCode?: string'
]) {
  assert.ok(reportApi.includes(token), `批记录表单列表接口类型必须包含 ${token}`)
}

for (const token of [
  '@Schema(description = "DCC 项目代码 ID")',
  'private Long dccProjectCodeId;'
]) {
  assert.ok(respVo.includes(token), `批记录表单列表返回 VO 必须包含 ${token}`)
}

assert.match(
  view,
  /String\s+projectCode,\s*\r?\n\s*Long\s+dccProjectCodeId,/,
  '批记录列表视图必须在 projectCode 旁携带正式 DCC 项目代码 ID。'
)
assert.match(
  service,
  /selectCurrentByRouteId\([\s\S]{0,500}getDccProjectCodeId\(/,
  '分页列表必须从工艺路线当前 DCC 绑定取正式项目代码 ID，不能由前端或名称猜测。'
)
assert.match(
  service,
  /copyReportWithVersionProduct[\s\S]{0,900}\.dccProjectCodeId\(/,
  '按产品展开批记录列表时必须保留对应 DCC 项目代码 ID。'
)

for (const token of [
  "const PROCESS_POOL_REPORT_SOURCE_REPORT_ID = 'PROCESS_POOL_REPORT'",
  "const isMainBatchRecordReport = (row: BatchRecordReportVO) => row.formSlotType === 'MAIN'",
  'sourceReportId: isMainBatchRecordReport(row) ? PROCESS_POOL_REPORT_SOURCE_REPORT_ID : undefined',
  'dccProjectCodeId: isMainBatchRecordReport(row) && row.dccProjectCodeId ? String(row.dccProjectCodeId) : undefined',
  'dccProjectCode: isMainBatchRecordReport(row) && row.dccProjectCodeId ? row.projectCode : undefined',
  'targetReportId: row.reportId'
]) {
  assert.ok(formList.includes(token), `表单中心链接入口必须带出自动初始化参数: ${token}`)
}
assert.ok(
  !formList.includes('sourceReportId: row.reportId'),
  '点击当前批记录表单时，该表单只能作为目标表单，不能被错误设为源表单。'
)

for (const token of [
  'const requestedSourceReportId = String(route.query.sourceReportId || \'\')',
  'const requestedProcessPoolDccProjectCodeId = parseNumber(route.query.dccProjectCodeId)',
  'const requestedProcessPoolDccProjectCodeKeyword = String(route.query.dccProjectCode || \'\')',
  'const processPoolDccProjectCodeInitialOptionsLoaded = ref(false)',
  'const sourceType = ref(resolveSourceTypeByReportId(requestedSourceReportId))',
  'const sourceReportId = ref(requestedSourceReportId)',
  'const selectedProcessPoolDccProjectCodeId = ref<number | undefined>(requestedProcessPoolDccProjectCodeId)',
  '!processPoolDccProjectCodeInitialOptionsLoaded.value',
  'loadProcessPoolDccProjectCodeOptions(requestedProcessPoolDccProjectCodeKeyword)',
  'processPoolDccProjectCodeInitialOptionsLoaded.value = true',
  'dccProjectCodeId: isProcessPoolReportSelected.value ? selectedProcessPoolDccProjectCodeId.value : undefined'
]) {
  assert.ok(cellLink.includes(token), `单元格链接页必须用路由参数初始化报工数据/DCC 项目: ${token}`)
}

assert.match(
  cellLink,
  /function\s+resolveSourceTypeByReportId[\s\S]*PROCESS_POOL_REPORT_SOURCE_REPORT_ID[\s\S]*SOURCE_TYPE_PROCESS_POOL_REPORT/,
  '单元格链接页必须先根据 sourceReportId 识别报工数据来源，再请求工作台上下文。'
)

console.log('PASS: batch-record form list link auto DCC static contract')
