const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const vue = require('vue')
const { parse, compileScript, compileTemplate, compileStyle } = require('vue/compiler-sfc')
const base = 'src/views/dcc/controlled-file/'
const read = file => fs.readFileSync(base + file, 'utf8')

test('all changed view templates, scripts and styles compile', () => {
  for (const file of ['detail/index.vue', 'detail/ReviewSectionNavigation.vue',
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
  const fields = ['项目', '存储文件夹', '文件分类', '产品编号', '产品名称', '编制人', '培训要求', '申请生效方式', '提交备注']
  for (const source of [overview, shared]) {
    const positions = fields.map(field => source.indexOf(`label="${field}"`))
    assert.ok(positions.every((position, index) => position >= 0 && (!index || position > positions[index - 1])))
  }
  assert.match(page, /reviewWindowWidth.value >= 1200 \? 3 : reviewWindowWidth.value >= 768 \? 2 : 1/)
  assert.doesNotMatch(overview, /label="文件名称"|label="版本"|label="文件编号"/)
})

test('preview entry and attachment status live in its toolbar with controlled hints and existing transforms', () => {
  const page = read('detail/index.vue'), viewer = read('view/index.vue'), attachments = read('detail/ControlledFileAttachments.vue')
  assert.match(page, /#document-entry>[\s\S]*?主文档[\s\S]*?<ControlledFileAttachments compact toolbar/)
  assert.match(page, /:preview-source="detailBodyPreviewState.source"/)
  assert.match(viewer, /<slot v-if="reviewToolbar" name="document-entry"/)
  assert.match(viewer, /reviewToolbar && resolvedWatermarkText/)
  for (const action of ['缩小', '放大', '旋转', '复原', '放大预览']) assert.ok(viewer.includes(action))
  assert.match(attachments, /v-else-if="file"[^>]*>无附件/)
  assert.match(attachments, /<ControlledFileAttachments :file="file" :body-read-context="bodyReadContext"/)
  assert.match(attachments, /listVisible.value = false/)
})

function navigation() {
  const clicks = [], tops = [0, 200, 500, 700], hooks = {}
  const container = { scrollHeight: 1000, clientHeight: 500, scrollTop: 0, parentElement: null }
  const props = { sections: tops.map((_, index) => ({ label: String(index), element: () => ({
    getBoundingClientRect: () => ({ top: tops[index] }), scrollIntoView: options => clicks.push({ index, options })
  }) })) }
  const env = { ref: vue.ref, defineProps: () => props, getComputedStyle: () => ({ overflowY: 'auto' }),
    window: { addEventListener: () => {}, removeEventListener: () => {} },
    onMounted: fn => { hooks.mount = fn }, onBeforeUnmount: fn => { hooks.dispose = fn },
    requestAnimationFrame: () => 1, cancelAnimationFrame: () => {} }
  const script = parse(read('detail/ReviewSectionNavigation.vue')).descriptor.scriptSetup.content.replace(/^import .*$/gm, '')
  vm.runInNewContext(ts.transpileModule(script + '\nglobalThis.state = { navigation, active, update, navigate }',
    { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, env)
  env.state.navigation.value = { parentElement: container, getBoundingClientRect: () => ({ bottom: 50 }) }
  return { state: env.state, clicks, tops, container }
}

test('navigation scrolls to the actual section; highlight follows scrolling and the bottom section', () => {
  const h = navigation()
  h.state.navigate(2)
  assert.equal(h.clicks[0].index, 2)
  assert.equal(h.clicks[0].options.behavior, 'smooth')
  h.state.update()
  assert.equal(h.state.active.value, 0)
  h.tops[1] = -100
  h.tops[2] = 70
  h.state.update()
  assert.equal(h.state.active.value, 2)
  h.container.scrollTop = 500
  h.state.update()
  assert.equal(h.state.active.value, 3)
  assert.match(read('detail/ReviewSectionNavigation.vue'), /border-bottom-color: #008577; color: #008577/)
})
