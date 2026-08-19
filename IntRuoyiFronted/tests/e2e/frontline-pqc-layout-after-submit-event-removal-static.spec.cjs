const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

const template = source.slice(0, source.indexOf('<script'))
const style = source.slice(source.indexOf('<style'))

const extractBraceBlock = (text, marker) => {
  const markerIndex = text.indexOf(marker)
  assert.ok(markerIndex >= 0, `Expected source block ${marker} to exist.`)
  const openBraceIndex = text.indexOf('{', markerIndex)
  assert.ok(openBraceIndex > markerIndex, `Expected source block ${marker} to have an opening brace.`)
  let depth = 0
  for (let index = openBraceIndex; index < text.length; index += 1) {
    if (text[index] === '{') {
      depth += 1
    } else if (text[index] === '}') {
      depth -= 1
      if (depth === 0) {
        return text.slice(markerIndex, index + 1)
      }
    }
  }
  throw new Error(`Expected source block ${marker} to have a closing brace.`)
}

const mainStart = template.indexOf('<main class="frontline-operator-main is-pqc">')
const mainEnd = template.indexOf('\n      </main>', mainStart)
assert.ok(mainStart >= 0 && mainEnd > mainStart, 'PQC main layout block must exist.')
const mainTemplate = template.slice(mainStart, mainEnd)

assert.doesNotMatch(
  template,
  /data-pqc-production-submit-select|<span>生产提交事件<\/span>|frontline-pqc-submit-receipt/,
  'Deleted production-submit-event UI must stay removed from the PQC operator.'
)
assert.match(
  mainTemplate,
  /class="frontline-work-panel frontline-pqc-content-panel"[\s\S]*class="frontline-work-panel frontline-pqc-fill-panel"[\s\S]*class="frontline-pqc-submit-bar"/,
  'The PQC submit bar must belong to the same main grid as the left content and right fill panel.'
)

const mainStyle = extractBraceBlock(style, '.frontline-operator-main')
assert.match(
  mainStyle,
  /&\.is-pqc\s*\{[\s\S]*grid-template-columns:\s*minmax\(620px,\s*1\.28fr\)\s+minmax\(500px,\s*0\.92fr\)/,
  'After deleting production-submit-event, the PQC right fill panel must receive a wider column.'
)
assert.match(
  mainStyle,
  /&\.is-pqc\s*\{[\s\S]*grid-template-rows:\s*minmax\(0,\s*1fr\)\s+104px/,
  'The PQC main grid must reserve a dedicated left-bottom action row.'
)

const contentStyle = extractBraceBlock(style, '.frontline-pqc-content-panel')
const fillStyle = extractBraceBlock(style, '.frontline-pqc-fill-panel')
const submitStyle = extractBraceBlock(style, '.frontline-pqc-submit-bar')
const listStyle = extractBraceBlock(style, '.frontline-pqc-inspection-list')

assert.match(contentStyle, /grid-column:\s*1/, 'PQC content panel must stay in the left column.')
assert.match(contentStyle, /grid-row:\s*1/, 'PQC content panel must occupy only the content row.')
assert.match(fillStyle, /grid-column:\s*2/, 'PQC fill panel must stay in the right column.')
assert.match(fillStyle, /grid-row:\s*1\s*\/\s*3/, 'PQC fill panel must span the content and action rows.')
assert.match(submitStyle, /grid-column:\s*1/, 'PQC action bar must be constrained to the left column.')
assert.match(submitStyle, /grid-row:\s*2/, 'PQC action bar must sit in the left-bottom action row.')
assert.match(
  listStyle,
  /grid-template-rows:\s*auto\s+minmax\(88px,\s*auto\)\s+minmax\(0,\s*1fr\)/,
  'PQC inspection details and item tabs must stay near the top, leaving flexible whitespace only after useful controls.'
)

assert.doesNotMatch(
  [mainStyle, contentStyle, fillStyle, submitStyle, listStyle].join('\n'),
  /position:\s*absolute|margin-(?:top|left|right|bottom):\s*-/,
  'The relayout must use normal grid flow, not absolute positioning or negative margins.'
)

console.log('PASS: frontline PQC layout after production submit event removal static contract')
