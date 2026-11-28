const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { runInNewContext } = require('node:vm')
const { test } = require('node:test')
const assert = require('node:assert/strict')
const ts = require('typescript')
const source = readFileSync(resolve(__dirname, '../src/views/mes/pro/task/calendar/index.vue'), 'utf8')
const helper = source.slice(source.indexOf('function requireWorkOrderQuantity('), source.indexOf('function resolveTaskDailyQuantity('))
const validateDetail = source.slice(source.indexOf('function validateDayDetailWorkOrderQuantities('), source.indexOf('function requireWorkOrderQuantity('))
const aggregation = source.slice(source.indexOf('const selectedDayWorkOrderRows ='), source.indexOf('const selectedDayOrderCount ='))
function calculate(rows) {
 const code = ts.transpileModule(helper + '\n' + aggregation + '\nresult = selectedDayWorkOrderPlanQuantity.value', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
 const context = { selectedDayTaskRows: { value: rows }, computed: fn => ({ get value() { return fn() } }), resolveTaskDailyQuantity: t => t.dailyQuantity, buildTaskKey: t => t.taskId }
 runInNewContext(code, context)
 return context.result
}
test('actual split tasks count the original order once, regardless of process coefficient', () => {
 assert.equal(calculate([{workOrderId:1,workOrderQuantity:2019,quantity:969,dailyQuantity:969},{workOrderId:1,workOrderQuantity:2019,quantity:1050,dailyQuantity:1050},{workOrderId:1,workOrderQuantity:2019,quantity:1616,dailyQuantity:800}]),2019)
})
test('multiple work orders sum their original quantities', () => {
 assert.equal(calculate([1,2].flatMap(workOrderId => [969,1050].map(quantity => ({workOrderId,workOrderQuantity:2019,quantity,dailyQuantity:quantity})))) ,4038)
})

test('day loading catches invalid quantities before publishing reactive detail and always stops loading', async () => {
 const load = source.slice(source.indexOf('async function loadDayDetail('), source.indexOf('async function saveRules('))
 const context = {
  detailRequestSerial: 0, dayDetail: { value: null }, detailLoading: { value: false }, detailErrorMessage: { value: '' }, calendarRecoveryState: { value: null },
  ProScheduleCalendarApi: { getDayDetail: async () => ({date:'2026-11-26', workshops:[{lines:[{tasks:[{workOrderId:1,quantity:969}]}]}]}) },
  normalizeDate: v => v, clearCalendarRecovery: () => {}, buildCalendarRecoveryState: error => ({title:error.message})
 }
 const code = ts.transpileModule(helper+'\n'+validateDetail+'\n'+load+'\nresult=loadDayDetail("2026-11-26")', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
 runInNewContext(code, context)
 await context.result
 assert.equal(context.dayDetail.value, null)
 assert.equal(context.detailLoading.value, false)
 assert.match(context.detailErrorMessage.value, /原始计划量缺失或无效/)
 context.ProScheduleCalendarApi.getDayDetail = async () => ({date:'2026-11-26',workshops:[{lines:[{tasks:[{workOrderId:1,workOrderQuantity:2019},{workOrderId:1,workOrderQuantity:969}]}]}]})
 runInNewContext('result=loadDayDetail("2026-11-26")',context)
 await context.result
 assert.equal(context.dayDetail.value,null)
 assert.match(context.detailErrorMessage.value,/计划量不一致/)
 context.ProScheduleCalendarApi.getDayDetail = async () => ({date:'2026-11-26',workshops:[{lines:[{tasks:[{workOrderId:1,workOrderQuantity:2019,quantity:969}]}]}]})
 runInNewContext('result=loadDayDetail("2026-11-26")',context)
 await context.result
 assert.equal(context.dayDetail.value.workshops[0].lines[0].tasks[0].workOrderQuantity,2019)
 assert.equal(context.detailLoading.value,false)
 assert.equal(context.detailErrorMessage.value,'')
})
test('missing or inconsistent original quantity fails visibly', () => {
 assert.throws(() => calculate([{workOrderId:1,quantity:969}]), /原始计划量缺失或无效/)
 assert.throws(() => calculate([{workOrderId:1,workOrderQuantity:2019},{workOrderId:1,workOrderQuantity:969}]), /计划量不一致/)
})
test('preview takes formal quantity from work order analysis, never task slices', () => {
 assert.match(source, /workOrderQuantity: requireWorkOrderQuantity\(\s*previewWorkOrderAnalysisMap.value.get\(workOrderId\)\?\.quantity, workOrderId/)
})
const groupAggregation = source.slice(source.indexOf('const daySummaryTaskWorkOrderGroups ='), source.indexOf('const selectedDaySummaryTaskRows ='))
test('group cards keep formal plan quantity separate from daily and process totals', () => {
 const rows = [969,1050].map(quantity => ({workOrderId:1,workOrderQuantity:2019,quantity,dailyQuantity:quantity,lineNameTitle:'Line'}))
 const context = { daySummaryDialogTaskRows: { value: rows }, computed: fn => ({ get value() { return fn() } }), resolveTaskDailyQuantity:t=>t.dailyQuantity, buildDaySummaryTaskWorkOrderKey:t=>String(t.workOrderId) }
 runInNewContext(ts.transpileModule(helper+'\n'+groupAggregation+'\nresult=daySummaryTaskWorkOrderGroups.value', {compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
 assert.equal(context.result[0].workOrderPlanQuantity,2019)
 assert.equal(context.result[0].processTaskQuantity,2019)
 assert.equal(context.result[0].dailyProcessQuantity,2019)
})
test('preview computed reads formal analysis quantity for every task slice', () => {
 const preview = source.slice(source.indexOf('const previewTaskRows ='),source.indexOf('const previewTaskRowsByDate ='))
 const context = { computed:fn=>({get value(){return fn()}}), autoSchedulePreview:{value:{tasks:[969,1050].map((quantity,i)=>({type:303,originalId:i+1,quantity}))}}, resolvePreviewWorkOrderId:()=>1, previewParentTaskMap:{value:new Map()}, resolvePreviewTaskLineName:()=> 'Line', resolvePreviewWorkOrderCode:()=> 'WO1', resolvePreviewParentProductName:()=> 'Product', inferPreviewShiftCode:()=> 'DAY', normalizeDateTimeText:()=> '', previewWorkOrderAnalysisMap:{value:new Map([[1,{quantity:2019}]])} }
 runInNewContext(ts.transpileModule(helper+'\n'+preview+'\nresult=previewTaskRows.value', {compilerOptions:{target:ts.ScriptTarget.ES2022}}).outputText,context)
 assert.equal(calculate(context.result),2019)
 assert.equal(context.result[0].quantity,969)
 context.previewWorkOrderAnalysisMap.value.clear()
 assert.throws(()=>runInNewContext('previewTaskRows.value',context),/原始计划量缺失或无效/)
})

