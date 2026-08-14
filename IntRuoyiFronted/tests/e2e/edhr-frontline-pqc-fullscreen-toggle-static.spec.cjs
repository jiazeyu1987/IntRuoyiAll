const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const pqcStart = source.indexOf('v-if="isPqcMode"')
const pqcEnd = source.indexOf('\n    <div\n      v-else', pqcStart)
assert.ok(pqcStart >= 0 && pqcEnd > pqcStart, 'PQC operator template block must exist.')
const pqcTemplate = source.slice(pqcStart, pqcEnd)

assert.match(
  source,
  /ref="frontlinePanelRef"[\s\S]*:class="\{[\s\S]*'is-pqc-fullscreen': isPqcFullscreen,[\s\S]*\}"/,
  'fullscreen state must be applied to the operator panel that also owns PQC pickers and dialogs.'
)
assert.match(
  pqcTemplate,
  /data-pqc-fullscreen-toggle/,
  'PQC top action must expose a stable fullscreen toggle selector.'
)
assert.match(
  pqcTemplate,
  /@click="handlePqcFullscreenToggle"[\s\S]*{{ pqcFullscreenActionText }}/,
  'PQC top action must toggle fullscreen and render computed 最大化/主页 text.'
)
assert.doesNotMatch(
  pqcTemplate,
  /@click="handleHome">主页<\/button>/,
  'PQC top action must not route home by default; it must default to 最大化.'
)
assert.match(
  source,
  /const pqcFullscreenActionText = computed\(\(\) =>\s*isPqcFullscreen\.value \? '主页' : '最大化'\s*\)/,
  'PQC action text must be 最大化 before fullscreen and 主页 while fullscreen.'
)
assert.match(
  source,
  /const enterPqcFullscreen = async \(\) =>[\s\S]*frontlinePanelRef\.value[\s\S]*requestFullscreen\(\)/,
  'PQC fullscreen entry must call requestFullscreen on the panel element.'
)
assert.match(
  source,
  /const exitPqcFullscreen = async \(\) =>[\s\S]*document\.exitFullscreen\(\)/,
  'PQC fullscreen exit must call document.exitFullscreen.'
)
assert.match(
  source,
  /const handlePqcFullscreenToggle = async \(\) =>[\s\S]*isPqcFullscreen\.value[\s\S]*exitPqcFullscreen\(\)[\s\S]*enterPqcFullscreen\(\)/,
  'PQC toggle handler must switch between enter and exit fullscreen.'
)
assert.match(
  source,
  /document\.addEventListener\('fullscreenchange', syncPqcFullscreenState\)/,
  'PQC fullscreen state must listen for browser fullscreenchange.'
)
assert.match(
  source,
  /onUnmounted\(\(\) =>[\s\S]*document\.removeEventListener\('fullscreenchange', syncPqcFullscreenState\)/,
  'PQC fullscreen listener must be removed on component unmount.'
)
assert.match(
  source,
  /\.frontline-operator-panel\.is-pqc-fullscreen[\s\S]*width:\s*100vw[\s\S]*height:\s*100vh/,
  'PQC fullscreen panel must fill the viewport.'
)
assert.match(
  source,
  /\.frontline-operator-panel\.is-pqc-fullscreen \.frontline-operator-screen\.is-pqc[\s\S]*grid-template-rows:\s*minmax\(118px, auto\) minmax\(0, 1fr\) 104px/,
  'PQC fullscreen operator screen must allow the compact header row to grow for wrapped summary text.'
)
assert.match(
  source,
  /\.frontline-operator-panel\.is-pqc-fullscreen \.frontline-operator-top\.is-pqc[\s\S]*grid-template-columns:\s*minmax\(480px, 1\.55fr\) minmax\(220px, 0\.85fr\) minmax\(200px, 1fr\) 150px/,
  'PQC fullscreen header columns must reserve enough space for the complete order summary.'
)
assert.match(
  source,
  /\.frontline-operator-panel\.is-pqc-fullscreen \.frontline-operator-main\.is-pqc[\s\S]*grid-template-columns:\s*minmax\(760px, 1\.72fr\) minmax\(390px, 0\.78fr\)/,
  'PQC fullscreen main layout must match the screenshot left/right panel proportions.'
)

console.log('PASS: eDHR frontline PQC fullscreen toggle static contract')
