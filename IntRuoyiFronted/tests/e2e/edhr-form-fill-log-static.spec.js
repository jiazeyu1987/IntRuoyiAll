const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const page = read('src/views/mes/pro/edhr/FormFillLogPage.vue')
const api = read('src/api/mes/pro/edhr/formFillLog.ts')
const route = read('src/router/modules/remaining.ts')
const quickFilterHook = read('src/hooks/web/useTableQuickFilter.ts')
const pageWithoutVueComments = page.replace(/<!--[\s\S]*?-->/g, '')

const extractFunctionBody = (name) => {
  const match = pageWithoutVueComments.match(new RegExp(`const\\s+${name}\\s*=\\s*\\(row:\\s*FormFillLogPageRespVO\\)\\s*=>\\s*\\{([\\s\\S]*?)\\n\\}`))
  assert.ok(match, `必须存在真实函数 ${name}`)
  return match[1]
}

assert.doesNotMatch(page, /<!--[\s\S]*focus[\s\S]*work-order[\s\S]*-->/, '关键路由断言不得靠 Vue 注释命中')

const templateMatch = pageWithoutVueComments.match(/<UnifiedListTemplate[\s\S]*?<\/UnifiedListTemplate>/)
assert.ok(templateMatch, '表单填写日志列表必须使用 UnifiedListTemplate')
assert.match(templateMatch[0], /table-key="mes\.pro\.edhr\.formFillLog\.main"/, '必须使用稳定 table-key')
assert.match(templateMatch[0], /@pagination="getList"/, '分页必须通过标准列表模板触发查询')
assert.match(route, /title:\s*'表单日志'/, '前端路由标题必须显示为表单日志')

const tableSlot = pageWithoutVueComments.match(/<template\s+#table(?:\s*=\s*"[^"]*")?\s*>[\s\S]*?<\/template>/)
assert.ok(tableSlot, '标准列表模板必须提供 table slot')
assert.match(tableSlot[0], /data-user-table-column-explicit/, '表格必须启用显式列配置')
assert.match(tableSlot[0], /data-user-table-key="mes\.pro\.edhr\.formFillLog\.main"/, '内部表格必须使用同一 user table key')
assert.match(tableSlot[0], /@header-dragend="handleHeaderDragend"/, '列宽拖拽必须持久化')

for (const label of ['表单名称', '批号', '生产工单号', '执行编号', '填写人', '填写时间', '填写单元数', '写入摘要', '证据状态']) {
  assert.match(pageWithoutVueComments, new RegExp(`label="${label}"`), `列表必须包含列：${label}`)
}

for (const filter of ['formKeyword', 'changedAtRange', 'actorName', 'batchCode', 'workOrderCode', 'executionCode']) {
  assert.match(pageWithoutVueComments, new RegExp(filter), `过滤项必须包含 ${filter}`)
}
assert.match(
  pageWithoutVueComments,
  /\{\s*key:\s*'formKeyword'[\s\S]*label:\s*'表单'/,
  '表单快速过滤必须使用 formKeyword 支持表单名称或报表 ID 关键词'
)
assert.match(
  pageWithoutVueComments,
  /\{\s*key:\s*'formKeyword'[\s\S]*queryParamKey:\s*'formKeyword'/,
  '表单快速过滤必须通过 queryParamKey 写入 formKeyword，而不是落入未使用的 quickFilter'
)
for (const filter of ['changedAtRange', 'actorName', 'batchCode', 'workOrderCode', 'executionCode']) {
  assert.match(
    pageWithoutVueComments,
    new RegExp(`\\{\\s*key:\\s*'${filter}'[\\s\\S]*queryParamKey:\\s*'${filter}'`),
    `${filter} 快速过滤必须映射到同名查询参数`
  )
}
assert.match(
  quickFilterHook,
  /definition\.type\s*===\s*'dateRange'[\s\S]*\[\s*quickFilter\.value\?\.value,\s*quickFilter\.value\?\.valueEnd\s*\]/,
  '快速过滤 hook 必须把 dateRange queryParamKey 写成起止数组'
)
assert.match(pageWithoutVueComments, /const\s+normalizeTextParam\s*=\s*\(value\?:\s*string\)/, '查询构造必须容忍快速过滤删除的非当前字段')
assert.match(pageWithoutVueComments, /Array\.isArray\(queryParams\.changedAtRange\)/, '时间范围必须容忍快速过滤删除 changedAtRange')
assert.doesNotMatch(
  pageWithoutVueComments,
  /queryParams\.(?:actorName|batchCode|workOrderCode|executionCode)\.trim\(\)/,
  '非当前快速过滤字段可能被 hook 删除，buildQuery 不得直接 trim'
)
assert.match(pageWithoutVueComments, /formKeyword:\s*normalizeTextParam\(queryParams\.formKeyword\)/, '查询参数必须提交 formKeyword')
assert.doesNotMatch(
  pageWithoutVueComments,
  /\{\s*key:\s*'batchRecordReportId'[\s\S]*label:\s*'表单'/,
  '表单快速过滤不得继续只绑定 batchRecordReportId'
)
assert.match(page, /import \{ formatEdhrDateTime \} from '@\/views\/mes\/pro\/edhr\/shared\/dateTime'/, '填写时间必须使用统一 eDHR 时间格式化工具')
assert.match(pageWithoutVueComments, /const\s+formatFormLogDateTime\s*=\s*\(value\?:\s*string\s*\|\s*number\s*\|\s*null\)/, '必须提供表单日志时间格式化函数')
assert.match(pageWithoutVueComments, /return\s+formatEdhrDateTime\(value\)/, '填写时间必须通过共享 eDHR 时间格式化函数显示')
assert.match(
  pageWithoutVueComments,
  /label="填写时间"[\s\S]*formatFormLogDateTime\(row\.changedAt\)/,
  '填写时间列必须调用年月日时分秒格式化函数'
)
assert.doesNotMatch(
  pageWithoutVueComments,
  /label="填写时间"[\s\S]{0,180}prop="changedAt"[\s\S]{0,80}\/>/,
  '填写时间列不得直接渲染后端原始 changedAt'
)

assert.match(pageWithoutVueComments, /openBatchExecutionDetail\(row\)/, '批号列必须调用独立批次详情跳转')
assert.match(pageWithoutVueComments, /openBatchExecutionWorkOrder\(row\)/, '生产工单号列必须调用工单聚焦跳转')
const batchDetailFunction = extractFunctionBody('openBatchExecutionDetail')
const workOrderFunction = extractFunctionBody('openBatchExecutionWorkOrder')
assert.match(batchDetailFunction, /path:\s*'\/mes\/pro\/feedback\/edhr-batch-execution\/detail'/, '批号跳转目标必须是 eDHR 批次执行详情')
assert.doesNotMatch(batchDetailFunction, /focus/, '批号跳转不得带工单 focus')
assert.match(workOrderFunction, /path:\s*'\/mes\/pro\/feedback\/edhr-batch-execution\/detail'/, '生产工单号跳转目标必须是 eDHR 批次执行详情')
assert.match(workOrderFunction, /(?:\['focus'\]|focus)\s*:\s*'work-order'/, '生产工单号跳转必须带 focus=work-order')
assert.match(pageWithoutVueComments, /批次上下文缺失/, '缺少 batchExecutionId 时必须显示上下文缺失')
assert.doesNotMatch(templateMatch[0], /label="修改原因"|reasonCategory|reasonText/, '表单填写日志主列表不得暴露修改原因列或过滤')
assert.match(pageWithoutVueComments, /const\s+formatCellLocation\s*=\s*\(row:\s*FormFillLogItemRespVO\)/, '明细单元格定位必须提供人可读格式化函数')
assert.match(pageWithoutVueComments, /const\s+formatCellLocationDetail\s*=\s*\(row:\s*FormFillLogItemRespVO\)/, '明细单元格定位必须提供行列说明函数')
assert.match(pageWithoutVueComments, /const\s+formatCellLocationTooltip\s*=\s*\(row:\s*FormFillLogItemRespVO\)/, '明细单元格定位必须将原始路径放入提示')
assert.match(pageWithoutVueComments, /const\s+columnIndexToLetters\s*=\s*\(columnIndex:\s*number\)/, '明细单元格定位必须支持 Excel 风格列字母')
assert.match(pageWithoutVueComments, /return\s+`\$\{columnIndexToLetters\(columnIndex\)\}\$\{rowIndex \+ 1\}`/, '明细单元格定位主显示必须是 Excel 风格坐标')
assert.match(pageWithoutVueComments, /第\$\{rowIndex \+ 1\}行，第\$\{columnIndex \+ 1\}列/, '明细单元格定位说明必须展示带逗号的一基行列号')
assert.match(pageWithoutVueComments, /原始路径：\$\{row\.fieldPath\}/, '技术 fieldPath 只能作为原始路径提示展示')
assert.match(
  pageWithoutVueComments,
  /class="edhr-form-fill-log-page__cell-location-code"[\s\S]*formatCellLocation\(row\)[\s\S]*class="edhr-form-fill-log-page__cell-location-detail"[\s\S]*formatCellLocationDetail\(row\)/,
  '明细列必须以坐标为主、行列说明为辅展示单元格位置'
)
assert.doesNotMatch(
  pageWithoutVueComments,
  /label="单元格(?:定位|位置)"[\s\S]{0,500}\{\{\s*row\.fieldPath/,
  '明细列不得直接把技术 fieldPath 作为主显示'
)

assert.match(api, /\/mes\/pro\/batch-record-execution\/form-fill-log\/page/, 'API 必须调用填写日志分页接口')
assert.match(api, /\/mes\/pro\/batch-record-execution\/form-fill-log\/detail/, 'API 必须调用填写日志明细接口')
assert.match(api, /FormFillLogPageReqVO/, 'API 必须声明分页请求类型')
assert.match(api, /contextStatus/, 'API 必须暴露上下文状态')

assert.match(pageWithoutVueComments, /data-edhr-form-log-source-tabs/, '表单日志必须提供日志来源页签')
assert.match(pageWithoutVueComments, /label="表单填写日志"/, '原表单填写日志必须保留为独立页签')
assert.match(pageWithoutVueComments, /label="报工修改日志"/, '报工修改记录必须迁移到表单日志页签')
assert.match(pageWithoutVueComments, /data-production-report-revision-log-table/, '报工修改日志必须有稳定列表锚点')
assert.match(pageWithoutVueComments, /row-key="revisionId"/, '报工修改日志必须用修订记录作为列表主对象')
for (const label of ['生产工单号', '工序', '原报工人', '原提交时间', '修改人', '修改时间', '修改原因', '修改字段数', '修改摘要']) {
  assert.match(pageWithoutVueComments, new RegExp(`label="${label}"`), `报工修改日志列表必须包含列：${label}`)
}
assert.match(pageWithoutVueComments, /openProductionReportRevisionDetail\(row\)/, '报工修改日志必须提供详情入口')
assert.match(pageWithoutVueComments, /data-production-report-revision-log-detail-drawer/, '报工修改日志详情必须在表单日志中打开')
assert.match(pageWithoutVueComments, /getProductionReportRevisionLogPage\(buildProductionReportRevisionQuery\(\)\)/, '报工修改日志分页必须调用正式 API wrapper')
assert.match(pageWithoutVueComments, /getProductionReportRevisionLogDetail\(row\.revisionId\)/, '报工修改日志详情必须按修订记录读取')
assert.match(pageWithoutVueComments, /productionReportRevisionLoadError/, '报工修改日志必须暴露加载错误')
assert.match(pageWithoutVueComments, /productionReportRevisionDetailError/, '报工修改日志详情必须暴露加载错误')
assert.doesNotMatch(pageWithoutVueComments, /getProcessPoolProductionReportRevisionLogs/, '表单日志不得复用工作台按 eventId 查询的旧入口')

assert.match(api, /ProductionReportRevisionLogPageReqVO/, 'API 必须声明报工修改日志分页请求类型')
assert.match(api, /ProductionReportRevisionLogPageRespVO/, 'API 必须声明报工修改日志分页响应类型')
assert.match(api, /ProductionReportRevisionLogDetailRespVO/, 'API 必须声明报工修改日志详情响应类型')
assert.match(api, /\/mes\/pro\/batch-record-execution\/form-fill-log\/production-report-revision\/page/, 'API 必须调用表单日志下的报工修改分页接口')
assert.match(api, /\/mes\/pro\/batch-record-execution\/form-fill-log\/production-report-revision\/detail/, 'API 必须调用表单日志下的报工修改详情接口')

console.log('PASS: eDHR form fill log static contract')
