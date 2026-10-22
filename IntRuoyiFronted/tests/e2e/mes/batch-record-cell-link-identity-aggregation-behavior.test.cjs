const fs = require('node:fs')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ts = require('typescript')
const page = fs.readFileSync('src/views/mes/pro/batchrecordcelllink/index.vue', 'utf8')
const body = page.match(/const availableAggregationOptions = computed\(\(\) => \{([\s\S]*?)\n\}\)/)[1]
const compiled = ts.transpileModule(`function available(selectedSourceCell, PROCESS_POOL_REPORT_AGGREGATION_OPTIONS) {${body}}`,
  { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const context = {}
vm.runInNewContext(compiled, context)
const options = ['SUM', 'MIN', 'MAX', 'FIRST', 'LAST'].map(value => ({ value, sourceValueTypes: ['NUMBER'] }))
for (const sourceFieldCode of ['operatorUserId', 'actualEmployeeId']) {
  assert.equal(context.available({ value: { sourceFieldCode, valueType: 'NUMBER' } }, options)
    .map(option => option.value).join(','), 'FIRST,LAST')
}
assert.equal(context.available({ value: { sourceFieldCode: 'outputQuantity', valueType: 'NUMBER' } }, options)
  .map(option => option.value).join(','), 'SUM,MIN,MAX,FIRST,LAST')
console.log('PASS identity mapping options: only FIRST/LAST; existing quantity options preserved')
