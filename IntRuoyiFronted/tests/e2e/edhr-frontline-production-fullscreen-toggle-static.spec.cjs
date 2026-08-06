const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(
  root,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

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
  'production fill must render the reference 1920 prototype screen.'
)
assert.match(
  productionHeader,
  /class="frontline-home-button home-btn"/,
  'production fill top-right action must use the reference Home button class.'
)
assert.match(
  productionHeader,
  /@click="handleHome"[\s\S]*>\s*主页\s*<\/button>/,
  'production fill top-right action must route Home and display 主页.'
)
assert.doesNotMatch(
  productionHeader,
  /ref="productionScreenRef"|frontline-production-fullscreen-button|handleProductionFullscreenToggle|productionFullscreenButtonLabel|aria-pressed="isProductionFullscreen"/,
  'production fill must not keep the removed fullscreen toggle contract.'
)

for (const removedToken of [
  'const productionScreenRef = ref<HTMLElement>()',
  'const isProductionFullscreen = ref(false)',
  "isProductionFullscreen.value ? '主页' : '最大化'",
  'const syncProductionFullscreenState = () =>',
  'document.fullscreenElement === productionScreenRef.value',
  'const handleProductionFullscreenToggle = async () =>',
  "document.addEventListener('fullscreenchange', syncProductionFullscreenState)",
  "document.removeEventListener('fullscreenchange', syncProductionFullscreenState)"
]) {
  assert.ok(!source.includes(removedToken), `production fullscreen code must be removed: ${removedToken}`)
}

assert.match(
  source,
  /\.frontline-operator-screen\s*\{[\s\S]*width:\s*1920px;[\s\S]*height:\s*1080px;[\s\S]*grid-template-rows:\s*130px 1fr 126px;/,
  'production screen must use the fixed reference prototype canvas.'
)
assert.match(
  source,
  /\.frontline-production-submit-bar\s*\{[\s\S]*grid-template-columns:\s*300px 1fr;/,
  'production footer must keep the reference 300px + 1fr button layout.'
)

console.log('PASS: eDHR frontline production prototype home action static contract')
