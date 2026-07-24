const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()

const read = (relativePath) => {
  const absolutePath = path.resolve(root, relativePath)
  assert(fs.existsSync(absolutePath), `${relativePath} must exist`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const assertIncludes = (source, needle, message) => {
  assert(source.includes(needle), message)
}

const flowConfigApi = read('src/api/mes/pro/route/flowconfig.ts')
assertIncludes(flowConfigApi, "export type ProRouteFlowRecordCategory", 'route flow API must type record categories.')
assertIncludes(flowConfigApi, "export type ProRouteFlowValidationProfile", 'route flow API must type validation profiles.')
assertIncludes(flowConfigApi, 'permissionScopeId?: number | null', 'route flow API must carry object permission scope id.')

const routeFlowConfigPanel = read('src/views/mes/pro/route/RouteFlowConfigPanel.vue')
assert(!routeFlowConfigPanel.includes('已启用但未绑定批记录表格'), 'route flow page must allow enabled process rows without a batch record report.')
assertIncludes(routeFlowConfigPanel, "BATCH_RECORD_CATEGORY: ProRouteFlowRecordCategory = 'BATCH_RECORD'", 'route flow page must fix batch record category.')
assertIncludes(routeFlowConfigPanel, "BATCH_RECORD_VALIDATION_PROFILE: ProRouteFlowValidationProfile = 'CONTROLLED_BATCH'", 'route flow page must fix controlled batch validation profile.')
assert(!routeFlowConfigPanel.includes('placeholder="权限范围ID"'), 'route flow page must hide internal permission scope id editing.')
assert(!routeFlowConfigPanel.includes('class="route-flow-config-panel-report-scope"'), 'route flow page must remove scope id input control.')
assert(!routeFlowConfigPanel.includes('内部记录表'), 'route flow page must not split batch record table into an internal record type.')
assert(!routeFlowConfigPanel.includes('availableValidationProfileOptions(report)'), 'route flow page must not expose validation profile filtering for a single batch record type.')
assertIncludes(routeFlowConfigPanel, 'recordCategory: BATCH_RECORD_CATEGORY', 'route flow save payload must submit the fixed batch record category.')
assertIncludes(routeFlowConfigPanel, 'validationProfile: BATCH_RECORD_VALIDATION_PROFILE', 'route flow save payload must submit the fixed validation profile.')

const batchExecutionApi = read('src/api/mes/pro/edhr/batchExecution.ts')
assertIncludes(batchExecutionApi, "export interface EdhrSignatureTimeReqVO", 'batch execution API must expose selected signature time request.')
assertIncludes(batchExecutionApi, 'signatureTime?: EdhrSignatureTimeReqVO', 'batch execution signature requests must include signatureTime.')
assertIncludes(batchExecutionApi, 'recordCategory?: EdhrRecordCategory', 'batch execution API must expose record category metadata.')
assertIncludes(batchExecutionApi, 'permissionScopeId?: number | null', 'batch execution API must expose permission scope metadata.')
assertIncludes(batchExecutionApi, 'workTaskId?: number', 'batch execution open response must expose the eDHR work task id.')

const batchExecutionDetailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
assertIncludes(batchExecutionDetailPage, 'opened.workTaskId', 'batch execution detail page must route by backend returned workTaskId.')
assertIncludes(batchExecutionDetailPage, 'opened.executionPageQuery?.workTaskId', 'batch execution detail page must preserve query workTaskId.')

const fieldAuditApi = read('src/api/mes/pro/edhr/fieldAudit.ts')
assertIncludes(fieldAuditApi, 'signatureTime?: EdhrSignatureTimeReqVO', 'field audit save request must include signatureTime.')

const approvalApi = read('src/api/mes/pro/edhr/approval.ts')
assertIncludes(approvalApi, 'signatureTime?: EdhrSignatureTimeReqVO', 'approval and reject requests must include signatureTime.')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
assertIncludes(executionPage, 'buildSignatureTimePayload', 'execution page must build the selected signature time payload.')
assertIncludes(executionPage, 'selectedSignedAt', 'execution page must let users choose a signature display time.')
assertIncludes(executionPage, 'selectedTimeReason', 'execution page must collect the selected time audit reason.')
assertIncludes(executionPage, 'recordCategoryLabel', 'execution page must display record category metadata.')
assert(!executionPage.includes('缺少权限范围'), 'execution page must not expose object permission scope as a filler-facing blocker.')

const signatureTimeTool = read('src/views/mes/pro/edhr/signatureTime.ts')
assertIncludes(signatureTimeTool, 'normalizeSelectedSignedAt', 'signature time payload must normalize date-picker text for backend LocalDateTime.')

const approvalPage = read('src/views/mes/pro/edhr/ApprovalPage.vue')
assertIncludes(approvalPage, "path: '/approval-center'", 'approval page must redirect to approval center.')
assertIncludes(approvalPage, "moduleCode: 'EDHR'", 'approval page redirect must preserve eDHR module context.')

const approvalDetailPage = read('src/views/mes/pro/edhr/ApprovalDetailPage.vue')
assertIncludes(approvalDetailPage, 'buildSignatureTimePayload', 'approval detail page must submit selected signature time payload.')

const signaturesPage = read('src/views/mes/pro/edhr/SignaturePage.vue')
assertIncludes(signaturesPage, 'selectedSignedAt', 'signature list must show user-selected signature time.')
assertIncludes(signaturesPage, 'signatureDisplayAt', 'signature list must show display signature time.')
assertIncludes(signaturesPage, 'selectedTimeAuditHash', 'signature list must show selected-time audit hash.')

const operationAuditApi = read('src/api/mes/pro/edhr/operationAudit.ts')
assertIncludes(operationAuditApi, '/mes/pro/edhr-operation-audit/page', 'operation audit API must call page endpoint.')
assertIncludes(operationAuditApi, 'permissionDecision?: EdhrOperationAuditPermissionDecision', 'operation audit API must expose permission decision.')

const operationAuditPage = read('src/views/mes/pro/edhr/OperationAuditPage.vue')
assertIncludes(operationAuditPage, 'EdhrOperationAuditApi.getPage', 'operation audit page must query backend audit API.')
assertIncludes(operationAuditPage, 'permissionDecision', 'operation audit page must filter permission decisions.')
assertIncludes(operationAuditPage, 'operationType', 'operation audit page must filter operation type.')
assertIncludes(operationAuditPage, 'operationResult', 'operation audit page must filter operation result.')

const permissionApi = read('src/api/mes/pro/edhr/permission.ts')
assertIncludes(permissionApi, '/mes/pro/edhr-permission-scopes/evaluate', 'permission API must call evaluate endpoint.')
assertIncludes(permissionApi, '/mes/pro/edhr-permission-scopes/get', 'permission API must call get endpoint.')
assertIncludes(permissionApi, '/mes/pro/edhr-permission-scopes/save', 'permission API must call save endpoint.')
assertIncludes(permissionApi, 'abilities: EdhrPermissionAbility[]', 'permission API must submit ability matrix.')
assertIncludes(permissionApi, 'rules: EdhrPermissionRuleSaveVO[]', 'permission API must save object permission rules.')

const permissionPage = read('src/views/mes/pro/edhr/PermissionMatrixPage.vue')
assertIncludes(permissionPage, 'EdhrPermissionApi.evaluate', 'permission matrix page must evaluate object permissions through backend.')
assertIncludes(permissionPage, 'EdhrPermissionApi.save', 'permission matrix page must save object permission rules.')
assertIncludes(permissionPage, 'EdhrPermissionApi.get', 'permission matrix page must load object permission rules.')
assertIncludes(permissionPage, 'abilityOptions', 'permission matrix page must expose object abilities.')
assertIncludes(permissionPage, 'permissionScopeId', 'permission matrix page must support scope id checks.')
assertIncludes(permissionPage, 'ruleRows', 'permission matrix page must render editable rule rows.')
assertIncludes(permissionPage, '保存规则', 'permission matrix page must expose a save rules action.')

const routerSource = read('src/router/modules/remaining.ts')
assertIncludes(routerSource, 'edhr-operation-audit', 'router must expose eDHR operation audit page.')
assertIncludes(routerSource, 'edhr-permission-matrix', 'router must expose eDHR permission matrix page.')

console.log('PASS: eDHR tail four goals frontend static contract')
