const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { runInNewContext } = require('node:vm')
const { test } = require('node:test')
const assert = require('node:assert/strict')
const ts = require('typescript')

const source = readFileSync(resolve(__dirname, '../src/views/mes/pro/scheduleorder/scheduleQuantity.ts'), 'utf8')
const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } })
const context = { exports: {} }
runInNewContext(compiled.outputText, context)
const { formatQuantity, wholePieceScheduleHint } = context.exports

test('fractional demand stays visible and explains whole piece scheduling', () => {
  assert.equal(formatQuantity(2019 * 0.8), '1615.2')
  assert.equal(wholePieceScheduleHint(2019 * 0.8), '剩余按 1616 个排程')
  assert.equal(Math.ceil(Math.ceil(2019 * 0.8) * 60 / 50), 1940)
})

test('the hint uses remaining demand after production feedback', () => {
  assert.equal(wholePieceScheduleHint(15.2), '剩余按 16 个排程')
  assert.equal(wholePieceScheduleHint(0), '')
  assert.equal(wholePieceScheduleHint(2019), '')
})

test('missing quantity is distinct from zero', () => {
  assert.equal(formatQuantity(0), '0')
  assert.equal(formatQuantity(null), '—')
  assert.equal(formatQuantity(NaN), '—')
  assert.equal(wholePieceScheduleHint(null), '')
})
