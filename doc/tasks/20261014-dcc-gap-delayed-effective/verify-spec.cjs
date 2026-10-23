// Own-task documentation validator and read-only source fingerprint capture.
const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto')
const cp = require('node:child_process'), assert = require('node:assert/strict')
const root = path.resolve(__dirname, '../../..'), own = __dirname
const prefixes = {
  DCC: 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/',
  BPM: 'IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/',
  FE: 'IntRuoyiFronted/src/views/dcc/controlled-file/',
  API: 'IntRuoyiFronted/src/api/dcc/controlledFile/',
  JT: 'IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/'
}
const docs = ['task.md', 'execution-log.md', 'design.md', 'contract.md', 'integration-notes.md',
  'review-ready.md', 'changed-files.md', 'verification-report.md']
const decode = bytes => new TextDecoder('utf-8', { fatal: true }).decode(bytes)
const contents = docs.map(name => decode(fs.readFileSync(path.join(own, name))))
for (let i = 0; i < contents.length; i++) {
  assert.ok(contents[i].startsWith('# '), docs[i] + ' missing title')
  assert.ok(!contents[i].includes('\uFFFD'), docs[i] + ' literal replacement character')
}
const all = contents.join('\n'), refs = new Set()
for (const match of all.matchAll(/(?:DCC|BPM|FE|API|JT|IntRuoyiBackend|IntRuoyiFronted)\/[A-Za-z0-9_./-]+\.(?:java|ts|vue|xml|sql|yaml|cjs)/g)) {
  const alias = match[0].split('/')[0]
  refs.add(prefixes[alias] ? prefixes[alias] + match[0].slice(alias.length + 1) : match[0])
}
for (const item of ['IntRuoyiBackend/config/dcc-workflow-v5/upload.bpmn20.xml',
  'IntRuoyiBackend/config/dcc-workflow-v5/revision.bpmn20.xml',
  'IntRuoyiBackend/sql/mysql/20261011_dcc_application_effective_distribution.sql',
  'IntRuoyiBackend/sql/mysql/20261012_dcc_application_execution_contract.sql',
  ...['dcc-upload-ic05-application-options.test.cjs', 'dcc-detail-application-ic05.test.cjs',
    'dcc-upload-effective-activation-times.test.cjs'].map(x => 'IntRuoyiFronted/tests/unit/' + x)]) refs.add(item)
const expectedNew = /(?:DccDelayedEffective|DccControlledFileDelayedEffective|DelayedEffectiveConfirmationPanel|DccWorkflowV6|dcc-delayed-effective|dcc-workflow-v6|20261014_dcc_delayed_effective)/
const missing = [], baseline = [], warnings = []
const hash = data => crypto.createHash('sha256').update(data).digest('hex')
for (const relativePath of [...refs].sort()) {
  const absolute = path.join(root, relativePath)
  if (!fs.existsSync(absolute)) {
    if (expectedNew.test(relativePath)) baseline.push({ path: relativePath, before: 'ABSENT_PROPOSED_NEW' })
    else missing.push(relativePath)
    continue
  }
  const bytes = fs.readFileSync(absolute), txt = decode(bytes)
  const replacementCharacters = txt.split('\uFFFD').length - 1
  if (replacementCharacters) warnings.push({ path: relativePath, literalReplacementCharacters: replacementCharacters })
  baseline.push({ path: relativePath, before: 'EXISTS', rawSha256: hash(bytes),
    normalizedUtf8LfSha256: hash(Buffer.from(txt.replace(/\r\n/g, '\n').replace(/\r/g, '\n'), 'utf8')),
    literalReplacementCharacters: replacementCharacters })
}
assert.deepEqual(missing, [], 'unresolved existing paths')
const anchors = [
  ['DCC/service/file/DccApplicationEffectiveSelection.java', ['isComplete', 'requireSubmission']],
  ['DCC/service/file/DccWorkflowDatePolicy.java', ['requireEffectiveConfiguration', 'requireSubmittedEffectiveConfiguration']],
  ['DCC/service/file/DccControlledFileWorkflowServiceImpl.java', ['submitWorkingIteration', 'createApprovalProcess', 'approveTaskWithProcessDefinitionKey', 'uploadTrainingRecord', 'syncStatusAfterApprove']],
  ['DCC/service/file/DccControlledFileLifecycleService.java', ['recordStandardizationSignatureEffect', 'completeControl', 'activateLocked']],
  ['DCC/service/file/DccControlledFileFinalizationServiceImpl.java', ['finalizeOrdinaryApproval', 'requirePublicEnd']],
  ['DCC/service/file/DccControlledFileSignatureEvidenceServiceImpl.java', ['buildCanonicalPayload', 'payloadVersion']],
  ['DCC/service/file/DccElectronicSignatureManagementServiceImpl.java', ['buildCanonicalPayload', 'verifySignature']],
  ['FE/upload/submitter.ts', ['buildUploadEffectiveChoice', 'buildSubmitPayload', 'buildUploadConfirmationSummary']],
  ['API/applicationRead.ts', ['WorkingApplicationAttributes', 'saveWorkingApplicationAttributes']]
]
for (const [ref, list] of anchors) {
  const alias = ref.split('/')[0]
  const txt = decode(fs.readFileSync(path.join(root, prefixes[alias] + ref.slice(alias.length + 1))))
  for (const anchor of list) assert.ok(txt.includes(anchor), ref + ' anchor ' + anchor)
}
const task = contents[0]
for (const label of ['## Goal', '## Current Status', '## Milestones', '## Expected Verification', '## BDD', '## Cleanup Keep']) assert.ok(task.includes(label))
assert.match(task, /## Current Status\s+(?:blocked|in_progress)/)
for (const name of [...docs, 'source-baseline.json', 'delayed-upload-red.cjs', 'verify-spec.cjs', 'run-private-java.cjs', 'PrivateJUnit.java', 'SelectionProbe.java',
  'a06-write-baseline.json','a07-backend-write-baseline.json','main-handback.json','current-source-checkpoint.json'])
  assert.ok(task.includes('- doc/tasks/20261014-dcc-gap-delayed-effective/' + name), 'Keep ' + name)
const branch = cp.execFileSync('git', ['branch', '--show-current'], { cwd: root, encoding: 'utf8' }).trim()
assert.equal(branch, 'int_qms')
const head = cp.execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim()
const report = { scope: 'SPEC_ONLY_CURRENT_SOURCE_NOT_PRODUCTION_WRITE_BEFORE_SNAPSHOT', branch, head,
  capturedAtUtc: new Date().toISOString(), normalization: 'UTF-8 text, CRLF and CR replaced by LF',
  existingSources: baseline.filter(x => x.before === 'EXISTS').length,
  proposedNewSources: baseline.filter(x => x.before !== 'EXISTS').length, warnings, files: baseline }
const manifest = path.join(own, 'source-baseline.json')
if (process.argv.includes('--current-docs')) {
  report.scope='CURRENT_DOCUMENT_STRUCTURE_AND_REFERENCED_SOURCE_CHECKPOINT_NOT_BEFORE_SNAPSHOT'
  fs.writeFileSync(path.join(own,'current-source-checkpoint.json'),JSON.stringify(report,null,2)+'\n','utf8')
}
else if (process.argv.includes('--capture')) fs.writeFileSync(manifest, JSON.stringify(report, null, 2) + '\n', 'utf8')
else {
  const saved = JSON.parse(decode(fs.readFileSync(manifest)))
  assert.equal(saved.branch, branch)
  assert.equal(saved.head, head)
  // Shared source changes after the SPEC snapshot are explicit review signals, not silently recaptured.
  assert.deepEqual(saved.files, baseline, 'source changed after captured design baseline')
}
process.stdout.write(JSON.stringify({ result: 'PASS', documents: docs.length,
  existingSources: report.existingSources, proposedNewSources: report.proposedNewSources,
  anchorChecks: anchors.reduce((n, [ref, items]) => n + items.length, 0), branch, head,
  utf8Bytes: 'PASS', warnings, scope: 'OWN_TASK_RECORDS_ONLY' }) + '\n')
