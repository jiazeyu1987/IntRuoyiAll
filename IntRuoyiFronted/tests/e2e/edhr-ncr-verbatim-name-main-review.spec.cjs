const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')
const exactModule = { exports: {} }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.resolve(__dirname, '../../src/utils/exactIntegerJson.ts'), 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS } }).outputText, { exports: exactModule.exports })
const { parseExactIntegerJson } = exactModule.exports

// Execute the actual SFC functions without a browser or API writes.
function loadFunctions(relativePath, names) {
  const source = fs.readFileSync(path.resolve(__dirname, '../../src', relativePath), 'utf8')
  const script = source.match(/<script\b[^>]*>([\s\S]*?)<\/script>/)[1]
  const ast = ts.createSourceFile('review.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
  const statements = ast.statements.filter((statement) => ts.isVariableStatement(statement)
    && statement.declarationList.declarations.some((declaration) => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length, 'required production functions must exist')
  const context = { parseExactIntegerJson, resolveUrlPathFileName: () => { throw new Error('authoritative name must not use URL fallback') } }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(
    statements.map((statement) => statement.getText(ast)).join('\n')
      + '\n' + names.map((name) => `globalThis.${name} = ${name};`).join('\n'),
    { compilerOptions: { target: ts.ScriptTarget.ES2020 } }
  ).outputText, context)
  return context
}

const page = loadFunctions('views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue',
  ['parseReviewMaterialsJson', 'resolveReviewMaterialName'])
const detail = loadFunctions('views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue',
  ['resolveNonconformanceReviewMaterials'])
const fileName = '  原件+100%25.pdf  '
const material = { fileId: '9007199254740993', fileName, url: 'https://storage.example/object?signature=local-test' }
const review = { reviewMaterialsJson: JSON.stringify({ activeMaterials: [material] }) }

test('NCR persisted original name survives JSON parsing and rendering verbatim', () => {
  const parsed = page.parseReviewMaterialsJson(review)
  assert.equal(parsed[0].fileName, fileName)
  assert.equal(page.resolveReviewMaterialName(parsed[0]), fileName)
})

test('historical active-order detail preserves original name and opaque file ID together', () => {
  const rendered = detail.resolveNonconformanceReviewMaterials(review)
  assert.equal(rendered[0].fileName, fileName)
  assert.equal(rendered[0].fileId, material.fileId)
})

test('persisted numeric Long identity is exact in NCR and historical detail', () => {
  const numericReview = { reviewMaterialsJson: '{"activeMaterials":[{"fileId":9007199254740993,"fileName":"original.pdf","url":"https://storage.example/original"}]}' }
  assert.equal(String(page.parseReviewMaterialsJson(numericReview)[0].fileId), '9007199254740993')
  assert.equal(String(detail.resolveNonconformanceReviewMaterials(numericReview)[0].fileId), '9007199254740993')
})

test('exact JSON parser preserves quoted content, escapes, decimals and safe integers', () => {
  const text = 'fileId:9007199254740993 "quoted" \\ path'
  const parsed = parseExactIntegerJson('{"text":' + JSON.stringify(text) + ',"small":42,"decimal":1.25,"exponent":1e2,"negative":-9007199254740993,"list":[9223372036854775807]}')
  assert.equal(parsed.text, text)
  assert.equal(parsed.small, 42)
  assert.equal(parsed.decimal, 1.25)
  assert.equal(parsed.exponent, 100)
  assert.equal(parsed.negative, '-9007199254740993')
  assert.equal(parsed.list[0], '9223372036854775807')
})

test('exact JSON parser rejects malformed input rather than repairing it', () => {
  for (const value of ['{"fileId":01}', '{"fileId":1+2}', '{"fileId":9007199254740993,}', '{"name":"unterminated}']) {
    assert.throws(() => parseExactIntegerJson(value))
  }
})
