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
const productionStageAttr = source.lastIndexOf('data-frontline-production-stage', productionStart)
assert.ok(productionStageAttr >= 0, 'production stage wrapper must exist.')
const productionStageStart = source.lastIndexOf('<div', productionStageAttr)
const productionBlockStart = source.lastIndexOf('<div', productionStart)
const productionFooterEnd = source.indexOf('</footer>', productionStart)
assert.ok(productionBlockStart >= 0 && productionFooterEnd > productionStart, 'production block must include wrapper and footer.')
const productionBlock = source.slice(productionBlockStart, productionFooterEnd)
const productionStage = source.slice(productionStageStart, productionBlockStart)

assert.match(
  productionStage,
  /class="frontline-production-stage"[\s\S]*data-frontline-production-stage[\s\S]*:style="productionStageStyle"/,
  'production DOM must use an outer scale-to-fit stage.'
)
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
    'width: 1920px;',
    'height: 1080px;',
    'padding: 28px;',
    'grid-template-rows: 130px 1fr 126px;',
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
  /width:\s*min\(100%,\s*1600px\)|min-height:\s*min\(1080px,\s*calc\(100vh - 144px\)\)|grid-template-rows:\s*auto minmax\(0,\s*1fr\) 126px/,
  'production screen style must not keep the responsive re-layout.'
)
assertIncludesAll(
  extractBlock('.frontline-production-stage {'),
  ['position: relative;', 'width: 1920px;', 'height: 1080px;', 'max-width: 100%;'],
  'production stage style'
)
assertIncludesAll(
  extractBlock('.frontline-production-stage .frontline-operator-screen {'),
  ['position: absolute;', 'inset: 0;', 'transform: scale(var(--frontline-production-scale, 1));', 'transform-origin: top left;'],
  'production stage screen transform style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-top {'),
  ['grid-template-columns: 1fr 1fr 240px;', 'gap: 20px;'],
  'production top reference grid style'
)
assert.doesNotMatch(
  extractBlock('.frontline-operator-top {'),
  /width:\s*min\(100%,\s*68vw\)|aspect-ratio:\s*1920 \/ 1080/,
  'production top layout must not use the previous local responsive grid.'
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

assertIncludesAll(
  extractBlock('.frontline-production-quantity-panel {'),
  ['grid-template-rows: auto auto auto minmax(0, 1fr);', 'gap: 16px;'],
  'quantity panel style'
)
assertIncludesAll(
  extractBlock('.frontline-production-device-panel {'),
  ['grid-template-rows: auto 98px 1fr;', 'gap: 18px;', 'overflow: hidden;'],
  'device panel style'
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
    'width: min(96%, 1770px);',
    'aspect-ratio: 1920 / 1080;',
    'grid-template-rows: auto minmax(0, 1fr) auto;',
    'padding: 32px;',
    'border: 3px solid var(--frontline-line);',
    'border-radius: 28px;'
  ],
  'production picker card style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker__options {'),
  ['grid-template-columns: repeat(6, minmax(0, 1fr));', 'gap: 12px;', 'align-content: start;', 'min-height: 0;', 'max-height: none;', 'overflow: auto;'],
  'production picker option grid style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker__option {'),
  [
    'display: flex;',
    'align-items: center;',
    'justify-content: center;',
    'height: auto;',
    'aspect-ratio: 1920 / 1080;',
    'min-height: 0;',
    'padding: 8px 10px;',
    'font-size: 30px;',
    'line-height: 1.1;',
    'text-align: center;',
    'word-break: break-word;',
    'overflow: hidden;',
    'border-radius: 22px;'
  ],
  'production picker option style'
)
assertIncludesAll(
  extractBlock('.frontline-operator-panel.is-production-mode .frontline-picker__close {'),
  ['height: 86px;', 'font-size: 36px;', 'border-radius: 22px;'],
  'production picker close style'
)

console.log('PASS: eDHR frontline production pixel parity static contract')
