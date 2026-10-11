const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')
const compiler = require('vue/compiler-sfc')

const source = fs.readFileSync(path.join(__dirname, 'BatchRecordCellRulesConfirmDialog.vue'), 'utf8')
const rulesSource = fs.readFileSync(path.join(__dirname, '../batchrecord-shared/batchRecordTemplateRules.ts'), 'utf8')

function declarations(text, names, dependencies) {
  const script = text.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)?.[1] || text
  const ast = ts.createSourceFile('source.ts', script, ts.ScriptTarget.Latest, true)
  const statements = ast.statements.filter((statement) => ts.isVariableStatement(statement) &&
    statement.declarationList.declarations.every((declaration) => names.includes(declaration.name.getText(ast))))
  assert.equal(statements.length, names.length, 'Actual production declarations must exist')
  const code = ts.transpileModule(statements.map((statement) => statement.getText(ast).replace(/^export\s+/, '')).join('\n') +
    `\nreturn {${names.join(',')}}`, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText
  return new Function(...Object.keys(dependencies), code)(...Object.values(dependencies))
}

const rules = declarations(rulesSource, ['normalizeCellRule', 'cleanedRuleConstraints'], {
  cleanedAttachmentRule: (value) => value,
  cellRuleDefaultComponentMap: { NUMBER: 'input-number', STRING: 'input-text', SIGNATURE: 'signature' }
})

function unitInput(rule) {
  const parsed = compiler.parse(source)
  assert.deepEqual(parsed.errors, [])
  const ast = parsed.descriptor.template.ast
  const matches = []
  function visit(node) {
    if (node.type === 1 && node.tag === 'el-form-item' && node.props.some((prop) =>
      prop.type === 6 && prop.name === 'label' && prop.value?.content === '计量单位')) matches.push(node)
    for (const child of node.children || []) visit(child)
  }
  visit(ast)
  assert.equal(matches.length, 1, 'NUMBER field must expose exactly one real unit editor')
  const compiled = compiler.compileTemplate({
    source: matches[0].loc.source, id: 'unit-field', filename: 'unit-field.vue',
    compilerOptions: { mode: 'function', prefixIdentifiers: true }
  })
  assert.deepEqual(compiled.errors, [])
  const render = new Function('Vue', compiled.code)({ ...vue, resolveComponent: (name) => ({ name }) })
  const vnode = render({ selectedRule: rule }, [])
  return vnode.type === vue.Comment ? null : vnode.children.default()[0]
}

test('actual NUMBER unit input updates the selected existing field and leaves its identity unchanged', () => {
  const rule = { rowIndex: 8, columnIndex: 7, valueType: 'NUMBER', unit: '' }
  const input = unitInput(rule)
  assert.equal(input.type.name, 'el-input')
  assert.equal(input.props['aria-label'], '计量单位')
  assert.equal(input.props.maxlength, '32')
  input.props['onUpdate:modelValue']('pcs')
  assert.deepEqual(rule, { rowIndex: 8, columnIndex: 7, valueType: 'NUMBER', unit: 'pcs' })
})

test('STRING and SIGNATURE retain their fields and have no new unit input', () => {
  for (const valueType of ['STRING', 'SIGNATURE']) {
    const rule = { rowIndex: 8, columnIndex: 8, valueType, unit: undefined }
    assert.equal(unitInput(rule), null)
    assert.equal(rule.unit, undefined)
  }
})

test('actual save handler sends edited unit and actual response handler restores it', async () => {
  const rule = { rowIndex: 8, columnIndex: 7, valueType: 'NUMBER', label: '不合格数量/pcs', unit: '' }
  unitInput(rule).props['onUpdate:modelValue']('pcs')
  const sent = []
  const ruleRows = vue.ref([rule])
  const assistRows = vue.ref([])
  const common = {
    ...rules, ruleRows, assistRows, assistGridRowCount: vue.ref(3), assistGridColumnCount: vue.ref(3),
    normalizeAssistRows: (value) => value, normalizeAssistGridSizeValue: (value) => value,
    syncAssistAssignmentsWithRows: () => {}, summary: {}, sheetLayout: vue.ref(null), sheetLayoutError: vue.ref(''),
    parseSheetLayout: () => ({ rows: {} }), resolveErrorMessage: (error) => error.message,
    ensureSelectedRuleStillExists: () => {}, syncAssistGridStateWithRows: () => {},
    ruleIdentity: (value) => `${value.rowIndex}:${value.columnIndex}`
  }
  const response = declarations(source, ['applyCellRulesResponse'], common).applyCellRulesResponse
  const dependencies = {
    ...common, applyCellRulesResponse: response, reportId: vue.ref('9b762363dd614a4f92a88705b2cae41a'),
    saving: vue.ref(false), errorMessage: vue.ref(''), validateRuleRowsBeforeSave: () => {},
    normalizedAssistRowsForSave: () => [], normalizedWholeFormFillRuleForSave: () => null,
    buildCellStateSignature: () => 'edited', savedCellStateSignature: vue.ref('before'),
    buildSignatureMarkersForSave: () => [], markEditableStateClean: () => {}, emit: () => {},
    message: { success: () => {}, error: (value) => { throw new Error(value) } },
    BatchRecordReportApi: { saveCellRules: async (payload) => { sent.push(payload); return { rules: payload.rules } } }
  }
  const handlers = declarations(source, ['cloneRecord', 'toManualReviewedRule', 'confirmAllRules'], dependencies)
  await handlers.confirmAllRules()
  assert.equal(sent.length, 1)
  assert.equal(sent[0].rules[0].unit, 'pcs')
  assert.equal(sent[0].rules[0].source, 'MANUAL')
  assert.equal(sent[0].rules[0].reviewed, true)
  assert.deepEqual([sent[0].rules[0].rowIndex, sent[0].rules[0].columnIndex], [8, 7])
  assert.equal(ruleRows.value[0].unit, 'pcs')
  unitInput(ruleRows.value[0]).props['onUpdate:modelValue']('')
  await handlers.confirmAllRules()
  assert.equal(sent[1].rules[0].unit, '')
  assert.equal(ruleRows.value[0].unit, '')
})

function saveHarness({ rows = [], normalizedRows = [], fieldRules, saveError } = {}) {
  const sent = []
  const permissions = []
  const notices = []
  const ruleRows = vue.ref(fieldRules || [{ rowIndex: 8, columnIndex: 7, valueType: 'NUMBER',
    componentFlag: 'input-number', label: '不合格数量/pcs', unit: 'pcs' }])
  const assistRows = vue.ref(rows)
  const common = {
    ...rules, ruleRows, assistRows, assistGridRowCount: vue.ref(12), assistGridColumnCount: vue.ref(9),
    normalizeAssistRows: (value) => value, normalizeAssistGridSizeValue: (value) => value,
    syncAssistAssignmentsWithRows: () => {}, summary: {}, sheetLayout: vue.ref(null), sheetLayoutError: vue.ref(''),
    parseSheetLayout: () => ({ rows: {} }), resolveErrorMessage: (error) => error.message,
    ensureSelectedRuleStillExists: () => {}, syncAssistGridStateWithRows: () => {},
    ruleIdentity: (value) => `${value.rowIndex}:${value.columnIndex}`
  }
  const response = declarations(source, ['applyCellRulesResponse'], common).applyCellRulesResponse
  const dependencies = {
    ...common, applyCellRulesResponse: response, reportId: vue.ref('9b762363dd614a4f92a88705b2cae41a'),
    saving: vue.ref(false), errorMessage: vue.ref(''), validateRuleRowsBeforeSave: () => {},
    normalizedAssistRowsForSave: () => normalizedRows, normalizedWholeFormFillRuleForSave: () => null,
    buildAssistResponsibilitySignature: () => 'unchanged', savedAssistResponsibilitySignature: vue.ref('unchanged'),
    buildCellStateSignature: () => 'edited', savedCellStateSignature: vue.ref('before'),
    buildSignatureMarkersForSave: () => [], markEditableStateClean: () => {}, emit: () => {},
    message: { success: (value) => notices.push(value), error: (value) => notices.push(value) },
    EdhrProcessFormPermissionRuleApi: { saveByReport: async (payload) => permissions.push(payload) },
    BatchRecordReportApi: { saveCellRules: async (payload) => {
      const wire = JSON.parse(JSON.stringify(payload))
      sent.push(wire)
      if (saveError) throw new Error(saveError)
      // The existing backend treats absent/null rows as no assist configuration;
      // an explicit empty list requires coverage of all current fillable cells.
      if (Array.isArray(wire.assistRows) && wire.assistRows.length === 0 && wire.rules.length > 0) {
        throw new Error('missing assist row coverage [8:7]')
      }
      return { reportId: payload.reportId, rules: wire.rules, assistRows: wire.assistRows || [],
        assistGridRowCount: wire.assistGridRowCount, assistGridColumnCount: wire.assistGridColumnCount }
    } }
  }
  return { sent, permissions, notices, dependencies, common,
    handlers: declarations(source, ['cloneRecord', 'toManualReviewedRule', 'confirmAllRules'], dependencies) }
}

test('actual source-form save omits absent assist configuration and avoids the explicit empty coverage branch', async () => {
  const h = saveHarness()
  await h.handlers.confirmAllRules()
  assert.equal(h.sent.length, 1)
  assert.equal(h.dependencies.errorMessage.value, '')
  for (const key of ['assistRows', 'assistGridRowCount', 'assistGridColumnCount']) {
    assert.equal(Object.hasOwn(h.sent[0], key), false, `No assist configuration must omit ${key}`)
  }
  assert.equal(h.sent[0].rules[0].unit, 'pcs')
  assert.deepEqual(h.permissions, [], 'A field edit must not create responsibility grants')
  assert.deepEqual(h.notices, ['填写配置已保存'])
})

test('actual save preserves pre-existing nonempty assist mappings and sizes without changing grants', async () => {
  const rows = [{ rowKey: 'AR_001', description: '既有辅助映射', sort: 1,
    fields: [{ rowIndex: 8, columnIndex: 7 }] }]
  const h = saveHarness({ rows })
  await h.handlers.confirmAllRules()
  assert.deepEqual(h.sent[0].assistRows, rows)
  assert.equal(h.sent[0].assistGridRowCount, 12)
  assert.equal(h.sent[0].assistGridColumnCount, 9)
  assert.deepEqual(h.permissions, [])
  assert.equal(h.dependencies.errorMessage.value, '')
})

test('actual save preserves normalized active assist mappings without rewriting unchanged responsibility', async () => {
  const rows = [{ rowKey: 'ASSIST_GRID_ROLE_341_r0_c0', description: '既有辅助映射', sort: 1,
    fields: [{ rowIndex: 8, columnIndex: 7 }] }]
  const h = saveHarness({ rows, normalizedRows: rows })
  await h.handlers.confirmAllRules()
  assert.deepEqual(h.sent[0].assistRows, rows)
  assert.equal(h.sent[0].assistGridRowCount, 12)
  assert.equal(h.sent[0].assistGridColumnCount, 9)
  assert.deepEqual(h.permissions, [])
})

test('existing explicit removal of all configured cells retains backend null-row deletion semantics', async () => {
  const rows = [{ rowKey: 'AR_001', description: '既有辅助映射', sort: 1,
    fields: [{ rowIndex: 8, columnIndex: 7 }] }]
  const h = saveHarness({ rows })
  const { removeRuleCellsForKeys } = declarations(source, ['removeRuleCellsForKeys'], {
    ...h.common, orderAssistGridRows: (value) => value,
    cellIdentity: (row, column) => `${row}:${column}`,
    ensureSelectedAssistSubjectStillExists: () => {}, ensureSelectedAssistGridCellStillExists: () => {}
  })
  removeRuleCellsForKeys(['8:7'])
  assert.deepEqual(h.common.ruleRows.value, [])
  assert.deepEqual(h.common.assistRows.value, [])
  await h.handlers.confirmAllRules()
  assert.deepEqual(h.sent[0].rules, [])
  assert.equal(h.sent[0].assistRows, undefined)
  assert.equal(h.sent[0].assistGridRowCount, undefined)
  assert.equal(h.sent[0].assistGridColumnCount, undefined)
  assert.deepEqual(h.permissions, [])
})

test('backend save rejection remains visible and never reports success or writes permission', async () => {
  const h = saveHarness({ saveError: '正式字段保存拒绝' })
  await h.handlers.confirmAllRules()
  assert.equal(h.dependencies.errorMessage.value, '正式字段保存拒绝')
  assert.deepEqual(h.notices, ['正式字段保存拒绝'])
  assert.deepEqual(h.permissions, [])
  assert.equal(h.dependencies.saving.value, false)
})

test('active assist responsibility still rejects an invalid empty mapping before any request', async () => {
  const h = saveHarness()
  const { normalizedAssistRowsForSave } = declarations(source, ['normalizedAssistRowsForSave'], {
    ...h.common, hasAssistFillConfiguration: vue.ref(true), orderAssistGridRows: (value) => value
  })
  const handlers = declarations(source, ['cloneRecord', 'toManualReviewedRule', 'confirmAllRules'], {
    ...h.dependencies, normalizedAssistRowsForSave
  })
  await handlers.confirmAllRules()
  assert.deepEqual(h.sent, [])
  assert.deepEqual(h.permissions, [])
  assert.equal(h.dependencies.errorMessage.value, '辅助填写责任缺少映射，请至少保留一个辅助表格映射。')
})
