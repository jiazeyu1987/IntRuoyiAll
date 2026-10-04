const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

const pagePath = path.resolve(__dirname, '../../src/views/dcc/controlled-file/upload/index.vue')
const descriptor = parse(fs.readFileSync(pagePath, 'utf8')).descriptor
const findReadiness = node => {
  if (node.type === 1 && node.props.some(prop => prop.type === 6 && prop.name === 'data-testid'
    && prop.value?.content === 'dcc-upload-route-readiness')) return node
  for (const child of node.children || []) {
    const found = findReadiness(child)
    if (found) return found
  }
}
const readiness = findReadiness(descriptor.template.ast)
assert.ok(readiness, 'the production upload readiness region must exist')
const source = `<template>${readiness.loc.source}</template><script setup>
const values = __values
const routeReadiness = values.routeReadiness
const routeReadinessError = values.routeReadinessError
const uploadApprovers = values.uploadApprovers
const uploadApproverError = values.uploadApproverError
const approvalUsersLoading = values.approvalUsersLoading
</script>`
const compiled = compileScript(parse(source).descriptor, { id: 'actual-upload-route-readiness', inlineTemplate: true })

function render(values) {
  const exports = {}
  vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: {
    module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022
  } }).outputText, { exports, require: id => {
    assert.equal(id, 'vue')
    return vue
  }, __values: { routeReadinessError: '', uploadApprovers: {}, uploadApproverError: '', approvalUsersLoading: false, ...values } })
  const renderer = vue.createRenderer({
    createElement: type => ({ type, children: [], props: {}, parent: null }),
    createText: text => ({ text, parent: null }), createComment: text => ({ comment: text, parent: null }),
    insert(node, parent, anchor) { node.parent = parent; const index = anchor ? parent.children.indexOf(anchor) : -1
      if (index < 0) parent.children.push(node); else parent.children.splice(index, 0, node) },
    remove(node) { const siblings = node.parent?.children; if (siblings) siblings.splice(siblings.indexOf(node), 1) },
    setText: (node, text) => { node.text = text }, setElementText: (node, text) => { node.text = text; node.children = [] },
    patchProp: (node, key, _old, value) => { node.props[key] = value },
    parentNode: node => node.parent, nextSibling: node => node.parent?.children[node.parent.children.indexOf(node) + 1] || null
  })
  const app = renderer.createApp(exports.default)
  app.component('el-alert', { props: ['title', 'type'], setup: (props, context) => () =>
    vue.h('section', { 'data-alert-type': props.type }, [vue.h('h2', props.title), context.slots.default?.()]) })
  const root = { children: [] }
  app.mount(root)
  const text = node => (node.text || '') + (node.children || []).map(text).join('')
  const elements = (node, type) => [...(node.type === type ? [node] : []), ...(node.children || []).flatMap(child => elements(child, type))]
  return { text: text(root), rows: elements(root, 'li').map(text), elements: type => elements(root, type), close: () => app.unmount() }
}

test('actual upload renders each formal stage and person when blocker reasons are identical', () => {
  const input = JSON.parse(JSON.stringify({ ready: false, nodes: [], blockers: [
    { reasonCode: 'APPROVER_SIGNATURE_IMAGE_INVALID', message: '审批人未配置有效签名图片', stageNo: 1,
      stageCode: 'MATRIX_REVIEW', stageName: '会签审核', userId: '9007199254740993', userName: '甲会签人' },
    { reasonCode: 'APPROVER_SIGNATURE_IMAGE_INVALID', message: '审批人未配置有效签名图片', stageNo: 3,
      stageCode: 'DOC_CONTROL_REVIEW', stageName: '文控审核', userId: '9223372036854775807', userName: '乙文控人' }
  ] }))
  const view = render({ routeReadiness: input })
  try {
    assert.equal(view.rows.length, 2)
    for (const [index, blocker] of input.blockers.entries()) {
      const row = view.rows[index]
      assert.ok(row.includes(blocker.stageName), `stage missing from blocker ${index}`)
      assert.ok(row.includes(String(blocker.stageNo)), `stage number missing from blocker ${index}`)
      assert.ok(row.includes(blocker.userName), `person missing from blocker ${index}`)
      assert.ok(row.includes(blocker.userId), `exact account ID missing from blocker ${index}`)
      assert.ok(row.includes(blocker.message))
    }
    assert.notEqual(view.rows[0], view.rows[1])
  } finally { view.close() }
})

test('missing facts are explicit and known IDs or stage codes remain actual facts', () => {
  const view = render({ routeReadiness: { ready: false, nodes: [], blockers: [
    { reasonCode: 'NO_ROUTE', message: '路线未配置', stageNo: null, stageName: null, stageCode: null, userId: null, userName: null },
    { reasonCode: 'NO_POST', message: '审批人未配置系统岗位', stageNo: 2, stageName: null,
      stageCode: 'MATRIX_APPROVAL', userId: '9007199254740997', userName: null }
  ] } })
  try {
    assert.equal(view.rows.length, 2)
    assert.match(view.rows[0], /阶段未记录/)
    assert.match(view.rows[0], /姓名未记录/)
    assert.match(view.rows[0], /账号ID：未记录/)
    assert.match(view.rows[1], /MATRIX_APPROVAL/)
    assert.match(view.rows[1], /9007199254740997/)
    assert.doesNotMatch(view.text, /admin|当前用户|默认审批人/)
  } finally { view.close() }
})

test('existing readiness error and successful branch do not render stale blocker rows', () => {
  for (const values of [
    { routeReadinessError: '正式路线读取失败', routeReadiness: { ready: false, nodes: [], blockers: [{ message: '旧blocker', reasonCode: 'OLD' }] } },
    { routeReadiness: { ready: true, nodes: [{ stageNo: 1 }], blockers: [{ message: '旧blocker', reasonCode: 'OLD' }] } }
  ]) {
    const view = render(values)
    try {
      assert.equal(view.rows.length, 0)
      assert.doesNotMatch(view.text, /旧blocker/)
      assert.ok(view.text.includes(values.routeReadinessError || '审批路线已就绪，共 1 个节点'))
    } finally { view.close() }
  }
})

test('blocker identities and reasons stay text content instead of creating supplied markup', () => {
  const view = render({ routeReadiness: { ready: false, nodes: [], blockers: [{
    reasonCode: 'IMAGE', message: '<img src=x>图片无效', stageNo: 1, stageName: '<b>真实阶段</b>',
    userId: '9223372036854775807', userName: '<script>姓名</script>'
  }] } })
  try {
    assert.ok(view.rows[0].includes('<b>真实阶段</b>'))
    assert.ok(view.rows[0].includes('<script>姓名</script>'))
    assert.ok(view.rows[0].includes('<img src=x>图片无效'))
    assert.equal(view.elements('script').length, 0)
    assert.equal(view.elements('img').length, 0)
  } finally { view.close() }
})
