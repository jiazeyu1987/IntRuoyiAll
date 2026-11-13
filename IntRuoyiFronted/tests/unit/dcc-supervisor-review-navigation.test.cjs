const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate, compileStyle } = require('vue/compiler-sfc')
const base = 'src/views/dcc/controlled-file/'
const read = file => fs.readFileSync(base + file, 'utf8')

test('all changed view templates, scripts and styles compile', () => {
  for (const file of ['detail/index.vue', 'detail/ReviewSectionNavigation.vue', 'relations/DccFileSelector.vue',
    'detail/DetailSignoffAssignment.vue', 'workflow/SignoffAssignmentPanel.vue',
    'detail/ControlledFileAttachments.vue', 'view/index.vue', 'shared/ControlledFileBasicInfoPanel.vue']) {
    const { descriptor, errors } = parse(read(file), { filename: file })
    assert.deepEqual(errors, [], file)
    const script = compileScript(descriptor, { id: file })
    assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: file,
      compilerOptions: { bindingMetadata: script.bindings } }).errors, [], file)
    descriptor.styles.forEach(style => assert.deepEqual(compileStyle({ source: style.content,
      filename: file, id: file, scoped: true }).errors, [], file))
  }
})

test('overview is three desktop columns and shared business fields retain the same names and order', () => {
  const page = read('detail/index.vue'), shared = read('shared/ControlledFileBasicInfoPanel.vue')
  const overview = page.slice(page.indexOf('title="文件概览"'), page.indexOf('ref="reviewBodyRef"'))
  const fields = ['项目', '存储文件夹', '文件分类', '产品编号', '产品名称', '编制人', '提交备注', '申请生效方式', '培训要求']
  const positions = fields.map(field => overview.indexOf(`label="${field}"`))
  assert.ok(positions.every((position, index) => position >= 0 && (!index || position > positions[index - 1])))
  for (const field of fields) assert.ok(shared.includes(`label="${field}"`))
  assert.match(overview, /:column="3"/)
  assert.doesNotMatch(overview, /label="文件名称"|label="版本"|label="文件编号"/)
})

test('preview entry and attachment status live in its toolbar with controlled hints and existing transforms', () => {
  const page = read('detail/index.vue'), preview = read('detail/ReviewDocumentPreview.vue'), viewer = read('view/index.vue'), attachments = read('detail/ControlledFileAttachments.vue')
  assert.match(preview, /#document-entry>[\s\S]*?主文档[\s\S]*?<ControlledFileAttachments[^>]*compact toolbar review/)
  assert.match(page, /:source="detailBodyPreviewState.source"/)
  assert.match(preview, /:preview-source="source"/)
  assert.match(viewer, /<slot v-if="reviewToolbar" name="document-entry"/)
  assert.match(viewer, /reviewToolbar && resolvedWatermarkText/)
  for (const action of ['缩小', '放大', '旋转', '复原', '放大预览']) assert.ok(viewer.includes(action))
  assert.match(attachments, /v-else-if="file"[^>]*>无附件/)
  assert.match(attachments, /<ControlledFileAttachments :file="file" :body-read-context="bodyReadContext"/)
  assert.match(attachments, /listVisible.value = false/)
})

function navigation(useWindow = false) {
  const clicks = [], tops = [0, 200, 500, 700, 900], hooks = {}
  const container = { scrollHeight: 1500, clientHeight: 500, scrollTop: 0, clientTop: 2, parentElement: null, getBoundingClientRect: () => ({ top: 100 }), scrollTo: options => clicks.push({ options }) }
  const props = { sections: tops.map((_, index) => ({ label: String(index), element: () => ({
    getBoundingClientRect: () => ({ top: tops[index] }), scrollIntoView: options => clicks.push({ index, options })
  }) })) }
  const env = { ref: vue.shallowRef, defineProps: () => props, getComputedStyle: () => ({ overflowY: 'auto', top: '0px' }),
    document: { body: {}, documentElement: { scrollHeight: 1500 } },
    window: { scrollY: 0, innerHeight: 500, scrollTo: options => clicks.push({ options }), addEventListener: () => {}, removeEventListener: () => {} },
    onMounted: fn => { hooks.mount = fn }, onBeforeUnmount: fn => { hooks.dispose = fn },
    onActivated: fn => { hooks.activate = fn }, watchPostEffect: fn => { hooks.observe = fn },
    ResizeObserver: class { observe() {} disconnect() {} },
    requestAnimationFrame: () => 1, cancelAnimationFrame: () => {} }
  const script = parse(read('detail/ReviewSectionNavigation.vue')).descriptor.scriptSetup.content.replace(/^import .*$/gm, '')
  vm.runInNewContext(ts.transpileModule(script + '\nglobalThis.state = { navigation, active, update, navigate }',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  env.state.navigation.value = { parentElement: useWindow ? env.document.body : container, getBoundingClientRect: () => ({ height: 50 }) }
  return { state: env.state, clicks, tops, container, env, props, hooks }
}

test('navigation scrolls to the actual section; highlight follows scrolling and the bottom section', () => {
  const h = navigation()
  h.state.navigate(2)
  assert.equal(h.clicks[0].options.top, 336)
  assert.equal(h.clicks[0].options.behavior, 'smooth')
  h.state.update()
  assert.equal(h.state.active.value, 0)
  h.tops[1] = -100
  h.tops[2] = 160
  h.state.update()
  assert.equal(h.state.active.value, 2)
  h.container.scrollTop = 1000
  h.state.update()
  assert.equal(h.state.active.value, 4)
  assert.match(read('detail/ReviewSectionNavigation.vue'), /border-bottom-color: #008577; color: #008577/)
})

test('both review stages have the same five live anchors, including an empty relations section', () => {
  const page = parse(read('detail/index.vue')).descriptor
  const ast = ts.createSourceFile('page.ts', page.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = ['reviewOverviewRef', 'reviewBodyRef', 'reviewTaskRef', 'reviewApplicationRef', 'reviewRelationsRef', 'reviewSections']
  const code = ast.statements.filter(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => names.includes(d.name.getText(ast)))).map(s => s.getText(ast)).join('\n')
  const env = { ref: vue.ref }
  vm.runInNewContext(ts.transpileModule(code + '\nglobalThis.sections = reviewSections; globalThis.refs = [reviewOverviewRef, reviewBodyRef, reviewTaskRef, reviewApplicationRef, reviewRelationsRef]', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  assert.deepEqual(Array.from(env.sections, section => section.label), ['文件概览', '正文与附件', '当前任务', '申请与审批', '关联文件'])
  env.refs.forEach((ref, index) => { ref.value = { anchor: index }; assert.equal(env.sections[index].element().anchor, index) })
  assert.match(page.template.content, /<ReviewSectionNavigation v-if="isApprovalUploadHandlingPage" :sections="reviewSections"/)
  assert.match(page.template.content, /ref="reviewRelationsRef"[\s\S]*?<DetailRelationsPanel[\s\S]*?<ContentWrap v-else-if="isApprovalUploadHandlingPage"/)
  assert.doesNotMatch(page.template.content, /填写指派信息/)
  assert.match(page.template.content, /ref="reviewRelationsRef"[^>]*'review-section--last': isApprovalUploadHandlingPage/)
  assert.match(page.styles[0].content, /\.review-section--last\s*\{\s*min-height: calc\(100vh - 240px\)/)
  const h = navigation()
  for (let index = 0; index < 5; index++) { h.state.navigate(index); assert.equal(h.clicks.at(-1).options.top, h.tops[index] - 164); assert.equal(h.state.active.value, index) }
  h.tops[2] = 180; h.tops[3] = 210; h.state.update(); assert.equal(h.state.active.value, 0)
})


test('window scrolling uses measured height and selects the final available section at the bottom', () => {
  const h = navigation(true)
  h.env.window.scrollY = 200
  h.state.navigate(3)
  assert.equal(h.clicks.at(-1).options.top, 838)
  h.tops[1] = -20; h.tops[2] = 60
  h.state.update()
  assert.equal(h.state.active.value, 2)
  h.props.sections[4].element = () => undefined
  h.env.window.scrollY = 1000
  h.state.update()
  assert.equal(h.state.active.value, 3)
})

test('resize observation tracks async sections and releases observations on invalidation', () => {
  const h = navigation()
  const observed = []
  h.env.ResizeObserver = class {
    observe(element) { observed.push(element) }
    disconnect() { observed.length = 0 }
  }
  let cleanup
  h.hooks.observe(fn => { cleanup = fn })
  assert.equal(observed.length, 7)
  cleanup()
  assert.equal(observed.length, 0)
  h.state.navigation.value.getBoundingClientRect = () => ({ height: 80 })
  h.state.navigate(2)
  assert.equal(h.clicks.at(-1).options.top, 306)
})
