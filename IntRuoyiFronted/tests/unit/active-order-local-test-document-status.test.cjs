const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { createSSRApp } = require('vue')
const { renderToString } = require('vue/server-renderer')

const base = path.resolve(__dirname, '../../src/views/mes/pro/processpool/components')
const source = fs.readFileSync(path.join(base, 'activeOrderDisplayLabels.ts'), 'utf8')
const exportsObject = {}
const code = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
}).outputText
Function('exports', code)(exportsObject)
const { pickDocumentStatusLabel } = exportsObject

test('the actual detail status expression renders LOCAL_TEST explicitly as local test', async () => {
  const panel = fs.readFileSync(path.join(base, 'ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
  const expression = panel.match(/<strong>\{\{ pickDocumentStatusLabel\(document\.documentStatus\) \}\}<\/strong>/)?.[0]
  assert.ok(expression, 'the panel must use the tested status mapper')
  const app = createSSRApp({
    template: expression,
    setup: () => ({ document: { documentStatus: 'LOCAL_TEST' }, pickDocumentStatusLabel })
  })
  assert.equal(await renderToString(app), '<strong>本地测试</strong>')
  assert.notEqual(pickDocumentStatusLabel('LOCAL_TEST'), pickDocumentStatusLabel('C'))
})

test('formal ERP status meanings remain unchanged', () => {
  for (const [status, label] of Object.entries({ Z: '暂存', A: '创建', B: '审核中', C: '已审核', D: '重新审核' })) {
    assert.equal(pickDocumentStatusLabel(status), label)
  }
})

test('unknown document states still fail rather than displaying a default success', () => {
  for (const value of ['NEW_STATUS', 'LOCAL_TEST_UNKNOWN']) {
    assert.throws(() => pickDocumentStatusLabel(value), {
      message: `领料单状态缺少中文描述：${value}`
    })
  }
})
