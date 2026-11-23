const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),ts=require('typescript'),path=require('node:path')
const file=path.resolve(__dirname,'../../src/views/mes/pro/feedback/frontlineDeviceEmployeeContext.ts')
const ast=ts.createSourceFile(file,fs.readFileSync(file,'utf8'),ts.ScriptTarget.Latest,true)
for(const mode of ['Production','Pqc']) test(`${mode} superseded order-list response cannot clear new selection, options, error or loading`,async()=>{
 const name=`loadFrontline${mode}ActiveOrders`
 const statement=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)===name))
 const code=ts.transpileModule(statement.getText(ast).replace('export ',''),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.None}}).outputText
 let resolveOld,rejectOld,current=true
 const api={[`getFrontline${mode}ActiveOrders`]:()=>new Promise((resolve,reject)=>{resolveOld=resolve;rejectOld=reject})}
 const load=Function('ProFeedbackApi','resolveFrontlineErrorMessage','pruneFrontlinePqcProcessCache','clearFrontlinePqcSelectionIfUnavailable','isSameFrontlineActiveOrder',code+`;return ${name}`)(api,e=>e.message,()=>{throw Error('stale prune')},()=>{throw Error('stale clear')},()=>false)
 const state={selectedActiveOrder:{activeOrderId:2},activeOrderOptions:[{activeOrderId:2}]}
 const pending=load(state,()=>current);current=false;state.lastError='new error';state.loadingActiveOrders=true
 resolveOld([{activeOrderId:1}]);await pending
 assert.equal(state.selectedActiveOrder.activeOrderId,2);assert.equal(state.activeOrderOptions[0].activeOrderId,2);assert.equal(state.lastError,'new error');assert.equal(state.loadingActiveOrders,true)
 current=true;const failed=load(state,()=>current);current=false;state.lastError='latest error';rejectOld(Error('old failure'));await assert.rejects(failed,/old failure/);assert.equal(state.lastError,'latest error')
})

for(const failure of [false,true])test(`PQC process stale ${failure?'failure':'success'} preserves new employee selection and loading`,async()=>{
 const statement=ast.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(ast)==='selectFrontlinePqcProcess'))
 const code=ts.transpileModule(statement.getText(ast).replace('export ',''),{compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.None}}).outputText
 let resolve,reject
 const select=Function('getFrontlinePqcEmployeeCandidatesWithCache','resolveFrontlineErrorMessage',code+';return selectFrontlinePqcProcess')(()=>new Promise((a,b)=>{resolve=a;reject=b}),e=>e.message)
 const state={processSelectionRequestToken:0},pending=select(state,{qaProcessId:1})
 state.processSelectionRequestToken++;state.employeeOptions=[{userId:2}];state.lastError='new error';state.loadingEmployees=true
 if(failure){reject(Error('old employee failure'));await assert.rejects(pending,/old employee failure/)}else{resolve([{userId:1}]);await pending}
 assert.equal(state.employeeOptions[0].userId,2);assert.equal(state.lastError,'new error');assert.equal(state.loadingEmployees,true)
})
