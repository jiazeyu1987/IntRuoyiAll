/* Effective offline RED: missing runner; no mocked business success. */
const assert = require('node:assert/strict')
const fs = require('node:fs'), path = require('node:path')
const { createRequire } = require('node:module')
const { spawnSync } = require('node:child_process')
const runner = require('./g30-public-version-relations-runner.cjs')
const base = { mode: 'version', action: 'checkout', taskMarker: '20261001-dcc-integration-g30', frontendUrl: 'http://127.0.0.1:8067/', tenant: '芋道源码', actorDisplayLabel: 'Actual Actor', menuLabels: ['DCC', '受控浏览'], projectName: '20261001-dcc-integration-g30 Project', folderName: 'Actual Folder', versionView: '全部版本', fileNumber: '20261001-dcc-integration-g30-N1', versionNo: 'A/1', expectedFileId: '9007199254740993', expectedMasterId: '9007199254740994', reason: '20261001-dcc-integration-g30 change', expectedCheckoutActor: 'Actual Actor' }
let tests = 0
function bad(c, code) { assert.throws(() => runner.validate(c), e => typeof e.safeCode === 'string' && (!code || e.safeCode.includes(code))); tests++ }
assert.equal(runner.validate(base).expectedFileId, '9007199254740993'); tests++
for (const field of ['expectedFileId', 'expectedMasterId']) for (const value of [9007199254740993, '9223372036854775808', '0', '01']) bad({ ...base, [field]: value }, 'EXACT_ID')
for (const mode of ['obsolete', 'distribution']) bad({ ...base, mode }, 'BLOCKED_UNIMPLEMENTED')
for (const v of [{ password: 'secret' }, { nested: { accessToken: 'secret' } }]) bad({ ...base, ...v }, 'SERIALIZED')
for (const url of ['http://localhost:8081/', 'http://elsewhere:8067/', 'https://localhost:8067/']) bad({ ...base, frontendUrl: url }, 'FRONTEND')
bad({ ...base, action: 'guessed' }, 'VERSION_ACTION')
bad({ ...base, expectedCheckoutActor: undefined })
bad({ ...base, unexplained: true }, 'UNKNOWN')
const reference = { mode: 'reference', action: 'create', taskMarker: base.taskMarker, frontendUrl: base.frontendUrl, tenant: base.tenant, actorDisplayLabel: base.actorDisplayLabel, menuLabels: base.menuLabels, projectName: base.projectName, folderName: base.folderName, versionView: base.versionView, reason: base.reason, selectorCurrentText: 'Actual selected destination', targetProjectName: `${base.taskMarker} Source`, targetFolderName: 'Source Folder', targetFileNumber: `${base.taskMarker}-N2`, targetFileName: `${base.taskMarker}-Source.pdf`, targetVersion: 'A/1', selectorScope: '全局搜索', expectedProjectCount: 1, expectedTargetFileId: '9007199254740995', formalProjectLeaderDisplay: base.actorDisplayLabel }
assert.equal(runner.validate(reference).targetVersion, 'A/1'); tests++
bad({ ...reference, expectedProjectCount: '1' }, 'COUNT')
bad({ ...reference, expectedTargetFileId: true }, 'EXACT_ID')
bad({ ...reference, formalProjectLeaderDisplay: 'Somebody Else' }, 'LEADER')
const r = createRequire(path.resolve(__dirname, '../../../IntRuoyiFronted/package.json')); const ts = r('typescript'); const file = path.join(__dirname, 'g30-public-version-relations-runner.cjs'); const source = fs.readFileSync(file, 'utf8'); const ast = ts.createSourceFile(file, source, ts.ScriptTarget.Latest, true, ts.ScriptKind.JS)
let gotos = 0, launches = 0
function walk(node) { if (ts.isCallExpression(node)) { const e = node.expression; const name = ts.isIdentifier(e) ? e.text : ts.isPropertyAccessExpression(e) ? e.name.text : ''; assert.ok(!['fetch', 'axios', 'evaluate', 'route', 'addInitScript', 'eval', 'Function', 'request'].includes(name)); if (name === 'goto') { gotos++; assert.match(node.getText(ast), /\/login/) } if (name === 'launch') launches++ } ts.forEachChild(node, walk) }
walk(ast); assert.equal(gotos, 1); assert.equal(launches, 1); assert.ok(!source.includes('force: true')); tests++
const temp = path.join(__dirname, `g30-input-${process.pid}.json`)
try { fs.writeFileSync(temp, JSON.stringify(base)); const mismatch = spawnSync(process.execPath, [file, 'reference', temp, '--validate-only'], { encoding: 'utf8' }); assert.equal(mismatch.status, 2); assert.match(mismatch.stdout, /MODE_CONTRACT_MISMATCH/); tests++; fs.writeFileSync(temp, JSON.stringify({ ...base, nested: { password: 'NEVER_PRINT_SECRET' } })); const leaked = spawnSync(process.execPath, [file, 'version', temp, '--validate-only'], { encoding: 'utf8' }); assert.equal(leaked.status, 2); assert.ok(!leaked.stdout.includes('NEVER_PRINT_SECRET') && !leaked.stderr.includes('NEVER_PRINT_SECRET')); tests++ }
finally { fs.unlinkSync(temp) }
console.log(`PASS ${tests} pure G30 mode/identity/credential contract cases; realUI=0`)
