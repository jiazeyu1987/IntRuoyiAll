const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const ts = require('typescript')
const vue = require('vue')
const { renderToString } = require('vue/server-renderer')
const { parse, compileScript } = require('vue/compiler-sfc')
const elementPlus = require('element-plus')
const root = 'src/views/dcc/registration-certificate/shared/'
const transpile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText
const facts = {}
new Function('exports', transpile(fs.readFileSync(root + 'facts.ts', 'utf8')))(facts)
const script = compileScript(parse(fs.readFileSync(root + 'FactsFields.vue', 'utf8')).descriptor, { id: 'creation-layout', inlineTemplate: true })
const component = {}
new Function('exports', 'require', transpile(script.content))(component, id => id === './facts' ? facts : require(id))
async function render(model, section) {
  const app = vue.createSSRApp({ render: () => vue.h(elementPlus.ElForm, {}, () => vue.h(component.default, { modelValue: model, creation: true, creationSection: section })) })
  app.use(elementPlus.default)
  app.provide(elementPlus.ID_INJECTION_KEY, { prefix: 1024, current: 0 })
  app.provide(elementPlus.ZINDEX_INJECTION_KEY, { current: 0 })
  return renderToString(app)
}
const labels = html => [...html.matchAll(/<(?:label|div)\b[^>]*class="el-form-item__label"[^>]*>(.*?)<\/(?:label|div)>/gs)].map(match => match[1].replace(/<[^>]*>/g, '').trim())

for (const certificateType of ['DOMESTIC', 'CE', 'FDA']) {
  test(`${certificateType}: grouping renders every existing creation field exactly once without changing data`, async () => {
    const model = { ...facts.emptyFacts(), certificateType, remark: '原有备注', ceCertificates: [{ revision: '1', certificateNo: 'CE-01' }] }
    const before = JSON.stringify(model)
    const original = labels(await render(model))
    const sections = []
    for (const section of ['attributes', 'certificate', 'supplement']) sections.push(await render(model, section))
    const grouped = sections.flatMap(labels)
    assert.deepEqual(grouped.toSorted(), original.filter(label => !['注册证类型', '产品归属'].includes(label)).toSorted())
    assert.equal(grouped.length, new Set(grouped).size, 'no duplicated fields across sections')
    assert.deepEqual(labels(sections[2]), ['备注'])
    assert.equal(sections[0].includes('证书Rev.记录'), false)
    assert.equal(sections[1].includes('证书Rev.记录'), certificateType === 'CE')
    assert.equal(JSON.stringify(model), before, 'layout must not mutate certificate facts')
  })
}
