const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const viewSource = fs.readFileSync(path.join(__dirname, 'FrontlineFixedTemplatePanel.vue'), 'utf8')

const findRule = (selector) => {
  const index = viewSource.indexOf(selector)
  assert.notEqual(index, -1, `missing selector: ${selector}`)
  const bodyStart = viewSource.indexOf('{', index)
  assert.notEqual(bodyStart, -1, `missing rule body for selector: ${selector}`)
  let depth = 0
  for (let cursor = bodyStart; cursor < viewSource.length; cursor += 1) {
    const char = viewSource[cursor]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return viewSource.slice(bodyStart + 1, cursor)
      }
    }
  }
  assert.fail(`unterminated rule body for selector: ${selector}`)
}

const panelRule = findRule('.frontline-operator-panel.is-pqc-fullscreen,')
assert.match(
  panelRule,
  /padding:\s*12px\s+16px;/,
  'PQC fullscreen outer panel should only keep a small breathing space instead of the old large red-box margin.'
)
assert.doesNotMatch(
  panelRule,
  /padding:\s*2[0-9]px/,
  'PQC fullscreen outer panel must not keep the old 20px+ padding.'
)

const screenRule = findRule(
  '.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-screen.is-pqc,'
)
assert.match(screenRule, /width:\s*100%;/, 'PQC fullscreen screen should fill the outer panel width.')
assert.match(screenRule, /max-width:\s*none;/, 'PQC fullscreen screen should not be capped by a fixed max-width.')
assert.match(screenRule, /height:\s*100%;/, 'PQC fullscreen screen should fill the outer panel height.')
assert.match(screenRule, /min-height:\s*0;/, 'PQC fullscreen screen should not force unused vertical blank space.')
assert.doesNotMatch(
  screenRule,
  /max-width:\s*1480px/,
  'PQC fullscreen screen must remove the old 1480px cap that created large left/right whitespace.'
)

const productionPanelRule = findRule('.frontline-operator-panel.is-production-fullscreen,')
assert.match(
  productionPanelRule,
  /padding:\s*0;/,
  'production fullscreen must keep its existing zero outer padding.'
)
assert.match(
  productionPanelRule,
  /place-items:\s*center;/,
  'production fullscreen must keep its existing centered layout.'
)

const mainRule = findRule(
  '.frontline-operator-panel.is-pqc-fullscreen .frontline-operator-main.is-pqc,'
)
assert.match(
  mainRule,
  /grid-template-columns:\s*minmax\(0,\s*1\.28fr\)\s+minmax\(0,\s*0\.92fr\);/,
  'PQC fullscreen main grid columns should be fluid so the content absorbs the freed horizontal space.'
)
assert.match(
  mainRule,
  /grid-template-rows:\s*minmax\(0,\s*1fr\)\s+112px;/,
  'PQC fullscreen main grid should keep a fixed footer row and let content absorb remaining height.'
)
