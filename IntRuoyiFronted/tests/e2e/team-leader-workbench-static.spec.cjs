const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const realE2e = readUtf8('tests/e2e/team-leader-workbench-real-flow.e2e.js')

const requirePageMarker = (marker, message) => {
  assert.match(page, new RegExp(marker), message)
}

const requireApiEndpoint = (functionName, endpoint, message) => {
  assert.match(api, new RegExp(`${functionName}\\s*=\\s*async`), `${message}: missing function`)
  assert.match(api, new RegExp(endpoint.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${message}: missing endpoint`)
}

requirePageMarker('data-team-leader-report-workbench', 'page must expose a report confirmation workbench area')
requirePageMarker('data-team-leader-config-center', 'page must expose a team configuration center area')
requirePageMarker('报工确认工作台', 'page must use the business label 报工确认工作台')
requirePageMarker('班组配置中心', 'page must use the business label 班组配置中心')
requirePageMarker('data-team-leader-active-order-config', 'config center must include active order maintenance')
requirePageMarker('data-team-leader-active-order-work-order-code', 'active order maintenance must collect the formal production work order code through a remote dropdown.')
requirePageMarker('data-team-leader-employee-config', 'config center must include employee profile and process binding maintenance')
requirePageMarker('data-team-leader-device-config', 'config center must include equipment maintenance')
requirePageMarker('data-team-leader-parameter-config', 'config center must include equipment parameter maintenance')
requirePageMarker('data-team-leader-process-relation-config', 'config center must include process-employee/device/exception relations')
requirePageMarker('data-team-leader-active-order-select', 'abnormal reporting must select from active orders')
requirePageMarker('data-team-leader-defect-reason-select', 'abnormal reporting must select configured process defect reasons')
requirePageMarker('data-team-leader-structured-detail', 'submission detail must show structured report content')
requirePageMarker('data-team-leader-fifo-allocation', 'report workbench must keep FIFO allocation action')
requirePageMarker('data-team-leader-allocation-table', 'report workbench must keep manual allocation table')

assert.doesNotMatch(page, /label="提交看板"/, 'old top-level 提交看板 tab must be replaced by report workbench')
assert.doesNotMatch(page, /label="班组维护"/, 'old top-level 班组维护 tab must be replaced by config center')
assert.doesNotMatch(page, /label="生产工单ID"/, 'abnormal report must not be a primary hand-entered work order id form')
assert.doesNotMatch(page, /label="来源提交ID"/, 'abnormal report must not require hand-entered source submission id')
assert.doesNotMatch(page, /<template #header>员工工序绑定<\/template>/, 'old employee binding card must be replaced by employee profile and relation config')
assert.doesNotMatch(page, /<template #header>设备参数上下限<\/template>/, 'old parameter-only card must be replaced by full device and parameter config')
assert.doesNotMatch(
  page,
  /\n\s*<template>\s*\n\s*<ContentWrap v-if="loadError"/,
  'production leader body must not be wrapped in a bare HTML template element because browsers hide template contents.'
)

requireApiEndpoint('addTeamLeaderActiveOrder', '/active-order/add', 'active order add API')
requireApiEndpoint('searchTeamLeaderActiveOrderCandidates', '/active-order/candidates', 'active order candidate search API')
assert.match(
  api,
  /interface TeamLeaderActiveOrderAddReqVO\s*\{\s*workOrderId:\s*number\s*\}/,
  'active order add API payload must only include the selected workOrderId.'
)
assert.match(
  page,
  /addTeamLeaderActiveOrder\(\{\s*workOrderId:\s*await\s+requireSelectedActiveOrderCandidateWorkOrderId\(\)\s*\}\)/,
  'active order UI submit must send only a candidate-verified workOrderId and reject free-text/null selection before the API call.'
)
assert.match(
  page,
  /const\s+activeOrderCandidateKeyword\s*=\s*ref\(''\)[\s\S]*const\s+findActiveOrderCandidateByCode\s*=[\s\S]*candidate\.workOrderCode\.trim\(\)\s*===\s*workOrderCode\.trim\(\)/,
  'active order submit guard must track typed order-number text and match it against real candidates.'
)
assert.match(
  page,
  /const\s+resolveActiveOrderCandidateByKeyword\s*=\s*async\s*\(\)\s*=>[\s\S]*activeOrderCandidateKeyword\.value[\s\S]*searchTeamLeaderActiveOrderCandidates\(keyword\)[\s\S]*return findActiveOrderCandidateByCode\(keyword\)[\s\S]*const\s+requireSelectedActiveOrderCandidateWorkOrderId\s*=\s*async\s*\(\)\s*=>[\s\S]*await resolveActiveOrderCandidateByKeyword\(\)[\s\S]*throw new Error\('请选择订单号'\)[\s\S]*return requirePositiveNumber\(selectedCandidate\.workOrderId,\s*'请选择订单号'\)/,
  'active order submit guard must resolve exact typed text to a real candidate and still block unmatched free text.'
)
assert.doesNotMatch(
  page,
  /data-team-leader-active-order-route-id|data-team-leader-active-order-route-version-id|data-team-leader-active-order-transfer-ids|activeOrderForm\.(routeId|routeVersionId|transferIdsText)|parsePositiveIntegerList/,
  'active order add dialog must not expose route/version/transfer inputs or parsing logic.'
)
requireApiEndpoint('removeTeamLeaderActiveOrder', '/active-order/remove', 'active order remove API')
requireApiEndpoint('createTeamEmployeeProfile', '/employee-profile/create', 'employee profile API')
requireApiEndpoint('saveTeamProcessEmployeeBinding', '/process-employee-binding/save', 'process employee relation API')
requireApiEndpoint('createTeamDevice', '/team-device/create', 'team device create API')
requireApiEndpoint('updateTeamDeviceStatus', '/team-device/status/update', 'team device status API')
requireApiEndpoint('saveTeamProcessDeviceBinding', '/process-device-binding/save', 'process device relation API')
requireApiEndpoint('saveTeamRuntimeDeviceParameterRule', '/runtime-device-parameter-rule/save', 'runtime device parameter API')
requireApiEndpoint('saveTeamProcessDefectReason', '/process-defect-reason/save', 'process defect reason API')

assert.doesNotMatch(
  page,
  /catch\s*\([^)]*\)\s*\{\s*\}/,
  'page must not silently swallow request errors'
)

assert.match(
  realE2e,
  /WORKSPACE_ROOT\s*=\s*path\.resolve\(__dirname,\s*'\.\.\/\.\.\/\.\.'\)/,
  'real P6 E2E evidence paths must resolve from the workspace root, not from pnpm process.cwd().'
)
assert.match(
  realE2e,
  /EVIDENCE_FILE\s*=\s*path\.resolve\(\s*WORKSPACE_ROOT,\s*'doc',\s*'tasks',\s*TASK_ID,\s*'p6-real-e2e-evidence\.md'\s*\)/,
  'real P6 E2E must write Markdown evidence into the root task directory.'
)
assert.doesNotMatch(
  realE2e,
  /EVIDENCE_FILE\s*=\s*path\.resolve\(\s*process\.cwd\(\)/,
  'real P6 E2E must not write task evidence under IntRuoyiFronted/doc because pnpm changes cwd.'
)
assert.match(
  realE2e,
  /PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH/,
  'real P6 E2E must honor the approved local Chrome path from PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH.'
)
assert.match(
  realE2e,
  /executablePath:\s*chromiumExecutablePath/,
  'real P6 E2E must pass executablePath to chromium.launch when a local Chrome path is provided.'
)
assert.match(
  realE2e,
  /locator\(`\$\{selector\}:visible`\)\.first\(\)/,
  'real P6 E2E helper locators must target visible controls instead of hidden Element Plus duplicates.'
)
assert.match(
  realE2e,
  /async function selectLoginTenant/,
  'real P6 E2E must explicitly select the writable test tenant before login.'
)
assert.match(
  realE2e,
  /\.el-select input:visible/,
  'real P6 E2E must operate the visible Element Plus tenant selector on the login page.'
)
assert.match(
  realE2e,
  /clickFirst\(item,\s*\[\s*'\.el-select__wrapper'/,
  'real P6 E2E must open Element Plus form selects through the visible select wrapper, not the intercepted inner input.'
)
assert.match(
  realE2e,
  /async function fillFormItemForAction/,
  'real P6 E2E must fill duplicated labels by scoping each field to the form that owns the target action button.'
)
assert.match(
  realE2e,
  /async function selectRemoteFormItemOption[\s\S]*active-order\/candidates[\s\S]*candidate\.workOrderCode === optionText/,
  'real P6 E2E must choose active orders from the formal remote order-number candidates.'
)
assert.match(
  realE2e,
  /Object\.keys\(payload\)\.sort\(\)[\s\S]*\['workOrderId'\][\s\S]*Number\(payload\.workOrderId\)/,
  'real P6 E2E must assert the add-active-order request body contains only workOrderId.'
)
assert.doesNotMatch(
  realE2e,
  /fillFormItemForAction\(activeOrderDialog,\s*'加入活跃订单',\s*'(生产订单ID|路线ID|路线版本ID)'/,
  'real P6 E2E must not fill removed active-order route/version/id inputs.'
)
assert.match(
  realE2e,
  /getByRole\('button',\s*\{\s*name:\s*actionText\s*\}\)[\s\S]*locator\('xpath=ancestor::\*\[contains/,
  'real P6 E2E must resolve Element Plus form scope from the action button ancestor before filling duplicated labels like 状态 or 工序ID.'
)
assert.match(
  realE2e,
  /async function clickButtonAndWaitForSuccess/,
  'real P6 E2E must wait for the write API response and assert business success before recording UI write steps.'
)
assert.match(
  realE2e,
  /waitForResponse\(/,
  'real P6 E2E must capture the matching network response for each configuration write action.'
)
assert.match(
  realE2e,
  /strictEqual\(\s*body\.code,\s*0/,
  'real P6 E2E must assert CommonResult code=0 for configuration writes instead of treating any click as success.'
)
assert.match(
  realE2e,
  /\/mes\/pro\/feedback\/frontline\/submit/,
  'real P6 E2E must wait for the formal frontline submit response before treating employee reporting as complete.'
)
assert.match(
  realE2e,
  /\/mes\/pro\/process-pool\/team-leader\/submission\/allocation\/confirm/,
  'real P6 E2E must wait for the formal team-leader allocation confirm response before treating review as complete.'
)
assert.match(
  realE2e,
  /确认报工分配接口业务失败/,
  'real P6 E2E must assert the allocation confirm CommonResult business code instead of relying on a transient toast.'
)
assert.match(
  realE2e,
  /parseStoredAccessToken/,
  'real P6 E2E must parse the nested localStorage ACCESS_TOKEN wrapper before read-only trace verification.'
)
assert.match(
  realE2e,
  /parsed\.v\s*\|\|\s*parsed\.value\s*\|\|\s*parsed\.accessToken\s*\|\|\s*parsed\.token/,
  'real P6 E2E token parser must support the local storage wrapper value field used by this frontend.'
)
assert.doesNotMatch(
  realE2e,
  /await clickButton\(device,\s*'更新状态'\)/,
  'real P6 E2E must not click duplicated-label forms without waiting for the exact device-status write response.'
)
assert.doesNotMatch(
  realE2e,
  /fillFirst\(page,\s*\[\s*'input\[placeholder\*="租户"\]/,
  'real P6 E2E must not fill hidden tenant-name inputs as the login tenant selector.'
)

assert.match(
  realE2e,
  /assertTraceNumber\(\s*allocationBody\.data,\s*'totalAllocatedQuantity',\s*config\.outputQuantity/,
  'real P6 E2E must assert allocation trace totalAllocatedQuantity equals the submitted output quantity.'
)
assert.match(
  realE2e,
  /assertTraceValue\(\s*orderProcessBody\.data,\s*'completionStatus',\s*'COMPLETED'/,
  'real P6 E2E must assert the order-process trace reaches COMPLETED.'
)
assert.match(
  realE2e,
  /assertTraceValue\(\s*orderProcessBody\.data,\s*'backfillStatus',\s*'SUCCESS'/,
  'real P6 E2E must assert the order-process trace backfillStatus is SUCCESS.'
)
assert.match(
  realE2e,
  /assertTracePresent\(\s*orderProcessBody\.data,\s*'backfillExecutionId'/,
  'real P6 E2E must assert backfillExecutionId is present.'
)
assert.match(
  realE2e,
  /assertBatchRecordBackfillTrace\(\s*batchRecordBody\.data,\s*config/,
  'real P6 E2E must assert batch-record trace contains field audit or cell projection evidence.'
)
assert.match(
  realE2e,
  /discoverSubmittedEventId/,
  'real P6 E2E must dynamically discover the eventId created by the real employee submission.'
)
assert.match(
  realE2e,
  /submitDate:\s*resolveSubmissionQueryDate\(config\)/,
  'real P6 E2E must provide the required submitDate when querying the team-leader submission page.'
)
assert.match(
  realE2e,
  /resolveVerifyPath/,
  'real P6 E2E must resolve verification paths after dynamic eventId discovery.'
)
assert.match(
  realE2e,
  /__EVENT_ID__/,
  'real P6 E2E must support an explicit __EVENT_ID__ placeholder instead of requiring a pre-known eventId.'
)

console.log('PASS: team leader workbench static contract is wired')
