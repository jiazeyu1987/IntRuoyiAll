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

test('approval task, route and timeline labels omit the removed owner confirmation duty', () => {
  for (const resolve of [stages.resolveDccStageDisplayName, stages.resolveDccTimelineActivityName]) {
    assert.equal(resolve('MATRIX_APPROVAL', '批准并确认文件负责人'), '会签批准')
    assert.equal(resolve('MATRIX_APPROVAL', '质量批准'), '质量批准')
    assert.equal(resolve('OTHER_TASK', '批准并确认文件负责人'), '批准并确认文件负责人')
  }
})

test('both actual current-task templates render approval without asking to confirm an owner', async () => {
  const { createSSRApp } = require('vue')
  const { renderToString } = require('vue/server-renderer')
  const source = fs.readFileSync(root + '/src/views/dcc/controlled-file/detail/index.vue', 'utf8')
  const templates = source.match(/<div[^>]*>\s*当前任务：[\s\S]*?<\/div>/g)
  assert.equal(templates.length, 2)
  const task = { taskDefinitionKey: 'MATRIX_APPROVAL', name: '批准并确认文件负责人' }
  for (const template of templates) {
    const html = await renderToString(createSSRApp({
      template,
      setup: () => ({ approvalTodoTask: task, resolveDccStageDisplayName: stages.resolveDccStageDisplayName })
    }))
    assert.match(html, /当前任务：会签批准/)
    assert.doesNotMatch(html, /确认文件负责人/)
  }
  assert.equal(task.name, '批准并确认文件负责人')
})
