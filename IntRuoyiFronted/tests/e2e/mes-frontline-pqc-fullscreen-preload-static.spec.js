const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const contextSource = read('src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')

function blockAfter(source, marker) {
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `missing marker: ${marker}`)
  const openIndex = source.indexOf('{', markerIndex)
  assert.notEqual(openIndex, -1, `missing function body for marker: ${marker}`)
  let depth = 0
  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(openIndex, index + 1)
      }
    }
  }
  assert.fail(`unterminated function body for marker: ${marker}`)
}

const fullscreenToggleBlock = blockAfter(panelSource, 'const handlePqcFullscreenToggle = async () =>')
const preloadBlock = blockAfter(contextSource, 'export const preloadFrontlinePqcSwitchingCache = async')
const activeOrderSelectBlock = blockAfter(contextSource, 'export const selectFrontlinePqcActiveOrder = async')
const pqcProcessSelectBlock = blockAfter(contextSource, 'export const selectFrontlinePqcProcess = async')

assert.match(
  contextSource,
  /pqcProcessOptionsCache:\s*Map<string,\s*FrontlineDeviceRouteProcessVO\[]>/,
  'PQC state must keep a typed process-list cache per active work order and route.'
)
assert.match(
  contextSource,
  /pqcProcessOptionsRequests:\s*Map<string,\s*Promise<FrontlineDeviceRouteProcessVO\[]>>/,
  'PQC state must coalesce in-flight process preload requests by active order cache key.'
)
assert.match(
  contextSource,
  /pqcEmployeeOptionsCache\?:\s*FrontlineEmployeeCandidateVO\[]/,
  'PQC state must cache the formal PQC personnel list because it is shared across process switches.'
)
assert.match(
  contextSource,
  /pqcEmployeeOptionsRequest\?:\s*Promise<FrontlineEmployeeCandidateVO\[]>/,
  'PQC state must coalesce in-flight personnel preload requests.'
)

assert.match(
  preloadBlock,
  /loadFrontlinePqcActiveOrders\(state\)/,
  'Fullscreen preload must refresh or load the formal active-order list before preloading dependent data.'
)
assert.match(
  preloadBlock,
  /Promise\.all\([\s\S]*activeOrders\.map[\s\S]*getFrontlinePqcActiveOrderProcesses/,
  'Fullscreen preload must request every active order process list in parallel.'
)
assert.match(
  preloadBlock,
  /getFrontlinePqcEmployeeCandidates/,
  'Fullscreen preload must request the formal PQC personnel list once.'
)
assert.doesNotMatch(
  preloadBlock,
  /switchFrontlinePqcActualEmployee|switch-employee/,
  'Fullscreen preload must not call the context-changing PQC switch-employee POST.'
)

assert.match(
  panelSource,
  /preloadFrontlinePqcSwitchingCache/,
  'PQC fill panel must import and call the preload helper.'
)
const fullscreenEnterIndex = fullscreenToggleBlock.indexOf('await enterPqcFullscreen()')
const preloadIndex = fullscreenToggleBlock.indexOf('await preloadFrontlinePqcSwitchingCache(deviceState)')
assert.ok(fullscreenEnterIndex >= 0, 'PQC fullscreen toggle must still enter browser fullscreen.')
assert.ok(preloadIndex >= 0, 'PQC fullscreen toggle must preload switching cache after entering fullscreen.')
assert.ok(
  fullscreenEnterIndex < preloadIndex,
  'PQC preload must run after requestFullscreen so browser user activation is not lost.'
)

assert.match(
  activeOrderSelectBlock,
  /pqcProcessOptionsCache/,
  'Selecting a PQC active order must use the preloaded process cache when available.'
)
assert.match(
  pqcProcessSelectBlock,
  /pqcEmployeeOptionsCache/,
  'Selecting a PQC process must use the preloaded personnel cache when available.'
)

console.log('PASS: frontline PQC fullscreen preload static contract')
