const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const referencePath = 'C:/Users/BJB110/Desktop/3/frontline-production-operator-1920.html'
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const reference = fs.readFileSync(referencePath, 'utf8').replace(/\r\n/g, '\n')
const productionPage = read('src/views/mes/pro/edhr-batch/BatchProductionFillPage.vue')
const panel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

for (const token of [
  'width: 1920px;',
  'height: 1080px;',
  'grid-template-rows: 130px 1fr 126px;',
  'grid-template-columns: 1fr 1fr 240px;',
  'grid-template-columns: 1050px 1fr;',
  'grid-template-columns: 300px 1fr;'
]) {
  assert.ok(reference.includes(token), `reference prototype must keep expected token: ${token}`)
}

assert.doesNotMatch(
  productionPage,
  /<ContentWrap>|data-edhr-frontline-production-page-title|按活跃订单、工序和设备填写一线生产记录/,
  'production fill page must not render an extra admin title shell outside the prototype screen.'
)
assert.match(
  productionPage,
  /<FrontlineFixedTemplatePanel\s+mode="production"\s*\/>/,
  'production fill page must directly render the fixed production prototype panel.'
)

const productionStart = panel.indexOf('data-frontline-production-operator')
assert.ok(productionStart >= 0, 'production operator screen must exist.')
const productionBlockStart = panel.lastIndexOf('<div', productionStart)
assert.ok(productionBlockStart >= 0, 'production operator screen wrapper must exist.')
const productionEnd = panel.indexOf('</footer>', productionStart)
assert.ok(productionEnd > productionStart, 'production operator block must include a footer.')
const productionBlock = panel.slice(productionBlockStart, productionEnd)

for (const token of [
  'class="frontline-operator-screen screen"',
  'class="frontline-operator-top top is-production"',
  'class="frontline-operator-main frontline-production-main main"',
  'frontline-work-panel panel quantity-panel frontline-production-quantity-panel',
  'frontline-work-panel panel device-panel frontline-production-device-panel',
  'frontline-production-submit-bar bottom',
  'frontline-production-reset-button minor-btn',
  'frontline-production-submit-button submit-btn'
]) {
  assert.ok(productionBlock.includes(token), `production DOM must carry reference prototype class token: ${token}`)
}

assert.match(
  productionBlock,
  /class="[^"]*\bfrontline-top-card\b[^"]*\btop-box\b[^"]*\bfrontline-production-selection-card\b[^"]*"/,
  'production selector cards must keep the reference top-card classes while adding the production selection class.'
)
assert.match(
  productionBlock,
  /class="[^"]*\bfrontline-home-button\b[^"]*\bhome-btn\b[^"]*"/,
  'top-right action must keep the reference prototype Home button class for styling.'
)
assert.match(
  productionBlock,
  /data-production-fullscreen-toggle[\s\S]*:aria-label="productionFullscreenActionText"[\s\S]*:aria-pressed="isProductionFullscreen"[\s\S]*@click="handleProductionFullscreenToggle"[\s\S]*{{ productionFullscreenActionText }}/,
  'top-right action must mirror PQC explicit 最大化/主页 fullscreen switching.'
)
assert.doesNotMatch(
  productionBlock,
  /@click="handleHome"[\s\S]*>\s*主页\s*<\/button>/,
  'production prototype block must not default to Home routing before fullscreen.'
)

const screenStyleStart = panel.indexOf('.frontline-operator-screen {')
assert.ok(screenStyleStart >= 0, 'production screen style block must exist.')
const screenStyleEnd = panel.indexOf('&.is-pqc', screenStyleStart)
assert.ok(screenStyleEnd > screenStyleStart, 'base production screen style block must end before PQC overrides.')
const screenStyle = panel.slice(screenStyleStart, screenStyleEnd)

for (const token of [
  'width: min(100%, 1600px);',
  'min-height: min(1080px, calc(100vh - 144px));',
  'grid-template-rows: auto minmax(0, 1fr) 126px;',
  'padding: 28px;',
  'background: var(--frontline-bg);'
]) {
  assert.ok(screenStyle.includes(token), `production screen style must preserve reference content responsively: ${token}`)
}
assert.doesNotMatch(
  screenStyle,
  /width:\s*1920px;|height:\s*1080px;|border-radius:\s*18px/,
  'base production screen must not use the old fixed 1920x1080 canvas or admin-contained card.'
)

assert.match(
  panel,
  /\.frontline-production-submit-bar\s*\{[\s\S]*grid-template-columns:\s*300px 1fr;/,
  'production footer must use the reference 300px + 1fr layout.'
)

console.log('PASS: eDHR frontline production prototype parity static contract')
