const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const panel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue').replace(/\r\n/g, '\n')
const context = read('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts').replace(/\r\n/g, '\n')
const feedbackApi = read('src/api/mes/pro/feedback/index.ts').replace(/\r\n/g, '\n')

const extractFunctionBlock = (source, name) => {
  const asyncStart = source.indexOf(`const ${name} = async`)
  const normalStart = source.indexOf(`const ${name} = (`)
  const exportAsyncStart = source.indexOf(`export const ${name} = async`)
  const start = [asyncStart, normalStart, exportAsyncStart]
    .filter((index) => index >= 0)
    .sort((a, b) => a - b)[0]
  assert.ok(start >= 0, `missing function ${name}`)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, `missing function body ${name}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex + 1, index)
      }
    }
  }
  assert.fail(`unterminated function ${name}`)
}

assert.match(
  context,
  /interface\s+FrontlineProductionRuntimeCacheEntry[\s\S]*runtimeConfig:\s*FrontlineRuntimeConfigVO[\s\S]*employeeOptions:\s*FrontlineEmployeeCandidateVO\[\]/,
  'production runtime cache entries must contain only lightweight runtime config and employee candidates.'
)
assert.match(
  context,
  /productionRuntimeCache:\s*FrontlineProductionRuntimeCache/,
  'frontline state must own an in-memory production runtime cache.'
)
assert.doesNotMatch(
  context,
  /localStorage|sessionStorage|indexedDB/,
  'production runtime cache must stay in memory and must not persist stale business state.'
)
assert.doesNotMatch(
  context,
  /formBindings|batchRecordFormNames|batchRecordReport|attachment|fileContent|draft/,
  'production runtime cache must not cache form slot data, batch-record content, attachments, or drafts.'
)
assert.match(
  feedbackApi,
  /export interface FrontlineRuntimeConfigVO \{[\s\S]*employeeSwitchSnapshots:\s*FrontlineSwitchActualEmployeeRespVO\[\][\s\S]*\}/,
  'runtime-config response must expose formal switch snapshots for every employee.'
)

const preloadBlock = extractFunctionBlock(context, 'preloadFrontlineProductionRuntimeCache')
assert.match(
  preloadBlock,
  /Promise\.all\(/,
  'maximum-entry preload must fail fast when any formal runtime-config request fails.'
)
assert.doesNotMatch(
  preloadBlock,
  /Promise\.allSettled|catch\s*\(\s*\)\s*\{/,
  'maximum-entry preload must not silently swallow failed runtime-config requests.'
)
assert.match(
  preloadBlock,
  /requireFrontlineProcessActiveOrderId\(process\)[\s\S]*ProFeedbackApi\.getFrontlineRuntimeConfig\(\{[\s\S]*activeOrderId[\s\S]*routeId:\s*process\.routeId[\s\S]*routeProcessId:\s*process\.routeProcessId[\s\S]*processId:\s*process\.processId[\s\S]*\}\)/,
  'preload must use the formal runtime-config API for each route process.'
)
assert.doesNotMatch(
  preloadBlock,
  /switchFrontlineActualEmployee|ProFeedbackApi\.switchFrontlineActualEmployee/,
  'maximum-entry preload must not batch-call the context-changing employee switch POST.'
)
assert.match(
  preloadBlock,
  /state\.lastError\s*=\s*resolveFrontlineErrorMessage\(error\)[\s\S]*throw error/,
  'preload failure must be visible through formal error state and re-thrown.'
)

const cacheRuntimeBlock = extractFunctionBlock(context, 'cacheFrontlineRuntimeConfig')
assert.match(
  cacheRuntimeBlock,
  /requireFrontlineProcessActiveOrderId\(process\)[\s\S]*runtimeConfig\.employeeSwitchSnapshots\.forEach[\s\S]*cacheFrontlineEmployeeSwitchResult\(/,
  'runtime-config caching must cache every formal employee switch snapshot.'
)
assert.match(
  cacheRuntimeBlock,
  /actualEmployeeId:\s*snapshot\.actualEmployeeId/,
  'each employee switch snapshot must be cached by the same process and employee cache key used by manual switching.'
)

const selectProcessBlock = extractFunctionBlock(context, 'selectFrontlineProcess')
assert.match(
  selectProcessBlock,
  /const requestToken = \+\+state\.processSelectionRequestToken/,
  'process switching must create a request token to protect against stale responses.'
)
assert.match(
  selectProcessBlock,
  /readFrontlineRuntimeConfigCache\(state,\s*process\)[\s\S]*applyFrontlineRuntimeConfig\(state,\s*process,\s*cachedRuntimeConfig\.runtimeConfig/,
  'process switching must apply cached runtime config before making another request.'
)
assert.match(
  selectProcessBlock,
  /ProFeedbackApi\.getFrontlineRuntimeConfig/,
  'process switching must still use the formal runtime-config API on cache miss.'
)
assert.match(
  selectProcessBlock,
  /if\s*\(state\.processSelectionRequestToken !== requestToken\)/,
  'process switching must ignore stale runtime-config responses.'
)

const switchEmployeeBlock = extractFunctionBlock(context, 'switchFrontlineActualEmployee')
assert.match(
  switchEmployeeBlock,
  /const requestToken = \+\+state\.employeeSwitchRequestToken/,
  'employee switching must create a request token to protect against stale template responses.'
)
assert.match(
  switchEmployeeBlock,
  /readFrontlineEmployeeSwitchCache\(state,\s*payload\)[\s\S]*applyFrontlineEmployeeSwitchResult\(state,\s*cachedSwitch\.result/,
  'employee switching must reuse a prior successful formal switch result for the same process and employee.'
)
assert.match(
  switchEmployeeBlock,
  /ProFeedbackApi\.switchFrontlineActualEmployee/,
  'employee switching must still use the formal switch API on cache miss.'
)
assert.match(
  switchEmployeeBlock,
  /cacheFrontlineEmployeeSwitchResult\(state,\s*payload,\s*result\)/,
  'successful formal employee switches must be cached for later fast switching.'
)
assert.match(
  switchEmployeeBlock,
  /if\s*\(state\.employeeSwitchRequestToken !== requestToken\)/,
  'employee switching must ignore stale template responses.'
)

assert.match(
  panel,
  /preloadFrontlineProductionRuntimeCache/,
  'production panel must import and call the runtime cache preloader.'
)
const preloadPanelBlock = extractFunctionBlock(panel, 'preloadProductionRuntimeCacheForFullscreen')
assert.match(
  preloadPanelBlock,
  /preloadFrontlineProductionRuntimeCache\(\s*deviceState,\s*switchableProcessOptions\.value\.filter\(isFrontlineProductionProcess\)\s*\)/,
  'production fullscreen preload must target the current switchable production process list.'
)
assert.doesNotMatch(
  preloadPanelBlock,
  /isFrontlinePqcProcess|loadFrontlinePqcActiveOrders/,
  'production fullscreen preload must not traverse PQC orders or PQC process choices.'
)
const fullscreenToggleBlock = extractFunctionBlock(panel, 'handleProductionFullscreenToggle')
assert.match(
  fullscreenToggleBlock,
  /await enterProductionFullscreen\(\)[\s\S]*await preloadProductionRuntimeCacheForFullscreen\(\)/,
  'clicking 最大化 must enter fullscreen and immediately preload production runtime cache.'
)
assert.doesNotMatch(
  fullscreenToggleBlock,
  /preloadProductionRuntimeCacheForFullscreen\(\)\.catch|void preloadProductionRuntimeCacheForFullscreen/,
  'maximum-entry preload must not be fire-and-forget because errors must stay visible.'
)

console.log('PASS: frontline production maximum runtime cache static contract')
