const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
module.exports.loadUploadDepartments = () => {
  const c = { exports: {}, Error }
  const file = path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/signoff-departments.ts')
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return c.exports
}
module.exports.loadUploadMatrix = () => {
  const c = { exports: {}, Error }
  const file = path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/approval-matrix.ts')
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c)
  return c.exports
}

module.exports.matrixFixture = (scope = {}, version = 1) => {
  const node = { id: '40', stageNo: 1, stageCode: 'MATRIX_REVIEW', stageName: '会签', stageOrder: 1, sort: 1,
    candidateSourceType: 'DEPT', candidateSourceId: '7', candidateSourceIds: '7', approveMethod: 'ALL', approveRatio: 100,
    requireAllApprovals: true, required: true, stageType: 'SIGNOFF', subjectLabel: '质量', marker: '●', subjectType: 'DEPT',
    subjectId: '7', subjectName: '质量', subjectDepartmentPath: '质量', ruleRemark: null }
  const approvalIds = scope.approvalIds || ['9']
  const approval = { ...node, id: '41', stageNo: 3, stageCode: 'MATRIX_APPROVAL', stageName: '批准', stageType: 'APPROVAL',
    subjectType: 'USER', subjectId: approvalIds[0], subjectName: '正式批准主体', subjectDepartmentPath: '正式批准部门',
    candidateSourceType: 'USER', candidateSourceId: approvalIds[0], candidateSourceIds: approvalIds.join(',') }
  return { formatVersion: 'DCC_APPROVAL_MATRIX_BASELINE_V2', tenantId: String(scope.tenantId || '1'),
    categoryId: String(scope.categoryId || '2'), fileTypeTaxonomyId: String(scope.taxonomyId || '4'), routeId: '9007199254740993',
    routeVersionNo: version, actionType: 'LEGACY', effectiveTime: '2026-10-11 00:00:00.000000000', routeRemark: null,
    matrixDigest: String(version).repeat(64), nodes: [node, approval] }
}
module.exports.matrixReadiness = (readiness, scope = {}, beforeVersion = 1, afterVersion = 1) => {
  const before = module.exports.matrixFixture(scope, beforeVersion), after = module.exports.matrixFixture(scope, afterVersion)
  const approval = (readiness.nodes || []).find(node => node.stageCode === 'MATRIX_APPROVAL')
  let runtimeRuleGroups = []
  if (approval) {
    const raw = after.nodes.find(node => node.stageType === 'APPROVAL')
    const candidateIds = raw.candidateSourceIds.split(',')
    runtimeRuleGroups = [{ sourceNodeId: '41', stageType: raw.stageType, marker: raw.marker, required: raw.required, subjectType: raw.subjectType,
      candidateSourceType: raw.candidateSourceType, candidateSourceIds: candidateIds, approveMethod: raw.approveMethod, approveRatio: raw.approveRatio,
      requireAllApprovals: raw.requireAllApprovals, applied: true }]
    readiness = { ...readiness, nodes: readiness.nodes.map(node => node === approval ? { ...node,
      candidateSourceType: raw.candidateSourceType, candidateSourceIds: candidateIds, approveRatio: raw.approveRatio, requireAllApprovals: raw.requireAllApprovals } : node) }
  }
  return { ...(scope.actionType !== 'OBSOLETE' && readiness.ready === true ? { supervisorSourceDigest:'c'.repeat(64), supervisorSourceToken:'fixture-server-supervisor-source' } : {}),
    ...readiness, routeId: after.routeId, routeVersionNo: after.routeVersionNo, matrixDigest: after.matrixDigest,
    derivationVersion: 'DCC_PUBLIC_MATRIX_NATIVE_V1', applicationActionType: scope.actionType || 'NEW',
    approvalMatrix: after, matrixBefore: before, matrixAfter: after, matrixChangeRequired: beforeVersion !== afterVersion,
    executionContract: 'PUBLIC_MATRIX_NATIVE_V1', preparedProcessDefinitionId: 'dcc-controlled-file-' + (scope.actionType === 'REVISION' ? 'revision' : 'upload') + ':5:fixture-exact', preparedDefinitionDigest: 'a'.repeat(64),
    runtimeRuleGroups, runtimeCapabilityLimitations: [],
    approvalMatrixBaselineToken: 'fixture-server-baseline', approvalMatrixConfirmationToken: beforeVersion !== afterVersion ? `fixture-confirm-${afterVersion}` : null }
}
module.exports.matrixSelection = () => ({ executionContract: 'PUBLIC_MATRIX_NATIVE_V1', expectedProcessDefinitionId: 'dcc-controlled-file-upload:5:fixture-exact', expectedDefinitionDigest: 'a'.repeat(64), expectedSupervisorSourceDigest:'c'.repeat(64),supervisorSourceToken:'fixture-server-supervisor-source',expectedApprovalRouteId: '9007199254740993', expectedApprovalRouteVersionNo: 1,
  expectedApprovalMatrixDigest: '1'.repeat(64), approvalMatrixBaselineToken: 'fixture-server-baseline',
  approvalMatrixConfirmationToken: null, matrixChangeConfirmed: false, applicationContextKey: 'fixture-scope' })
module.exports.matrixRejection = (scope = {}, reason = 'APPROVAL_MATRIX_EXPECTATION_STALE', beforeVersion = 1, afterVersion = 3) => {
  const comparison = module.exports.matrixReadiness({}, scope, beforeVersion, afterVersion)
  const error = new Error(reason + '：审批配置已变化，请刷新正式预检并确认本次配置')
  error.code = reason === 'APPROVAL_MATRIX_EXPECTATION_STALE' ? 1080000363 : 1080000364
  error.details = { reasonCode: reason, matrixBefore: comparison.matrixBefore, matrixAfter: comparison.matrixAfter,
    approvalMatrixBaselineToken: comparison.approvalMatrixBaselineToken, approvalMatrixConfirmationToken: comparison.approvalMatrixConfirmationToken }
  return error
}
