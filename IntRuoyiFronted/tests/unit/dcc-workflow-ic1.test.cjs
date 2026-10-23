const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vm = require('node:vm')
const { parse, compileScript, compileTemplate } = require('vue/compiler-sfc')
const source = fs.readFileSync('src/views/dcc/controlled-file/workflow/workflow-actions.ts', 'utf8')
const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText
const ctx = { exports: {}, Error }; vm.runInNewContext(code, ctx)
const { submitSignoffAssignment, lifecyclePresentation } = ctx.exports
test('training upload sessions bind the formal file and opaque workflow round', () => {
  const session=ctx.exports.trainingUploadSession('9007199254740993','bpm:round-1','client-session')
  assert.equal(session,'dcc-training:9007199254740993:11:bpm:round-1:client-session')
  assert.notEqual(session,ctx.exports.trainingUploadSession('9007199254740994','bpm:round-1','client-session'))
  assert.notEqual(session,ctx.exports.trainingUploadSession('9007199254740993','bpm:round-2','client-session'))
})
test('invalid or overlength training identities cannot form an upload ticket session', () => {
  assert.throws(()=>ctx.exports.trainingUploadSession(9007199254740993,'round','client'))
  assert.throws(()=>ctx.exports.trainingUploadSession('42','','client'))
  assert.throws(()=>ctx.exports.trainingUploadSession('42','round',''))
  assert.throws(()=>ctx.exports.trainingUploadSession('9223372036854775807','r'.repeat(64),'c'.repeat(64)))
  assert.ok(ctx.exports.trainingUploadSession('9223372036854775807','r'.repeat(64),'nonce').length<=128)
})
test('self assignment preserves ID and awaits saved fact', async () => {
  let calls = 0
  const result = await submitSignoffAssignment({ taskId:'t-1', assigneeUserId:'9007199254740993',password:'pwd',reason:' assign ' },async req => {
    calls++; assert.equal(req.assigneeUserId,'9007199254740993'); assert.equal(req.reason,'assign'); return true
  })
  assert.equal(result.success,true); assert.equal(calls,1)
})
test('signature failure is visible and never reports assignment success', async () => {
  const result=await submitSignoffAssignment({ taskId:'t-1',assigneeUserId:99,password:'wrong',reason:'assign' },async () => { throw new Error('签名密码错误') })
  assert.equal(result.success,false); assert.equal(result.error,'签名密码错误')
})
test('missing password and reason block the save command', async () => {
  let calls=0
  for(const form of [{password:'',reason:'a'},{password:'p',reason:''}]) {
    const result=await submitSignoffAssignment({taskId:'t',assigneeUserId:99,...form},async()=>{calls++})
    assert.equal(result.success,false)
  }
  assert.equal(calls,0)
})
test('future controlled distribution and activation are independent', () => {
  const facts={controlledTime:'2026-10-01 08:00',effectiveDate:'2026-10-10',status:'CONTROLLED_PENDING_EFFECTIVE'}
  const state=lifecyclePresentation(facts); assert.equal(state.controlled,true);assert.equal(state.executable,false)
  assert.match(state.warning,/生效前不得执行/);assert.equal(state.distributionCompleted,false)
  assert.equal(lifecyclePresentation({...facts,distributedTime:'2026-10-01 09:00'}).executable,false)
  assert.equal(lifecyclePresentation({...facts,status:'ACTIVE',activatedTime:'2026-10-10 00:00'}).executable,true)
})
test('rejected application explains new-round signoff', () => {
  assert.match(lifecyclePresentation({effectiveDate:'2026-10-10',status:'REJECTED'}).reworkHint,/重新指派、会签和批准/)
})

test('unsafe numeric and malformed signing identities never reach the save command', async () => {
  let saves = 0
  for (const id of [9007199254740993, '0', '-1', ' 99', 'not-an-account']) {
    const result = await submitSignoffAssignment({taskId: 'task-1', assigneeUserId: id,
      password: 'test-credential', reason: '指派'}, async () => { saves++ })
    assert.equal(result.success, false)
  }
  assert.equal(saves, 0)
})

test('obsolete and rejected files never expose a distribution action', () => {
  for (const status of ['OBSOLETE', 'SUPERSEDED', 'REJECTED', 'PENDING_DOC_CONTROL_REVIEW']) {
    assert.equal(lifecyclePresentation({controlledTime: '2026-10-01', effectiveDate: '2026-10-10', status}).canDistribute, false)
  }
  assert.equal(lifecyclePresentation({controlledTime: '2026-10-01', effectiveDate: '2026-10-10',
    status: 'CONTROLLED_PENDING_EFFECTIVE'}).canDistribute, true)
})
test('both independent Vue components compile without syntax errors', () => {
  for(const file of ['SignoffAssignmentPanel','ControlledLifecyclePanel']) {
    const src=fs.readFileSync(`src/views/dcc/controlled-file/workflow/${file}.vue`,'utf8')
    const { descriptor,errors }=parse(src);assert.equal(errors.length,0)
    const script=compileScript(descriptor,{id:file});assert.ok(script.content)
    const compiled=compileTemplate({source:descriptor.template.content,filename:file,id:file})
    assert.equal(compiled.errors.length,0)
  }
})
