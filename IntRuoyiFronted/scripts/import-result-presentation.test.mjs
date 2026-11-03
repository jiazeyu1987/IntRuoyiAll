import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'
import ts from 'typescript'

const require = createRequire(import.meta.url)
const Vue = require('vue')
const { renderToString } = require('vue/server-renderer')
const { parse, compileScript } = require('vue/compiler-sfc')
const componentFile = new URL(
  '../src/components/ImportResultContent/ImportResultContent.vue',
  import.meta.url
)
const { descriptor } = parse(readFileSync(componentFile, 'utf8'), {
  filename: componentFile.pathname
})
const componentCode = compileScript(descriptor, {
  id: 'import-result',
  inlineTemplate: true
}).content
const evaluate = (source, resolve) => {
  const exports = {}
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 }
  }).outputText
  new Function('exports', 'require', code)(exports, resolve)
  return exports
}
const component = evaluate(componentCode, require).default
let shown
const presentation = evaluate(
  readFileSync(new URL('../src/utils/importResult.ts', import.meta.url), 'utf8'),
  (name) => {
    if (name === 'vue') return Vue
    if (name.endsWith('ImportResultContent.vue')) return { default: component, __esModule: true }
    if (name === 'element-plus')
      return {
        ElMessageBox: {
          alert(content, title, options) {
            shown = { content, title, options }
            return Promise.resolve('confirm')
          }
        }
      }
    throw new Error(`Unexpected presentation dependency: ${name}`)
  }
)
const render = () => renderToString(Vue.createSSRApp({ render: () => shown.content }))

test('all-failed import shows failure rows without zero success counts or raw HTML', async () => {
  await presentation.showRecordImportResult({
    entity: '用户账号',
    created: [],
    updated: [],
    failures: { alice: '邮箱已存在', '<script>alert(1)</script>': '检查 <邮箱>' }
  })
  assert.equal(shown.title, '导入失败')
  assert.equal(shown.options.confirmButtonText, '知道了')
  assert.equal(shown.options.type, 'warning')
  const html = await render()
  assert.match(html, /本次失败 2 条/)
  assert.match(html, /失败记录（2 条）/)
  assert.match(html, /<details[^>]*open/)
  assert.match(html, /&lt;script&gt;alert\(1\)&lt;\/script&gt;/)
  assert.doesNotMatch(html, /<script>|新增记录|更新记录|新增 0|更新 0/)
  assert.equal((html.match(/<tr>/g) ?? []).length - 1, 2)
})

test('partial success preserves every record and collapses the successful list', async () => {
  const created = Array.from({ length: 150 }, (_, i) => `item-${i}`)
  await presentation.showRecordImportResult({
    entity: '物料编码',
    created,
    updated: ['updated-item'],
    failures: { bad: '规格缺失' }
  })
  assert.equal(shown.title, '部分记录导入失败')
  const html = await render()
  assert.match(html, /新增 150 条，更新 1 条，失败 1 条/)
  assert.match(html, /item-149/)
  assert.match(html, /updated-item/)
  assert.match(html, /<details class="import-result-content__section"><summary>新增记录（150 条）/)
  assert.match(html, /避免重复提交已成功的记录/)
})

test('empty result does not fabricate processed records', async () => {
  await presentation.showRecordImportResult({
    entity: '设备',
    created: [],
    updated: [],
    failures: {}
  })
  const html = await render()
  assert.match(html, /本次没有新增、更新或失败的记录/)
  assert.doesNotMatch(html, /<details|<table|<dl/)
})

test('department warnings remain visible when user import succeeds', async () => {
  await presentation.showImportResult({
    title: '钉钉导入完成',
    summary: '已创建 1 个用户。',
    warning: true,
    stats: [
      { label: '创建用户', value: 1 },
      { label: '未设置部门负责人', value: 1 }
    ],
    sections: [
      {
        title: '未设置负责人的部门',
        columns: ['部门', '原因'],
        rows: [['生产部', '负责人账号不存在']],
        expanded: true
      }
    ]
  })
  assert.equal(shown.options.type, 'warning')
  const html = await render()
  assert.match(html, /生产部/)
  assert.match(html, /负责人账号不存在/)
})
