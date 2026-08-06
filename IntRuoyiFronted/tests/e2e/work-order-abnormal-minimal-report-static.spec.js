const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'IntRuoyiFronted')
const backendRoot = path.join(workspaceRoot, 'IntRuoyiBackend')

const read = (file) => fs.readFileSync(file, 'utf8')

const page = read(path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
))
const api = read(path.join(
  frontendRoot,
  'src/api/mes/pro/processpool/teamLeader.ts'
))
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

const abnormalSectionStart = page.indexOf('<div class="team-leader-workbench__section-title">订单异常上报</div>')
assert(abnormalSectionStart >= 0, '必须保留订单异常上报模块。')
const abnormalSectionEnd = page.indexOf('</el-form>', abnormalSectionStart)
assert(abnormalSectionEnd > abnormalSectionStart, '订单异常上报模块必须包含独立表单。')
const abnormalSection = page.slice(abnormalSectionStart, abnormalSectionEnd)
assert(abnormalSection.includes('label="订单号"'), '异常上报表单必须展示订单号。')
assert(abnormalSection.includes('label="异常说明"'), '异常上报表单必须展示异常说明。')
assert(!abnormalSection.includes('label="工序ID"'), '异常上报表单不得展示工序ID。')
assert(!abnormalSection.includes('label="异常原因"'), '异常上报表单不得展示异常原因。')
assert(!abnormalSection.includes('data-team-leader-defect-reason-select'), '异常上报表单不得渲染异常原因选择器。')

const requestType = api.slice(
  api.indexOf('export interface WorkOrderAbnormalReportReqVO'),
  api.indexOf('export interface TeamEmployeeBindingSaveReqVO')
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
  /markAndReportWorkOrderAbnormal\(\{\s*workOrderId:\s*requireSelectedActiveOrderWorkOrderId\(\),\s*abnormalDescription:\s*abnormalForm\.abnormalDescription\.trim\(\)\s*\}\)/.test(submitBlock),
  '异常上报提交 payload 必须只包含 workOrderId 与 abnormalDescription。'
)
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
const controllerEnd = controller.indexOf('@PostMapping("/employee-binding/add")', controllerStart)
assert(controllerStart >= 0 && controllerEnd > controllerStart, '必须能定位异常上报 Controller 方法。')
const controllerBlock = controller.slice(controllerStart, controllerEnd)
assert(controllerBlock.includes('.workOrderId(reqVO.getWorkOrderId())'), 'Controller 必须传递 workOrderId。')
assert(controllerBlock.includes('.abnormalDescription(reqVO.getAbnormalDescription())'), 'Controller 必须传递 abnormalDescription。')
for (const field of ['getRouteProcessId', 'getProcessId', 'getSourceEventId', 'getAbnormalReasonCode']) {
  assert(!controllerBlock.includes(field), `Controller 异常上报方法不得读取 ${field}。`)
}

const validateStart = service.indexOf('private void validateReq')
const validateEnd = service.indexOf('private static boolean isBlank', validateStart)
assert(validateStart >= 0 && validateEnd > validateStart, '必须能定位异常上报服务校验。')
const validateBlock = service.slice(validateStart, validateEnd)
assert(validateBlock.includes('reqBO.getWorkOrderId() == null'), '服务校验必须要求 workOrderId。')
assert(validateBlock.includes('isBlank(reqBO.getAbnormalDescription())'), '服务校验必须要求 abnormalDescription。')
assert(!validateBlock.includes('getAbnormalReasonCode'), '服务校验不得要求异常原因。')
