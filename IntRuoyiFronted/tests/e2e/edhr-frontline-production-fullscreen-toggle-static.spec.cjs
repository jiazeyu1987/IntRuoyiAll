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
const productionStageAttr = source.lastIndexOf('data-frontline-production-stage', productionStart)
assert.ok(productionStageAttr >= 0, 'production operator must be wrapped by a scale-to-fit stage.')
const productionStageStart = source.lastIndexOf('<div', productionStageAttr)
const productionBlockStart = source.lastIndexOf('<div', productionStart)
assert.ok(productionStageStart >= 0, 'production stage wrapper must exist.')
assert.ok(productionBlockStart > productionStageStart, 'production screen must be inside the stage wrapper.')
const productionHeaderEnd = source.indexOf('</header>', productionStart)
assert.ok(productionHeaderEnd > productionStart, 'production operator header must exist.')
const productionHeader = source.slice(productionBlockStart, productionHeaderEnd)
const productionStageOpen = source.slice(productionStageStart, productionBlockStart)

assert.match(
  productionStageOpen,
  /class="frontline-production-stage"[\s\S]*data-frontline-production-stage[\s\S]*:style="productionStageStyle"/,
  'production fill must use a measured outer stage instead of changing the reference canvas layout.'
)
assert.match(
  productionHeader,
  /class="frontline-operator-screen screen"/,
  'production fill must render the approved operator screen.'
)
assert.match(
  productionHeader,
  /class="frontline-operator-top top is-production"/,
  'production header must keep the reference top row class.'
)
assert.match(
  productionHeader,
  /class="frontline-top-card top-box[^"]*"[\s\S]*<div class="top-label">工序<\/div>/,
  'process selector must keep the reference top-card/top-box structure.'
)
assert.match(
  productionHeader,
  /class="frontline-top-card top-box[^"]*"[\s\S]*<div class="top-label">员工<\/div>/,
  'employee selector must keep the reference top-card/top-box structure.'
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
  'const PRODUCTION_CANVAS_WIDTH = 1920',
  'const PRODUCTION_CANVAS_HEIGHT = 1080',
  'const productionViewportScale = ref(1)',
  'const productionStageStyle = computed(() =>',
  '--frontline-production-scale',
  'const enterProductionFullscreen = async () =>',
  'const handleProductionFullscreenToggle = async () =>',
  "document.addEventListener('fullscreenchange', syncPqcFullscreenState)",
  "document.removeEventListener('fullscreenchange', syncPqcFullscreenState)"
]) {
  assert.ok(source.includes(requiredToken), `production fullscreen/stage code must exist: ${requiredToken}`)
}

assert.match(
  source,
  /const productionStageStyle = computed\(\(\) =>[\s\S]*`\$\{PRODUCTION_CANVAS_WIDTH \* scale\}px`[\s\S]*`\$\{PRODUCTION_CANVAS_HEIGHT \* scale\}px`/,
  'production stage style must size the outer layout box to the scaled 1920x1080 canvas.'
)
assert.match(
  source,
  /ResizeObserver/,
  'production stage must update scale when the app content area changes size.'
)
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
  extractBlock('.frontline-production-stage {'),
  /position:\s*relative;[\s\S]*width:\s*1920px;[\s\S]*height:\s*1080px;[\s\S]*max-width:\s*100%;/,
  'production stage must reserve the scaled reference canvas footprint in normal page flow.'
)
assert.match(
  extractBlock('.frontline-production-stage .frontline-operator-screen {'),
  /position:\s*absolute;[\s\S]*inset:\s*0;[\s\S]*transform:\s*scale\(var\(--frontline-production-scale,\s*1\)\);[\s\S]*transform-origin:\s*top left;/,
  'production stage must scale the full reference canvas externally.'
)

const screenBlock = extractBlock('.frontline-operator-screen {')
assert.match(
  screenBlock,
  /width:\s*1920px;[\s\S]*height:\s*1080px;[\s\S]*grid-template-rows:\s*130px 1fr 126px;/,
  'production screen must keep the strict reference 1920x1080 canvas and row rhythm.'
)
assert.doesNotMatch(
  screenBlock,
  /width:\s*min\(100%,\s*1600px\)|grid-template-rows:\s*auto minmax\(0,\s*1fr\) 126px/,
  'production screen must not keep the responsive re-layout that broke reference parity.'
)
assert.match(
  extractBlock('.frontline-operator-top {'),
  /grid-template-columns:\s*1fr 1fr 240px;/,
  'production top area must keep the reference 1fr 1fr 240px layout.'
)
assert.doesNotMatch(
  source,
  /\.frontline-operator-top\.is-production\s*\{[\s\S]*width:\s*min\(100%,\s*68vw\)|aspect-ratio:\s*1920 \/ 1080/,
  'production top area must not keep the previous local responsive selection grid.'
)
assert.match(
  source,
  /\.frontline-production-submit-bar\s*\{[\s\S]*grid-template-columns:\s*300px 1fr;/,
  'production footer must keep the reference 300px + 1fr button layout.'
)

console.log('PASS: eDHR frontline production strict canvas scale-to-fit static contract')
