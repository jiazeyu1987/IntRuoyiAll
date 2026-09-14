const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)

const styleStart = source.indexOf('<style scoped>')
const mediaStart = source.indexOf('@media', styleStart)
assert.ok(styleStart >= 0 && mediaStart > styleStart, 'QA page scoped style must exist.')
const baseStyle = source.slice(styleStart, mediaStart)

const blockOf = (selector) => {
  const start = baseStyle.indexOf(selector)
  assert.ok(start >= 0, `${selector} must exist in base style.`)
  const open = baseStyle.indexOf('{', start)
  const close = baseStyle.indexOf('}', open)
  assert.ok(open > start && close > open, `${selector} style block must be complete.`)
  return baseStyle.slice(open + 1, close)
}

const headerBlock = blockOf('.qa-regulation-page__header')
assert.match(
  headerBlock,
  /flex-wrap:\s*wrap/,
  'The QA header must wrap at all viewport widths so long project names cannot push the parse button off-screen.'
)

const publishBlock = blockOf('.qa-regulation-page__version-publish')
assert.match(
  publishBlock,
  /flex:\s*1\s+1\s+\d+px/,
  'The publish action cluster must be allowed to shrink and wrap instead of forcing horizontal overflow.'
)
assert.match(
  publishBlock,
  /flex-wrap:\s*wrap/,
  'The publish action cluster must wrap its version/date/save/publish/parse controls.'
)
assert.match(
  publishBlock,
  /min-width:\s*0/,
  'The publish action cluster must not impose a minimum width that hides the parse button.'
)

assert.match(
  baseStyle,
  /data-qa-regulation-word-import[\s\S]*flex-shrink:\s*0/,
  'The parse command itself must stay as a visible action when the action cluster wraps.'
)

console.log('PASS qa-regulation-word-import-button-layout-static')
