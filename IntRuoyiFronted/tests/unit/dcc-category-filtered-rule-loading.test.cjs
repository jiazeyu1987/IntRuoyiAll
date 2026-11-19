const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs'), vm = require('node:vm'), ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')
const { ref, computed } = require('vue')

function host(kind) {
  const source = fs.readFileSync(`src/views/dcc/controlled-file/categories/components/Category${kind}RulesTab.vue`, 'utf8')
  const { descriptor, errors } = parse(source)
  assert.deepEqual(errors, [])
  compileScript(descriptor, { id: `category-${kind}` })
  assert.match(descriptor.template.content, /:load-filter-rows="loadFilterRows"/)
  assert.match(descriptor.template.content, /@filtered-rows="handleFilteredRows"/)
  assert.match(descriptor.template.content, /:disabled="!row.rulesLoaded"/)
  const ast = ts.createSourceFile('tab.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const names = ['loadFilterRows', 'handleFilteredRows', 'ensureVisibleRuleRowsLoaded', 'openRuleDrawer', 'saveRules']
  const actual = ast.statements.filter(n => ts.isVariableStatement(n) && n.declarationList.declarations.some(d => names.includes(d.name.text))).map(n => n.getText(ast)).join('\n')
  const calls = [], writes = [], errorsSeen = []
  const oldPage = [{ categoryId: 1, rules: [] }]
  const context = {
    props: { active: true }, loaded: ref(true), visibleCategoryIds: ref([1]), filteredRows: ref(oldPage),
    loadedRuleCategoryIds: ref(new Set([1])), ruleMap: ref(new Map([[1, []]])),
    loading: ref(false), saving: ref(false), errorMessage: ref(''), drawerErrorMessage: ref(''),
    categories: ref([{ id: 31, name: '老吴全流程测试', trainingRequired: true }]),
    selectedCategory: ref(), drawerMode: ref('edit'), editingRules: ref([]), drawerVisible: ref(false),
    mapRulesToDrafts: rows => rows.map(r => ({ ...r })), createDepartmentRuleDraft: r => ({ ...r }),
    buildDepartmentRulePayload: rows => rows.map(r => ({ ...r })), resolveErrorMessage: e => e.message,
    message: { success() {}, warning: m => errorsSeen.push(m) },
    [`getCategory${kind}Rules`]: async id => { calls.push(id); if (context.fail) throw new Error('规则读取失败'); return [{ departmentId: 8, active: true }] },
    [`replaceCategory${kind}Rules`]: async (id, rules) => { writes.push({ id, rules }); return rules }
  }
  vm.createContext(context)
  vm.runInContext(ts.transpileModule(actual + '\nglobalThis.handlers = { loadFilterRows, handleFilteredRows, ensureVisibleRuleRowsLoaded, openRuleDrawer, saveRules }', { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText, context)
  return { c: context, h: context.handlers, calls, writes }
}

for (const kind of ['Training', 'Distribution']) {
  test(`${kind}: filtered category outside original page loads and can edit and save its own rules`, async () => {
    const { c, h, calls, writes } = host(kind)
    await h.ensureVisibleRuleRowsLoaded()
    assert.deepEqual(calls, [])
    const displayed = { categoryId: 31, rules: [] }
    c.filteredRows.value = [displayed]
    assert.equal((await h.loadFilterRows())[0].categoryId, 31)
    await h.handleFilteredRows([displayed])
    assert.deepEqual(calls, [31])
    assert.equal(c.loadedRuleCategoryIds.value.has(31), true)
    assert.equal(c.ruleMap.value.get(31)[0].departmentId, 8)
    h.openRuleDrawer({ ...displayed, rules: c.ruleMap.value.get(31) }, 'edit')
    assert.equal(c.drawerVisible.value, true)
    assert.equal(c.selectedCategory.value.id, 31)
    c.editingRules.value[0].departmentId = 9
    await h.saveRules()
    assert.equal(writes[0].id, 31)
    assert.equal(writes[0].rules[0].departmentId, 9)
    assert.equal(c.ruleMap.value.get(31)[0].departmentId, 9)
    await h.handleFilteredRows([displayed])
    assert.deepEqual(calls, [31])
    await h.handleFilteredRows([{ categoryId: 41 }])
    assert.deepEqual(calls, [31, 41])
  })

  test(`${kind}: failed loading stays visibly failed and retries without enabling edit on empty data`, async () => {
    const { c, h, calls } = host(kind)
    c.fail = true
    await h.handleFilteredRows([{ categoryId: 31 }])
    assert.equal(c.loading.value, false)
    assert.equal(c.errorMessage.value, '规则读取失败')
    assert.equal(c.loadedRuleCategoryIds.value.has(31), false)
    assert.equal(c.ruleMap.value.has(31), false)
    c.fail = false
    await h.handleFilteredRows([{ categoryId: 31 }])
    assert.equal(c.loadedRuleCategoryIds.value.has(31), true)
    assert.equal(c.errorMessage.value, '')
    assert.deepEqual(calls, [31, 31])
    c.props.active = false
    await h.handleFilteredRows([{ categoryId: 51 }])
    assert.deepEqual(calls, [31, 31])
  })
}
