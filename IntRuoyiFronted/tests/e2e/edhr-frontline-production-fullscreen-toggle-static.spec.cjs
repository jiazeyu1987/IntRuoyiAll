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
  /ref="productionScreenRef"/,
  'production fill fullscreen must target the production operator screen.'
)
assert.match(
  productionHeader,
  /frontline-production-fullscreen-button/,
  'production fill top-right action must use a dedicated fullscreen button class.'
)
assert.match(
  productionHeader,
  /@click="handleProductionFullscreenToggle"/,
  'production fill top-right action must toggle fullscreen instead of routing home.'
)
assert.match(
  productionHeader,
  /{{\s*productionFullscreenButtonLabel\s*}}/,
  'production fill top-right action must render a state-driven label.'
)
assert.doesNotMatch(
  productionHeader,
  /@click="handleHome"[\s\S]*主页/,
  'production fill default header must not keep the old Home route button.'
)

for (const token of [
  'const productionScreenRef = ref<HTMLElement>()',
  'const isProductionFullscreen = ref(false)',
  "isProductionFullscreen.value ? '主页' : '最大化'",
  'const syncProductionFullscreenState = () =>',
  'document.fullscreenElement === productionScreenRef.value',
  'const handleProductionFullscreenToggle = async () =>',
  'await screen.requestFullscreen()',
  'await document.exitFullscreen()',
  "document.addEventListener('fullscreenchange', syncProductionFullscreenState)",
  "document.removeEventListener('fullscreenchange', syncProductionFullscreenState)"
]) {
  assert.ok(source.includes(token), `production fullscreen contract missing: ${token}`)
}

assert.match(
  source,
  /\.frontline-operator-screen:fullscreen\s*\{[\s\S]*width:\s*100vw[\s\S]*height:\s*100vh/,
  'native fullscreen production screen must fill the viewport.'
)
assert.match(
  source,
  /\.frontline-operator-screen\.is-frontline-fullscreen\s+\.frontline-production-fullscreen-button/,
  'fullscreen state must keep the Home/restore button visually aligned with the approved screenshot.'
)

console.log('PASS: eDHR frontline production fullscreen toggle static contract')
