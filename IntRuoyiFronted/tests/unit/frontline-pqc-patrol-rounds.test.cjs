const {test}=require('node:test')
const assert=require('node:assert/strict')
const fs=require('node:fs')
const ts=require('typescript')
const {ref,reactive,computed}=require('vue')
const source=fs.readFileSync('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue','utf8').match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast=ts.createSourceFile('panel.ts',source,ts.ScriptTarget.Latest,true)
function extract(names,deps){
  const statements=ast.statements.filter(s=>ts.isVariableStatement(s)&&s.declarationList.declarations.every(d=>names.includes(d.name.getText(ast))))
  assert.equal(statements.length,names.length)
  const code=ts.transpileModule(statements.map(s=>s.getText(ast)).join('\n')+`\nreturn {${names.join(',')}}`,{compilerOptions:{target:ts.ScriptTarget.ES2020}}).outputText
  return new Function(...Object.keys(deps),code)(...Object.values(deps))
}
const makeTask=n=>({pqcTaskId:100+n,inspectionType:'PATROL',inspectionRuleKey:'PATROL',roundNo:n,
  regulationVersionId:20,qaProcessId:30,businessDate:'2026-11-04',shiftCode:'PATROL',taskStatus:'PENDING',
  plannedInspectionQuantity:5,inspectionItems:[{itemCode:'A'}]})

test('each patrol round displays its earliest persisted task creation time across processes',()=>{
  const options=ref([
    {pqcTaskOptions:[{...makeTask(1),createTime:2000},{...makeTask(2),createTime:4000}]},
    {pqcTaskOptions:[{...makeTask(1),createTime:1000},{...makeTask(2),createTime:5000}]}
  ])
  const {pqcPatrolRounds}=extract(['pqcPatrolRounds'],{
    computed,allSwitchablePqcProcessOptions:options,toDateTimeValue:value=>new Date(value)
  })
  assert.deepEqual(pqcPatrolRounds.value,[
    {roundNo:1,pendingCount:2,totalCount:2,cancelled:false,cancelReason:undefined,createTime:1000},
    {roundNo:2,pendingCount:2,totalCount:2,cancelled:false,cancelReason:undefined,createTime:4000}
  ])
  options.value.reverse()
  assert.equal(pqcPatrolRounds.value[0].createTime,1000)
})
function harness(){
  const tasks=[1,2,3,12].map(makeTask)
  const process={activeOrderId:10,qaProcessId:30,regulationVersionId:20,pqcTaskOptions:tasks}
  const dependencies={ref,reactive,computed,deviceState:reactive({selectedProcess:process,selectedEmployee:{userId:1},template:{}}),
    selectedPqcPatrolRound:ref(1),selectedPqcInspectionRuleKey:ref('PATROL'),activePqcTaskOptionId:ref(101),
    selectedPqcInspectionKey:ref('A'),activePqcTabKey:ref('A'),isPqcProcessNavigationBlocked:ref(false),
    pqcDraft:reactive({inspectionQuantity:9,scrapQuantity:1}),pqcTaskDrafts:reactive({}),
    pqcSignatureDialogVisible:ref(false),pqcSignaturePassword:ref(''),pqcSubmitResultUncertain:ref(false),
    context:{actualEmployeeId:1},employeeTemplateCode:ref('PQC'),payloadPreview:ref({}),
    allSwitchablePqcProcessOptions:ref([process]),PQC_INSPECTION_RULE_LABELS:{PATROL:'巡检'},
    isFrontlinePqcProcess:p=>Boolean(p?.qaProcessId),hasPqcTaskOptionSnapshot:t=>t.taskStatus==='PENDING',
    pqcTaskOptionIncludesItem:(t,key)=>!key||t.inspectionItems.some(i=>i.itemCode===key),
    resolveSelectedPqcInspectionItemKey:()=> 'A',normalizePqcTaskOptionItemKey:()=> 'A',
    applyPqcItemEquipmentDefaults:()=>{},mapPqcInspectionItem:i=>i,
    showFrontlineError:e=>{throw new Error(String(e))},clearPqcTaskOptionDraft:()=>{throw new Error('unexpected clear')},
    findFirstPqcProcessForInspectionRule:()=>{throw new Error('round exists on current process')},
    handleSelectProcess:()=>{throw new Error('same process must retain round drafts')},
    switchPqcCurrentLoginEmployeeForActiveTask:async()=>{dependencies.deviceState.selectedEmployee={userId:1}}
  }
  const functions=extract(['getPqcTaskOptions','getPqcTaskOptionsForInspectionItem','getPqcTaskOptionsByRule',
    'getPqcTaskOptionForRule','getSelectedPqcTaskOption','getPqcTaskDraftKey','createPqcTaskDraftState',
    'getPqcTaskDraft','persistCurrentPqcTaskDraft','applyPqcTaskOptionToDraft','applyPqcTaskOptionToSelectedProcess',
    'selectPqcInspectionRule','selectPqcPatrolRound','getPqcItemSelectionKey'],dependencies)
  return {...dependencies,...functions,tasks,process}
}
test('selecting arbitrary patrol rounds binds the exact task and preserves separate quantity drafts',async()=>{
  const h=harness()
  assert.deepEqual(h.getPqcTaskOptionsByRule(h.process,'PATROL').map(t=>t.roundNo),[1])
  await h.selectPqcPatrolRound(3)
  assert.equal(h.activePqcTaskOptionId.value,103)
  assert.equal(h.pqcDraft.inspectionQuantity,5)
  assert.equal(h.getSelectedPqcTaskOption(h.process).roundNo,3)
  h.pqcDraft.inspectionQuantity=13
  h.pqcDraft.scrapQuantity=2
  await h.selectPqcPatrolRound(12)
  assert.equal(h.activePqcTaskOptionId.value,112)
  assert.equal(h.pqcDraft.inspectionQuantity,5)
  await h.selectPqcPatrolRound(1)
  assert.equal(h.activePqcTaskOptionId.value,101)
  assert.equal(h.pqcDraft.inspectionQuantity,9)
  assert.equal(h.pqcDraft.scrapQuantity,1)
  await h.selectPqcPatrolRound(3)
  assert.equal(h.pqcDraft.inspectionQuantity,13)
  assert.equal(h.pqcDraft.scrapQuantity,2)
  assert.notEqual(h.getPqcTaskDraftKey(h.tasks[0]),h.getPqcTaskDraftKey(h.tasks[2]))
  assert.notEqual(h.getPqcItemSelectionKey(h.tasks[0],'A'),h.getPqcItemSelectionKey(h.tasks[2],'A'))
})
test('an old task ID cannot select a task in another patrol round and completed rounds stay excluded',()=>{
  const h=harness()
  h.selectedPqcPatrolRound.value=12
  assert.equal(h.getSelectedPqcTaskOption(h.process).pqcTaskId,112)
  h.tasks[3].taskStatus='SUBMITTED'
  assert.equal(h.getSelectedPqcTaskOption(h.process),undefined)
  assert.deepEqual(h.getPqcTaskOptionsByRule(h.process,'PATROL'),[])
})
test('navigation lock blocks patrol round changes before draft or task mutation',async()=>{
  const h=harness()
  h.isPqcProcessNavigationBlocked.value=true
  await h.selectPqcPatrolRound(3)
  assert.equal(h.selectedPqcPatrolRound.value,1)
  assert.equal(h.activePqcTaskOptionId.value,101)
  assert.deepEqual(Object.keys(h.pqcTaskDrafts),[])
})
test('creation uses the server round, refreshes tasks and delegates to the existing selection handler',async()=>{
  const calls=[],oldProcess={regulationVersionId:20,qaProcessId:30},freshProcess={...oldProcess,pqcTaskOptions:[makeTask(13)]}
  const dependencies={deviceState:{selectedActiveOrder:{activeOrderId:10},selectedProcess:oldProcess},
    isPqcProcessNavigationBlocked:ref(false),pqcPatrolRoundCreating:ref(false),
    pqcPatrolRounds:ref([{roundNo:1},{roundNo:12}]),currentLoginUserId:ref(1),
    persistCurrentPqcTaskDraft:()=>calls.push('persist'),
    ProFeedbackApi:{createFrontlinePqcPatrolRound:async body=>{calls.push(body);return 13},getPqcProcesses:async()=>[freshProcess]},
    invalidateFrontlinePqcProcessCacheForActiveOrder:()=>calls.push('invalidate'),isFrontlinePqcProcess:p=>Boolean(p?.qaProcessId),
    selectPqcInspectionRule:async(type,n)=>calls.push([type,n]),resolveErrorMessage:e=>e.message,showFrontlineError:e=>{throw Error(e)}
  }
  const {createPqcPatrolRound}=extract(['createPqcPatrolRound'],dependencies)
  await createPqcPatrolRound()
  assert.deepEqual(calls,['persist',{activeOrderId:10,expectedPreviousRoundNo:12},'invalidate',['PATROL',13]])
  assert.equal(dependencies.deviceState.selectedProcess,freshProcess)
  assert.equal(dependencies.pqcPatrolRoundCreating.value,false)
})
const operationHarness=()=>{
  const h=harness();h.deviceState.selectedActiveOrder={activeOrderId:10};
  h.pqcDraft.inspectionQuantity=9;h.pqcDraft.scrapQuantity=1;h.persistCurrentPqcTaskDraft();
  const pieces={'101:A':['合格','不合格']},selections={};
  const selected=h.getPqcItemSelectionKey(h.tasks[0],'A');selections[selected]={equipmentId:77};
  const deps={...h,pqcPatrolRoundCreating:ref(false),pqcRoundOperation:reactive({mode:'DRAFT',targetTaskId:103,error:''}),
    pqcRoundOperationSource:ref({task:h.tasks[0]}),pqcRoundOperationTargets:ref([{task:h.tasks[2]}]),
    activePqcTaskOption:computed(()=>h.tasks.find(t=>t.pqcTaskId===h.activePqcTaskOptionId.value)),
    pqcTouchedTaskIds:new Set([101]),pqcPieceValues:pieces,pqcItemSelections:selections,
    getPqcPieceStateKeyForTask:(code,task)=>`${task.pqcTaskId}:${code}`,
    clearPqcLocalTaskData:ids=>{for(const id of ids){
      const task=h.tasks.find(t=>t.pqcTaskId===id);delete h.pqcTaskDrafts[h.getPqcTaskDraftKey(task)];
      delete pieces[`${id}:A`];delete selections[h.getPqcItemSelectionKey(task,'A')];deps.pqcTouchedTaskIds.delete(id)
    }},
    pqcRoundOperationTasks:ref(h.tasks.map(task=>({task}))),
    ProFeedbackApi:{cancelFrontlinePqcPatrolRound:async()=>{},correctFrontlinePqcPatrolRound:async()=>{}},
    refreshPqcRoundProcesses:async()=>{},
    pqcRoundOperationDialog:ref({close(){}}),message:{success(){}},resolveErrorMessage:e=>e.message
  };
  const functions=extract(['executePqcRoundOperation'],deps);
  return {...deps,...functions};
}
test('moving a process draft transfers quantity, piece values and equipment, clearing source draft',async()=>{
  const h=operationHarness();await h.executePqcRoundOperation();
  assert.equal(h.pqcRoundOperation.error,'');assert.equal(h.activePqcTaskOptionId.value,103);
  assert.equal(h.pqcDraft.inspectionQuantity,9);assert.equal(h.pqcDraft.scrapQuantity,1);
  assert.deepEqual(h.pqcPieceValues['103:A'],['合格','不合格']);assert.equal(h.pqcPieceValues['101:A'],undefined);
  assert.equal(h.pqcItemSelections[h.getPqcItemSelectionKey(h.tasks[2],'A')].equipmentId,77);
  assert.equal(h.pqcTaskDrafts[h.getPqcTaskDraftKey(h.tasks[0])],undefined);
  await h.selectPqcPatrolRound(1);assert.equal(h.pqcDraft.inspectionQuantity,5);
})
test('moving to a locally edited target refuses overwrite and retains the source',async()=>{
  const h=operationHarness();h.pqcTouchedTaskIds.add(103);await h.executePqcRoundOperation();
  assert.match(h.pqcRoundOperation.error,/禁止覆盖/);assert.equal(h.activePqcTaskOptionId.value,101);
  assert.deepEqual(h.pqcPieceValues['101:A'],['合格','不合格']);assert.equal(h.pqcPieceValues['103:A'],undefined);
})


test('cancelling an empty round needs only confirmation and preserves local drafts for recovery',async()=>{
  const h=operationHarness(),calls=[];
  h.pqcRoundOperation.mode='CANCEL';h.pqcRoundOperation.roundNo=3;
  h.ProFeedbackApi.cancelFrontlinePqcPatrolRound=async body=>calls.push(body);
  await h.executePqcRoundOperation();
  assert.equal(h.pqcRoundOperation.error,'');
  assert.deepEqual(calls,[{activeOrderId:10,roundNo:3}]);
  assert.deepEqual(h.pqcPieceValues['101:A'],['合格','不合格']);
  assert.equal(h.pqcItemSelections[h.getPqcItemSelectionKey(h.tasks[0],'A')].equipmentId,77);
  assert.equal(h.pqcTaskDrafts[h.getPqcTaskDraftKey(h.tasks[0])].inspectionQuantity,9);
});

test('restoring a cancelled round confirms its original number and selects it without clearing drafts',async()=>{
  const h=operationHarness(),calls=[];
  h.pqcRoundOperation.mode='RESTORE';h.pqcRoundOperation.roundNo=1;
  h.ProFeedbackApi.restoreFrontlinePqcPatrolRound=async body=>calls.push(body);
  h.refreshPqcRoundProcesses=async round=>calls.push({selectedRound:round});
  const deps={...h};delete deps.executePqcRoundOperation;
  const {executePqcRoundOperation}=extract(['executePqcRoundOperation'],deps);
  await executePqcRoundOperation();
  assert.equal(h.pqcRoundOperation.error,'');
  assert.deepEqual(calls,[{activeOrderId:10,roundNo:1},{selectedRound:1}]);
  assert.deepEqual(h.pqcPieceValues['101:A'],['合格','不合格']);
  assert.equal(h.pqcTaskDrafts[h.getPqcTaskDraftKey(h.tasks[0])].inspectionQuantity,9);
});

test('failed round recovery keeps the confirmation open and reports the backend error',async()=>{
  const h=operationHarness();let closed=false,refreshed=false;
  h.pqcRoundOperation.mode='RESTORE';h.pqcRoundOperation.roundNo=1;
  h.ProFeedbackApi.restoreFrontlinePqcPatrolRound=async()=>{throw Error('放行申请已锁定')};
  h.refreshPqcRoundProcesses=async()=>{refreshed=true};
  h.pqcRoundOperationDialog.value.close=()=>{closed=true};
  const deps={...h};delete deps.executePqcRoundOperation;
  const {executePqcRoundOperation}=extract(['executePqcRoundOperation'],deps);
  await executePqcRoundOperation();
  assert.match(h.pqcRoundOperation.error,/放行申请已锁定/);
  assert.equal(closed,false);assert.equal(refreshed,false);
  assert.equal(h.pqcPatrolRoundCreating.value,false);
});
test('submitted round correction needs only target selection and confirmation',async()=>{
  const h=operationHarness(),calls=[];
  h.pqcRoundOperation.mode='SUBMITTED';
  h.pqcRoundOperationSource.value.task.submittedEventId=77;
  h.pqcRoundOperationSource.value.task.submittedContentHash='original-hash';
  h.ProFeedbackApi.correctFrontlinePqcPatrolRound=async body=>calls.push(body);
  await h.executePqcRoundOperation();
  assert.equal(h.pqcRoundOperation.error,'');
  assert.deepEqual(calls,[{sourceTaskId:101,targetTaskId:103,expectedSubmittedEventId:77,expectedContentHash:'original-hash'}]);
});


test('switching inspection items keeps the selected patrol round and its quantity draft',async()=>{
  const h=harness();h.tasks.forEach(task=>task.inspectionItems.push({itemCode:'B'}));
  await h.selectPqcPatrolRound(3);h.pqcDraft.inspectionQuantity=9;
  const {selectPqcInspectionTab}=extract(['selectPqcInspectionTab'],h);
  await selectPqcInspectionTab('B');
  assert.equal(h.selectedPqcPatrolRound.value,3);
  assert.equal(h.activePqcTaskOptionId.value,103);
  assert.equal(h.selectedPqcInspectionKey.value,'B');
  assert.equal(h.pqcDraft.inspectionQuantity,9);
});
