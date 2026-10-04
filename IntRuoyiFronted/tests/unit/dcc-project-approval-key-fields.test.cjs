const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')
const { descriptor } = parse(fs.readFileSync('src/views/approval-center/index.vue', 'utf8'))
const ast = ts.createSourceFile('approval.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const names = [
  'EMPTY_APPROVAL_DISPLAY', 'EMPTY_CONTEXT_VALUE_PATTERN', 'ENGLISH_LETTER_PATTERN', 'CHINESE_CHARACTER_PATTERN', 'BUSINESS_CONTEXT_CODE_PATTERN',
  'APPROVAL_BUSINESS_TITLE_LABELS', 'APPROVAL_STATUS_LABELS', 'APPROVAL_NODE_LABELS', 'APPROVAL_BUSINESS_KEY_PREFIX_LABELS',
  'normalizeApprovalDisplayText', 'isApprovalDisplayValueVisible', 'containsEnglishLetters', 'containsChineseCharacters', 'isBusinessContextCode',
  'resolveMappedApprovalText', 'resolveBusinessIdentifierValueLabel', 'resolveNodeNameLabel', 'resolveBusinessContextValueLabel',
  'normalizeDccContextTag', 'findDccContextTagValue', 'resolveDccKeyFields', 'resolveVisibleDccKeyFields'
]
const source = names.map(name => {
  const st = ast.statements.find(node => ts.isVariableStatement(node) && node.declarationList.declarations.some(d => ts.isIdentifier(d.name) && d.name.text === name))
  assert.ok(st, 'actual source declaration ' + name)
  return st.getText(ast)
}).join('\n')
const c = { exports: {}, String, Object, Array }
vm.runInNewContext(ts.transpileModule(source + '\nexports.resolve=resolveVisibleDccKeyFields;', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, c)
const resolve = c.exports.resolve
const native = sourceTaskType => ({ moduleCode: 'DCC', sourceTaskType, businessCode: 'PROJECT-A01', businessKey: '9007199254740993', currentNodeName: sourceTaskType.endsWith('REVIEW') ? '项目产品审核' : '项目产品批准', businessContextTags: ['版本：A/1', '文件类型：质量体系'] })
const file = { moduleCode: 'DCC', sourceTaskType: 'DCC_CONTROLLED_FILE_TASK', businessCode: 'SOP-001', currentNodeName: '文控审核', businessContextTags: ['版本：A/1', '文件类型：质量体系'] }
test('actual key-field resolver uses formal project code and node for both native project source types', () => {
  for (const type of ['DCC_PROJECT_PRODUCT_REVIEW', 'DCC_PROJECT_PRODUCT_APPROVAL']) {
    assert.deepEqual(JSON.parse(JSON.stringify(resolve(native(type)))), [{ label: '项目代码', value: 'PROJECT-A01' }, { label: '当前审批节点', value: native(type).currentNodeName }])
  }
})
test('actual original controlled-file resolver retains its four exact formal fields', () => {
  assert.deepEqual(JSON.parse(JSON.stringify(resolve(file))), [{ label: '文件编号', value: 'SOP-001' }, { label: '版本', value: 'A/1' }, { label: '文件类型', value: '质量体系' }, { label: '当前审批节点', value: '文控审核' }])
})
test('a missing actual project code never uses native request ID or file tags as project fields', () => {
  const result = resolve({ ...native('DCC_PROJECT_PRODUCT_REVIEW'), businessCode: undefined })
  assert.deepEqual(JSON.parse(JSON.stringify(result)), [{ label: '当前审批节点', value: '项目产品审核' }])
})
test('actual key-fields template renders native project words and preserves controlled-file words', () => {
  const find = node => {
    if (node.type === 1 && node.props.some(p => p.type === 6 && p.name === 'data-testid' && p.value?.content === 'approval-center-dcc-key-fields')) return node
    for (const child of node.children || []) { const found = find(child); if (found) return found }
  }
  const node = find(descriptor.template.ast); assert.ok(node)
  const compiled = compileScript(parse('<template>' + node.loc.source + '</template><script setup>const row=__host.row;const resolveVisibleDccKeyFields=__host.resolve;</script>').descriptor, { id: 'project-native-key-fields', inlineTemplate: true })
  for (const row of [native('DCC_PROJECT_PRODUCT_REVIEW'), native('DCC_PROJECT_PRODUCT_APPROVAL'), file]) {
    const host = { exports: {}, require: () => vue, __host: { row, resolve } }
    vm.runInNewContext(ts.transpileModule(compiled.content, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText, host)
    const renderer = vue.createRenderer({ createElement: type => ({ type, children: [], props: {} }), createText: text => ({ text }), createComment: () => ({}), insert: (n, p) => p.children.push(n), remove() {}, setText: (n, t) => { n.text = t }, setElementText: (n, t) => { n.text = t }, patchProp: (n, k, _old, value) => { n.props[k] = value }, parentNode: () => null, nextSibling: () => null })
    const app = renderer.createApp(host.exports.default), root = { children: [] }; app.mount(root)
    const text = n => (n.text || '') + (n.children || []).map(text).join('')
    try {
      const shown = text(root)
      if (row === file) {
        for (const word of ['文件编号：SOP-001', '版本：A/1', '文件类型：质量体系', '当前审批节点：文控审核']) assert.ok(shown.includes(word), word)
      } else {
        assert.ok(shown.includes('项目代码：PROJECT-A01'))
        assert.ok(shown.includes('当前审批节点：' + row.currentNodeName))
        for (const word of ['文件编号', '版本', '文件类型']) assert.equal(shown.includes(word), false, word)
      }
    } finally { app.unmount() }
  }
})
