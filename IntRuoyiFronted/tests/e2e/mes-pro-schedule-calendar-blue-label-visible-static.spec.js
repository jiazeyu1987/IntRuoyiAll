const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const source = fs.readFileSync(pagePath, 'utf8')
const extractStyleRule = (selector) => {
  const escapedSelector = selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const ruleMatch = source.match(new RegExp(`${escapedSelector}\\s*\\{[^}]*\\}`))
  assert.ok(ruleMatch, `Calendar style rule must exist: ${selector}`)
  return ruleMatch[0]
}

assert.ok(source.includes('class="calendar-day-badge is-primary"'), 'Calendar must keep the blue simulation badge.')
assert.ok(source.includes('class="calendar-day-badge is-editable"'), 'Calendar must keep the blue editable badge.')
assert.ok(source.includes('class="calendar-mode-text"'), 'Calendar must keep the blue shift mode text.')

assert.match(
  source,
  /\.calendar-cell-head\s*\{[\s\S]*align-items:\s*flex-start;[\s\S]*justify-content:\s*space-between;[\s\S]*flex-wrap:\s*wrap;[\s\S]*gap:\s*6px\s+8px;/,
  'Calendar cell head must wrap narrow header content instead of clipping blue labels.'
)
assert.match(
  source,
  /\.calendar-day-meta\s*\{[\s\S]*flex:\s*1\s+1\s+72px;[\s\S]*min-width:\s*0;[\s\S]*flex-wrap:\s*wrap;[\s\S]*gap:\s*6px;/,
  'Calendar day metadata must allow badges to wrap inside narrow cells.'
)
assert.match(
  source,
  /\.calendar-mode-text\s*\{[\s\S]*flex:\s*0\s+0\s+auto;[\s\S]*max-width:\s*100%;[\s\S]*line-height:\s*22px;/,
  'Calendar blue shift mode text must remain visible and line-height aligned when wrapping.'
)
assert.ok(
  !extractStyleRule('.calendar-mode-text').includes('white-space: nowrap'),
  'Calendar blue shift mode text must not force nowrap inside narrow cells.'
)
assert.doesNotMatch(source, /catch\s*\{\s*\}/, 'Calendar label visibility fix must not introduce empty catch blocks.')

console.log('PASS: MES schedule calendar blue labels visible static contract')
