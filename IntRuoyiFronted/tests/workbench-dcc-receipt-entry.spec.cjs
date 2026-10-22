const assert=require('node:assert/strict')
const test=require('node:test')
const ts=require('typescript')
const vue=require('vue')
const {parse}=require('vue/compiler-sfc')
const {read,compile}=require('./helpers/workbench-s3-fixture.cjs')
const evaluate=(source,deps={})=>{const m={exports:{}};Function('require','module','exports',compile(source))(id=>{assert.ok(deps[id],id);return deps[id]},m,m.exports);return m.exports}
const detail=parse(read('src/views/dcc/controlled-file/detail/index.vue')).descriptor
const ast=ts.createSourceFile('detail.ts',detail.scriptSetup.content,ts.ScriptTarget.Latest,true)
const names=['controlledFileId','currentUserId','viewerMode','getCurrentElectronicReceiptRecipient','getCurrentDistributionRecipient','resetElectronicReceiptDialog','openElectronicReceiptDialog','closeElectronicReceiptDialog','validateElectronicReceiptDialog','submitElectronicReceiptDialog']
const statements=ast.statements.filter(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>names.includes(d.name.getText(ast)))).map(s=>s.getText(ast))
assert.equal(statements.length,names.length)
const presentation=evaluate(read('src/views/dcc/controlled-file/view/presentation.ts'))
const templateAst=detail.template.ast
const findNodes=(node,predicate,result=[])=>{if(predicate(node))result.push(node);for(const child of node.children||[])findNodes(child,predicate,result);return result}
const directive=(node,name,arg)=>node.props?.find(p=>p.type===7&&p.name===name&&(!arg||p.arg?.content===arg))
function receipt(route){
 const calls=[],warnings=[],reloads=[],dialog=vue.reactive({visible:false,submitting:false,inlineError:'',fieldErrors:{},form:{password:'',comment:''}})
 const bindings={computed:vue.computed,route,userStore:{getUser:{id:5}},isControlledFileViewerMode:presentation.isControlledFileViewerMode,electronicReceiptDialog:dialog,electronicReceiptLoadingRecipientId:vue.ref(),message:{warning:x=>warnings.push(x),success:()=>{}},acknowledgeElectronicDistribution:async(...args)=>calls.push(args),reloadAll:async()=>reloads.push(true),isControlledFileTaskPasswordInvalidError:()=>false,DCC_APPROVAL_WRONG_PASSWORD_MESSAGE:'invalid',resolveDccApprovalSignatureErrorMessage:()=> 'failed'}
 const handlers=Function(...Object.keys(bindings),compile(statements.join('\n'))+';return {controlledFileId,viewerMode,getCurrentElectronicReceiptRecipient,openElectronicReceiptDialog,submitElectronicReceiptDialog};')(...Object.values(bindings))
 return {...handlers,dialog,calls,warnings,reloads}
}
test('actual DCC registration opens detail normal template branch and original receipt dialog from official own row',async()=>{
 const longs=evaluate(read('src/utils/exactJavaLong.ts'))
 const unit=evaluate(read('src/workbench-sources/dcc/registration.ts'),{'@/utils/exactJavaLong':longs}).default
 let route;await unit.sources[0].open({push:async r=>route=r},{controlledFileId:'9007199254740993',distributionId:'9223372036854775807',recipientId:'9007199254740995'})
 assert.equal(route.name,'DccControlledFileDetail');assert.equal(route.params.id,'9007199254740993');assert.equal(route.query.distributionId,'9223372036854775807');assert.equal(route.query.recipientId,'9007199254740995');assert.equal(route.query.viewer,undefined)
 const h=receipt(route);assert.equal(h.viewerMode.value,false)
 const outer=templateAst.children.filter(n=>n.type===1)
 assert.equal(directive(outer[0],'if').exp.content,'viewerMode');assert.ok(directive(outer[1],'else'))
 const buttons=findNodes(outer[1],n=>n.type===1&&n.tag==='el-button'&&directive(n,'on','click')?.exp.content==='openElectronicReceiptDialog(row)')
 assert.equal(buttons.length,1);const condition=directive(buttons[0],'if').exp.content
 const own={id:41,distributionMedium:'PUBLIC_FOLDER',recipients:[{id:51,userId:5,acknowledgedAt:null},{id:52,userId:6}]}
 assert.ok(Function('row','getCurrentElectronicReceiptRecipient','return '+condition)(own,h.getCurrentElectronicReceiptRecipient))
 h.openElectronicReceiptDialog(own);assert.equal(h.dialog.visible,true);assert.equal(h.dialog.distributionId,41);assert.equal(h.dialog.recipientId,51)
 // Query has unrelated huge identities; original handler derives receipt identities from formal detail row.
 h.dialog.form.password='fixture-password';h.dialog.form.comment=' confirmed '
 await h.submitElectronicReceiptDialog()
 assert.deepEqual(h.calls,[['9007199254740993',41,51,{password:'fixture-password',comment:'confirmed'}]])
 assert.equal(h.dialog.visible,false);assert.equal(h.reloads.length,1)
})
test('actual detail receipt methods reject other user/already acknowledged/non-electronic row without submission',async()=>{
 for(const row of [{id:41,distributionMedium:'PUBLIC_FOLDER',recipients:[{id:51,userId:6}]},{id:41,distributionMedium:'PUBLIC_FOLDER',recipients:[{id:51,userId:5,acknowledgedAt:'2026-10-09'}]},{id:41,distributionMedium:'PAPER',recipients:[{id:51,userId:5}]}]){
  const h=receipt({params:{id:'10'},query:{distributionId:'41',recipientId:'51'}})
  assert.equal(h.getCurrentElectronicReceiptRecipient(row),undefined);h.openElectronicReceiptDialog(row);assert.equal(h.dialog.visible,false);assert.equal(h.warnings.length,1)
  h.dialog.form.password='fixture-password';await h.submitElectronicReceiptDialog();assert.equal(h.calls.length,0)
 }
})
test('original API/controller/service chain preserves formal record and login ownership checks',async()=>{
 const apiSource=read('src/api/dcc/controlledFile/workflow.ts'),apiAst=ts.createSourceFile('workflow.ts',apiSource,ts.ScriptTarget.Latest,true)
 const declaration=apiAst.statements.find(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.some(d=>d.name.getText(apiAst)==='acknowledgeElectronicDistribution'))
 const calls=[]
 // Run the current wrapper with the existing task-action transport boundary.
 const wrapper=Function('postControlledFileTaskAction',compile(declaration.getText(apiAst).replace(/^export /,''))+';return acknowledgeElectronicDistribution;')((...args)=>{calls.push(args);return true})
 await wrapper('10','41','51',{password:'fixture-password',comment:''})
 assert.deepEqual(calls[0],['/dcc/controlled-files/10/distributions/41/recipients/51/acknowledge',{password:'fixture-password',comment:''}])
 const fs=require('node:fs'),path=require('node:path'),base=path.resolve(__dirname,'../../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc')
 const controller=fs.readFileSync(path.join(base,'controller/admin/file/DccDistributionReceiptController.java'),'utf8')
 assert.match(controller,/hasPermission\('dcc:controlled-file:query'\)/);assert.match(controller,/acknowledgeElectronicDistribution\(getLoginUserId\(\), id, distributionId,\s*recipientId, reqVO\)/)
 const service=fs.readFileSync(path.join(base,'service/file/DccDistributionReceiptServiceImpl.java'),'utf8').split('public void acknowledgeElectronicDistribution(')[1].split('@Override')[0]
 for(const guard of ['controlledFileMapper.selectById(controlledFileId)','ACK_ALLOWED_FILE_STATUSES.contains(file.getStatus())','distributionMapper.selectById(distributionId)','controlledFileId.equals(distribution.getControlledFileId())','PUBLIC_FOLDER.getCode().equals(distribution.getDistributionMedium())','distributionRecipientMapper.selectById(recipientId)','distributionId.equals(recipient.getDistributionId())','userId.equals(recipient.getUserId())','recipient.getAcknowledgedAt() != null','verifyPasswordAndCreateSignature(userId, controlledFileId'])assert.ok(service.includes(guard),guard)
 assert.doesNotMatch(service,/getQuery|getParameter/)
})
