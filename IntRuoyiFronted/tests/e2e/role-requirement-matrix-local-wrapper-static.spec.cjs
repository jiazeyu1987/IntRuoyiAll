const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const wrapperPath = path.resolve(
  frontendRoot,
  '../doc/tasks/20260805-ac-m04-acceptance-sync/run-rrm-real-e2e-local.ps1'
)

assert.ok(fs.existsSync(wrapperPath), 'local RRM real E2E safety wrapper must exist.')

const source = fs.readFileSync(wrapperPath, 'utf8').replace(/\r\n/g, '\n')

for (const [id, username] of [
  ['512', 'huzonggang'],
  ['659', 'shangmengying'],
  ['964', 'liuyueyue'],
  ['1301', 'sunxiaoqing'],
  ['1520', 'lvyujie'],
  ['1618', 'zhengxiaofang'],
  ['910272', 'aoteman']
]) {
  assert.match(
    source,
    new RegExp(`['"]${id}['"]\\s*=\\s*['"]${username}['"]`),
    `wrapper must freeze RRM account ${id}/${username}.`
  )
}

for (const functionName of [
  'Read-RrmAccountSnapshot',
  'New-RrmTemporaryPassword',
  'New-BcryptHash',
  'Resolve-UnoccupiedSignatureIds',
  'Set-RrmTemporaryPassword',
  'Restore-RrmAccounts',
  'Set-RrmEnvironment',
  'Clear-RrmEnvironment'
]) {
  assert.match(source, new RegExp(`function\\s+${functionName}\\b`), `wrapper must define ${functionName}.`)
}

assert.match(
  source,
  /\[byte\[\]\]\s*\$randomBytes\s*=\s*New-Object\s+byte\[\]\s+12[\s\S]*RandomNumberGenerator\]::Create\(\)[\s\S]*\$randomNumberGenerator\.GetBytes\(\$randomBytes\)[\s\S]*\$randomNumberGenerator\.Dispose\(\)/,
  'temporary login password must use a Windows PowerShell-compatible cryptographic RNG.'
)
assert.doesNotMatch(
  source,
  /RandomNumberGenerator\]::GetBytes\(/,
  'temporary login password must not use the unsupported static GetBytes overload.'
)
assert.match(
  source,
  /BitConverter\]::ToString\(\$randomBytes\)\s*-replace\s*['"]-['"],\s*['"]{2}/,
  'temporary login password must use a Windows PowerShell-compatible byte-to-hex conversion.'
)
assert.doesNotMatch(
  source,
  /Convert\]::ToHexString\(/,
  'temporary login password must not use the unavailable Convert.ToHexString API.'
)
assert.match(
  source,
  /System\.getenv\("RRM_LOCAL_E2E_TEMP_PASSWORD"\)/,
  'BCrypt helper must read the temporary password from process memory.'
)
assert.match(
  source,
  /spring-security-crypto-[^'"]+\.jar[\s\S]*commons-logging-[^'"]+\.jar/,
  'BCrypt helper must use the existing backend Spring Security runtime dependency.'
)

assert.match(
  source,
  /START TRANSACTION;[\s\S]*FOR UPDATE;[\s\S]*UPDATE system_users[\s\S]*RRM_TEMP_UPDATE_ROWS=/,
  'temporary account update must lock, update, and expose a guarded row-count assertion.'
)
assert.match(
  source,
  /finally\s*\{[\s\S]*Restore-RrmAccounts\s+-Snapshot\s+\$accountSnapshot/,
  'the finally block must always restore the seven account snapshots.'
)

const restoreFunction = source.match(
  /function\s+Restore-RrmAccounts\b[\s\S]*?\n\}/
)?.[0] || ''
assert.match(restoreFunction, /UPDATE system_users[\s\S]*CASE id/, 'restore must write exact per-ID snapshots.')
assert.match(
  restoreFunction,
  /WHERE id IN \(\$targetIdList\);/,
  'restore must address all seven frozen IDs without an updater-marker predicate.'
)

for (const tableName of [
  'mes_pro_process_pool_event',
  'mes_pro_process_pool_event_revision',
  'mes_pro_process_pool_pqc_record',
  'mes_pro_process_pool_review_copy',
  'mes_pro_process_pool_submission_review',
  'mes_pro_batch_record_execution_signature',
  'mes_pro_edhr_batch_execution_signature',
  'bpm_approval_signature_record',
  'dcc_controlled_file_signature',
  'dcc_electronic_signature_image',
  'showroom_change_request_signature'
]) {
  assert.match(
    source,
    new RegExp(tableName),
    `signature allocation must check formal table ${tableName}.`
  )
}
assert.match(
  source,
  /99009100[\s\S]*99009199/,
  'signature IDs must come from the task-owned deterministic allocation block.'
)

assert.match(
  source,
  /SELECT CONCAT\('PQC_REVIEW_SCOPE=', COUNT\(\*\)\)[\s\S]*leader_user_id = 512[\s\S]*leader_type = 'PQC'[\s\S]*scope_type = 'EMPLOYEE'[\s\S]*employee_user_id = 914524[\s\S]*enabled = b'1'[\s\S]*tenant_id = 1[\s\S]*deleted = b'0'/,
  'database preflight must require the exact PQC leader-to-actual-employee review scope.'
)
assert.match(
  source,
  /@\{\s*Key = 'PQC_REVIEW_SCOPE';\s*Value = 1\s*\}/,
  'database preflight must fail unless exactly one PQC review scope exists.'
)
assert.match(
  source,
  /SELECT CONCAT\('PQC_SELECTED_TASK=', COUNT\(\*\)\)[\s\S]*id = 981941[\s\S]*code = 'RRM-20260805-PQC-922987'[\s\S]*work_order_id = 980008[\s\S]*workstation_id = 980009[\s\S]*route_id = 922119[\s\S]*process_id = 922987[\s\S]*item_id = 902149[\s\S]*quantity = 10\.000000[\s\S]*status = 0[\s\S]*tenant_id = 1[\s\S]*deleted = b'0'/,
  'database preflight must require the exact production task for the PQC-selected process.'
)
assert.match(
  source,
  /@\{\s*Key = 'PQC_SELECTED_TASK';\s*Value = 1\s*\}/,
  'database preflight must fail unless exactly one PQC-selected production task exists.'
)
assert.match(
  source,
  /SELECT CONCAT\('TRANSFER_TRACE_ACTIVE_ORDER=', COUNT\(\*\)\)[\s\S]*mes_pro_process_pool_active_order_transfer_trace trace[\s\S]*trace\.active_order_id = 12[\s\S]*trace\.source_type IS NOT NULL[\s\S]*trace\.source_status IS NOT NULL[\s\S]*trace\.quantity IS NOT NULL[\s\S]*trace\.material_stock_id IS NOT NULL[\s\S]*trace\.batch_id IS NOT NULL[\s\S]*trace\.idempotency_key IS NOT NULL/,
  'database preflight must require existing formal transfer trace rows for active order 12.'
)
assert.match(
  source,
  /@\{\s*Key = 'TRANSFER_TRACE_ACTIVE_ORDER';\s*Value = 2\s*\}/,
  'database preflight must fail unless the existing transfer trace active order has exactly two formal rows.'
)
assert.match(
  source,
  /['"]RRM_TRANSFER_TRACE_ACTIVE_ORDER_ID['"]/,
  'wrapper must save and restore RRM_TRANSFER_TRACE_ACTIVE_ORDER_ID.'
)
assert.match(
  source,
  /['"]RRM_TRANSFER_IDS['"]/,
  'wrapper must save and restore RRM_TRANSFER_IDS.'
)
assert.match(
  source,
  /\$env:RRM_TRANSFER_IDS\s*=\s*['"]1,2['"]/,
  'wrapper must inject the formal transfer IDs used by active-order joining.'
)
assert.match(
  source,
  /\$env:RRM_TRANSFER_TRACE_ACTIVE_ORDER_ID\s*=\s*['"]12['"]/,
  'wrapper must inject the existing formal transfer trace active order ID.'
)

for (const passwordKey of [
  'RRM_PRODUCTION_EMPLOYEE_PASSWORD',
  'RRM_PRODUCTION_LEADER_PASSWORD',
  'RRM_QA_PASSWORD',
  'RRM_PQC_INSPECTOR_PASSWORD',
  'RRM_PQC_LEADER_PASSWORD',
  'RRM_RELEASE_OWNER_PASSWORD',
  'RRM_UNAUTHORIZED_PASSWORD'
]) {
  assert.match(
    source,
    new RegExp(`\\$env:${passwordKey}\\s*=\\s*\\$TemporaryPassword`),
    `${passwordKey} must use the in-memory temporary password.`
  )
}

assert.match(
  source,
  /\$env:RRM_TENANT\s*=\s*-join\s*\(\[char\[\]\]\(0x828B,\s*0x9053,\s*0x6E90,\s*0x7801\)\)/,
  'the Chinese tenant name must be constructed from ASCII-safe Unicode code points.'
)

const checkIndex = source.indexOf('e2e:role-requirement-matrix:real:check')
const fullRealIndex = source.indexOf(
  "'e2e:role-requirement-matrix:real'",
  checkIndex + 1
)
assert.ok(checkIndex >= 0, 'wrapper must run real:check.')
assert.ok(
  fullRealIndex > checkIndex,
  'wrapper must run full real E2E only after real:check.'
)

const invokeRrmPnpmStart = source.indexOf('function Invoke-RrmPnpm')
const invokeRrmPnpmEnd = source.indexOf('\n$accountSnapshot', invokeRrmPnpmStart)
assert.ok(invokeRrmPnpmStart >= 0, 'wrapper must define Invoke-RrmPnpm.')
assert.ok(invokeRrmPnpmEnd > invokeRrmPnpmStart, 'wrapper must close Invoke-RrmPnpm before main execution.')
const invokeRrmPnpm = source.slice(invokeRrmPnpmStart, invokeRrmPnpmEnd)
assert.match(
  invokeRrmPnpm,
  /\$originalErrorActionPreference\s*=\s*\$ErrorActionPreference[\s\S]*?\$ErrorActionPreference\s*=\s*['"]Continue['"][\s\S]*?&\s+\$pnpmPath\s+\$ScriptName\s+2>&1\s*\|\s*ForEach-Object\s*\{\s*Write-Host\s+\$_\s*\}[\s\S]*?\$pnpmExitCode\s*=\s*\$LASTEXITCODE[\s\S]*?return\s+\$pnpmExitCode[\s\S]*?finally\s*\{[\s\S]*?\$ErrorActionPreference\s*=\s*\$originalErrorActionPreference/,
  'pnpm output must stream to the terminal and its exit code must be captured immediately.'
)
assert.doesNotMatch(
  invokeRrmPnpm,
  /return\s+\$LASTEXITCODE/,
  'Invoke-RrmPnpm must not defer reading LASTEXITCODE until the return statement.'
)

assert.doesNotMatch(
  source,
  /\b(Set-Content|Add-Content|Out-File|Export-Csv)\b[\s\S]{0,120}(password|credential|snapshot)/i,
  'credentials and account snapshots must not be written to files.'
)

console.log('PASS role-requirement-matrix local wrapper static contract')
