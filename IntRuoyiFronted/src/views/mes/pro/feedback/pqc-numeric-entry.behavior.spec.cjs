const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const source = fs.readFileSync(`${__dirname}/pqcNumericEntry.ts`, 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const testModule = { exports: {} }
new Function('exports', 'module', compiled)(testModule.exports, testModule)
const { parsePqcNumericColumn, isPqcNumericValue } = testModule.exports

const samples = Array.from({ length: 150 }, (_, i) => (10 + i / 100).toFixed(2))
for (const newline of ['\n', '\r\n', '\r']) {
  const parsed = parsePqcNumericColumn(samples.join(newline) + newline, 150)
  assert.deepEqual(parsed.errors, [])
  assert.deepEqual(parsed.values, samples, 'Preserve sample order and measured decimal places')
}
assert.equal(parsePqcNumericColumn('1\n\n3', 3).errors.some(s => s.includes('第 2 行为空')), true)
assert.equal(parsePqcNumericColumn('1\t2\n3', 2).errors.some(s => s.includes('多列')), true)
assert.equal(parsePqcNumericColumn('1\n2', 150).errors.some(s => s.includes('当前 2 行')), true)
assert.equal(parsePqcNumericColumn(samples.concat('99').join('\n'), 150).errors.some(s => s.includes('当前 151 行')), true)
assert.equal(parsePqcNumericColumn('1\n\n', 2).errors.some(s => s.includes('第 2 行为空')), true)
for (const value of ['', ' ', 'NaN', 'Infinity', '1e999', '1e-3', '0x10', '1,23', '12 mm', '--1']) {
  assert.equal(isPqcNumericValue(value), false, value)
}
for (const value of ['0', '-0.02', '+12.000', '.5']) assert.equal(isPqcNumericValue(value), true, value)
assert.equal(isPqcNumericValue('12.000', 0), true)
assert.equal(isPqcNumericValue('12.0100', 2), true)
assert.equal(isPqcNumericValue('12.001', 2), false)
assert.equal(isPqcNumericValue('12.1', 0), false)
assert.deepEqual(parsePqcNumericColumn(samples.join('\n'), 150, 2).errors, [])
assert.match(parsePqcNumericColumn('12.001', 1, 2).errors[0], /小数精度/)
console.log('PASS: 150-sample paste order/precision, line endings, blank/multi-column/count errors, finite decimal validation')
