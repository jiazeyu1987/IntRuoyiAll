const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),ts=require('typescript')
const root=path.resolve(__dirname,'../..')
function load(){const c={exports:{},Error};vm.runInNewContext(ts.transpileModule(fs.readFileSync(path.join(root,'src/views/dcc/controlled-file/upload/approval-matrix.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText,c);return c.exports}
const scope={tenantId:'1',projectId:'5',categoryId:'2',taxonomyId:'6',actionType:'NEW',applicationContextKey:'custom-scope',approvalSourceMode:'APPLICATION_CUSTOM'}
const config={approverUserId:'77',documentControlUserId:'66'}
function response(value=config,ids=['51','52'],ready=true){return require('./dcc-upload-a32-fixture.cjs').response(scope,value,ids,ready)}
const plain=x=>JSON.parse(JSON.stringify(x))
test('actual matrix state carries custom choices and accepts typed custom response without fake public identity',()=>{
 const m=load(),state=new m.UploadApprovalMatrixState(scope);state.setApplicationCustomMatrix(config);const request=state.begin(['51','52']);assert.deepEqual(plain(request.request.applicationCustomMatrix),config)
 assert.equal(request.request.approvalMatrixBaselineToken,undefined);assert.equal(state.accept(request,response()),true)
 const value=state.selection('SUBMIT');assert.equal(value.approvalSourceMode,'APPLICATION_CUSTOM');assert.deepEqual(plain(value.applicationCustomMatrix),config);assert.equal(value.expectedApplicationCustomMatrixDigest,response().applicationCustomMatrixDigest);assert.equal(value.expectedApprovalRouteId,undefined);assert.equal(value.applicationCustomMatrixPhase,'PREVIEW')
})
test('changing custom actors invalidates old readiness and late replies cannot authorize replacement choice',()=>{
 const m=load(),state=new m.UploadApprovalMatrixState(scope);state.setApplicationCustomMatrix(config);const old=state.begin(['51']);state.setApplicationCustomMatrix({...config,approverUserId:'78'});assert.equal(state.accept(old,response()),false);assert.throws(()=>state.selection('SUBMIT'),/预检|读取|校验/)
 const req=state.begin(['51']);assert.throws(()=>state.accept(req,response()),/本次自定义.*不一致/)
})
test('custom save preserves partial selection while unready and public-shaped response cannot authorize custom source',()=>{
 const m=load(),state=new m.UploadApprovalMatrixState(scope);state.setApplicationCustomMatrix({approverUserId:null,documentControlUserId:null});const req=state.begin([])
 const notReady={...response({approverUserId:null,documentControlUserId:null},[],false),blockers:[{reasonCode:'CUSTOM_MATRIX_SIGNOFF_REQUIRED',message:'请至少选择一个会签部门'}]}
 state.accept(req,notReady);assert.equal(state.selection('SAVE').applicationCustomMatrix.approverUserId,null);assert.throws(()=>state.selection('SUBMIT'),/至少选择/)
 assert.throws(()=>state.accept(state.begin([]),{...notReady,routeId:'999'}),/公共|来源/)
})
