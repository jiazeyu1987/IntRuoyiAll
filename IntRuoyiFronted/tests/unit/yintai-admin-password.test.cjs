const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const path = require('node:path')
const test = require('node:test')
const ts = require('typescript')
const source = readFileSync(path.join(__dirname, '../../src/views/system/user/systemPasswordPolicy.ts'), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
const policy = { exports: {} }
new Function('exports', compiled.outputText)(policy.exports)
const { isSystemPasswordStrong } = policy.exports

test('requested strong password passes without a weak password exception', () => {
  assert.equal(isSystemPasswordStrong('Int@2025'), true)
  assert.equal(isSystemPasswordStrong('admin123'), false)
})
