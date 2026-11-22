const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const { test } = require('node:test')

const source = fs.readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/route/RouteFlowGraphDesigner.vue'), 'utf8')
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('designer.ts', script, ts.ScriptTarget.Latest, true)
const names = ['getSelectedProcessRecordBindingSaveDrafts', 'saveSelectedProcessAttributeDrafts']
const declarations = ast.statements.filter(s => ts.isVariableStatement(s) &&
  s.declarationList.declarations.some(d => names.includes(d.name.getText(ast))))
assert.equal(declarations.length, names.length)
const code = ts.transpileModule(declarations.map(s => s.getText(ast)).join('\n') +
  '\nexports.save = saveSelectedProcessAttributeDrafts', {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText

function setup({ changed = true, fail = false } = {}) {
  const rows = [1, 2, 3].map(id => ({ routeProcessId: String(id), inputMaterialIds: [id],
    outputMaterialIds: [], batchRecordReports: [], remark: `process-${id}`,
    formBindings: [{ globalSyncKey: 'GFB-1', formBindingKey: `global-${id}` },
      { globalSyncKey: null, formBindingKey: `local-${id}` }] }))
  const writes = []
  const ctx = { exports: {}, props: { routeId: '10' },
    selectedProcessAttributesSaving: { value: false },
    getChangedSelectedProcessScheduleDrafts: () => [],
    getChangedSelectedProcessRecordBindingDrafts: () => changed ? [rows[2]] : [],
    getAllRouteProcessAttributeDrafts: () => rows.map(draft => ({ draft })),
    requireCandidateRouteVersionId: () => '20',
    buildSelectedProcessRecordBindingConfigSaveRow: draft => draft,
    ProRouteFlowConfigApi: { saveBatchRecordConfig: async payload => {
      writes.push(JSON.parse(JSON.stringify(payload)))
      if (fail) throw new Error('全局配置保存失败')
    } }
  }
  vm.runInNewContext(code, ctx)
  return { ctx, rows, writes }
}

test('仅新增工序表单变化也完整提交全局组，保留每道工序本地绑定及物料', async () => {
  const h = setup()
  await h.ctx.exports.save()
  assert.equal(h.writes.length, 1)
  assert.equal(h.writes[0].routeVersionId, '20')
  const configs = h.writes[0].processConfigs
  assert.deepEqual(configs.map(c => c.routeProcessId), ['1', '2', '3'])
  for (let i = 0; i < configs.length; i++) {
    assert.deepEqual(configs[i].formBindings, h.rows[i].formBindings)
    assert.deepEqual(configs[i].inputMaterialIds, h.rows[i].inputMaterialIds)
    assert.equal(configs[i].remark, h.rows[i].remark)
  }
})

test('表单无变化不发送多余批记录写入', async () => {
  const h = setup({ changed: false })
  await h.ctx.exports.save()
  assert.deepEqual(h.writes, [])
})

test('批记录保存失败向上传递且释放保存状态', async () => {
  const h = setup({ fail: true })
  await assert.rejects(h.ctx.exports.save(), /全局配置保存失败/)
  assert.equal(h.ctx.selectedProcessAttributesSaving.value, false)
})
