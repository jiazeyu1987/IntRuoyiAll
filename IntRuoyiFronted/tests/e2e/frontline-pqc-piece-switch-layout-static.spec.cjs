const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panelPath = path.join(
  root,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const source = fs.readFileSync(panelPath, 'utf8').replace(/\r\n/g, '\n')

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

const pieceStart = source.indexOf('data-pqc-piece-modal')
const pieceEnd = source.indexOf('\n      </div>', pieceStart)
assert.ok(pieceStart >= 0 && pieceEnd > pieceStart, 'PQC piece dialog template must exist.')
const pieceTemplate = source.slice(pieceStart, pieceEnd)

assert.equal(
  (pieceTemplate.match(/data-pqc-piece-choice-switch/g) || []).length,
  1,
  'Choice piece cards must render one switch control in the v-for template.'
)
assert.match(
  pieceTemplate,
  /<el-switch[\s\S]*data-pqc-piece-choice-switch[\s\S]*inline-prompt/,
  'Choice piece cards must use one inline switch.'
)
assert.match(
  pieceTemplate,
  /:model-value="pqcPieceDraftValues\[pieceIndex - 1\] === '合格'"/,
  'The switch must receive a valid boolean model without normalizing an empty draft to a result.'
)
assert.match(
  pieceTemplate,
  /active-text="合格"/,
  'The active switch state must visibly represent 合格.'
)
assert.match(
  pieceTemplate,
  /inactive-text="不合格"/,
  'The inactive switch state must visibly represent 不合格.'
)
assert.match(
  pieceTemplate,
  /@update:model-value="updatePqcPieceChoice\(pieceIndex - 1, \$event\)"/,
  'The switch must update the existing per-piece draft value through an explicit handler.'
)
assert.doesNotMatch(
  pieceTemplate,
  /class="pass"|class="fail"|>\s*合格\s*<\/button>|>\s*不合格\s*<\/button>/,
  'Choice piece cards must not keep the old two-button presentation.'
)
assert.doesNotMatch(
  pieceTemplate,
  /待选择|is-unset/,
  'Choice piece cards must not keep a pending display state when the product default is all-pass.'
)

const mapInspectionItemBlock = extractBraceBlock(source, 'const mapPqcInspectionItem =')
assert.match(
  mapInspectionItemBlock,
  /defaultValue:\s*isPqcNumericResultType\(item\.resultType\)[\s\S]*:\s*'合格'/,
  'Choice inspection items must default each piece to the formal 合格 value.'
)

const updateChoiceFunctionStart = source.indexOf('const updatePqcPieceChoice =')
const updateChoiceFunctionEnd = source.indexOf('\nconst stepPqcPieceValue =', updateChoiceFunctionStart)
assert.ok(
  updateChoiceFunctionStart >= 0 && updateChoiceFunctionEnd > updateChoiceFunctionStart,
  'The explicit per-piece choice update handler must exist.'
)
const updateChoiceFunction = source.slice(updateChoiceFunctionStart, updateChoiceFunctionEnd)
assert.match(
  updateChoiceFunction,
  /value === true[\s\S]*pqcPieceDraftValues\.value\[index\] = '合格'/,
  'The switch handler must map its active state to the formal 合格 value.'
)
assert.match(
  updateChoiceFunction,
  /value === false[\s\S]*pqcPieceDraftValues\.value\[index\] = '不合格'/,
  'The switch handler must map its inactive state to the formal 不合格 value.'
)
assert.match(
  updateChoiceFunction,
  /showFrontlineError\(/,
  'Unexpected switch values must remain visible as an error instead of being silently accepted.'
)

const pieceListStyle = extractBraceBlock(source, '.frontline-pqc-piece-list')
const pieceRowStyle = extractBraceBlock(source, '.frontline-pqc-piece-row')
const pieceChoiceStyle = extractBraceBlock(source, '.frontline-pqc-piece-choice')
const pieceSwitchStyle = extractBraceBlock(source, '.frontline-pqc-piece-switch')

assert.match(
  pieceListStyle,
  /grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\)/,
  'The desktop piece grid must keep five content columns.'
)
assert.match(
  pieceListStyle,
  /grid-auto-rows:\s*max-content/,
  'Piece grid rows must size from their content instead of stretching to the dialog height.'
)
assert.match(
  pieceListStyle,
  /align-content:\s*start/,
  'Content-sized rows must stay at the top of the scrollable list.'
)
assert.match(
  pieceRowStyle,
  /grid-template-rows:\s*auto\s+auto/,
  'Piece cards must use content-sized internal rows.'
)
assert.match(pieceRowStyle, /align-self:\s*start/, 'Piece cards must not stretch to the grid row height.')
assert.match(pieceRowStyle, /height:\s*fit-content/, 'Piece card height must follow its content.')
assert.match(pieceRowStyle, /min-height:\s*0/, 'Piece cards must not keep the old fixed minimum height.')
assert.match(pieceChoiceStyle, /display:\s*flex/, 'A single switch needs a flexible content row.')
assert.doesNotMatch(
  pieceChoiceStyle,
  /grid-template-columns:\s*1fr\s+1fr/,
  'Choice cards must not reserve two fixed button columns.'
)
assert.match(pieceSwitchStyle, /width:\s*100%/, 'The switch must use the available card width.')
assert.match(
  pieceSwitchStyle,
  /:deep\(\.el-switch__inner \.is-text\)[\s\S]*font-size:\s*24px/,
  'Switch labels must remain readable at the operator scale.'
)

console.log('PASS: frontline PQC piece switch and content-sized layout static contract')
