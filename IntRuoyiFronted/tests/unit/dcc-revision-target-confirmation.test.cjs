const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),ts=require('typescript'),vue=require('vue'),{parse}=require('vue/compiler-sfc')
const path = require('node:path')
const frontendRoot = path.resolve(__dirname, '../..')
const loadProduction = relativePath => {
 const filename = path.resolve(frontendRoot, relativePath), context = { exports: {}, Error }
 context.require = name => {
  assert.ok(name.startsWith('.'), `Unexpected production dependency ${name}`)
  return loadProduction(path.relative(frontendRoot, path.resolve(path.dirname(filename), name + '.ts')))
 }
 vm.runInNewContext(ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
 return context.exports
}
const { readApplicationOrdinaryAttachmentExpectation, sameApplicationProjectAttributes, buildRevisionCommand } = loadProduction('src/views/dcc/controlled-file/revision/revision-model.ts')
const { getDccVersionDisplayText } = loadProduction('src/views/dcc/controlled-file/shared/version-display.ts')
// Explicit isolated server-response facts; this test never computes a production manifest hash.
const SERVER_MANIFEST_HASH = 'd37c10fa9b05e2a6c281ed4f9ab6d1306587a4203f91b7de02ce4a9032f851bc'
const CHANGED_SERVER_MANIFEST_HASH = 'd37c10fa9b05e2a6c281ed4f9ab6d1306587a4203f91b7de02ce4a9032f851bd'
const ORDINARY_IDS = ['9223372036854775701', '9223372036854775702']
const savedOrdinaryAttachments = () => [
 { attachmentId: ORDINARY_IDS[0], controlledFileId: '11', fileName: 'One.pdf', attachmentPurpose: 'ATTACHMENT', sortNo: 0 },
 { attachmentId: ORDINARY_IDS[1], controlledFileId: '11', fileName: 'Two.docx', attachmentPurpose: 'ATTACHMENT', sortNo: 1 }
]
const src=parse(fs.readFileSync('src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue','utf8')).descriptor.scriptSetup.content,ast=ts.createSourceFile('panel.ts',src,ts.ScriptTarget.Latest,true)
const names=['readRevisionTargetVersion','confirmation','submitRevision'],decls=ast.statements.filter(s=>s.declarationList?.declarations.some(d=>names.includes(d.name.text)))
function host(onConfirm=async()=>{}){
 const attributes={targetMarkets:['NA'],otherMarket:null,licenseHolder:'NA',actualManufacturer:'NA',documentTransfer:'N',transferTo:null},facts={projectAttributes:attributes,selectedSignoffDepartmentIds:['100'],selectedDistributionDepartmentIds:[],needTraining:true,effectiveMode:'AFTER_STANDARDIZATION_SIGNATURE',effectiveDate:null},selection={executionContract:'PUBLIC_MATRIX_NATIVE_V1'},calls={dialogs:[],writes:[],submitted:[]}
 const c={selectionGeneration:0,getVisitTenantId:()=>undefined,getTenantId:()=>1,signoffIdentity:String,listCurrentRelations:async id=>({sourceControlledFileId:id,files:[]}),listHistoricalRelations:async()=>[],revisionSubmitKey:vue.ref(''),revisionSubmitPayload:vue.ref(''),generateUUID:()=> 'independent-submit-command',String,Number,JSON,Error,canSubmit:vue.ref(true),snapshot:vue.ref({projectId:'20',actual:attributes}),selectedDepartments:vue.ref(['100']),selectedDistributionDepartments:vue.ref([]),normalizeSignoffDepartments:x=>x,normalizeDistributionDepartments:x=>x,validateAttributes:x=>x,buildUploadEffectiveChoice:(effectiveMode,effectiveDate)=>({effectiveMode,effectiveDate}),resolveUploadTodayDate:()=> '2026-10-14',effectiveMode:vue.ref(facts.effectiveMode),effectiveDate:vue.ref(null),needTraining:vue.ref(true),departments:vue.ref([{id:'100',name:'正式部门'}]),selectedApplicationFile:vue.ref({id:'11',versionNo:'A/1-1',title:'body.pdf',fileNumber:'F1',requesterId:99,projectFolderId:1}),props:{file:{id:'10',versionNo:'A/1'}},approverNames:vue.ref('实际批准人'),approvalRule:vue.ref('本组任一'),approvalGroups:vue.ref([]),approvalCapabilityLimitations:vue.ref([]),ElMessageBox:{confirm:async(text,title)=>{calls.dialogs.push({text,title});await onConfirm(c)}},error:vue.ref(''),busy:vue.ref(false),submitted:vue.ref(false),generation:1,selectedIterationId:vue.ref('11'),selectedIntent:vue.ref('REPLACEMENT'),snapshotIntent:vue.ref('REPLACEMENT'),selectionLoading:vue.ref(false),applicationFacts:vue.ref(facts),revisionOptions:vue.ref({controlledBaselineId:'10',baselineVersionNo:'A/1',partialTarget:{versionNo:'A/2',unavailableReason:null},replacementTarget:{versionNo:'B/1',unavailableReason:null},iterations:[{id:'11',versionNo:'A/1-1',canPartial:true,canReplacement:true}]}),preparePanelMatrix:async()=>true,approvalMatrixState:vue.ref({selection:()=>selection}),submitControlledFileWorkingIteration:async(...args)=>{calls.writes.push(args);return '12'},isWorkflowId:id=>id==='12',emit:(...args)=>calls.submitted.push(args),isApprovalMatrixStaleError:()=>false}
 Object.assign(c, { readApplicationOrdinaryAttachmentExpectation, sameApplicationProjectAttributes, getDccVersionDisplayText })
 Object.assign(c.selectedApplicationFile.value, { attachments: savedOrdinaryAttachments(), ordinaryAttachmentManifestHash: SERVER_MANIFEST_HASH })
 vm.runInNewContext(ts.transpileModule(decls.map(d=>d.getText(ast)).join('\n')+'\nthis.confirm=confirmation;this.submit=submitRevision',{compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,c)
 const command={...facts,controlledBaselineId:'10',selectedIterationId:'11',revisionChangeType:'REPLACEMENT',changeDescription:'说明不包含任何版本号',idempotencyKey:'own-click'}
 return{c,calls,command}
}
test('revision full confirmation explicitly distinguishes formal baseline, selected work and predicted target even when description has no version',async()=>{
 const h=host();await h.c.submit(h.command);const text=h.calls.dialogs[0].text;assert.match(text,/来源受控版本：A\/1/);assert.match(text,/送审工作正文：草稿/);assert.match(text,/目标受控版本：B\/1/);assert.equal(h.calls.writes.length,1)
 assert.doesNotMatch(text, /A\/1-1/)
 assert.match(text, /One\.pdf（普通附件）.*Two\.docx（普通附件）/)
 assert.equal(h.c.selectedApplicationFile.value.versionNo, 'A/1-1')
 assert.equal(h.calls.writes[0][0], '11')
 assert.deepEqual(Array.from(h.calls.writes[0][1].expectedOrdinaryAttachmentIds), ORDINARY_IDS)
 assert.equal(h.calls.writes[0][1].expectedOrdinaryAttachmentManifestHash, SERVER_MANIFEST_HASH)
})
test('saving a partial draft then submitting tolerates response property order and absent optional values', async () => {
 const h = host()
 h.c.selectedIntent.value = h.c.snapshotIntent.value = 'PARTIAL'
 h.c.snapshot.value.actual = { transferTo: null, documentTransfer: 'N', actualManufacturer: 'NA', licenseHolder: 'NA', otherMarket: undefined, targetMarkets: ['NA'] }
 const command = buildRevisionCommand('10', '11', 'PARTIAL', '保存后直接提交', 'submit-saved', h.c.applicationFacts.value)
 await h.c.submit(command)
 assert.equal(h.calls.writes.length, 1)
 assert.equal(h.calls.writes[0][1].revisionChangeType, 'PARTIAL')
})
test('a real saved attribute change still rejects before confirmation or submission', async () => {
 const h = host()
 const command = buildRevisionCommand('10', '11', 'REPLACEMENT', '修改必须重核', 'stale-command', h.c.applicationFacts.value)
 h.c.snapshot.value.actual = { ...h.c.snapshot.value.actual, targetMarkets: ['CE'] }
 await assert.rejects(h.c.submit(command), /已保存申请属性/)
 assert.equal(h.calls.dialogs.length, 0)
 assert.equal(h.calls.writes.length, 0)
})
test('QMS and general library revision commands retain scope without requiring project fields', async () => {
 for (const libraryScope of ['QMS', 'GENERAL']) {
  const h = host()
  const marker = { libraryScope }
  h.c.snapshot.value.actual = marker
  h.c.applicationFacts.value.projectAttributes = marker
  const command = buildRevisionCommand('10', '11', 'REPLACEMENT', '独立库变更', 'library-command', h.c.applicationFacts.value)
  assert.equal(command.projectAttributes.libraryScope, libraryScope)
  assert.equal(Object.hasOwn(command.projectAttributes, 'targetMarkets'), false)
  await h.c.submit(command)
  assert.equal(h.calls.writes.length, 1)
  assert.equal(h.calls.writes[0][1].projectAttributes.libraryScope, libraryScope)
 }
 assert.equal(sameApplicationProjectAttributes({ libraryScope: 'QMS' }, { libraryScope: 'GENERAL' }), false)
})
test('unavailable or missing backend target rejects before a misleading confirmation or write',async()=>{
 for(const target of [{versionNo:null,unavailableReason:'版本不可分配'},undefined]){const h=host();h.c.revisionOptions.value.replacementTarget=target;await assert.rejects(h.c.submit(h.command),/目标|版本/);assert.equal(h.calls.dialogs.length,0);assert.equal(h.calls.writes.length,0)}
})

for (const intent of ['PARTIAL', 'REPLACEMENT']) {
 test(`${intent} submits current complete choices even when the saved draft was incomplete`, async () => {
  const h = host()
  h.c.canSubmit.value = false
  h.c.selectedIntent.value = intent
  h.c.snapshotIntent.value = intent
  h.command.revisionChangeType = intent
  await h.c.submit(h.command)
  assert.equal(h.calls.writes.length, 1)
  assert.equal(h.calls.writes[0][1].effectiveMode, 'AFTER_STANDARDIZATION_SIGNATURE')
  assert.match(h.calls.dialogs[0].text, /生效方式：文控审核后立即生效/)
 })
}

test('revision still refuses source permission loss and checkout locks before any confirmation or write', async () => {
 for (const block of ['permission', 'checkout', 'in-progress']) {
  const h = host()
  h.c.canSubmit.value = false
  if (block === 'permission') h.c.revisionOptions.value.iterations[0].canReplacement = false
  if (block === 'checkout') h.c.revisionOptions.value.checkedOutBy = '99'
  if (block === 'in-progress') h.c.revisionOptions.value.lockedReason = 'FORMAL_REVISION_IN_PROGRESS'
  await assert.rejects(h.c.submit(h.command), /权限|检出|办理中/)
  assert.equal(h.calls.dialogs.length, 0)
  assert.equal(h.calls.writes.length, 0)
 }
})
test('target changing during confirmation invalidates the original click with no write',async()=>{
 const h=host(async c=>{c.revisionOptions.value.replacementTarget.versionNo='C/1'});await assert.rejects(h.c.submit(h.command),/目标|内容.*变更/);assert.equal(h.calls.writes.length,0);assert.equal(h.c.busy.value,false)
 assert.equal(h.calls.dialogs.length, 1)
})

test('missing saved server manifest hash rejects before confirmation and sends no fabricated empty attachment list', async () => {
 for (const missing of [undefined, null]) {
  const h = host()
  if (missing === undefined) delete h.c.selectedApplicationFile.value.ordinaryAttachmentManifestHash
  else h.c.selectedApplicationFile.value.ordinaryAttachmentManifestHash = missing
  await assert.rejects(h.c.submit(h.command), /附件清单|服务器摘要/)
  assert.equal(h.calls.dialogs.length, 0)
  assert.equal(h.calls.writes.length, 0)
  assert.equal(h.c.busy.value, false)
  assert.deepEqual(Array.from(h.c.selectedApplicationFile.value.attachments, row => row.attachmentId), ORDINARY_IDS)
 }
})

test('saved ordinary manifest drift during confirmation rejects the original click with exact two attachments retained', async () => {
 const h = host(async c => { c.selectedApplicationFile.value.ordinaryAttachmentManifestHash = CHANGED_SERVER_MANIFEST_HASH })
 await assert.rejects(h.c.submit(h.command), /本次附件清单已变化/)
 assert.equal(h.calls.dialogs.length, 1)
 assert.match(h.calls.dialogs[0].text, /One\.pdf（普通附件）.*Two\.docx（普通附件）/)
 assert.equal(h.calls.writes.length, 0)
 assert.equal(h.c.busy.value, false)
 assert.deepEqual(Array.from(h.c.selectedApplicationFile.value.attachments, row => row.attachmentId), ORDINARY_IDS)
 assert.equal(h.c.selectedApplicationFile.value.ordinaryAttachmentManifestHash, CHANGED_SERVER_MANIFEST_HASH)
})

test('cancel or close of the actual revision confirmation retains both saved attachments and sends no submission',async()=>{
 for(const cancellation of ['cancel','close']){
  const h=host(async()=>{throw cancellation})
  await assert.rejects(h.c.submit(h.command),/已取消提交/)
  assert.equal(h.calls.dialogs.length,1);assert.match(h.calls.dialogs[0].text,/One\.pdf（普通附件）.*Two\.docx（普通附件）/)
  assert.equal(h.calls.writes.length,0);assert.equal(h.calls.submitted.length,0);assert.equal(h.c.busy.value,false)
  assert.deepEqual(Array.from(h.c.selectedApplicationFile.value.attachments,row=>row.attachmentId),ORDINARY_IDS)
  assert.equal(h.c.selectedApplicationFile.value.ordinaryAttachmentManifestHash,SERVER_MANIFEST_HASH)
  assert.equal(h.c.selectedIterationId.value,'11')
 }
})
