const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../../')
const pagePath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'
)
const sourceDetailPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionActiveOrderDetailPage.vue'
)
const activeOrderPanelPath = path.join(
  workspaceRoot,
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const source = fs.readFileSync(pagePath, 'utf8')
const sourceDetail = fs.readFileSync(sourceDetailPath, 'utf8')
const activeOrderPanel = fs.readFileSync(activeOrderPanelPath, 'utf8')
const operationStart = source.indexOf(
  "<el-table-column v-if=\"isEdhrBatchExecutionColumnVisible('operation')\""
)
const operationEnd = source.indexOf('          </el-table>', operationStart)

assert.ok(operationStart >= 0, '批次执行列表必须存在操作列')
assert.ok(operationEnd > operationStart, '无法定位批次执行列表操作列边界')

const operationColumn = source.slice(operationStart, operationEnd)

assert.match(operationColumn, />\s*详情\s*</, '操作列必须保留详情按钮')
assert.match(operationColumn, />\s*上市放行\s*</, '操作列必须显示上市放行按钮')
assert.match(operationColumn, />\s*上传\s*</, '操作列必须显示上传按钮')
assert.match(operationColumn, />\s*驳回\s*</, '操作列必须显示驳回占位按钮')
assert.doesNotMatch(operationColumn, />\s*编辑\s*</, '操作列不得继续显示编辑按钮')

assert.match(
  operationColumn,
  /v-hasPermi="\['mes:pro-edhr-release:approve'\]"/,
  '上市放行按钮必须只对最终放行权限可见'
)
assert.match(
  operationColumn,
  /@click="openReleaseDialog\(row\)"/,
  '上市放行必须打开负责人电子签名确认弹窗'
)
assert.doesNotMatch(
  operationColumn,
  /handlePlaceholderBatchAction\('上市放行'\)/,
  '上市放行不得继续调用占位处理器'
)
assert.match(
  operationColumn,
  /v-hasPermi="\['mes:pro-edhr-batch-execution:upload'\]"/,
  '上传按钮必须只对批次执行上传权限可见'
)
assert.match(
  operationColumn,
  /@click="openActiveOrderOtherUploadTab\(row\)"/,
  '上传按钮必须进入活跃订单其他上传 Tab'
)
assert.match(
  operationColumn,
  /v-hasPermi="\['mes:pro-production-release:pqc-reject'\]"/,
  '驳回按钮必须只对上市放行负责人权限可见'
)
assert.match(
  operationColumn,
  /@click="handleRejectClick\(row\)"/,
  '驳回必须进入二次确认和电子签名流程'
)
assert.doesNotMatch(
  operationColumn,
  /handlePlaceholderBatchAction\('驳回'\)/,
  '驳回不得继续调用占位处理器'
)
assert.doesNotMatch(
  operationColumn,
  /handlePlaceholderBatchAction\('上传'\)/,
  '上传按钮不得继续是占位动作'
)

const placeholderHandlerStart = source.indexOf('const handlePlaceholderBatchAction =')
assert.ok(placeholderHandlerStart >= 0, '必须存在占位按钮处理器')
const placeholderHandlerEnd = source.indexOf('\n\n', placeholderHandlerStart)
const placeholderHandler = source.slice(
  placeholderHandlerStart,
  placeholderHandlerEnd >= 0 ? placeholderHandlerEnd : source.length
)
assert.match(placeholderHandler, /功能暂未开放/, '占位按钮必须给出明确的未开放反馈')
assert.doesNotMatch(placeholderHandler, /上市放行/, '占位处理器不得包含上市放行动作')
assert.doesNotMatch(placeholderHandler, /上传/, '占位处理器不得包含上传动作')
assert.doesNotMatch(
  placeholderHandler,
  /await\s+\w+\(/,
  '占位按钮处理器不得调用异步业务接口'
)

assert.match(
  source,
  /rejectEdhrBatchExecutionToNonconformanceReview/,
  '驳回必须调用创建不合格评审的正式接口'
)

const rejectHandlerStart = source.indexOf('const handleRejectClick =')
assert.ok(rejectHandlerStart >= 0, '必须存在驳回二次确认处理器')
const rejectHandlerEnd = source.indexOf('\n\n', rejectHandlerStart)
const rejectHandler = source.slice(
  rejectHandlerStart,
  rejectHandlerEnd >= 0 ? rejectHandlerEnd : source.length
)
assert.match(rejectHandler, /message\.confirm/, '点击驳回后必须先进行二次确认')
assert.match(rejectHandler, /openRejectDialog/, '二次确认通过后必须打开电子签名弹窗')

assert.match(
  source,
  /title="驳回并发起不合格评审"/,
  '必须提供驳回并发起不合格评审弹窗'
)
assert.match(
  source,
  /v-model="rejectForm\.nonconformanceReason"/,
  '驳回弹窗必须采集不合格原因'
)
assert.match(
  source,
  /v-model="rejectForm\.signaturePassword"/,
  '驳回弹窗必须采集电子签名密码'
)
assert.match(
  source,
  /submitRejectBatchExecution/,
  '驳回弹窗必须提交真实驳回动作'
)

assert.match(
  source,
  /<Dialog[\s\S]{0,240}上市放行确认/,
  '上市放行必须使用二次确认弹窗'
)
assert.match(
  source,
  /releaseForm\.password[\s\S]{0,160}type="password"|type="password"[\s\S]{0,160}releaseForm\.password/,
  '上市放行弹窗必须使用电子密码输入框'
)
assert.match(
  source,
  /if \(!releaseForm\.password\.trim\(\)\)[\s\S]{0,180}return/,
  '未输入电子密码时前端必须阻止提交'
)
assert.match(
  source,
  /approveEdhrRelease\([\s\S]{0,420}password:\s*releaseForm\.password/,
  '上市放行必须调用正式最终放行 API 并传入电子密码'
)
assert.match(
  source,
  /上市放行成功/,
  '上市放行成功后必须给出成功提示'
)
assert.match(
  source,
  /edhr-batch-history[\s\S]{0,220}batchExecutionId/,
  '上市放行成功后必须跳转历史追溯列表并携带批次执行编号'
)

const uploadHandlerStart = source.indexOf('const openActiveOrderOtherUploadTab =')
assert.ok(uploadHandlerStart >= 0, '必须存在上传入口处理器')
const uploadHandlerEnd = source.indexOf('\n\n', uploadHandlerStart)
const uploadHandler = source.slice(
  uploadHandlerStart,
  uploadHandlerEnd >= 0 ? uploadHandlerEnd : source.length
)
assert.match(uploadHandler, /row\.id/, '上传入口必须使用批次执行编号定位正式活跃订单详情')
assert.match(
  uploadHandler,
  /edhr-batch-execution\/source-detail/,
  '上传入口必须进入批次作用域活跃订单详情页'
)
assert.match(uploadHandler, /tab:\s*'otherUpload'/, '上传入口必须携带其他上传 Tab 参数')
assert.match(uploadHandler, /from:\s*'execution'/, '上传入口必须保留返回批次执行列表的来源')

assert.match(
  sourceDetail,
  /:initial-active-tab="initialActiveTab"/,
  '批次作用域活跃订单详情页必须把目标 Tab 传给详情面板'
)
assert.match(sourceDetail, /route\.query\.tab/, '详情页必须读取路由 Tab 参数')
assert.match(sourceDetail, /otherUpload/, '详情页必须识别其他上传 Tab 参数')
assert.match(sourceDetail, /dossierFile:OTHER_FILE/, '其他上传必须映射到其他文件资料 Tab')

assert.match(activeOrderPanel, /initialActiveTab\?: string/, '活跃订单详情面板必须支持初始 Tab')
assert.match(
  activeOrderPanel,
  /props\.initialActiveTab/,
  '活跃订单详情面板重置 Tab 时必须读取初始 Tab'
)
assert.match(
  activeOrderPanel,
  /mes:pro-edhr-batch-execution:upload/,
  '其他文件上传控件必须接受批次执行上传权限'
)
assert.doesNotMatch(
  activeOrderPanel,
  /data-active-order-dossier-file-delete[\s\S]{0,220}mes:pro-edhr-batch-execution:upload/,
  '批次执行上传权限不得开放删除资料文件按钮'
)

console.log('edhr-batch-action-placeholders-static: PASS')
