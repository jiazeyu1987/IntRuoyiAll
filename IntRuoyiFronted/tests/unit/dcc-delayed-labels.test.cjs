const { test } = require('node:test'), assert = require('node:assert/strict'), fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const root = require('node:path').resolve(__dirname, '../..')
function load(file, resolve) {
  const c = { exports: {}, Error, Map, Set, require: resolve }
  vm.runInNewContext(ts.transpileModule(fs.readFileSync(root + '/src/' + file, 'utf8'), {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText, c); return c.exports
}
const projection = load('api/form-center/actionProjection.ts')
const lifecycle = load('views/dcc/controlled-file/shared/lifecycle.ts', id => {
  assert.equal(id, '@/api/form-center/actionProjection'); return projection
})
const stages = load('views/dcc/controlled-file/shared/stage-name.ts')
test('formal new pending status and task placeholders have their exact Chinese labels', () => {
  assert.equal(lifecycle.getDccControlledFileStatusLabel('PENDING_DELAYED_EFFECTIVE_CONFIRM'), '待延迟生效确认')
  assert.equal(lifecycle.DCC_CONTROLLED_FILE_STATUSES.includes('PENDING_DELAYED_EFFECTIVE_CONFIRM'), true)
  assert.equal(stages.resolveDccStageDisplayName('DELAYED_EFFECTIVE_CONFIRM', '???'), '延迟生效确认')
  assert.equal(stages.resolveDccTimelineActivityName('DELAYED_EFFECTIVE_CONFIRM', null), '延迟生效确认')
})
test('a label adds no public matrix stage, withdrawal or sensitive action permission', () => {
  assert.equal(lifecycle.DCC_CONTROLLED_FILE_STAGE_DEFINITIONS.length, 4)
  assert.equal(lifecycle.isDccControlledFileWithdrawableStatus('PENDING_DELAYED_EFFECTIVE_CONFIRM'), false)
  for (const action of ['PUBLISH', 'DOWNLOAD', 'PRINT', 'MAJOR_REVISION', 'MANUAL_RELEASE', 'OBSOLETE']) {
    const missing = { status: 'PENDING_DELAYED_EFFECTIVE_CONFIRM' }
    assert.equal(lifecycle.isDccControlledFileActionAllowed(missing, action), false)
    const blocked = { ...missing, actionProjection: { actionLocked: true, allowedActions: [], canWithdraw: false } }
    assert.equal(lifecycle.isDccControlledFileActionAllowed(blocked, action), false)
  }
})
test('old statuses and explicitly recorded historical task names keep their labels', () => {
  assert.equal(lifecycle.getDccControlledFileStatusLabel('ACTIVE'), '受控（已生效）')
  assert.equal(lifecycle.getDccControlledFileStatusLabel('PENDING_DOC_CONTROL_REVIEW'), '待文控审核')
  assert.equal(stages.resolveDccStageDisplayName('DOC_CONTROL_APPROVAL', null), '文控批准')
  assert.equal(stages.resolveDccStageDisplayName('DELAYED_EFFECTIVE_CONFIRM', '历史正式任务原名'), '历史正式任务原名')
})
