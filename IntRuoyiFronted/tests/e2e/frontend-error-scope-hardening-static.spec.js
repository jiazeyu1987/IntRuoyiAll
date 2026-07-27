const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readPage = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const extractFunction = (source, name) => {
  const start = source.indexOf(`const ${name} = async`)
  assert.notEqual(start, -1, `必须存在异步函数：${name}`)
  const boundaries = [
    source.indexOf('\nconst ', start + 1),
    source.indexOf('\nonMounted', start + 1),
    source.indexOf('\n</script>', start + 1)
  ].filter((index) => index !== -1)
  assert.ok(boundaries.length, `函数 ${name} 后必须存在脚本边界，便于静态截取。`)
  return source.slice(start, Math.min(...boundaries))
}

const batchDetail = readPage('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const batchSecondary = extractFunction(batchDetail, 'loadBatchDetailSecondaryData')
const batchPrimary = extractFunction(batchDetail, 'loadDetail')

assert.doesNotMatch(
  batchSecondary,
  /loadError\.value\s*=/,
  '批次详情延迟辅助加载失败不得污染主详情 loadError。'
)
assert.match(
  batchSecondary,
  /secondaryLoadError\.value\s*=\s*resolveErrorMessage/,
  '批次详情延迟辅助加载必须写入复盘区域自己的错误状态。'
)
assert.match(
  batchPrimary,
  /catch\s*\(error\)[\s\S]*loadError\.value\s*=\s*resolveErrorMessage/,
  '批次详情主查询失败仍必须写入页面级 loadError。'
)
assert.match(
  batchDetail,
  /v-if="secondaryLoadError"[\s\S]*:title="secondaryLoadError"/,
  '批次详情辅助错误必须在复盘区域保留真实错误文本。'
)

const directories = readPage('src/views/dcc/controlled-file/directories/index.vue')
const directoryPrimary = extractFunction(directories, 'getList')
const directoryChildren = extractFunction(directories, 'loadDirectoryChildren')

assert.doesNotMatch(
  directoryChildren,
  /loadErrorMessage\.value\s*=/,
  '单行子目录懒加载不得设置或清除目录页全局错误。'
)
assert.match(
  directoryChildren,
  /childLoadErrorMessages\[[^\]]+\]\s*=\s*resolveControlledFileReadErrorMessage/,
  '子目录懒加载失败必须记录到对应目录行。'
)
assert.match(
  directoryPrimary,
  /catch\s*\(error\)[\s\S]*loadErrorMessage\.value\s*=\s*resolveControlledFileReadErrorMessage/,
  '根目录查询失败仍必须写入目录页全局错误。'
)
assert.match(
  directories,
  /resolveDirectoryChildLoadError\(row\)[\s\S]*子目录加载失败/,
  '目录行必须显示子目录加载失败并保留真实错误。'
)

const fieldAudit = readPage('src/views/mes/pro/edhr/FieldAuditPage.vue')
const fieldAuditList = extractFunction(fieldAudit, 'getList')
const fieldAuditVerify = extractFunction(fieldAudit, 'handleVerify')
const fieldAuditExport = extractFunction(fieldAudit, 'handleExport')

assert.doesNotMatch(fieldAuditVerify, /loadError\.value\s*=/, '字段审计校验错误不得污染列表加载错误。')
assert.doesNotMatch(fieldAuditExport, /loadError\.value\s*=/, '字段审计导出错误不得污染列表加载错误。')
assert.match(fieldAuditVerify, /actionError\.value\s*=/, '字段审计校验错误必须写入操作错误。')
assert.match(fieldAuditExport, /actionError\.value\s*=/, '字段审计导出错误必须写入操作错误。')
assert.match(
  fieldAuditList,
  /catch\s*\(error\)[\s\S]*loadError\.value\s*=\s*resolveErrorMessage/,
  '字段审计列表失败仍必须写入列表加载错误。'
)

const fieldAuditDetail = readPage('src/views/mes/pro/edhr/FieldAuditDetailPage.vue')
const fieldAuditDetailLoad = extractFunction(fieldAuditDetail, 'loadDetail')
const fieldAuditDetailVerify = extractFunction(fieldAuditDetail, 'handleVerify')

assert.doesNotMatch(
  fieldAuditDetailVerify,
  /loadError\.value\s*=/,
  '字段审计详情校验不得覆盖详情加载错误。'
)
assert.match(
  fieldAuditDetailVerify,
  /verificationError\.value\s*=/,
  '字段审计详情校验必须写入校验错误。'
)
assert.match(
  fieldAuditDetailLoad,
  /catch\s*\(error\)[\s\S]*loadError\.value\s*=\s*resolveErrorMessage/,
  '字段审计详情主查询失败仍必须写入详情加载错误。'
)

const domainTraceDetail = readPage('src/views/mes/pro/edhr/DomainTraceDetailPage.vue')
const domainTraceLoad = extractFunction(domainTraceDetail, 'loadDetail')
const domainTraceVerify = extractFunction(domainTraceDetail, 'handleVerify')

assert.doesNotMatch(
  domainTraceVerify,
  /loadError\.value\s*=/,
  '主数据追溯校验不得覆盖详情加载错误。'
)
assert.match(domainTraceVerify, /verifyError\.value\s*=/, '主数据追溯校验失败必须写入校验错误。')
assert.match(
  domainTraceLoad,
  /catch\s*\(error\)[\s\S]*loadError\.value\s*=\s*resolveErrorMessage/,
  '主数据追溯详情主查询失败仍必须写入详情加载错误。'
)

const delivery = readPage('src/views/mes/pro/edhr-delivery/DeliveryPage.vue')
const deliveryProjects = extractFunction(delivery, 'getProjectList')
const deliveryPackages = extractFunction(delivery, 'getPackageList')
const deliveryGate = extractFunction(delivery, 'getGateSummary')
const deliveryCreate = extractFunction(delivery, 'handleCreateProject')

assert.doesNotMatch(deliveryPackages, /loadError\.value\s*=/, '证据包加载不得污染项目列表错误。')
assert.doesNotMatch(deliveryGate, /loadError\.value\s*=/, '门禁摘要加载不得污染项目列表错误。')
assert.doesNotMatch(deliveryCreate, /loadError\.value\s*=/, '交付项目创建失败不得伪装成列表加载失败。')
assert.match(deliveryPackages, /packageError\.value\s*=/, '证据包失败必须写入证据包区域错误。')
assert.match(deliveryGate, /gateError\.value\s*=/, '门禁摘要失败必须写入门禁区域错误。')
assert.match(deliveryCreate, /createError\.value\s*=/, '交付项目创建失败必须写入创建弹窗错误。')
assert.match(
  deliveryProjects,
  /catch\s*\(error\)[\s\S]*loadError\.value\s*=\s*resolveErrorMessage/,
  '交付项目主列表失败仍必须写入页面级加载错误。'
)

const validation = readPage('src/views/mes/pro/edhr-validation/ValidationPage.vue')
const validationPackages = extractFunction(validation, 'getPackageList')
const validationItems = extractFunction(validation, 'getItemList')
const validationRefresh = extractFunction(validation, 'refreshSelectedPackage')
const validationCreatePackage = extractFunction(validation, 'handleCreatePackage')
const validationCreateItem = extractFunction(validation, 'handleCreateItem')
const validationCreateTrace = extractFunction(validation, 'handleCreateTraceLink')
const validationEvaluate = extractFunction(validation, 'handleEvaluateTrace')

for (const [name, source] of [
  ['getItemList', validationItems],
  ['refreshSelectedPackage', validationRefresh],
  ['handleCreatePackage', validationCreatePackage],
  ['handleCreateItem', validationCreateItem],
  ['handleCreateTraceLink', validationCreateTrace],
  ['handleEvaluateTrace', validationEvaluate]
]) {
  assert.doesNotMatch(source, /loadError\.value\s*=/, `${name} 不得污染验证包主列表错误。`)
}
assert.match(validationItems, /itemError\.value\s*=/, '验证条目加载失败必须写入条目区域错误。')
assert.match(validationRefresh, /traceError\.value\s*=/, '验证包详情刷新失败必须写入追溯区域错误。')
assert.match(validationCreatePackage, /packageActionError\.value\s*=/, '验证包创建失败必须写入创建弹窗错误。')
assert.match(validationCreateItem, /itemActionError\.value\s*=/, '验证条目登记失败必须写入登记弹窗错误。')
assert.match(validationCreateTrace, /traceActionError\.value\s*=/, '追溯关系创建失败必须写入追溯弹窗错误。')
assert.match(validationEvaluate, /traceError\.value\s*=/, '追溯评估失败必须写入追溯区域错误。')
assert.match(
  validationPackages,
  /catch\s*\(error\)[\s\S]*loadError\.value\s*=\s*resolveErrorMessage/,
  '验证包主列表失败仍必须写入页面级加载错误。'
)

console.log('PASS: frontend primary, auxiliary, row, panel, and action errors stay in their own scope.')
