const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true)
const names = ['buildPqcInspectionRecordRows', 'formatActiveOrderPqcSubmittedSampleCount']
const declarations = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(declarations.length, names.length)
const code = ts.transpileModule(declarations.map(s => s.getText(ast)).join('\n') + '\nexports.build = buildPqcInspectionRecordRows;', {compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText

function rows(submissions) {
  const ctx = {
    exports: {}, pqcProcessGroups: {value:[{key:'process',qaProcessName:'组装',submissions}]},
    buildActiveOrderPqcProcessInspectionItemRows: s => [...new Set(s.processInspectionItems.map(i => i.itemCode))].map(key => ({key,itemIdentityKey:key,itemNameText:key})),
    resolveActiveOrderPqcItemIdentityKey: i => i.itemCode,
    normalizeActiveOrderPqcText: x => String(x || ''),
    formatActiveOrderPqcItemUniqueSummary: xs => xs.join(''),
    formatActiveOrderPqcItemSampleSummary: () => '',
    formatPqcInspectionMeasuredValues: () => '',
    summarizePqcInspectionJudgements: () => '通过',
    formatPqcInspectionRecordEquipmentText: () => '',
    resolvePqcInspectionTypeText: s => s.inspectionType,
    formatPqcInspectionRecordResultText: () => '',
    applyPqcInspectionRecordRowSpan: () => {}
  }
  vm.runInNewContext(code, ctx)
  return JSON.parse(JSON.stringify(ctx.exports.build()))
}

test('inspection report keeps each item actual sample count across first and patrol rounds', () => {
  const submission = (id, type, counts) => ({
    pqcTaskId:id, inspectionType:type, actualInspectionQuantity:Math.max(...counts),
    processInspectionItems:counts.flatMap((count,i) => Array.from({length:count},(_,n) => ({itemCode:`item${i}`,sampleNo:n+1})))
  })
  const result = rows([submission(1,'FIRST',[10,5]),submission(2,'PATROL',[4,2])])
  assert.deepEqual(result.map(r => [r.itemIdentityKey,r.inspectionTypeText,r.inspectionQuantityText]), [
    ['item0','FIRST','10'],['item0','PATROL','4'],['item1','FIRST','5'],['item1','PATROL','2']
  ])
})
