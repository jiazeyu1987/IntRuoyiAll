const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const front = path.resolve(__dirname, '../..')
const read = rel => fs.readFileSync(path.join(front, rel), 'utf8')
const page = read('src/views/dcc/controlled-file/upload/index.vue')
const ref = vue.ref
const PROJECT = '9007199254740993'
const CATALOG = '9223372036854775701'
const product = (source = 'DCC_CATALOG', project = PROJECT) => ({ projectCodeId: project, source,
  productMasterId: source === 'MDM_MASTER' ? CATALOG : null,
  productCatalogId: source === 'DCC_CATALOG' ? CATALOG : null,
  productRelationId: source === 'DCC_CATALOG' ? '9223372036854775702' : null,
  productCreateRequestId: source === 'DCC_CATALOG' ? '9223372036854775703' : null,
  productCode: source === 'UNBOUND' ? null : 'APPROVED-PRODUCT-CODE', productName: source === 'UNBOUND' ? null : '已批准真实产品' })
function helpers() {
  const file = 'src/views/dcc/controlled-file/upload/project-product-identity.ts'
  assert.ok(fs.existsSync(path.join(front, file)), 'formal product projection validator must exist')
  const context = { exports: {}, require: () => { throw Error('type-only dependency expected') }, Error, String, Number, BigInt }
  vm.runInNewContext(ts.transpileModule(read(file), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return context.exports
}
function state(loader) {
  const calls = []
  const context = { exports: {}, Error, JSON, String, Number, Promise,
    formData: vue.reactive({ dccProjectCodeId: PROJECT, productMasterId: null, productCode: '' }),
    selectedProjectCode: ref({ id: PROJECT, projectCode: 'PROJECT-CODE-IS-NOT-PRODUCT' }),
    isExternalReview: ref(false), projectProduct: ref(undefined), projectProductLoading: ref(false), projectProductError: ref(''),
    projectProductResolvedId: ref(null), projectProductRequestSequence: 0, projectProductMounted: true,
    previewControlledFileProjectProduct: async id => { calls.push(id); return loader(id) },
    readProjectProductIdentity: (value, id) => helpers().readProjectProductIdentity(value, id),
    resolveUploadErrorMessage: error => error.message }
  const from = page.indexOf('const applyDccProjectCodeProductNumber =')
  const to = page.indexOf('const loadBaseData =', from)
  assert.ok(from > 0 && to > from)
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(page.slice(from, to) + '\nexports.load = applyDccProjectCodeProductNumber', {
    compilerOptions: { target: ts.ScriptTarget.ES2022 }
  }).outputText, context)
  return { context, calls, load: context.exports.load }
}
test('actual project selection consumes formal server product instead of project code', async () => {
  const s = state(async () => product())
  await s.load()
  assert.deepEqual(s.calls, [PROJECT])
  assert.equal(s.context.formData.productCode, 'APPROVED-PRODUCT-CODE')
  assert.equal(s.context.projectProduct.value.productName, '已批准真实产品')
  assert.equal(s.context.projectProduct.value.productCatalogId, CATALOG)
  assert.equal(s.context.projectProduct.value.productMasterId, null)
  assert.equal(s.context.projectProductResolvedId.value, PROJECT)
  assert.equal(s.context.projectProductLoading.value, false)
})
test('projection validator preserves exact source identities and rejects mixed or foreign products', () => {
  const h = helpers()
  for (const source of ['MDM_MASTER', 'DCC_CATALOG', 'UNBOUND']) {
    const result = h.readProjectProductIdentity(product(source), PROJECT)
    assert.equal(result.source, source)
    assert.equal(result.projectCodeId, PROJECT)
  }
  for (const change of [ { source: 'PRODUCT_MASTER' }, { projectCodeId: '8' }, { productMasterId: '8' },
    { productCatalogId: null }, { productCode: '' }, { productName: null }, { productRelationId: 9007199254740992 } ]) {
    assert.throws(() => h.readProjectProductIdentity({ ...product(), ...change }, PROJECT))
  }
  assert.throws(() => h.readProjectProductIdentity({ ...product('UNBOUND'), productCode: 'GUESSED' }, PROJECT))
})
test('late product or error cannot replace the selected new project', async () => {
  let finishA, finishB
  const s = state(id => new Promise(resolve => { if (id === PROJECT) finishA = resolve; else finishB = resolve }))
  const old = s.load()
  s.context.formData.dccProjectCodeId = '8'
  s.context.selectedProjectCode.value = { id: '8', projectCode: 'OTHER-PROJECT-CODE' }
  const next = s.load()
  finishA(product())
  await old
  assert.equal(s.context.projectProductLoading.value, true)
  assert.equal(s.context.projectProduct.value, undefined)
  finishB(product('MDM_MASTER', '8'))
  await next
  assert.equal(s.context.projectProduct.value.source, 'MDM_MASTER')
  assert.equal(s.context.projectProduct.value.productMasterId, CATALOG)
  assert.equal(s.context.projectProductResolvedId.value, '8')
})
test('clearing or unmounting invalidates pending product without inventing an identity', async () => {
  for (const action of ['clear', 'unmount']) {
    let finish
    const s = state(() => new Promise(resolve => { finish = resolve }))
    const pending = s.load()
    if (action === 'clear') { s.context.formData.dccProjectCodeId = null; s.context.selectedProjectCode.value = undefined; await s.load() }
    else { s.context.projectProductMounted = false; s.context.projectProductRequestSequence++ }
    finish(product())
    await pending
    assert.equal(s.context.projectProduct.value, undefined)
    assert.equal(s.context.formData.productCode, '')
  }
})
test('UNBOUND remains empty and actual error does not fall back to project code', async () => {
  const unbound = state(async () => product('UNBOUND'))
  await unbound.load()
  assert.equal(unbound.context.formData.productCode, '')
  assert.equal(unbound.context.projectProduct.value.source, 'UNBOUND')
  const denied = state(async () => { throw Error('当前项目正式产品读取失败') })
  await denied.load()
  assert.match(denied.context.projectProductError.value, /正式产品读取失败/)
  assert.equal(denied.context.formData.productCode, '')
  assert.equal(denied.context.projectProductResolvedId.value, null)
})
test('late failed request does not overwrite a successful new project', async () => {
  let rejectOld
  const s = state(id => id === PROJECT ? new Promise((_resolve, reject) => { rejectOld = reject }) : Promise.resolve(product('MDM_MASTER', '8')))
  const old = s.load()
  s.context.formData.dccProjectCodeId = '8'
  await s.load()
  rejectOld(Error('旧项目无权限'))
  await old
  assert.equal(s.context.projectProductError.value, '')
  assert.equal(s.context.projectProduct.value.projectCodeId, '8')
  assert.equal(s.context.formData.productCode, 'APPROVED-PRODUCT-CODE')
})
test('existing public API wrapper sends the exact project string to its formal GET', async () => {
  const source = read('src/api/dcc/controlledFile/workflow.ts')
  const from = source.indexOf('export const previewControlledFileProjectProduct =')
  const to = source.indexOf('export const DCC_CONTROLLED_FILE_ACTIONS', from)
  const calls = [], context = { exports: {}, request: { get: async request => { calls.push(request); return product() } } }
  vm.runInNewContext(ts.transpileModule(source.slice(from, to), { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  await context.exports.previewControlledFileProjectProduct(PROJECT)
  assert.equal(calls.length, 1)
  assert.equal(calls[0].url, '/dcc/controlled-files/project-product')
  assert.equal(calls[0].params.projectCodeId, PROJECT)
  for (const id of [0, 9007199254740992, '01', '9223372036854775808'])
    assert.throws(() => context.exports.previewControlledFileProjectProduct(id))
  assert.equal(calls.length, 1)
})
test('actual product form shows server name and source provenance', () => {
  const { descriptor } = parse(page)
  const walk = node => {
    if (node.type === 1 && node.tag === 'el-form-item' && node.props.some(p => p.type === 6 && p.name === 'label' && p.value?.content === '产品编号')) return node
    for (const child of node.children || []) { const found = walk(child); if (found) return found }
  }
  const node = walk(descriptor.template.ast)
  const values = { formData: { productCode: 'APPROVED-PRODUCT-CODE' }, isProductRequiredForSelectedCategory: false,
    productCodeBindingHintClass: '', productCodeBindingHintText: '', selectedProjectCode: { projectName: '甲项目', projectCode: 'OTHER' },
    projectProductLoading: false, projectProductError: '', projectProduct: product(), formatProjectProductSource: () => '已批准项目产品目录' }
  const compiled = compileScript(parse(`<template>${node.loc.source}</template><script setup>const values=__values;${Object.keys(values).map(k => `const ${k}=values.${k}`).join('\n')}</script>`).descriptor, { id: 'actual-product-form', inlineTemplate: true })
  const context = { exports: {}, require: () => vue, __values: values }
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 } }).outputText, context)
  const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: () => ({}), insert: (c, p) => p.children.push(c), remove() {}, setText: (n, t) => { n.text=t }, setElementText: (n, t) => { n.text=t }, patchProp: (n, k, _o, v) => { n.props[k]=v }, parentNode: () => null, nextSibling: () => null })
  const app = renderer.createApp(context.exports.default)
  for (const tag of ['el-form-item', 'el-input']) app.component(tag, { setup: (_p, ctx) => () => vue.h(tag, ctx.attrs, ctx.slots.default?.()) })
  const root = { children: [] }; app.mount(root)
  const text = n => (n.text || '') + (n.children || []).map(text).join('')
  try { assert.match(text(root), /已批准真实产品/); assert.match(text(root), /已批准项目产品目录/); assert.match(text(root), new RegExp(CATALOG)) }
  finally { app.unmount() }
})
