const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')
const read = (file) => fs.readFileSync(file, 'utf8')

const page = read(path.join(frontendRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'))
const api = read(path.join(frontendRoot, 'src/api/mes/pro/processpool/teamLeader.ts'))
const reqVO = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesWorkOrderAbnormalReportReqVO.java'
))
const controller = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
))
const reqBO = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesWorkOrderAbnormalReportReqBO.java'
))
const service = read(path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesWorkOrderAbnormalReportServiceImpl.java'
))

const removedFields = ['routeProcessId', 'processId', 'sourceEventId', 'abnormalReasonCode']

assert(!page.includes("activeProductionModuleTab = 'exception'"), '生产管理导航不得保留异常页签。')
assert(!page.includes('showProductionExceptionModule'), '不得保留独立异常模块显示条件。')
assert(!page.includes('data-team-leader-abnormal-report\n'), '不得保留独立异常上报表单。')
assert(page.includes('data-team-leader-report-active-order-abnormal'), '活跃订单行必须提供报异常按钮。')
assert(page.includes('data-team-leader-abnormal-report-dialog'), '必须通过活跃订单行打开异常上报对话框。')
assert(page.includes('label="异常原因"'), '异常上报对话框必须只要求填写异常原因。')
assert(page.includes('openAbnormalDialog(row)'), '报异常按钮必须绑定当前活跃订单行。')
assert(page.includes("'team-leader-workbench__abnormal-work-order-id': row.abnormal"),
  '异常活跃订单的生产订单 ID 必须使用异常样式。')
assert(page.includes('allocatableActiveOrderOptions'), '工作分配候选必须使用排除异常订单后的集合。')

const requestType = api.slice(
  api.indexOf('export interface WorkOrderAbnormalReportReqVO'),
  api.indexOf('export interface TeamDefectReasonSaveReqVO')
)
assert(/workOrderId:\s*number/.test(requestType), '前端异常上报请求必须包含 workOrderId。')
assert(/abnormalDescription:\s*string/.test(requestType), '前端异常上报请求必须包含 abnormalDescription。')
for (const field of removedFields) {
  assert(!requestType.includes(field), `前端异常上报请求不得包含 ${field}。`)
}

const submitStart = page.indexOf('const submitAbnormal = async () => {')
const submitEnd = page.indexOf('const openActiveOrderDialog', submitStart)
assert(submitStart >= 0 && submitEnd > submitStart, '必须能定位异常上报提交函数。')
const submitBlock = page.slice(submitStart, submitEnd)
assert(
  /markAndReportWorkOrderAbnormal\(\{\s*workOrderId:\s*abnormalForm\.workOrderId,\s*abnormalDescription:\s*abnormalForm\.abnormalDescription\.trim\(\)\s*\}\)/.test(submitBlock),
  '异常上报提交 payload 必须锁定当前行生产订单且只包含异常原因。'
)
assert(submitBlock.includes('await loadActiveOrders()'), '异常上报成功后必须刷新活跃订单异常状态。')
for (const field of removedFields) {
  assert(!submitBlock.includes(field), `异常上报提交函数不得包含 ${field}。`)
}

assert(reqVO.includes('private Long workOrderId;'), '后端异常上报 VO 必须保留 workOrderId。')
assert(reqVO.includes('private String abnormalDescription;'), '后端异常上报 VO 必须保留 abnormalDescription。')
for (const field of removedFields) {
  assert(!reqVO.includes(field), `后端异常上报 VO 不得暴露 ${field}。`)
  assert(!reqBO.includes(field), `后端异常上报 BO 不得暴露 ${field}。`)
}

const controllerStart = controller.indexOf('public CommonResult<Long> markAndReportWorkOrderAbnormal')
const controllerEnd = controller.indexOf('@PostMapping("/defect-reason/create")', controllerStart)
assert(controllerStart >= 0 && controllerEnd > controllerStart, '必须能定位异常上报 Controller 方法。')
const controllerBlock = controller.slice(controllerStart, controllerEnd)
assert(controllerBlock.includes('.workOrderId(reqVO.getWorkOrderId())'), 'Controller 必须传递 workOrderId。')
assert(controllerBlock.includes('.abnormalDescription(reqVO.getAbnormalDescription())'), 'Controller 必须传递异常原因。')
for (const field of ['getRouteProcessId', 'getProcessId', 'getSourceEventId', 'getAbnormalReasonCode']) {
  assert(!controllerBlock.includes(field), `Controller 异常上报方法不得读取 ${field}。`)
}

const validateStart = service.indexOf('private void validateReq')
const validateEnd = service.indexOf('private static boolean isBlank', validateStart)
assert(validateStart >= 0 && validateEnd > validateStart, '必须能定位异常上报服务校验。')
const validateBlock = service.slice(validateStart, validateEnd)
assert(validateBlock.includes('reqBO.getWorkOrderId() == null'), '服务校验必须要求 workOrderId。')
assert(validateBlock.includes('isBlank(reqBO.getAbnormalDescription())'), '服务校验必须要求异常原因。')
assert(!validateBlock.includes('getAbnormalReasonCode'), '服务校验不得要求异常原因编码。')

console.log('work-order abnormal row action contract passed')
