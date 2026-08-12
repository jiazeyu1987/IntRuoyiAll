const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')
const realE2e = readUtf8('tests/e2e/team-leader-responsible-routes-real.e2e.js')

const flatPageHeader = page.match(
  /<ContentWrap\s+v-if="!showPqcModuleTabs && !showProductionModuleTabs">[\s\S]*?<\/ContentWrap>/
)
assert.ok(flatPageHeader, 'flat production leader page header must exist.')
assert.match(
  flatPageHeader[0],
  /v-if="isProductionLeader"[\s\S]*data-production-leader-responsible-routes/,
  'flat production leader mode must render the responsible-route header without requiring module tabs.'
)
assert.doesNotMatch(
  flatPageHeader[0],
  /data-production-leader-responsible-routes[\s\S]*?v-if="showProductionModuleTabs"|v-if="showProductionModuleTabs"[\s\S]*?data-production-leader-responsible-routes/,
  'flat responsible-route header must not be gated by showProductionModuleTabs.'
)

assert.match(
  api,
  /interface\s+TeamLeaderResponsibleRouteRespVO\s*\{[\s\S]*routeId:\s*number[\s\S]*routeCode\?:\s*string[\s\S]*routeName:\s*string[\s\S]*\}/,
  'responsible-route API must expose an independent typed route summary.'
)
assert.match(
  api,
  /getTeamLeaderResponsibleRouteList\s*=\s*async[\s\S]*\/responsible-routes/,
  'responsible-route API must use the formal responsibility endpoint.'
)
assert.match(
  page,
  /getTeamLeaderResponsibleRouteList/,
  'production leader workbench must import and call the formal responsibility API.'
)
assert.match(
  page,
  /const\s+responsibleRouteRows\s*=\s*ref<TeamLeaderResponsibleRouteRespVO\[\]>\(\[\]\)/,
  'responsible routes must have state independent from process-config maintenance rows.'
)
assert.match(
  page,
  /const\s+responsibleRouteLoading\s*=\s*ref\(false\)/,
  'responsible routes must expose their own loading state.'
)

const responsibleNames = page.match(
  /const\s+productionResponsibleRouteNames\s*=\s*computed\(\(\)\s*=>\s*\{[\s\S]*?\n\}\)/
)
assert.ok(responsibleNames, 'responsible route name computation must exist.')
assert.match(
  responsibleNames[0],
  /responsibleRouteRows\.value/,
  'responsible route names must come from the formal responsibility rows.'
)
assert.doesNotMatch(
  responsibleNames[0],
  /processConfigRows\.value/,
  'responsible route names must not reuse the all-route maintenance list.'
)
assert.doesNotMatch(
  page,
  /processConfigLoading\s*\?\s*'工艺路线加载中'/,
  'responsible route header must not use process-config loading state.'
)
assert.match(
  page,
  /responsibleRouteLoading\s*\?\s*'工艺路线加载中'\s*:\s*'暂无负责工艺路线'/,
  'responsible route header must use the independent loading state.'
)
assert.match(
  page,
  /const\s+loadResponsibleRoutes\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*responsibleRouteRows\.value\s*=\s*await\s+getTeamLeaderResponsibleRouteList\(\)[\s\S]*responsibleRouteRows\.value\s*=\s*\[\][\s\S]*throw\s+error[\s\S]*responsibleRouteLoading\.value\s*=\s*false[\s\S]*\}/,
  'responsible route load failure must clear formal state and propagate the error without fallback.'
)
assert.ok(
  (page.match(/loadResponsibleRoutes\(\)\.catch/g) || []).length >= 2,
  'responsible routes must load both on mount and when switching to the production leader tab.'
)
assert.match(
  page,
  /loadResponsibleRoutes\(\)\.catch\(\(error\)\s*=>\s*\{[\s\S]*负责工艺路线加载失败[\s\S]*\}\)/,
  'responsible route failures must be shown explicitly to the user.'
)
assert.match(
  realE2e,
  /assert\.deepEqual\(visibleRouteNames,\s*apiRouteNames,[\s\S]*visible route tags must follow responsible-routes/,
  'real E2E must render the current API responsible-route names after stale route ids are removed.'
)
assert.match(
  realE2e,
  /assert\.deepEqual\(processConfigRouteNames,\s*apiRouteNames,[\s\S]*process-config rows must come only from formal responsible routes/,
  'real E2E must assert process-config route names equal the formal responsible-route names.'
)
assert.match(
  realE2e,
  /\/mes\/pro\/process-pool\/production-leader/,
  'real E2E must navigate through the visible production leader workbench route.'
)
assert.match(
  realE2e,
  /forbiddenRouteIds\s*=\s*\[980091\]/,
  'real E2E must explicitly reject the deleted route id 980091.'
)
assert.match(
  realE2e,
  /assert\.ok\(responsibleRoutes\.length\s*>\s*0/,
  'real E2E must prove remaining valid responsible routes still load.'
)
assert.match(
  realE2e,
  /Number\(route\.routeId\)\s*===\s*forbiddenRouteId/,
  'real E2E must compare responsible route ids numerically when rejecting deleted routes.'
)
assert.match(
  realE2e,
  /getByRole\('tab',[\s\S]*name:\s*'工序配置',[\s\S]*exact:\s*true[\s\S]*\.click\(\)/,
  'real E2E must click the visible process-config tab control instead of its hidden tab pane.'
)
assert.doesNotMatch(
  realE2e,
  /按压式球囊扩充压力泵/,
  'real E2E must not keep the deleted pressure-pump route as an expected visible route.'
)
assert.doesNotMatch(
  realE2e,
  /maintainableRouteCount|hasNonResponsibilityMaintainableRoute|maintenance scope must remain broader/,
  'real E2E must not preserve the old admin maintenance-all-routes expectation.'
)

console.log('team leader responsible routes static contract passed')
