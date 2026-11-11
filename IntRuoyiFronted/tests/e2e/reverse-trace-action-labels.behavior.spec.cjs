const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const root = path.resolve(__dirname, '../../..')
const source = fs.readFileSync(path.join(root, 'IntRuoyiFronted/src/views/mes/pro/edhr-batch/reverseTraceActionLabels.ts'), 'utf8')
const javascript = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText
const exportsObject = {}
Function('exports', javascript)(exportsObject)
const format = exportsObject.formatReverseTraceAction

test('all production reverse trace actions have explicit Chinese labels', () => {
  const adapter = fs.readFileSync(path.join(root, 'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrFormalReverseTraceAdapter.java'), 'utf8')
  const actions = new Set([...adapter.matchAll(/"FLOW-\d+",\s*"([A-Z_]+)"/g)].map(match => match[1]))
  assert.equal(actions.size, 21)
  for (const action of actions) assert.match(format(action), /^[\u4e00-\u9fff]+$/)
  assert.equal(format('PQC_REVIEW'), '复核过程检验记录')
  assert.equal(format('PRODUCTION_OPERATOR'), '填写生产记录')
  assert.throws(() => format('UNRECOGNIZED'), /缺少中文名称/)
})
