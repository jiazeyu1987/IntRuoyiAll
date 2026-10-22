/* Offline input/AST/capture-contract tests; no business-success mock or browser. */
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')
const { spawnSync } = require('node:child_process')
const runner = require('./g29-public-lifecycle-runner.cjs')
const req = createRequire(path.resolve(__dirname, '../../../IntRuoyiFronted/package.json'))
const ts = req('typescript')
const filename = path.join(__dirname, 'g29-public-lifecycle-runner.cjs')
const source = fs.readFileSync(filename, 'utf8')
const ast = ts.createSourceFile(filename, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.JS)
let cases = 0
const c = { mode: 'project-product', action: 'create', taskMarker: '20261001-dcc-integration-g29', frontendUrl: 'http://127.0.0.1:8067/', tenant: '芋道源码', actorDisplayLabel: 'Actual Actor', menuLabels: ['DCC', '产品目录'], postconditions: [{ scope: 'project-records', text: 'PENDING_REVIEW', exact: true }], expectedSavedStatus: 'PENDING_REVIEW', projectCode: '20261001-dcc-integration-g29-P1', productCode: '20261001-dcc-integration-g29-PROD1', projectName: '20261001-dcc-integration-g29 Project', productName: '20261001-dcc-integration-g29 Product', reason: '20261001-dcc-integration-g29 reason', leaderOptionLabel: 'Actual Leader（leader）', folderTemplateLabel: 'Actual Template', reviewerOptionLabel: 'Actual Reviewer（reviewer）', classification: '一类', attributes: { markets: ['CE 欧盟'], licenseHolder: '是', actualManufacturer: '否', documentTransfer: '否' } }
function bad(input, mode, code) { assert.throws(() => runner.validate(input, mode), error => typeof error.safeCode === 'string' && (!code || error.safeCode.includes(code))); cases++ }
assert.equal(runner.validate(structuredClone(c), c.mode).projectCode, c.projectCode); cases++
for (const mode of runner.MODES.filter(m => !runner.SUPPORTED.has(m))) bad({ mode }, mode, 'BLOCKED_UNIMPLEMENTED')
for (const marker of ['wrong', '', '20261001-dcc-integration/escape']) bad({ ...c, taskMarker: marker }, c.mode)
for (const frontendUrl of ['http://localhost:8081/', 'http://remote:8067/', 'https://localhost:8067/', 'http://user:password@localhost:8067/', 'http://localhost:8067/?id=1']) bad({ ...c, frontendUrl }, c.mode, 'FRONTEND')
for (const payload of [{ password: 'secret' }, { nested: [{ authToken: 'secret' }] }, { bytes: Buffer.from('secret') }, { extra: { storageState: {} } }]) bad({ ...c, ...payload }, c.mode, 'SERIALIZED')
for (const field of ['menuLabels', 'postconditions', 'expectedSavedStatus', 'reason', 'attributes', 'projectCode']) { const n = structuredClone(c); delete n[field]; bad(n, c.mode) }
bad({ ...c, postconditions: [{ scope: 'body', text: c.projectCode, exact: true }] }, c.mode, 'FORMAL_SAVED_POSTCONDITION')
bad({ ...c, hiddenUnknown: true }, c.mode, 'UNKNOWN_CONTRACT')
bad({ ...c, postconditions: [{ scope: 'project-records', text: 'PENDING_REVIEW', exact: 'true' }] }, c.mode, 'POSTCONDITION_FIELDS')
const forbidden = new Set(['fetch', 'axios', 'eval', 'Function'])
let launches = 0, inputCalls = 0
function walk(node) {
  if (ts.isCallExpression(node)) {
    const expr = node.expression
    const name = ts.isIdentifier(expr) ? expr.text : ts.isPropertyAccessExpression(expr) ? expr.name.text : ''
    assert.ok(!forbidden.has(name) && !['evaluate', 'route', 'addInitScript', 'request', 'setStorageState'].includes(name), `forbidden bridge ${name}`)
    if (name === 'launch') launches++
    if (name === 'setInputFiles') inputCalls++
  }
  ts.forEachChild(node, walk)
}
walk(ast); assert.equal(launches, 1); assert.ok(inputCalls >= 2); assert.ok(source.includes("await pause(context, capture)")); assert.ok(source.indexOf('await pause(context, capture)') < source.indexOf('await input.fill(password)')); cases++
const temp = path.join(__dirname, `g29-invalid-contract-${process.pid}.json`)
try { fs.writeFileSync(temp, JSON.stringify({ ...c, nested: { password: 'DO_NOT_PRINT_THIS' } })); const result = spawnSync(process.execPath, [filename, c.mode, temp, '--validate-only'], { encoding: 'utf8' }); assert.equal(result.status, 2); assert.ok(!result.stdout.includes('DO_NOT_PRINT_THIS') && !result.stderr.includes('DO_NOT_PRINT_THIS')); assert.match(result.stdout, /BLOCKED_BEFORE_BROWSER/); cases++ }
finally { fs.unlinkSync(temp) }
console.log(`PASS ${cases} offline input/AST/security cases; browser/network/API/DB actions=0`)
