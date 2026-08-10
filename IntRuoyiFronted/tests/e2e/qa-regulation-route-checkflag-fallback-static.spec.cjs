const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = process.cwd()
const qaPagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const qaSource = fs.readFileSync(qaPagePath, 'utf8').replace(/\r\n/g, '\n')

const resolverStart = qaSource.indexOf('const resolveQaRouteProcessFromRoute =')
const resolverEnd = qaSource.indexOf('const applyFormalQaRouteScope =', resolverStart)
assert.ok(resolverStart >= 0 && resolverEnd > resolverStart, 'QA route process resolver must exist.')

const resolverSource = qaSource.slice(resolverStart, resolverEnd)
const loadBindingStart = qaSource.indexOf('async function loadQaRouteScopeFromRouteBinding')
const loadBindingEnd = qaSource.indexOf('const loadManualQaRouteOptions =', loadBindingStart)
assert.ok(
  loadBindingStart >= 0 && loadBindingEnd > loadBindingStart,
  'QA route scope loading function must exist.'
)

const loadBindingSource = qaSource.slice(loadBindingStart, loadBindingEnd)
const batchCandidateIndex = resolverSource.indexOf('batchRecordProcesses')
const routeProcessCandidateIndex = resolverSource.indexOf('routeProcessBatchRecordProcesses')
const keyRouteProcessCandidateIndex = resolverSource.indexOf('keyRouteProcesses')
const noCheckFlagThrowIndex = resolverSource.indexOf('当前工艺路线未标记唯一质检工序')

assert.match(
  qaSource,
  /const hasFormalQaBatchRecordBinding = \(config: ProRouteFlowProcessConfigVO\) =>/,
  'QA route resolver must model formal batch-record binding as the deterministic no-checkFlag source.'
)
assert.match(
  qaSource,
  /hasFormalQaBatchRecordBinding[\s\S]*config\.enabled === true[\s\S]*batchRecordReports/,
  'Only enabled BATCH configs with formal batchRecordReports may identify the QA process.'
)
assert.match(
  qaSource,
  /const hasFormalQaRouteProcessBatchRecordBinding = \(process: ProRouteProcessVO\) =>/,
  'QA route resolver must also honor the formal published route-process batchRecordReport projection.'
)
assert.match(
  qaSource,
  /hasFormalQaRouteProcessBatchRecordBinding[\s\S]*process\.batchRecordReportId[\s\S]*process\.batchRecordReportCode[\s\S]*process\.batchRecordReportName/,
  'The route-process projection candidate must come from batchRecordReport fields, not form slots.'
)
assert.match(
  qaSource,
  /const hasFormalQaKeyRouteProcess = \(process: ProRouteProcessVO\) =>[\s\S]*process\.keyFlag === true/,
  'QA route resolver must honor the formal route keyFlag marker when checkFlag and batch-record bindings are absent.'
)
assert.ok(
  batchCandidateIndex >= 0 && noCheckFlagThrowIndex > batchCandidateIndex,
  'Missing checkFlag must check for one formal batch-record process before showing the checkFlag maintenance error.'
)
assert.ok(
  routeProcessCandidateIndex >= 0 && noCheckFlagThrowIndex > routeProcessCandidateIndex,
  'Missing checkFlag must check the route-process batchRecordReport projection before showing the checkFlag maintenance error.'
)
assert.ok(
  keyRouteProcessCandidateIndex >= 0 && noCheckFlagThrowIndex > keyRouteProcessCandidateIndex,
  'Missing checkFlag must check for one formal route keyFlag process before showing the checkFlag maintenance error.'
)
assert.match(
  resolverSource,
  /if \(batchRecordProcesses\.length === 1\) \{[\s\S]*return batchRecordProcesses\[0\]/,
  'A route with exactly one formal batch-record process must load QA scope without requiring checkFlag.'
)
assert.match(
  resolverSource,
  /if \(checkProcesses\.length > 1\) \{[\s\S]*多个质检工序/,
  'Routes with multiple checkFlag processes must still fail fast.'
)
assert.match(
  resolverSource,
  /if \(batchRecordProcesses\.length > 1\) \{[\s\S]*多个正式批记录绑定工序/,
  'Routes with multiple formal batch-record process candidates must fail fast instead of guessing.'
)
assert.match(
  resolverSource,
  /if \(routeProcessBatchRecordProcesses\.length === 1\) \{[\s\S]*return routeProcessBatchRecordProcesses\[0\]/,
  'A route with exactly one published route-process batchRecordReport must load QA scope without requiring checkFlag.'
)
assert.match(
  resolverSource,
  /if \(routeProcessBatchRecordProcesses\.length > 1\) \{[\s\S]*多个默认批记录报表工序/,
  'Routes with multiple route-process batchRecordReport candidates must fail fast instead of guessing.'
)
assert.match(
  resolverSource,
  /if \(keyRouteProcesses\.length === 1\) \{[\s\S]*return keyRouteProcesses\[0\]/,
  'A route with exactly one keyFlag process must load QA scope without requiring checkFlag.'
)
assert.match(
  resolverSource,
  /if \(keyRouteProcesses\.length > 1\) \{[\s\S]*多个关键工序/,
  'Routes with multiple keyFlag processes must fail fast instead of guessing.'
)
assert.doesNotMatch(
  resolverSource,
  /formBindings/,
  'QA process selection must not use formBindings because form slots are not formal batch records.'
)
assert.match(
  loadBindingSource,
  /const \[currentRouteProcesses, scheduleConfigs, batchConfigs\] = await Promise\.all\([\s\S]*resolveQaVersionRouteProcesses\([\s\S]*currentRouteProcesses,[\s\S]*scheduleConfigs,[\s\S]*batchConfigs/,
  'Current route processes, schedule configs, and batch configs must be loaded before resolving frozen QA route-version processes.'
)
assert.match(
  loadBindingSource,
  /resolveQaRouteProcessFromRoute\(routeProcesses, batchConfigs\)/,
  'QA route process resolution must receive BATCH configs for deterministic no-checkFlag routes.'
)

console.log('PASS qa-regulation-route-checkflag-fallback-static')
