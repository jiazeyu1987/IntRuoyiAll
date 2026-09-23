const fs = require('fs')
const path = require('path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

function assertNotContains(source, forbidden, label) {
  if (source.includes(forbidden)) {
    throw new Error(`forbidden ${label}: ${forbidden}`)
  }
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')

function declaration(source, name, exported = false) {
  const prefix = exported ? 'export const ' : 'const '
  const start = source.indexOf(`${prefix}${name} =`)
  assert.notEqual(start, -1, `missing declaration: ${name}`)
  const rest = source.slice(start)
  const next = rest.slice(prefix.length).search(/^(?:export )?const /m)
  return next < 0 ? rest : rest.slice(0, next + prefix.length)
}

function workflowButton(handler, label, state) {
  const button = [...page.matchAll(/<el-button\b[\s\S]*?<\/el-button>/g)]
    .map((match) => match[0])
    .find((markup) => markup.includes(`@click="${handler}${state ? '(row)' : ''}"`))
  assert.ok(button, `missing workflow button: ${handler}`)
  assertContains(button, label, `${handler} label`)
  assertContains(button, 'workflowBusy', `${handler} duplicate-submit guard`)
  assertContains(button, '!workflowReason.trim()', `${handler} reason guard`)
  if (state) assertContains(button, `row.state === '${state}'`, `${handler} state gate`)
}

function workflowAction(handler, apiMethod, route, state) {
  const body = declaration(page, handler)
  assertContains(body, `RuntimeControlApi.${apiMethod}(`, `${handler} API binding`)
  assertContains(body, 'workflowReason.value.trim()', `${handler} reason payload`)
  assertContains(body, 'reportActionError(error)', `${handler} error feedback`)
  if (state) {
    assertContains(body, `workflow.state !== '${state}'`, `${handler} execution state gate`)
    assertContains(body, 'workflow.workflowId', `${handler} persisted workflow identity`)
  }
  const endpoint = declaration(api, apiMethod, true)
  assertContains(endpoint, 'request.post<', `${apiMethod} POST contract`)
  assertContains(endpoint, '/infra/runtime-control/release-workflows', `${apiMethod} workflow API`)
  assertContains(endpoint, route, `${apiMethod} route`)
  if (state) assertContains(endpoint, 'encodeURIComponent(workflowId)', `${apiMethod} identity route`)
  return body
}

assertContains(api, '/infra/runtime-control/actions/preview', 'action preview API')
assertContains(api, '/infra/runtime-control/actions', 'action execute API')
assertContains(api, '/infra/runtime-control/release-status', 'release status API')
assertContains(api, '/infra/runtime-control/rollback-candidates', 'rollback candidates API')
assertContains(api, '/infra/runtime-control/restore-candidates', 'restore candidates API')

// Release actions now belong to persisted workflows, not operationActions.
workflowButton('createReleaseWorkflow', '构建发布包')
workflowButton('publishWorkflowToTest', '部署测试服', 'READY')
workflowButton('acceptWorkflowTest', '标记测试通过', 'TEST_DEPLOYED')
workflowButton('previewProductionPromotion', '晋级正式服', 'TESTED')

const create = workflowAction('createReleaseWorkflow', 'createRuntimeControlReleaseWorkflow', "url: '/infra/runtime-control/release-workflows'")
assertContains(create, '!canOperate.value', 'build permission gate')
assertContains(create, 'getRuntimeControlReleaseWorkflowCreationContext()', 'server-selected source')
assertContains(create, 'sourceSelectionId: context.sourceSelectionId', 'fixed source payload')
workflowAction('publishWorkflowToTest', 'publishRuntimeControlReleaseWorkflowToTest', '/publish-test', 'READY')
workflowAction('acceptWorkflowTest', 'acceptRuntimeControlReleaseWorkflowTest', '/test-acceptance', 'TEST_DEPLOYED')
assertContains(declaration(api, 'acceptRuntimeControlReleaseWorkflowTest', true), "data: { result: 'PASS', conclusion }", 'test acceptance result and conclusion')
const preview = workflowAction('previewProductionPromotion', 'previewRuntimeControlReleaseWorkflowProduction', '/production-preview', 'TESTED')
assertContains(preview, '!canPromoteProd.value', 'production permission gate')
assertContains(preview, 'workflow.stateVersion', 'production version binding')

const promote = workflowAction('confirmProductionPromotion', 'promoteRuntimeControlReleaseWorkflowProduction', '/promote-prod')
assertContains(page, '@click="confirmProductionPromotion"', 'production confirmation button')
assertContains(promote, "productionDialog.confirmText !== 'PROD'", 'production confirmation gate')
assertContains(promote, '!preview?.eligible', 'production eligibility gate')
assertContains(promote, 'authorizeRuntimeControlReleaseWorkflowProduction(', 'server authorization')
for (const token of ['authorization.grantId', 'preview.previewId', 'preview.expectedStateVersion', 'productionDialog.idempotencyKey', 'productionDialog.workflowId']) {
  assertContains(promote, token, 'production authorization binding')
}
assertNotContains(promote, 'createRuntimeControlReleaseWorkflow', 'rebuild during production promotion')
assertContains(page, 'PROD', 'production confirmation literal')
assertContains(page, 'prodConfirmText', 'production confirmation payload')
assertContains(page, 'releaseTag', 'releaseTag payload')
assertContains(page, 'publishScope', 'publish scope payload')
assertContains(page, 'selectedRecoverySetCandidateId', 'tested recovery set payload')
assertContains(page, 'testConclusion', 'test conclusion payload')

assertNotContains(page, 'build-release publish-test', 'combined rebuild on promote')
assertNotContains(page, '重新构建正式服', 'production promote rebuild copy')

console.log('PASS: runtime-control one-button release static contract is wired')
