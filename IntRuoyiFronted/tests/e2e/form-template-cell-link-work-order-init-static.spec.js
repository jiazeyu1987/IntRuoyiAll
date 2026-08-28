const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const pageFile = resolve(process.cwd(), 'src/views/mes/pro/batchrecordcelllink/index.vue')
const page = readFileSync(pageFile, 'utf-8').replace(/\r\n/g, '\n')

const loadWorkbenchContextBody =
  page.match(/async function loadWorkbenchContext\(\) \{[\s\S]*?\n}\n\nconst handleSourceSelectionChange/)?.[0] || ''

assert.ok(loadWorkbenchContextBody, '单元格链接工作台必须保留 loadWorkbenchContext 初始化入口')

assert.match(
  loadWorkbenchContextBody,
  /const\s+defaultSourceReportId\s*=\s*data\.defaultSourceReportId\s*\|\|\s*forms\.value\[0\]\?\.reportId\s*\|\|\s*''/,
  '工作台初始化必须先归一化默认来源，避免状态字段互相错位'
)

assert.match(
  loadWorkbenchContextBody,
  /sourceReportId\.value\s*=\s*defaultSourceReportId/,
  '工作台初始化必须把默认来源写入 sourceReportId'
)

const sourceTypeAssignment =
  loadWorkbenchContextBody.match(
    /sourceType\.value\s*=\s*defaultSourceReportId[\s\S]*?:\s*SOURCE_TYPE_BATCH_RECORD_CELL/
  )?.[0] || ''

assert.ok(sourceTypeAssignment, '默认来源必须同步成正式来源类型')
assert.match(
  sourceTypeAssignment,
  /PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID[\s\S]*SOURCE_TYPE_PRODUCTION_WORK_ORDER/,
  '当表单模板入口默认来源是生产工单时，初始化必须同步为生产工单来源类型'
)
assert.match(
  sourceTypeAssignment,
  /PROCESS_POOL_REPORT_SOURCE_REPORT_ID[\s\S]*SOURCE_TYPE_PROCESS_POOL_REPORT/,
  '当默认来源是报工数据时，初始化必须同步为报工来源类型'
)
assert.match(
  sourceTypeAssignment,
  /PRODUCTION_PICK_LIST_SOURCE_REPORT_ID[\s\S]*SOURCE_TYPE_PRODUCTION_PICK_LIST/,
  '当默认来源是领料单数据时，初始化必须同步为领料单来源类型'
)
assert.match(
  sourceTypeAssignment,
  /PQC_AGGREGATE_DETAIL_SOURCE_REPORT_ID[\s\S]*SOURCE_TYPE_PQC_AGGREGATE_DETAIL/,
  '当默认来源是一线PQC数据时，初始化必须同步为PQC聚合来源类型'
)
assert.match(
  sourceTypeAssignment,
  /:\s*SOURCE_TYPE_BATCH_RECORD_CELL$/,
  '未知默认来源必须保持普通批记录单元格来源'
)

assert.ok(
  loadWorkbenchContextBody.indexOf('sourceType.value = defaultSourceReportId') <
    loadWorkbenchContextBody.indexOf('await Promise.all([loadSourceCells(), loadTargetCells()])'),
  'sourceType 必须在加载源单元格前完成同步，禁止请求 form-cells?reportId=PRODUCTION_WORK_ORDER'
)

assert.doesNotMatch(
  loadWorkbenchContextBody,
  /sourceType\.value\s*=\s*SOURCE_TYPE_BATCH_RECORD_CELL\s*\n\s*sourceReportId\.value\s*=\s*data\.defaultSourceReportId/,
  '不得先固定为普通批记录来源再把 sourceReportId 设置成 PRODUCTION_WORK_ORDER'
)

console.log('PASS form-template-cell-link-work-order-init-static')
