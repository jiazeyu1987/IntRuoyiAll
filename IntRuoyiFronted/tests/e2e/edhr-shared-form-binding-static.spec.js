const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const readText = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const batchExecutionApi = readText('src/api/mes/pro/edhr/batchExecution.ts')
const routeFlowConfigApi = readText('src/api/mes/pro/route/flowconfig.ts')
const batchDetailPage = readText('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const executionPage = readText('src/views/mes/pro/edhr/ExecutionPage.vue')
const routeProcessList = readText('src/views/mes/pro/route/RouteProcessList.vue')
const routeFlowGraphDesigner = readText('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')

const assertInterfaceContains = (source, interfaceName, patterns) => {
  const match = source.match(new RegExp(`export interface ${interfaceName} \\{([\\s\\S]*?)\\n\\}`))
  assert.ok(match, `${interfaceName} must exist.`)
  for (const { pattern, message } of patterns) {
    assert.match(match[1], pattern, message)
  }
}

assertInterfaceContains(batchExecutionApi, 'EdhrBatchExecutionTaskOpenRespVO', [
  {
    pattern: /instanceScope\?:\s*['"]PROCESS['"]\s*\|\s*['"]BATCH_SHARED['"]\s*\|\s*string/,
    message: 'open task response must expose instanceScope for shared execution routing.'
  },
  {
    pattern: /sharedFormKey\?:\s*string/,
    message: 'open task response must expose sharedFormKey for shared form identity.'
  },
  {
    pattern: /fillableScopeJson\?:\s*string/,
    message: 'open task response must expose fillableScopeJson for current task range.'
  }
])

assertInterfaceContains(batchExecutionApi, 'EdhrBatchExecutionTaskRespVO', [
  {
    pattern: /instanceScope\?:\s*['"]PROCESS['"]\s*\|\s*['"]BATCH_SHARED['"]\s*\|\s*string/,
    message: 'task list response must expose instanceScope for shared task display.'
  },
  {
    pattern: /sharedFormKey\?:\s*string/,
    message: 'task list response must expose sharedFormKey without hard-coded form slots.'
  },
  {
    pattern: /fillableScopeJson\?:\s*string/,
    message: 'task list response must expose fillableScopeJson for range hints.'
  }
])

assertInterfaceContains(routeFlowConfigApi, 'ProRouteFlowBatchRecordVO', [
  {
    pattern: /instanceScope\?:\s*['"]PROCESS['"]\s*\|\s*['"]BATCH_SHARED['"]\s*\|\s*string/,
    message: 'route flow batch record response must expose instanceScope for binding existing shared forms.'
  },
  {
    pattern: /sharedFormKey\?:\s*string\s*\|\s*null/,
    message: 'route flow batch record response must expose sharedFormKey.'
  },
  {
    pattern: /fillableScopeJson\?:\s*string\s*\|\s*null/,
    message: 'route flow batch record response must expose fillableScopeJson.'
  },
  {
    pattern: /requiredPolicy\?:\s*ProRouteFlowRequiredPolicy\s*\|\s*null/,
    message: 'route flow batch record response must expose requiredPolicy for optional route forms.'
  }
])

assertInterfaceContains(routeFlowConfigApi, 'ProRouteFlowBatchRecordSaveVO', [
  {
    pattern: /instanceScope\?:\s*['"]PROCESS['"]\s*\|\s*['"]BATCH_SHARED['"]\s*\|\s*string/,
    message: 'route flow batch record save payload must include instanceScope.'
  },
  {
    pattern: /sharedFormKey\?:\s*string\s*\|\s*null/,
    message: 'route flow batch record save payload must include sharedFormKey.'
  },
  {
    pattern: /fillableScopeJson\?:\s*string\s*\|\s*null/,
    message: 'route flow batch record save payload must include fillableScopeJson.'
  },
  {
    pattern: /requiredPolicy\?:\s*ProRouteFlowRequiredPolicy\s*\|\s*null/,
    message: 'route flow batch record save payload must include requiredPolicy.'
  }
])

assert.match(
  routeProcessList,
  /data-route-process-setting-field="shared-form-instance-scope"/,
  'route process list must expose a shared-form instance scope selector in the existing binding row.'
)

assert.match(
  routeProcessList,
  /data-route-process-setting-field="shared-form-key"/,
  'route process list must expose sharedFormKey input for BATCH_SHARED bindings.'
)

assert.match(
  routeProcessList,
  /data-route-process-setting-field="fillable-scope-json"/,
  'route process list must expose fillableScopeJson input for BATCH_SHARED bindings.'
)

assert.match(
  routeProcessList,
  /data-route-process-setting-field="required-policy"/,
  'route process list must expose requiredPolicy selector so operators can configure optional route forms.'
)

assert.match(
  routeProcessList,
  /requiredPolicy:\s*'REQUIRED'/,
  'route process list must default newly added route form bindings to REQUIRED.'
)

assert.match(
  routeProcessList,
  /requiredPolicy:\s*normalizeRecordBindingRequiredPolicy\(report\.requiredPolicy\)/,
  'route process list must preserve requiredPolicy returned by the backend.'
)

assert.match(
  routeProcessList,
  /instanceScope:\s*binding\.instanceScope\s*\|\|\s*'PROCESS'[\s\S]*sharedFormKey:\s*binding\.sharedFormKey\s*\|\|\s*null[\s\S]*fillableScopeJson:\s*binding\.fillableScopeJson\s*\|\|\s*null[\s\S]*requiredPolicy:\s*binding\.requiredPolicy\s*\|\|\s*'REQUIRED'/,
  'route process list save payload must preserve shared instance fields and requiredPolicy.'
)

assert.match(
  routeProcessList,
  /const shouldSaveRouteScheduleConfig[\s\S]*draft\.routeScheduleConfigId[\s\S]*capacityMode !== 'RESOURCE_CALCULATED'/,
  'route process list must not create route schedule config when only shared batch form binding is being saved on an unconfigured resource row.'
)

assert.match(
  routeProcessList,
  /if\s*\(shouldSaveRouteScheduleConfig\(draft,\s*capacityMode\)\)\s*\{[\s\S]*await ProRouteApi\.saveScheduleConfig\(schedulePayload\)[\s\S]*\}/,
  'route process list must guard route schedule config save so resource blockers do not prevent batch form binding save.'
)

assert.match(
  routeProcessList,
  /const shouldSaveRouteFlowScheduleConfig[\s\S]*draft\.originalProductionQuantityFactor[\s\S]*draft\.hasScheduleProcessConfig[\s\S]*productionQuantityFactor !== DEFAULT_PRODUCTION_QUANTITY_FACTOR/,
  'route process list must not create schedule flow config when only shared batch form binding is being saved.'
)

assert.match(
  routeProcessList,
  /const scheduleProcessConfig = buildScheduleProcessConfigSaveRow\(draft\)[\s\S]*if\s*\(shouldSaveRouteFlowScheduleConfig\(draft\)\)\s*\{[\s\S]*await ProRouteFlowConfigApi\.saveScheduleConfig\(\{[\s\S]*processConfigs:\s*\[scheduleProcessConfig\][\s\S]*\}\)[\s\S]*\}/,
  'route process list must guard schedule flow config save independently from batch record binding save.'
)

assert.match(
  routeProcessList,
  /const buildScheduleProcessConfigSaveRow[\s\S]*productionQuantityFactor[\s\S]*return\s*\{[\s\S]*routeProcessId:\s*draft\.routeProcessId[\s\S]*productionQuantityFactor[\s\S]*remark:\s*draft\.remark\s*\|\|\s*null[\s\S]*\}/,
  'route process list schedule flow payload must be built separately and must not carry batchRecordReports.'
)

assert.match(
  routeFlowGraphDesigner,
  /requiredPolicy:\s*'REQUIRED'/,
  'route flow graph designer must default newly added route form bindings to REQUIRED.'
)

assert.match(
  routeFlowGraphDesigner,
  /requiredPolicy:\s*normalizeRecordBindingRequiredPolicy\(report\.requiredPolicy\)/,
  'route flow graph designer must preserve requiredPolicy returned by the backend.'
)

assert.match(
  routeFlowGraphDesigner,
  /instanceScope:\s*binding\.instanceScope\s*\|\|\s*'PROCESS'[\s\S]*sharedFormKey:\s*binding\.sharedFormKey\s*\|\|\s*null[\s\S]*fillableScopeJson:\s*binding\.fillableScopeJson\s*\|\|\s*null[\s\S]*requiredPolicy:\s*binding\.requiredPolicy\s*\|\|\s*'REQUIRED'/,
  'route flow graph designer save payload must preserve shared instance fields and requiredPolicy even when editing other process attributes.'
)

assert.match(
  batchDetailPage,
  /\.\.\.\(opened\.executionPageQuery \|\| \{\}\)[\s\S]*id:\s*String\(opened\.executionId\)/,
  'batch task open must forward backend executionPageQuery before entering the execution page.'
)

assert.match(
  executionPage,
  /const BATCH_SHARED_INSTANCE_SCOPE\s*=\s*'BATCH_SHARED'/,
  'execution page must define the BATCH_SHARED scope explicitly.'
)

assert.match(
  executionPage,
  /const parseSharedFillScopeJson[\s\S]*sourceTableIndex[\s\S]*startRow[\s\S]*endRow/,
  'execution page must parse shared fill scope ranges with sourceTableIndex/startRow/endRow.'
)

assert.match(
  executionPage,
  /const resolveSharedExecutionSourceTableIndex[\s\S]*execution\.value\?\.metaJson/,
  'execution page must resolve the current sourceTableIndex from execution metaJson.'
)

assert.match(
  executionPage,
  /const sharedFillScopeGateError[\s\S]*fillableScopeJson[\s\S]*sourceTableIndex/,
  'BATCH_SHARED execution must fail fast on missing fillableScopeJson or sourceTableIndex.'
)

assert.match(
  executionPage,
  /const isFieldInCurrentFillScope[\s\S]*sourceTableIndex[\s\S]*field\.rowIndex/,
  'execution page must decide editable scope using sourceTableIndex and row range.'
)

assert.match(
  executionPage,
  /const isTemplateContextReadonlyForCurrentTask[\s\S]*isFieldInCurrentFillScope/,
  'template cells outside the current shared task range must render as readonly.'
)

assert.match(
  executionPage,
  /:disabled="isTemplateContextReadonlyForCurrentTask\(context\)"/,
  'template input controls must use shared-scope readonly guard.'
)

assert.match(
  executionPage,
  /const pendingFieldChanges[\s\S]*\.filter\(isFieldInCurrentFillScope\)/,
  'pending field audit payload must exclude out-of-scope shared form fields.'
)

assert.match(
  executionPage,
  /const pendingAttachmentChanges[\s\S]*\.filter\(isFieldInCurrentFillScope\)/,
  'pending attachment audit payload must exclude out-of-scope shared form fields.'
)

assert.match(
  executionPage,
  /const requiredEditableFields[\s\S]*\.filter\(isFieldInCurrentFillScope\)/,
  'required validation for BATCH_SHARED tasks must only require current task scope.'
)

console.log('PASS: eDHR shared form binding frontend static contract')
