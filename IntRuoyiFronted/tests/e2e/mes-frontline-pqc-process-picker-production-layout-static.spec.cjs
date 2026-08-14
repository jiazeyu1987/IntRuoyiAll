const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
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

const assertIncludesAll = (text, tokens, scope) => {
  for (const token of tokens) {
    assert.ok(text.includes(token), `${scope} must include: ${token}`)
  }
}

const pqcPickerAnchor = source.indexOf('v-if="activePicker && isPqcMode"')
assert.ok(pqcPickerAnchor >= 0, 'PQC picker block must exist.')
const pqcPickerStart = source.lastIndexOf('<div', pqcPickerAnchor)
const pqcPickerEnd = source.indexOf('</section>\n</template>', pqcPickerStart)
assert.ok(pqcPickerEnd > pqcPickerStart, 'PQC picker block boundary must be stable.')
const pqcPickerBlock = source.slice(pqcPickerStart, pqcPickerEnd)

assertIncludesAll(
  pqcPickerBlock,
  [
    'data-pqc-process-picker',
    "'frontline-picker--production-process': activePicker === 'process'",
    'class="frontline-picker__card picker-card"',
    'class="frontline-picker__options picker-options"',
    'class="frontline-picker__option picker-option"'
  ],
  'PQC process picker template'
)

const findInitialProcessAnchor = source.indexOf('const findInitialProcess = (')
assert.ok(findInitialProcessAnchor >= 0, 'findInitialProcess must exist.')
const findInitialProcessEnd = source.indexOf('\n}\n\nconst isCurrentLoginEmployee', findInitialProcessAnchor)
assert.ok(findInitialProcessEnd > findInitialProcessAnchor, 'findInitialProcess boundary must be stable.')
const findInitialProcessBlock = source.slice(findInitialProcessAnchor, findInitialProcessEnd)
assertIncludesAll(
  findInitialProcessBlock,
  [
    'const fallbackProcess = isPqcMode.value',
    'processes.find(hasPqcTaskSnapshot) || processes[0]',
    ': processes[0]',
    'return fallbackProcess'
  ],
  'PQC initial process selection'
)

assertIncludesAll(
  extractBlock('.frontline-picker--production-process {'),
  ['z-index: 30;', 'display: grid;', 'place-items: center;', 'border-radius: 0;', 'background: rgba(17, 26, 21, 0.38);'],
  'PQC process picker production-layout overlay style'
)
assertIncludesAll(
  extractBlock('.frontline-picker--production-process .frontline-picker__card {'),
  [
    'width: min(96%, 1770px);',
    'aspect-ratio: 1920 / 1080;',
    'grid-template-rows: auto minmax(0, 1fr) auto;',
    'padding: 32px;',
    'border-radius: 28px;'
  ],
  'PQC process picker production-layout card style'
)
assertIncludesAll(
  extractBlock('.frontline-picker--production-process .frontline-picker__options {'),
  ['grid-template-columns: repeat(6, minmax(0, 1fr));', 'gap: 12px;', 'align-content: start;', 'min-height: 0;', 'max-height: none;', 'overflow: auto;'],
  'PQC process picker production-layout option grid style'
)
assertIncludesAll(
  extractBlock('.frontline-picker--production-process .frontline-picker__option {'),
  [
    'display: flex;',
    'align-items: center;',
    'justify-content: center;',
    'height: auto;',
    'aspect-ratio: 1920 / 720;',
    'min-height: 0;',
    'padding: 6px 8px;',
    'font-size: 24px;',
    'line-height: 1.05;',
    'text-align: center;',
    'word-break: break-word;',
    'overflow: hidden;',
    'border-radius: 18px;'
  ],
  'PQC process picker production-layout compact option style'
)
assertIncludesAll(
  extractBlock('.frontline-picker--production-process .frontline-picker__close {'),
  ['height: 68px;', 'font-size: 30px;', 'border-radius: 18px;'],
  'PQC process picker production-layout compact close style'
)

console.log('PASS: PQC process picker uses production picker layout')
