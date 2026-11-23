const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const repoRoot = resolve(__dirname, '../../..')
const component = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
  ),
  'utf8'
)

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  component.includes('formatIntegerQuantity'),
  '生产记录表单必须使用整数数量格式化函数'
)

assert(
  /const formatIntegerQuantity[\s\S]*return Number\.isFinite\(parsed\) \? String\(Math\.round\(parsed\)\) : String\(value\)/.test(component),
  '数量相关展示必须四舍五入为整数文本，不能保留三位小数'
)

assert(
  !/const formatTraceQuantity[\s\S]*\.toFixed\(3\)/.test(component),
  '生产记录详情不能继续使用 toFixed(3) 展示数量'
)

const ts = require('typescript')
const sf = ts.createSourceFile('panel.ts', component.slice(component.indexOf('<script setup'), component.indexOf('</script>')).replace(/^.*?>/, ''), ts.ScriptTarget.Latest, true)
const wanted = ['toNumberOrUndefined','sumProductionRecordQuantities','toActiveOrderProductionEventSubmissionRow','buildProductionRecordRows','buildProductionRecordTotals','buildProductionRecordEvidenceRows']
const deviceNames = sf.statements.filter(n => ts.isVariableStatement(n)).flatMap(n => n.declarationList.declarations.map(d => d.name.getText(sf)))
const firstDevice = deviceNames.indexOf('formatProductionRecordParameterValue')
const lastDevice = deviceNames.indexOf('buildProductionRecordRowDeviceGroups')
wanted.push(...deviceNames.slice(firstDevice, lastDevice + 1).filter(name => !wanted.includes(name)))
const chunks = sf.statements.filter(n => ts.isVariableStatement(n) && n.declarationList.declarations.some(d => wanted.includes(d.name.getText(sf)))).map(n => n.getText(sf)).join('\n')
const js = ts.transpile(chunks, { target: ts.ScriptTarget.ES2020 })
const api = new Function(js + ';return {' + wanted.join(',') + '}')()
const buildRows = api.buildProductionRecordRows
for (const materials of [[], [{ outputQuantity: 99, lossQuantity: 9 }], [{}, {}]]) {
  const rows = buildRows({ submissions: [{ eventId: 327089, originalOutputQuantity: 25, originalLossQuantity: 1, submittedQuantity: 22, materials }] })
  assert(rows.length === 1 && rows[0].outputQuantity === 25 && rows[0].lossQuantity === 1 && rows[0].totalQuantity === 26, 'Formal quantities once')
  assert(api.buildProductionRecordTotals(rows).totalQuantity === 26, 'Real totals')
}
const rows = buildRows({submissions:[{eventId:1,originalOutputQuantity:25,originalLossQuantity:1},{eventId:2,originalOutputQuantity:26,originalLossQuantity:0}]})
const totals = api.buildProductionRecordTotals(rows)
assert(totals.outputQuantity === 51 && totals.lossQuantity === 1 && totals.totalQuantity === 52, 'Distinct event totals 51/1/52')
for (const submittedQuantity of [22,4]) {
 const row = buildRows({submissions:[{eventId:327091,originalOutputQuantity:26,originalLossQuantity:0,submittedQuantity}]})[0]
 assert(row.outputQuantity === 26 && row.submittedQuantity === submittedQuantity, 'Independent process allocation')
}
for (const field of ['originalOutputQuantity','originalLossQuantity']) for (const missing of [undefined, null, '', NaN]) {
  let failed = false
  try { buildRows({submissions:[{originalOutputQuantity:25,originalLossQuantity:1,[field]:missing}]}) } catch {failed=true}
  assert(failed, 'Missing formal quantity must fail: ' + field)
}
const event = {key:'event', materials:[{materialCode:'A',clearanceConfirmations:[{key:'workplace',confirmed:false}],deviceParameters:[{value:1}]},{materialCode:'B'}],clearanceConfirmations:[{key:'workplace',confirmed:true}],devices:[{deviceId:1}],deviceParameters:[{value:2}]}
const evidence = api.buildProductionRecordEvidenceRows(event)
assert(evidence[0].clearanceConfirmations[0].confirmed === false && evidence[1].clearanceConfirmations === event.clearanceConfirmations, 'Material clearance priority and submission inheritance')
assert(evidence[0].deviceParameters[0].value === 1 && evidence[1].deviceParameters[0].value === 2 && evidence[0].key !== evidence[1].key, 'Material parameter identity')
console.log('PASS: real production quantity, total and material evidence behavior')

const sharedDevice = {deviceId:7,deviceCode:'D7',deviceName:'Shared'}
const uniqueDevice = {deviceId:8,deviceCode:'D8',deviceName:'Root only'}
const deviceEvent = {key:'e',devices:[sharedDevice,uniqueDevice],deviceParameters:[],clearanceConfirmations:[],materials:[
 {materialCode:'A',devices:[sharedDevice],deviceParameters:[{deviceId:7,value:11}]},
 {materialCode:'B',devices:[sharedDevice],deviceParameters:[{deviceId:7,value:22}]}
]}
const groups = api.buildProductionRecordRowDeviceGroups(deviceEvent)
assert(groups.length === 3, 'Two material groups and root unique device only')
assert(groups.filter(g => g.deviceCodeText === 'D7').length === 2, 'No duplicate root shared device card')
assert(groups.find(g => g.deviceNameText.includes('A /')).parameters[0].parameterValueText === '11', 'Material A parameter')
assert(groups.find(g => g.deviceNameText.includes('B /')).parameters[0].parameterValueText === '22', 'Material B parameter')
assert(groups.filter(g => g.deviceCodeText === 'D8').length === 1, 'Root unique device retained once')
assert(api.buildProductionRecordRowDeviceGroups({...deviceEvent,devices:[],materials:[]}).length === 0, 'No empty device card')
console.log('PASS: real material device grouping behavior')
