const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const panelPath = path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

const extractFunctionBlock = (name) => {
  const start = source.indexOf('const ' + name + ' =')
  assert.ok(start >= 0, 'missing function: ' + name)
  const openIndex = source.indexOf('{', start)
  assert.ok(openIndex > start, 'missing function body: ' + name)
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
  assert.fail('unterminated function: ' + name)
}

const extractStyleBlock = (selector) => {
  const start = source.indexOf(selector + ' {')
  assert.ok(start >= 0, 'missing style block: ' + selector)
  const openIndex = source.indexOf('{', start)
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
  assert.fail('unterminated style block: ' + selector)
}

const productionHeaderStart = source.indexOf('data-frontline-production-selection-grid')
const productionHeaderEnd = source.indexOf('</header>', productionHeaderStart)
assert.ok(productionHeaderStart >= 0 && productionHeaderEnd > productionHeaderStart)
const productionHeader = source.slice(productionHeaderStart, productionHeaderEnd)

assert.match(
  productionHeader,
  /data-frontline-production-process-nav-card/,
  'production process top card must expose a stable navigation card selector.'
)
assert.match(
  productionHeader,
  /data-frontline-process-previous[\s\S]*frontline-production-process-nav-icon is-previous/,
  'production process top card must render the previous process button with the heavy CSS left arrow.'
)
assert.match(
  productionHeader,
  /data-frontline-process-next[\s\S]*frontline-production-process-nav-icon is-next/,
  'production process top card must render the next process button with the heavy CSS right arrow.'
)
assert.doesNotMatch(
  productionHeader,
  />\s*(前一个|后一个|←|→)\s*</,
  'production process navigation buttons must not show text labels or thin text-arrow glyphs.'
)
assert.match(
  productionHeader,
  /@click\.stop="handleNavigateProductionProcess\(-1\)"/,
  'previous process button must call the adjacent navigation handler with -1 without opening the picker.'
)
assert.match(
  productionHeader,
  /@click\.stop="handleNavigateProductionProcess\(1\)"/,
  'next process button must call the adjacent navigation handler with 1 without opening the picker.'
)
assert.match(
  productionHeader,
  /:disabled="isProductionProcessPreviousDisabled"/,
  'previous process button must be disabled at the first process or while switching.'
)
assert.match(
  productionHeader,
  /:disabled="isProductionProcessNextDisabled"/,
  'next process button must be disabled at the last process or while switching.'
)

for (const requiredToken of [
  'const selectedProductionProcessIndex = computed',
  'const previousProductionProcess = computed',
  'const nextProductionProcess = computed',
  'const isProductionProcessNavigationBlocked = computed',
  'const isProductionProcessPreviousDisabled = computed',
  'const isProductionProcessNextDisabled = computed',
  'const handleNavigateProductionProcess = async'
]) {
  assert.ok(source.includes(requiredToken), 'missing production process navigation token: ' + requiredToken)
}

const navigateBlock = extractFunctionBlock('handleNavigateProductionProcess')
assert.match(
  navigateBlock,
  /const targetProcess = direction < 0[\s\S]*previousProductionProcess\.value[\s\S]*nextProductionProcess\.value/,
  'navigation handler must choose the adjacent formal process from computed neighbors.'
)
assert.match(
  navigateBlock,
  /if \(!targetProcess \|\| isProductionProcessNavigationBlocked\.value\) \{[\s\S]*return[\s\S]*\}/,
  'navigation handler must not switch when there is no adjacent process or interaction is blocked.'
)
assert.match(
  navigateBlock,
  /await handleSelectProcess\(targetProcess\)/,
  'navigation handler must reuse the formal process selection workflow.'
)
assert.doesNotMatch(
  navigateBlock,
  /deviceState\.selectedProcess\s*=/,
  'navigation handler must not mutate selectedProcess directly.'
)

assert.match(
  source,
  /\.frontline-production-process-nav-card\s*\{[\s\S]*grid-template-columns:\s*140px minmax\(0,\s*1fr\) 140px;/,
  'process navigation card must reserve fixed side buttons around the current process label.'
)

const currentProcessStyle = extractStyleBlock('.frontline-production-process-current')
assert.match(
  currentProcessStyle,
  /align-items:\s*center;/,
  'current process text must be horizontally centered inside the red-box card.'
)
assert.match(
  currentProcessStyle,
  /text-align:\s*center;/,
  'current process label and value must use centered text alignment.'
)

const navButtonStyle = extractStyleBlock('.frontline-production-process-nav-button')
for (const [pattern, message] of [
  [/width:\s*100%;/, 'process nav buttons must fill the blue-box grid track width.'],
  [/height:\s*100%;/, 'process nav buttons must fill the blue-box grid track height.'],
  [/border-radius:\s*22px;/, 'process nav buttons must match the top-card border radius.'],
  [/background:\s*var\(--frontline-panel\);/, 'process nav buttons must match the top-card background.'],
  [/font-size:\s*112px;/, 'process nav arrow glyphs must render at twice the current arrow size.'],
  [/text-align:\s*center;/, 'process nav buttons must center their text.']
]) {
  assert.match(navButtonStyle, pattern, message)
}

const navIconStyle = extractStyleBlock('.frontline-production-process-nav-icon')
for (const [pattern, message] of [
  [/width:\s*78px;/, 'process nav arrow icon must use a wide visual arrow body.'],
  [/height:\s*46px;/, 'process nav arrow icon must use a tall visual arrow body.'],
  [/color:\s*currentColor;/, 'process nav arrow icon must inherit the button color.']
]) {
  assert.match(navIconStyle, pattern, message)
}

const navIconShaftStyle = extractStyleBlock('.frontline-production-process-nav-icon::before')
assert.match(
  navIconShaftStyle,
  /height:\s*14px;/,
  'process nav arrow shaft must be visibly thick instead of a thin font glyph.'
)
assert.match(
  navIconShaftStyle,
  /background:\s*currentColor;/,
  'process nav arrow shaft must be drawn with the current button color.'
)

const navIconHeadStyle = extractStyleBlock('.frontline-production-process-nav-icon::after')
assert.match(
  navIconHeadStyle,
  /border-left:\s*14px solid currentColor;/,
  'process nav arrow head must use a thick left border.'
)
assert.match(
  navIconHeadStyle,
  /border-bottom:\s*14px solid currentColor;/,
  'process nav arrow head must use a thick bottom border.'
)
assert.match(
  source,
  /\.frontline-production-process-nav-icon\.is-next\s*\{[\s\S]*transform:\s*scaleX\(-1\);/,
  'next process arrow must mirror the same heavy left-arrow drawing.'
)

console.log('PASS: frontline production process navigation buttons static contract')
