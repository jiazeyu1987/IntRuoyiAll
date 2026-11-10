const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'), 'utf8')
const start = source.indexOf('const normalizeProjectAccessRulePayload =')
const end = source.indexOf('const productOnboardingFormData', start)
const compiled = ts.transpileModule(source.slice(start, end), {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText

function normalize(rows) {
  const errors = []
  const run = new Function('projectAccessRuleFormRows', 'message',
    `${compiled}; return normalizeProjectAccessRulePayload()`)
  return { rules: run({ value: rows }, { error: text => errors.push(text) }), errors }
}

const rule = (subjectId, accessLevel, extra = {}) => ({
  subjectType: 'USER', subjectId, accessLevel, active: true, changeReason: '授权调整', ...extra
})

for (const [first, second] of [['OWNER', 'EDIT'], ['OWNER', 'VIEW'], ['EDIT', 'VIEW'], ['VIEW', 'VIEW']]) {
  test(`reject duplicate subject with ${first}/${second} before saving`, () => {
    const result = normalize([rule(1, 'OWNER'), rule(2, first), rule('2', second)])
    assert.equal(result.rules, null)
    assert.match(result.errors[0], /同一授权主体只能保留一条启用/)
  })
}

test('distinct subjects and disabled duplicate rules retain their exact requested permissions', () => {
  const result = normalize([rule(1, 'OWNER'), rule(2, 'EDIT'), rule(2, 'VIEW', { active: false }),
    rule(2, 'VIEW', { subjectType: 'ROLE' })])
  assert.equal(result.rules.length, 4)
  assert.deepEqual(result.rules.map(row => row.accessLevel), ['OWNER', 'EDIT', 'VIEW', 'VIEW'])
  assert.deepEqual(result.errors, [])
})

test('owner and change reason remain required', () => {
  assert.equal(normalize([rule(1, 'EDIT')]).rules, null)
  assert.equal(normalize([rule(1, 'OWNER', { changeReason: ' ' })]).rules, null)
})
