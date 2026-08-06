const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(
  root,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const extractBlock = (selector) => {
  const selectorIndex = source.indexOf(selector)
  assert.ok(selectorIndex >= 0, `missing selector: ${selector}`)
  const openIndex = source.indexOf('{', selectorIndex)
  assert.ok(openIndex > selectorIndex, `missing selector body: ${selector}`)
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
  assert.fail(`unterminated selector body: ${selector}`)
}

const productionStart = source.indexOf('data-frontline-production-operator')
assert.ok(productionStart >= 0, 'production operator block must exist.')
const productionBlockStart = source.lastIndexOf('<div', productionStart)
assert.ok(productionBlockStart >= 0, 'production operator wrapper must exist.')
const productionHeaderEnd = source.indexOf('</header>', productionStart)
assert.ok(productionHeaderEnd > productionStart, 'production operator header must exist.')
const productionHeader = source.slice(productionBlockStart, productionHeaderEnd)

assert.match(
  productionHeader,
  /class="frontline-operator-screen screen"/,
  'production fill must render the approved operator screen.'
)
assert.doesNotMatch(
  source,
  /frontline-production-stage|data-frontline-production-stage|productionStageStyle|PRODUCTION_CANVAS_WIDTH|PRODUCTION_CANVAS_HEIGHT|productionViewportScale|ResizeObserver|--frontline-production-scale/,
  'normal production fill must not use a fixed 1920x1080 stage, viewport ResizeObserver scaling, or full-canvas transform scaling.'
)
assert.match(
  productionHeader,
  /class="frontline-operator-top top is-production"[\s\S]*data-frontline-production-selection-grid/,
  'production header must expose a local selection grid instead of a scaled full-canvas stage.'
)
assert.match(
  productionHeader,
  /class="frontline-top-card top-box frontline-production-selection-card"[\s\S]*data-frontline-production-selection-card[\s\S]*<div class="top-label">工序<\/div>/,
  'process selector must be a production selection card inside the 16:9 grid.'
)
assert.match(
  productionHeader,
  /class="frontline-top-card top-box frontline-production-selection-card"[\s\S]*data-frontline-production-selection-card[\s\S]*<div class="top-label">员工<\/div>/,
  'employee selector must be a production selection card inside the 16:9 grid.'
)
assert.match(
  productionHeader,
  /class="[^"]*\bfrontline-home-button\b[^"]*\bhome-btn\b[^"]*"/,
  'production fill top-right action must keep the reference Home button class for prototype styling.'
)
assert.match(
  productionHeader,
  /data-production-fullscreen-toggle/,
  'production fill top-right action must expose a stable production fullscreen toggle selector.'
)
assert.match(
  productionHeader,
  /:aria-label="productionFullscreenActionText"[\s\S]*:aria-pressed="isProductionFullscreen"[\s\S]*@click="handleProductionFullscreenToggle"[\s\S]*{{ productionFullscreenActionText }}/,
  'production fill top-right action must mirror PQC: 最大化 before fullscreen, 主页 while fullscreen.'
)
assert.doesNotMatch(
  productionHeader,
  /@click="handleHome"[\s\S]*>\s*主页\s*<\/button>/,
  'production fill must not route Home by default; it should enter fullscreen only after explicit click.'
)

for (const requiredToken of [
  'const isProductionFullscreen = ref(false)',
  "isProductionFullscreen.value ? '主页' : '最大化'",
  'const enterProductionFullscreen = async () =>',
  'const handleProductionFullscreenToggle = async () =>',
  "document.addEventListener('fullscreenchange', syncPqcFullscreenState)",
  "document.removeEventListener('fullscreenchange', syncPqcFullscreenState)"
]) {
  assert.ok(source.includes(requiredToken), `production fullscreen code must exist: ${requiredToken}`)
}

assert.match(
  source,
  /const syncPqcFullscreenState = \(\) =>[\s\S]*isProductionFullscreen\.value\s*=\s*!isPqcMode\.value && document\.fullscreenElement === frontlinePanelRef\.value/,
  'shared fullscreenchange sync must update production state only when production mode owns the fullscreen panel.'
)
assert.match(
  source,
  /const enterProductionFullscreen = async \(\) =>[\s\S]*frontlinePanelRef\.value[\s\S]*requestFullscreen\(\)/,
  'production fullscreen entry must call requestFullscreen on the same panel element as PQC.'
)
assert.match(
  source,
  /const handleProductionFullscreenToggle = async \(\) =>[\s\S]*isProductionFullscreen\.value[\s\S]*exitProductionFullscreen\(\)[\s\S]*enterProductionFullscreen\(\)/,
  'production toggle handler must switch between enter and exit fullscreen.'
)

const productionCarrierBlock = extractBlock('.frontline-operator-panel.is-production-mode {')
assert.doesNotMatch(
  productionCarrierBlock,
  /position:\s*fixed;|inset:\s*0;|z-index:\s*2000;|min-height:\s*100vh;/,
  'production mode must not use a fixed full-viewport carrier before the user clicks 最大化.'
)
assert.match(
  productionCarrierBlock,
  /display:\s*grid;[\s\S]*place-items:\s*center;[\s\S]*overflow-x:\s*hidden;[\s\S]*background:\s*#dfe8e2;/,
  'production mode must stay in normal flow and hide accidental horizontal overflow.'
)
assert.match(
  source,
  /\.frontline-operator-panel\.is-production-fullscreen,\s*\n\.frontline-operator-panel\.is-production-mode:fullscreen\s*\{[\s\S]*width:\s*100vw;[\s\S]*height:\s*100vh;[\s\S]*padding:\s*0;/,
  'production fullscreen style must be applied only after explicit fullscreen state.'
)
assert.match(
  extractBlock('.frontline-operator-top.is-production {'),
  /width:\s*min\(100%,\s*68vw\);[\s\S]*max-width:\s*1280px;[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)\s+minmax\(0,\s*1fr\)\s+minmax\(132px,\s*0\.58fr\);/,
  'production selection area must be a local grid around two-thirds of the page, not the full 1920 canvas.'
)
assert.match(
  extractBlock('.frontline-operator-top.is-production {'),
  /\.frontline-production-selection-card,\s*\n\s*\.frontline-production-fullscreen-toggle\s*\{[\s\S]*aspect-ratio:\s*1920 \/ 1080;/,
  'production process, employee, and action grid cells must use the 1920:1080 ratio.'
)

const screenBlock = extractBlock('.frontline-operator-screen {')
assert.match(
  screenBlock,
  /width:\s*min\(100%,\s*1600px\);[\s\S]*min-height:\s*min\(1080px,\s*calc\(100vh - 144px\)\);[\s\S]*grid-template-rows:\s*auto minmax\(0,\s*1fr\) 126px;/,
  'normal production screen must be responsive in the page flow rather than fixed to 1920x1080.'
)
assert.doesNotMatch(
  screenBlock,
  /width:\s*1920px;|height:\s*1080px;/,
  'normal production screen must not hard-code the full 1920x1080 canvas.'
)
assert.match(
  source,
  /\.frontline-production-submit-bar\s*\{[\s\S]*grid-template-columns:\s*300px 1fr;/,
  'production footer must keep the reference 300px + 1fr button layout.'
)

console.log('PASS: eDHR frontline production local 16:9 selection grid static contract')
