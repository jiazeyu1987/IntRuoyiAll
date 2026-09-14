const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const backendConfig = readUtf8(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderProcessConfigServiceImpl.java'
)
const backendOverage = readUtf8(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOverageLimitServiceImpl.java'
)

const assertIncludes = (source, expected, message) => assert(source.includes(expected), message)
const assertMatches = (source, pattern, message) => assert.match(source, pattern, message)

assertIncludes(
  page,
  'data-team-leader-process-config-detail',
  'Process config rows must expose a stable detail action selector.'
)
assertMatches(
  page,
  /@click="openProcessConfigDetailDialog\(row\)"/,
  'Detail action must open the current row detail dialog.'
)
assertIncludes(
  page,
  'data-team-leader-process-config-detail-dialog',
  'Process config detail dialog must expose a stable selector.'
)
assertMatches(page, /title="工序详情"/, 'Detail dialog must have a descriptive title.')
assertMatches(page, /processConfigDetailVisible/, 'Detail dialog visibility must be state-owned.')
assertMatches(
  page,
  /processConfigDetailRow/,
  'Detail dialog must use the selected process config row.'
)
assertMatches(
  page,
  /v-for="reason in processConfigDetailRow\.lossReasons"/,
  'Detail must list all loss reasons.'
)
assertMatches(
  page,
  /v-for="device in processConfigDetailRow\.devices"/,
  'Detail must list all mapped devices.'
)
assertMatches(
  page,
  /v-for="parameter in device\.parameters"/,
  'Detail must list device parameters.'
)
assertMatches(
  page,
  /class="[^"]*team-leader-workbench__process-config-table-cell-list/,
  'Process config table cells must use the compact single-line layout class.'
 )
assertMatches(
  page,
  /class="team-leader-workbench__process-config-actions team-leader-workbench__process-config-table-actions"/,
  'Process config table actions must stay on one line.'
 )
assertMatches(
  page,
  /\.team-leader-workbench__process-config-table-cell-list[\s\S]*white-space:\s*nowrap/,
  'Process config table cells must prevent row expansion from wrapping.'
)
assertMatches(
  page,
  /\.team-leader-workbench__loss-reasons[\s\S]*flex-wrap:\s*nowrap[\s\S]*white-space:\s*nowrap/,
  'Loss reason tags must remain on one compact table row.'
)
assertMatches(
  page,
  /team-leader-workbench__pqc-management-table/,
  'PQC management table must have a dedicated compact layout class.'
)
assertMatches(
  page,
  /\.team-leader-workbench__pqc-management-table[\s\S]*\.team-leader-workbench__structured-list[\s\S]*flex-wrap:\s*nowrap[\s\S]*white-space:\s*nowrap/,
  'PQC management cells must prevent multi-value wrapping.'
)
assertMatches(
  page,
  /team-leader-workbench__pqc-management-actions/,
  'PQC management actions must use a compact single-line layout.'
)
assertMatches(
  page,
  /resolveProcessConfigOveragePercent\(processConfigDetailRow\)/,
  'Detail must display the formal overage percentage.'
)
assertMatches(
  page,
  /DEFAULT_PROCESS_CONFIG_OVERAGE_PERCENT|DEFAULT_OVERAGE_PERCENT|10/,
  'Frontend must define the explicit 10% default.'
)
assertIncludes(
  api,
  'overagePercent?: number | string | null',
  'API row must preserve nullable persisted overage value.'
)
assertMatches(
  backendConfig,
  /setOveragePercent\(overageLimitService\.findPercent\(/,
  'Backend process config rows must read the formal overage service value.'
)
assertMatches(
  backendOverage,
  /DEFAULT_OVERAGE_PERCENT|new BigDecimal\("10"\)/,
  'Backend overage validation path must use the explicit 10% default.'
)

console.log('team-leader-process-config-detail-default-overage-static PASS')
