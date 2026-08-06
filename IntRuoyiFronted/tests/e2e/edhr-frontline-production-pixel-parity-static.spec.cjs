const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const extractBlock = (selector) => {
  const anchoredSelector = `\n${selector}`
  const anchoredIndex = source.indexOf(anchoredSelector)
  const selectorIndex = anchoredIndex >= 0 ? anchoredIndex + 1 : source.indexOf(selector)
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

const assertIncludesAll = (text, tokens, scope) => {
  for (const token of tokens) {
    assert.ok(text.includes(token), `${scope} must include: ${token}`)
  }
}

const productionStart = source.indexOf('data-frontline-production-operator')
assert.ok(productionStart >= 0, 'production operator screen must exist.')
const productionBlockStart = source.lastIndexOf('<div', productionStart)
const productionFooterEnd = source.indexOf('</footer>', productionStart)
assert.ok(productionBlockStart >= 0 && productionFooterEnd > productionStart, 'production block must include wrapper and footer.')
const productionBlock = source.slice(productionBlockStart, productionFooterEnd)

assert.match(
  source,
  /'is-production-mode':\s*!isPqcMode/,
  'root panel must expose a production-only class so reference picker styles do not leak into PQC mode.'
)

assertIncludesAll(
  productionBlock,
  [
    '<div class="top-label">工序</div>',
    '<div class="top-value">{{ selectedProcessLabel }}</div>',
    '<div class="top-label">员工</div>',
    '<div class="top-value">{{ selectedEmployeeLabel }}</div>',
    'class="frontline-production-number-field field"',
    'class="frontline-production-number-field field total is-total"',
    'class="frontline-production-defect-section defect-section"',
    'class="frontline-production-device-current device-current"',
    'class="frontline-production-submit-bar bottom"'
  ],
  'production DOM'
)

const productionPickerIndex = productionBlock.indexOf('class="frontline-picker picker"')
const productionMainIndex = productionBlock.indexOf('<main')
assert.ok(
  productionPickerIndex > 0 && productionPickerIndex < productionMainIndex,
  'production picker overlay must live inside the 1920 screen before the main area, matching the reference HTML.'
)

assert.doesNotMatch(
  productionBlock,
  /frontline-production-quantity-body|:class="\{ 'is-no-device': !visibleDeviceCards\.length \}"|v-if="visibleDeviceCards\.length"/,
  'production DOM must not keep wrapper/no-device branches that change the reference 1920 layout.'
)

assertIncludesAll(
  source,
  [
    'class="frontline-picker picker"',
    'class="frontline-picker__card picker-card"',
    'class="frontline-picker__title picker-title"',
    'class="frontline-picker__options picker-options"',
    'class="frontline-picker__option picker-option"',
    'class="frontline-picker__close picker-close"'
  ],
  'picker DOM'
)

assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode {'),
  [
    'display: grid;',
    'place-items: center;',
    'width: 100%;',
    'min-height: calc(100vh - 96px);',
    'margin: 0;',
    'padding: 24px 0;',
    'background: #dfe8e2;',
    '"Microsoft YaHei UI"',
    '"PingFang SC"',
    '"Noto Sans CJK SC"'
  ],
  'production page body carrier style'
)
assert.doesNotMatch(
  extractBlock('.frontline-operator-panel.is-production-mode {'),
  /position:\s*fixed;|inset:\s*0;|z-index:\s*2000;|min-height:\s*100vh;/,
  'production page carrier must stay in normal page flow before explicit fullscreen.'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-fullscreen,\n.frontline-operator-panel.is-production-mode:fullscreen {'),
  ['width: 100vw;', 'height: 100vh;', 'padding: 0;', 'display: grid;', 'place-items: center;'],
  'production explicit fullscreen carrier style'
)

const screenBlock = extractBlock('.frontline-operator-screen {')
assertIncludesAll(
  screenBlock,
  [
    'width: min(100%, 1600px);',
    'min-height: min(1080px, calc(100vh - 144px));',
    'padding: 28px;',
    'grid-template-rows: auto minmax(0, 1fr) 126px;',
    'gap: 20px;',
    'overflow: hidden;',
    'position: relative;',
    '"Microsoft YaHei UI"',
    '"PingFang SC"',
    '"Noto Sans CJK SC"'
  ],
  'screen style'
)
assert.doesNotMatch(
  screenBlock,
  /width:\s*1920px;|height:\s*1080px;/,
  'normal production screen must not hard-code the full 1920x1080 canvas.'
)
assertIncludesAll(
  extractBlock('.frontline-operator-top.is-production {'),
  [
    'width: min(100%, 68vw);',
    'max-width: 1280px;',
    'grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(132px, 0.58fr);'
  ],
  'production top selection grid style'
)

assertIncludesAll(
  extractBlock('.frontline-operator-screen button,\n.frontline-operator-screen input {'),
  ['font: inherit;'],
  'screen control font inheritance'
)

const topCardBlock = extractBlock('.frontline-top-card {')
assertIncludesAll(
  topCardBlock,
  [
    'padding: 22px 26px;',
    'background: var(--frontline-panel);',
    'text-align: left;',
    'cursor: pointer;'
  ],
  'top card style'
)
assertIncludesAll(
  topCardBlock,
  [
    '.top-label',
    'font-size: 28px;',
    'font-weight: 700;',
    '.top-value',
    'margin-top: 12px;',
    'font-size: 42px;',
    'font-weight: 900;',
    'line-height: 1.1;'
  ],
  'top reference label/value style'
)

const quantityPanelBlock = extractBlock('.frontline-production-quantity-panel {')
assertIncludesAll(
  quantityPanelBlock,
  ['grid-template-rows: auto auto auto minmax(0, 1fr);', 'gap: 16px;'],
  'quantity panel style'
)
assert.doesNotMatch(
  quantityPanelBlock,
  /is-no-device|grid-template-rows:\s*auto minmax\(0,\s*1fr\)/,
  'quantity panel style must not keep alternate no-device layout.'
)
assert.doesNotMatch(source, /\.frontline-production-quantity-body\s*\{/, 'reference layout must not require a production quantity body wrapper.')

assertIncludesAll(
  extractBlock('.frontline-production-device-panel {'),
  ['grid-template-rows: auto 98px 1fr;', 'gap: 18px;', 'overflow: hidden;'],
  'device panel style'
)
assert.doesNotMatch(
  extractBlock('.frontline-production-device-tabs {'),
  /padding:\s*0 8px;|overflow:\s*hidden;|text-overflow:\s*ellipsis;/,
  'device tabs must not add non-reference trimming or side padding.'
)

assertIncludesAll(
  extractBlock('.frontline-production-submit-bar {'),
  ['grid-template-columns: 300px 1fr;', 'gap: 24px;', 'position: relative;', 'z-index: 2;'],
  'footer style'
)
assertIncludesAll(
  extractBlock('.frontline-production-reset-button,\n.frontline-production-submit-button {'),
  ['border: 0;', 'border-radius: 28px;', 'font-size: 54px;', 'font-weight: 900;'],
  'footer button style'
)

assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker {'),
  ['z-index: 10;', 'display: grid;', 'place-items: center;', 'background: rgba(17, 26, 21, 0.38);'],
  'production picker overlay style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker__card {'),
  [
    'width: min(92%, 1180px);',
    'aspect-ratio: 1920 / 1080;',
    'grid-template-rows: auto minmax(0, 1fr) auto;',
    'padding: 32px;',
    'border: 3px solid var(--frontline-line);',
    'border-radius: 28px;'
  ],
  'production picker card style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker__option {'),
  ['aspect-ratio: 1920 / 1080;', 'height: auto;', 'font-size: 42px;', 'border-radius: 22px;'],
  'production picker option style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker__options {'),
  ['min-height: 0;', 'overflow: auto;', 'align-content: start;'],
  'production picker option grid style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker__close {'),
  ['height: 86px;', 'font-size: 36px;', 'border-radius: 22px;'],
  'production picker close style'
)

console.log('PASS: eDHR frontline production pixel parity static contract')
