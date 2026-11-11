const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { ref } = require('vue')
const { parse } = require('vue/compiler-sfc')
const filename = path.resolve(__dirname,'../src/views/mes/pro/edhr-batch/components/MarketReleaseReceipt.vue')
const { descriptor, errors } = parse(fs.readFileSync(filename,'utf8'))
assert.deepEqual(errors,[])
const ast = ts.createSourceFile('receipt.ts',descriptor.scriptSetup.content,ts.ScriptTarget.Latest,true)
const declaration = ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)==='openReceipt'))
const code = ts.transpileModule(declaration.getText(ast),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.None}}).outputText
const valid = () => ({receiptId:'2120280699796520962',idempotencyKey:'original',prepareId:'prepare',signatureId:'63686',evidenceHash:'hash',resultCode:'APPROVE',resultingStatus:'RELEASED',committedAt:'time'})
function harness({result=valid(),props={},release={}}={}) {
  const calls=[],emitted=[]
  const ctx={props:{releaseTransactionId:'295',idempotencyKey:'original',sourceTaskId:'3006',prepareId:'prepare',...props},
    visible:ref(false),loading:ref(false),error:ref(''),receipt:ref(null),mounted:true,userStore:{getUser:{id:7}},
    identity:()=>ctx.session+'|'+JSON.stringify(ctx.props),session:'168:7',
    getEdhrRelease:async id=>{calls.push(['release',id]);return {releaseTransactionId:'295',approvedBy:'7',approvalIdempotencyKey:'original',releaseApprovalWorkTaskId:'3006',releaseStatus:'RELEASED',approvalSignoffEvidenceHash:'hash',...release}},
    getApprovalReviewReceipt:async query=>{calls.push(['receipt',query]);return result},emit:(...args)=>emitted.push(args)}
  const open = Function(...Object.keys(ctx),code+';return openReceipt;')(...Object.values(ctx))
  return {ctx,calls,emitted,open}
}
test('pending request queries exact original key and prepare, with no approval write',async()=>{
  const h=harness();await h.open()
  assert.deepEqual(h.calls,[['receipt',{moduleCode:'EDHR',sourceTaskType:'EDHR_WORK_TASK',sourceTaskId:'3006',idempotencyKey:'original',prepareId:'prepare'}]])
  assert.equal(h.emitted[0][0],'confirmed');assert.equal(h.ctx.receipt.value.signatureId,'63686')
})
test('uncommitted result stays unknown and never emits success',async()=>{
  const h=harness({result:null});await h.open();assert.equal(h.ctx.receipt.value,null);assert.equal(h.emitted.length,0)
})
for(const mismatch of [{idempotencyKey:'other'},{prepareId:'other'},{signatureId:''},{evidenceHash:''},{resultCode:'REJECT'},{resultingStatus:'PENDING_APPROVAL'}]) {
  test('rejects mismatched formal receipt '+JSON.stringify(mismatch),async()=>{
    const h=harness({result:{...valid(),...mismatch}});await h.open()
    assert.equal(h.ctx.receipt.value,null);assert.equal(h.emitted.length,0);assert.match(h.ctx.error.value,/不一致/)
  })
}
test('history reads original transaction and refuses another signer',async()=>{
  const h=harness({props:{idempotencyKey:undefined,sourceTaskId:undefined,prepareId:undefined},release:{approvedBy:'8'}})
  await h.open();assert.equal(h.calls.length,1);assert.equal(h.emitted.length,0);assert.match(h.ctx.error.value,/本人/)
})
test('history uses persisted original key without creating a new request',async()=>{
  const h=harness({props:{idempotencyKey:undefined,sourceTaskId:undefined,prepareId:undefined}})
  await h.open();assert.equal(h.calls[1][1].idempotencyKey,'original');assert.equal(h.emitted.length,1)
})
test('history rejects a receipt whose evidence differs from the released transaction',async()=>{
  const h=harness({props:{idempotencyKey:undefined,sourceTaskId:undefined,prepareId:undefined},release:{approvalSignoffEvidenceHash:'other'}})
  await h.open();assert.equal(h.ctx.receipt.value,null);assert.equal(h.emitted.length,0);assert.match(h.ctx.error.value,/不一致/)
})
test('tenant or dialog context change discards delayed response',async()=>{
  const h=harness();h.ctx.getApprovalReviewReceipt=async()=>{h.ctx.session='169:7';return valid()}
  const open=Function(...Object.keys(h.ctx),code+';return openReceipt;')(...Object.values(h.ctx))
  await open();assert.equal(h.ctx.receipt.value,null);assert.equal(h.emitted.length,0)
})
