const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../../../../../')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const controller = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProEdhrBatchExecutionController.java'
)
const servicePath = 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchActiveOrderDetailService.java'
assert.ok(fs.existsSync(path.join(repoRoot, servicePath)), '批次活跃订单详情服务必须存在。')
const service = read(servicePath)
const api = read('IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts')
const detailPage = read('IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const historyPage = read('IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const traceDrawer = read('IntRuoyiFronted/src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue')

assert.match(
  controller,
  /@GetMapping\("\/active-order-detail"\)[\s\S]*@PreAuthorize\("@ss\.hasPermission\('mes:pro-edhr-batch-execution:query'\)"\)[\s\S]*getActiveOrderDetail\(\s*@RequestParam\("batchExecutionId"\) Long batchExecutionId\s*\)/,
  '批次执行必须提供自己的活跃订单详情读取入口，并使用批次执行查询权限。'
)
assert.match(
  controller,
  /MesProcessPoolTeamLeaderController\.toActiveOrderDetailRespVO\(\s*batchActiveOrderDetailService\.getDetail\(batchExecutionId\)\s*\)/,
  '批次执行接口必须复用活跃订单详情同一份响应结构。'
)
assert.match(
  service,
  /batchExecutionService\.get\(batchExecutionId\)/,
  '批次执行详情服务必须先通过批次执行正式读取入口解析来源。'
)
assert.match(
  service,
  /batch\.getActiveOrderId\(\)/,
  '批次执行详情服务必须从正式批次来源关系取得 activeOrderId。'
)
assert.match(
  service,
  /activeOrderMapper\.selectByIdIgnoreDeleted\(activeOrderId\)/,
  '批次执行详情服务必须校验批次绑定的正式活跃订单来源，即使该来源已从当前活跃池归档。'
)
assert.match(
  service,
  /detailService\.getArchivedFormalDetail\(activeOrderId\)/,
  '批次执行详情服务必须复用活跃订单正式事实投影，且上市放行后仍能读取同一份详情。'
)
assert.match(
  api,
  /getEdhrBatchActiveOrderDetail[\s\S]*BATCH_EXECUTION_BASE_URL[\s\S]*active-order-detail[\s\S]*batchExecutionId/,
  '前端批次执行 API 必须调用批次执行自己的详情入口。'
)

for (const [name, source] of [
  ['批次执行详情页', detailPage],
  ['历史追溯页', historyPage],
  ['批次追溯抽屉', traceDrawer]
]) {
  assert.match(
    source,
    /getEdhrBatchActiveOrderDetail/,
    `${name} 必须通过批次执行自己的详情入口读取活跃订单详情。`
  )
  assert.doesNotMatch(
    source,
    /getTeamLeaderActiveOrderDetail/,
    `${name} 不得直接调用生产组长详情接口，否则没有生产组长权限时会打不开。`
  )
}

console.log('mes-edhr-batch-active-order-detail-source static contract passed')
