import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const backendRoot = path.resolve(root, '..', 'ruoyi-vue-pro')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackendText = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')
const readOptionalText = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}
const countRoutePathDefinitions = (source, routePath) =>
  Array.from(source.matchAll(new RegExp(`path:\\s*'${routePath.replaceAll('/', '\\/')}'`, 'g'))).length

test('BDD: 审批待办处理 -> 审批页使用 eDHR 自有 approve/reject API，不调用通用 BPM API', () => {
  const approvalApi = readOptionalText('src/api/mes/pro/edhr/approval.ts')
  const approvalPage = readOptionalText('src/views/mes/pro/edhr/ApprovalPage.vue')
  const approvalDetailPage = readOptionalText('src/views/mes/pro/edhr/ApprovalDetailPage.vue')
  const routerSource = readText('src/router/modules/remaining.ts')
  const source = `${approvalApi}\n${approvalPage}\n${approvalDetailPage}`

  assert.match(approvalApi, /EDHR_EXECUTION_STATUS[\s\S]*DRAFT:\s*0[\s\S]*SUBMITTED:\s*1[\s\S]*REJECTED:\s*2[\s\S]*APPROVED:\s*3/s)
  assert.match(approvalApi, /approval-pending-page/, '必须提供 eDHR 待我审批分页 helper')
  assert.match(approvalApi, /approval-done-page/, '必须提供 eDHR 我已审批分页 helper')
  assert.match(approvalApi, /getEdhrApprovalDetail[\s\S]*\/mes\/pro\/batch-record-execution\/approval-detail/s, '审批详情必须调用 eDHR approval-detail 接口')
  assert.doesNotMatch(approvalApi, /url:\s*'\/mes\/pro\/batch-record-execution\/get'/, '审批详情不得复用通用 get 接口')
  assert.match(approvalApi, /\/mes\/pro\/batch-record-execution\/approve/, '审批通过必须调用 eDHR approve 接口')
  assert.match(approvalApi, /\/mes\/pro\/batch-record-execution\/reject/, '审批驳回必须调用 eDHR reject 接口')
  assert.doesNotMatch(source, /\/bpm\/task\/(?:approve|reject)/, '不得调用通用 BPM approve/reject')
  assert.match(approvalPage, /待我审批[\s\S]*我已审批|pending[\s\S]*done/s, '审批页必须包含待办与已办 tab')
  assert.match(approvalPage, /password|密码/, '审批动作必须要求当前账号密码')
  assert.match(approvalPage, /rejectReason|驳回原因/, '驳回动作必须要求驳回原因')
  assert.match(approvalPage, /bpmTaskId/, '审批按钮必须受 bpmTaskId 控制')
  assert.match(approvalPage, /缺少 BPM 任务，无法处理|审批接口缺少执行记录或 BPM 任务，无法审批/, '任务或详情字段缺失必须显式展示错误')
  assert.equal(countRoutePathDefinitions(routerSource, 'pro/feedback/edhr-approval'), 0, 'eDHR 审批菜单页必须由 system_menu 动态路由提供，不得注册隐藏静态路由')
  assert.equal(countRoutePathDefinitions(routerSource, 'pro/feedback/edhr-approval/detail'), 1, 'eDHR 审批详情必须保留隐藏静态路由')
  assert.match(routerSource, /MesProFeedbackEdhrApprovalDetail/, '必须注册 eDHR 审批详情隐藏路由')
  assert.match(routerSource, /mes:pro-batch-record-execution:approve/, '审批详情路由必须使用 eDHR 审批权限')
  assert.match(approvalPage, /path:\s*'\/mes\/pro\/feedback\/edhr-approval\/detail'/, '审批页打开详情必须使用稳定 path，避免依赖动态菜单 route name')
  assert.match(approvalPage, /path:\s*'\/mes\/pro\/feedback\/edhr-tracking'/, '审批页打开追踪必须使用动态菜单 path')
  assert.match(approvalPage, /path:\s*'\/mes\/pro\/feedback\/edhr-signatures'/, '审批页打开签名必须使用动态菜单 path')
  assert.match(approvalDetailPage, /path:\s*'\/mes\/pro\/feedback\/edhr-approval'/, '审批详情返回列表必须使用动态菜单 path')
})

test('BDD: 审批列表筛选 -> 执行编号、工单号、批次号、提交人ID、提交时间范围必须发送到后端真实字段', () => {
  const approvalPage = readOptionalText('src/views/mes/pro/edhr/ApprovalPage.vue')
  const approvalApi = readOptionalText('src/api/mes/pro/edhr/approval.ts')

  assert.match(approvalApi, /interface\s+EdhrApprovalPageReqVO[\s\S]*executionCode\?:\s*string[\s\S]*workOrderCode\?:\s*string[\s\S]*batchCode\?:\s*string[\s\S]*submittedBy\?:\s*number[\s\S]*submittedAtStart\?:\s*string[\s\S]*submittedAtEnd\?:\s*string/s, '审批列表 API 查询对象必须只声明后端支持的筛选字段')
  assert.match(approvalPage, /<el-form-item\s+label="提交人ID">/, '提交人使用 ID 筛选时 UI 文案必须明确为提交人ID')
  assert.match(approvalPage, /<el-form-item\s+label="提交时间">[\s\S]*type="datetimerange"[\s\S]*value-format="YYYY-MM-DD HH:mm:ss"/, '审批页必须提供提交时间范围筛选')
  assert.match(approvalPage, /const buildQuery = \(\) => \(\{[\s\S]*executionCode:[\s\S]*workOrderCode:[\s\S]*batchCode:[\s\S]*submittedBy:\s*resolveSubmittedByFilter\(\)[\s\S]*submittedAtStart:\s*submittedAtRange\.value\?\.\[0\][\s\S]*submittedAtEnd:\s*submittedAtRange\.value\?\.\[1\]/s, 'buildQuery 必须发送提交人ID与提交时间范围，不得静默丢弃筛选值')
  assert.match(approvalPage, /提交人ID必须为数字/, '提交人ID非法时必须显式报错')
})

test('BDD: 审批按钮门槛 -> 后端 canApprove/canReject、SUBMITTED、bpmTaskId 与审批权限必须同时满足', () => {
  const approvalApi = readOptionalText('src/api/mes/pro/edhr/approval.ts')
  const approvalPage = readOptionalText('src/views/mes/pro/edhr/ApprovalPage.vue')
  const approvalDetailPage = readOptionalText('src/views/mes/pro/edhr/ApprovalDetailPage.vue')

  assert.match(approvalApi, /canApprove\?:\s*boolean[\s\S]*canReject\?:\s*boolean/s, '审批行与详情必须接收后端 canApprove/canReject')
  assert.match(approvalPage, /const canApproveRow = \(row: EdhrApprovalRowVO\) =>[\s\S]*hasApprovePermission\.value[\s\S]*row\.canApprove\s*===\s*true[\s\S]*row\.status\s*===\s*EDHR_EXECUTION_STATUS\.SUBMITTED[\s\S]*Boolean\(row\.bpmTaskId\)/s, '列表通过按钮必须同时检查权限、canApprove、SUBMITTED 与 bpmTaskId')
  assert.match(approvalPage, /const canRejectRow = \(row: EdhrApprovalRowVO\) =>[\s\S]*hasApprovePermission\.value[\s\S]*row\.canReject\s*===\s*true[\s\S]*row\.status\s*===\s*EDHR_EXECUTION_STATUS\.SUBMITTED[\s\S]*Boolean\(row\.bpmTaskId\)/s, '列表驳回按钮必须同时检查权限、canReject、SUBMITTED 与 bpmTaskId')
  assert.match(approvalPage, /const submitAction = async \(\) => \{[\s\S]*resolveApprovalDisabledReason\(currentRow\.value\)[\s\S]*resolveRejectDisabledReason\(currentRow\.value\)/s, '列表提交动作必须复查审批门槛，防止绕过禁用按钮')
  assert.match(approvalDetailPage, /hasApprovePermission = computed\(\(\) => hasPermission\(\[APPROVE_PERMISSION\]\)\)/, '详情页审批门槛必须包含前端审批权限')
  assert.match(approvalDetailPage, /const resolveActionDisabledReason = \(mode: 'approve' \| 'reject'\) =>[\s\S]*detail\.value\.canApprove\s*!==\s*true[\s\S]*detail\.value\.canReject\s*!==\s*true/s, '详情页必须根据后端 canApprove/canReject 阻断动作')
  assert.match(approvalDetailPage, /const canApprove = computed\(\(\) => !resolveActionDisabledReason\('approve'\)\)/, '详情页通过按钮必须复用完整动作门槛')
  assert.match(approvalDetailPage, /const canReject = computed\(\(\) => !resolveActionDisabledReason\('reject'\)\)/, '详情页驳回按钮必须复用完整动作门槛')
  assert.match(approvalDetailPage, /const openActionDialog = \(mode: 'approve' \| 'reject'\) => \{[\s\S]*resolveActionDisabledReason\(mode\)[\s\S]*message\.error/, '详情页打开动作弹窗时必须阻断不允许动作')
  assert.match(approvalDetailPage, /const submitAction = async \(\) => \{[\s\S]*resolveActionDisabledReason\(actionMode\.value\)/s, '详情页提交动作必须再次复查动作门槛')
})

test('BDD: 动态菜单契约 -> eDHR 菜单 component 字符串能解析到 src/views 页面', () => {
  const routerHelper = readText('src/utils/routerHelper.ts')
  assert.match(routerHelper, /import\.meta\.glob\('\.\.\/views\/\*\*\/\*\.\{vue,tsx\}'\)/, '动态路由必须从 src/views 收集 Vue 页面')
  assert.match(routerHelper, /item\.includes\(componentPath\)/, 'system_menu.component 必须按 views 相对路径片段匹配')

  for (const componentPath of [
    'mes/pro/edhr-batch/BatchExecutionListPage',
    'mes/pro/edhr/ApprovalPage',
    'mes/pro/edhr/TrackingPage',
    'mes/pro/edhr/SignaturePage'
  ]) {
    assert.ok(
      fs.existsSync(path.join(root, 'src/views', `${componentPath}.vue`)),
      `system_menu.component=${componentPath} 必须能解析到 src/views/${componentPath}.vue`
    )
  }
})

test('BDD: 审批动作协议 -> 前端 payload 与后端 approve/reject VO 保持单一协议', () => {
  const approvalApi = readOptionalText('src/api/mes/pro/edhr/approval.ts')
  const approvalPage = readOptionalText('src/views/mes/pro/edhr/ApprovalPage.vue')
  const approvalDetailPage = readOptionalText('src/views/mes/pro/edhr/ApprovalDetailPage.vue')
  const controller = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordExecutionController.java')
  const approveReq = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionApproveReqVO.java')
  const rejectReq = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionRejectReqVO.java')
  const frontendActionSource = `${approvalApi}\n${approvalPage}\n${approvalDetailPage}`

  assert.match(approveReq, /private\s+Long\s+executionId;/, '后端 approve VO 必须使用 executionId 作为执行记录主键')
  assert.match(rejectReq, /private\s+Long\s+executionId;/, '后端 reject VO 必须使用 executionId 作为执行记录主键')
  assert.match(approveReq, /private\s+String\s+processInstanceId;[\s\S]*private\s+Long\s+approvalSnapshotId;[\s\S]*private\s+String\s+approvalSnapshotHash;[\s\S]*private\s+String\s+bpmTaskId;[\s\S]*private\s+String\s+password;/, '后端 approve VO 必须要求流程、快照、任务与密码字段')
  assert.match(rejectReq, /private\s+String\s+processInstanceId;[\s\S]*private\s+Long\s+approvalSnapshotId;[\s\S]*private\s+String\s+approvalSnapshotHash;[\s\S]*private\s+String\s+bpmTaskId;[\s\S]*private\s+String\s+password;[\s\S]*private\s+String\s+reason;/, '后端 reject VO 必须要求流程、快照、任务、密码与 reason')
  const actionResp = readBackendText('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionApprovalActionRespVO.java')
  assert.match(controller, /CommonResult<MesProBatchRecordExecutionApprovalActionRespVO>\s+approve/, '后端 approve 必须返回审批动作响应对象')
  assert.match(controller, /CommonResult<MesProBatchRecordExecutionApprovalActionRespVO>\s+reject/, '后端 reject 必须返回审批动作响应对象')
  for (const field of ['executionId', 'status', 'processInstanceId', 'bpmTaskId', 'signatureId', 'trackingEventId']) {
    assert.match(actionResp, new RegExp(`private\\s+[^;]+\\s+${field};`), `审批动作响应必须返回 ${field}`)
  }
  assert.match(approvalApi, /interface\s+EdhrApprovalActionReqVO[\s\S]*executionId:\s*number[\s\S]*processInstanceId:\s*string[\s\S]*approvalSnapshotId:\s*number[\s\S]*approvalSnapshotHash:\s*string[\s\S]*bpmTaskId:\s*string[\s\S]*password:\s*string/s, '前端 approve payload 必须发送 executionId、流程、快照、任务与密码')
  assert.match(approvalApi, /interface\s+EdhrRejectReqVO[\s\S]*reason:\s*string/s, '前端 reject payload 必须发送 reason')
  assert.match(approvalApi, /interface\s+EdhrApprovalActionRespVO[\s\S]*executionId:\s*number[\s\S]*status:\s*EdhrExecutionStatus[\s\S]*signatureId:\s*number[\s\S]*trackingEventId:\s*number/s, '前端必须声明审批动作响应必填字段')
  assert.match(approvalApi, /request\.put<EdhrApprovalActionRespVO>[\s\S]*\/mes\/pro\/batch-record-execution\/approve/s, '前端 approve helper 必须按动作响应对象处理')
  assert.match(approvalApi, /request\.put<EdhrApprovalActionRespVO>[\s\S]*\/mes\/pro\/batch-record-execution\/reject/s, '前端 reject helper 必须按动作响应对象处理')
  assert.doesNotMatch(frontendActionSource, /(^|\n)\s*id:\s*(currentRow|detail)|response\.status/, '前端审批动作不得发送旧 id 协议')
  assert.doesNotMatch(frontendActionSource, /(currentRow|detail)\.value[\s\S]{0,200}\sas\s+any/, '前端审批动作不得用 as any 掩盖必填字段缺失')
  assert.match(frontendActionSource, /result\.status\s*!==\s*EDHR_EXECUTION_STATUS\.APPROVED[\s\S]*后端未返回 APPROVED/, 'approve 响应必须显式校验 APPROVED')
  assert.match(frontendActionSource, /result\.status\s*!==\s*EDHR_EXECUTION_STATUS\.REJECTED[\s\S]*后端未返回 REJECTED/, 'reject 响应必须显式校验 REJECTED')
})
