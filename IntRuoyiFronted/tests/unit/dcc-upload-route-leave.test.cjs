const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('vue/compiler-sfc')
const source = parse(fs.readFileSync('src/views/dcc/controlled-file/upload/index.vue', 'utf8')).descriptor.scriptSetup.content
const ast = ts.createSourceFile('upload.ts', source, ts.ScriptTarget.Latest, true)
const hook = ast.statements.find(s => ts.isExpressionStatement(s) && ts.isCallExpression(s.expression) && s.expression.expression.getText(ast) === 'onBeforeRouteLeave')
function setup({busy=false, submitted=false, cleanup=true}={}) {
 const calls=[]
 const c={exports:{},onBeforeRouteLeave:f=>c.exports.leave=f,submitLoading:{value:busy},uploadSubmitted:{value:submitted},
  cleanupCurrentUploadSession:async()=>{calls.push('cleanup');return cleanup},
  resetSelectedPreview:()=>calls.push('source'),resetDrawingPdfUpload:()=>calls.push('drawing'),resetAttachmentUploads:()=>calls.push('attachments'),
  uploadRef:{value:{clearFiles:()=>calls.push('source-input')}},drawingPdfUploadRef:{value:{clearFiles:()=>calls.push('drawing-input')}},attachmentUploadRef:{value:{clearFiles:()=>calls.push('attachment-input')}},
  uploadSessionId:'OLD',createControlledFileUploadSessionId:()=> 'NEW'}
 vm.runInNewContext(ts.transpileModule(hook.getText(ast),{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 return {c,calls}
}
test('route navigation cannot clean or leave while formal submit binds its ticket',async()=>{
 const {c,calls}=setup({busy:true});assert.equal(await c.exports.leave(),false);assert.deepEqual(calls,[]);assert.equal(c.uploadSessionId,'OLD')
})
test('successful unsent leave invalidates cleaned cached tickets and rotates the session',async()=>{
 const {c,calls}=setup();assert.equal(await c.exports.leave(),true)
 assert.deepEqual(calls,['cleanup','source','drawing','attachments','source-input','drawing-input','attachment-input']);assert.equal(c.uploadSessionId,'NEW')
})
test('cleanup refusal retains the draft and prevents leaving rather than simulating success',async()=>{
 const {c,calls}=setup({cleanup:false});assert.equal(await c.exports.leave(),false);assert.deepEqual(calls,['cleanup']);assert.equal(c.uploadSessionId,'OLD')
})
test('successfully bound submit preserves its receipt for the normal completed-page reset',async()=>{
 const {c,calls}=setup({submitted:true});assert.equal(await c.exports.leave(),true);assert.deepEqual(calls,['cleanup']);assert.equal(c.uploadSessionId,'OLD')
})

test('bound success may navigate to the normal receipt while submit finally is still pending',async()=>{
 const {c,calls}=setup({busy:true,submitted:true});assert.equal(await c.exports.leave(),true);assert.deepEqual(calls,['cleanup']);assert.equal(c.uploadSessionId,'OLD')
})
