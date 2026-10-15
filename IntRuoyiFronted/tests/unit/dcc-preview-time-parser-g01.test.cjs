const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const root = require('node:path').resolve(__dirname, '../..'), source = fs.readFileSync(root + '/src/api/dcc/controlledFile/workflow.ts', 'utf8')
const ast = ts.createSourceFile('api.ts', source, ts.ScriptTarget.Latest, true)
const st = ast.statements.find(s => s.declarationList?.declarations.some(d => d.name.text === 'parseControlledFilePreviewMetadata'))
const c = { exports: {}, Error, Date, DccControlledFileContractError: Error, assertRecordPayload: x => x, assertNoForbiddenDccFileCapabilityFields() {},
  readOptionalString: (x, k) => x[k] == null ? undefined : typeof x[k] === 'string' ? x[k] : (() => { throw Error('invalid type') })(),
  assertRequiredString: (x,k) => x[k], assertRequiredPreviewKind: (x,k) => x[k], assertRequiredObject: (x,k) => x[k], parseControlledPreviewWatermark: x => x }
vm.runInNewContext(ts.transpileModule(st.getText(ast), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, c)
test('actual parser rejects normalized impossible calendars and UTC offsets above 18', () => {
  for(const previewRegisteredAt of ['2026-02-30T00:00:00Z','2025-02-29T00:00:00Z','2026-10-15T00:00:00+19:00','2026-10-15T00:00:00+18:01','2026-10-15T24:00:00Z'])
    assert.throws(() => c.exports.parseControlledFilePreviewMetadata({ previewRegisteredAt }))
})
test('actual parser preserves legal server time and unavailable reason before required registration', () => {
  for(const time of ['2024-02-29T00:00:00Z','2026-10-15T00:00:00.123456+08:00','2026-10-15T00:00:00+18:00'])
    assert.equal(c.exports.parseControlledFilePreviewMetadata({previewRegisteredAt:time}).previewRegisteredAt,time)
  assert.throws(() => c.exports.parseControlledFilePreviewMetadata({previewRegisteredAt:null,previewUnavailableReason:'本版本没有正文预览权限'}), /本版本没有正文预览权限/)
  assert.throws(() => c.exports.parseControlledFilePreviewMetadata({previewRegisteredAt:null}))
})
