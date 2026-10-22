const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const { ref, reactive, computed } = require('vue')

const viewRoot = path.resolve(__dirname, '../../src/views/mes/pro')
const source = fs.readFileSync(
  path.join(viewRoot, 'batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue'),
  'utf8'
)
const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const parsed = ts.createSourceFile('dialog.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
// Execute the actual component state, loading, validation and save functions.
// Only external APIs, messages and lifecycle macros are replaced by test doubles.
const executable = parsed.statements
  .filter((statement) => !ts.isImportDeclaration(statement))
  .map((statement) => statement.getText(parsed))
  .join('\n')
const compile = (code) => ts.transpileModule(code, {
  compilerOptions: { target: ts.ScriptTarget.ES2020, module: ts.ModuleKind.CommonJS }
}).outputText
const templateModule = { exports: {} }
new Function('exports', compile(fs.readFileSync(
  path.join(viewRoot, 'batchrecord-shared/batchRecordTemplateRules.ts'), 'utf8'
)))(templateModule.exports)
const clone = (value) => JSON.parse(JSON.stringify(value))
const originalRule = {
  candidateSourceType: 'ROLE', candidateSourceIds: [910415], completionPolicy: 'ALL',
  dueMinutes: 30, enabled: false, remark: '正式填写责任'
}
const requestedRule = { ...originalRule, candidateSourceType: 'USERS', candidateSourceIds: [672, 1284] }
const rules = Array.from({ length: 125 }, (_, rowIndex) => ({
  rowIndex, columnIndex: 0, valueType: rowIndex === 124 ? 'SIGNATURE' : 'STRING',
  label: `字段${rowIndex}`, reviewed: true, source: 'MANUAL', required: false, constraints: {}
}))
const layout = rules.map((rule, index) => ({
  rowKey: `ASSIST_GRID_U795_R${Math.floor(index / 5)}_C${index % 5}`,
  description: rule.label, sort: index + 1,
  fields: [{ rowIndex: rule.rowIndex, columnIndex: rule.columnIndex }]
}))
const signatureMarker = {
  rowIndex: 124, columnIndex: 0, enabled: true, actionType: 'FORM_REVIEW',
  label: '字段124', signatureCellKey: 'review-signature', displayFormat: 'ACTOR_SIGNED_AT',
  reviewSourceType: 'ROLE', reviewSourceIds: [910414], reviewSourceName: '复核角色'
}
const sheetLayoutJson = JSON.stringify({ rows: Object.fromEntries(rules.map((rule) => [
  rule.rowIndex, { cells: { 0: {
    text: rule.label, ...(rule.valueType === 'SIGNATURE' ? { edhrSignature: signatureMarker } : {})
  } } }
])) })

async function createDialog({ auxiliary = false, wholeRule = originalRule, rows = layout, cellRules = rules } = {}) {
  let data = {
    rules: clone(cellRules), assistRows: clone(rows), assistGridRowCount: 25,
    assistGridColumnCount: 5, sheetLayoutJson, suggestions: [], unreviewedFillableCellCount: 0
  }
  let permission = {
    fillRule: clone(wholeRule),
    fillAssignments: auxiliary ? rows.map((row) => ({
      scopeKey: row.rowKey, candidateSourceType: 'USERS', candidateSourceIds: [795],
      completionPolicy: 'ANY_ONE', enabled: true, remark: row.description
    })) : []
  }
  const calls = []
  const events = []
  const messages = []
  const fail = {}
  const apiCall = (name, body) => {
    calls.push({ name, ...(body ? { body: clone(body) } : {}) })
    if (fail[name]) throw new Error(fail[name])
  }
  const dependencies = {
    ref, reactive, computed, watch: () => {},
    defineOptions: () => {},
    defineProps: () => ({ modelValue: true, report: { reportId: 'report-130' } }),
    defineEmits: () => (...event) => events.push(event),
    useMessage: () => ({
      success: (text) => messages.push({ success: text }),
      error: (text) => messages.push({ error: text }),
      warning: (text) => messages.push({ warning: text })
    }),
    ElMessageBox: { confirm: async () => {} },
    ...templateModule.exports,
    getSimpleUserList: async () => [
      { id: 672, nickname: '同名员工', username: 'a' },
      { id: 1284, nickname: '同名员工', username: 'b' },
      { id: 795, nickname: '原员工', username: 'c' }
    ],
    getSimpleRoleList: async () => [{ id: 910415, name: '原填写角色' }],
    BatchRecordReportApi: {
      getCellRules: async () => { apiCall('readCells'); return clone(data) },
      saveCellRules: async (body) => {
        apiCall('cells', body)
        data = { ...data, ...clone(body) }
        return clone(data)
      }
    },
    EdhrProcessFormPermissionRuleApi: {
      getByReport: async () => clone(permission),
      saveByReport: async (body) => {
        apiCall('permission', body)
        permission = {
          fillRule: clone(body.fillRule || null),
          fillAssignments: clone(body.fillAssignments || [])
        }
        return clone(permission)
      }
    }
  }
  const exposed = [
    'loadCellRules', 'confirmAllRules', 'activeConfigMode', 'hasAssistFillConfiguration',
    'wholeFormFillRule', 'ruleRows', 'assistRows', 'assistGridRowCount', 'assistGridColumnCount',
    'hasUnsavedChanges', 'savedStateSignature', 'savedWholeFormFillRuleSignature',
    'loadedFillAssignmentCount', 'saving', 'errorMessage', 'assistUserOptions',
    'pendingAssistSubjectType', 'pendingAssistSubjectId', 'addAssistResponsibilitySubject',
    'handleAssistGridCellClick',
    'clearAssistMappingsForRuleKeys', 'removeAssistResponsibilitySubject',
    'assistResponsibilitySubjects',
    'mapSourceCellToSelectedAssistGridCell', 'removeAssistGridCellMapping'
  ]
  const dialog = new Function(...Object.keys(dependencies), `${compile(executable)}\nreturn {${exposed.join(',')}}`)(
    ...Object.values(dependencies)
  )
  await dialog.loadCellRules()
  assert.equal(dialog.errorMessage.value, '', '实际加载链通过')
  calls.length = 0
  return { dialog, calls, events, messages, fail }
}

;(async () => {
  for (const wholeRule of [originalRule, null]) {
    const { dialog, calls } = await createDialog({ wholeRule })
    assert.equal(dialog.hasAssistFillConfiguration.value, false, '125布局行不自动创建辅助权限')
    dialog.activeConfigMode.value = 'assistMapping'
    assert.equal(dialog.hasAssistFillConfiguration.value, false, '切换页签不创建权限')
    dialog.assistGridRowCount.value = 26
    assert.equal(dialog.hasAssistFillConfiguration.value, false, '调整布局尺寸不创建权限')
    dialog.activeConfigMode.value = 'source'
    dialog.wholeFormFillRule.value = clone(requestedRule)
    await dialog.confirmAllRules()
    assert.deepEqual(calls.map((call) => call.name), ['cells', 'permission'])
    assert.deepEqual(calls[0].body.assistRows, layout, '整表保存不删除布局行')
    assert.equal(calls[0].body.assistGridRowCount, 26)
    assert.deepEqual(calls[0].body.signatureCellMarkers, [signatureMarker], '保留正式签名标识')
    assert.deepEqual(calls[1].body.fillRule, requestedRule)
    assert.ok(!('fillAssignments' in calls[1].body), '不从布局rowKey推断正式填写权限')
    assert.equal(dialog.hasUnsavedChanges.value, false)
  }
  const ruleOnly = await createDialog()
  assert.deepEqual(ruleOnly.dialog.assistUserOptions.value.slice(0, 2).map((option) => option.label),
    ['同名员工（编号：672）', '同名员工（编号：1284）'])
  ruleOnly.dialog.wholeFormFillRule.value = clone(requestedRule)
  await ruleOnly.dialog.confirmAllRules()
  assert.deepEqual(ruleOnly.calls.map((call) => call.name), ['permission', 'readCells'], '仅改责任只写正式权限')
  assert.deepEqual(ruleOnly.dialog.assistRows.value, layout)

  const cellsOnly = await createDialog()
  cellsOnly.dialog.ruleRows.value[0].label = '新字段名称'
  await cellsOnly.dialog.confirmAllRules()
  assert.deepEqual(cellsOnly.calls.map((call) => call.name), ['cells'])
  assert.deepEqual(cellsOnly.calls[0].body.assistRows, layout)

  for (const removeMapping of [
    (d) => d.removeAssistGridCellMapping(layout[0].rowKey),
    (d) => {
      d.ruleRows.value[0].valueType = 'NUMBER'
      d.clearAssistMappingsForRuleKeys(new Set(['0:0']))
    },
    (d) => d.removeAssistResponsibilitySubject(d.assistResponsibilitySubjects.value[0])
  ]) {
    const state = await createDialog()
    await removeMapping(state.dialog)
    assert.equal(state.dialog.hasAssistFillConfiguration.value, false, '删除布局映射不升级正式整表授权')
    const remainingRows = clone(state.dialog.assistRows.value)
    await state.dialog.confirmAllRules()
    assert.deepEqual(state.calls.map((call) => call.name), ['cells'])
    assert.deepEqual(state.calls[0].body.assistRows, remainingRows, '保存现行剩余布局，包括主动清空布局')
    assert.equal(state.calls[0].body.assistGridRowCount, 25)
    assert.equal(state.calls[0].body.assistGridColumnCount, 5)
    assert.deepEqual(state.dialog.wholeFormFillRule.value, originalRule, '原整表责任不变')
  }

  const auxiliary = await createDialog({ auxiliary: true, wholeRule: null })
  assert.equal(auxiliary.dialog.hasAssistFillConfiguration.value, true, '正式辅助权限仍走辅助链')
  auxiliary.dialog.activeConfigMode.value = 'assistMapping'
  await auxiliary.dialog.confirmAllRules()
  assert.deepEqual(auxiliary.calls.map((call) => call.name), ['cells'], '未改责任不重写正式辅助权限')
  auxiliary.calls.length = 0
  const a = auxiliary.dialog
  a.pendingAssistSubjectType.value = 'USERS'
  a.pendingAssistSubjectId.value = 672
  a.addAssistResponsibilitySubject()
  a.removeAssistGridCellMapping(layout[0].rowKey)
  a.handleAssistGridCellClick('ASSIST_GRID_USERS672_R0_C0')
  a.mapSourceCellToSelectedAssistGridCell({ rowIndex: 0, columnIndex: 0, identity: '0:0', text: '字段0' })
  await a.confirmAllRules()
  assert.deepEqual(auxiliary.calls.map((call) => call.name), ['cells', 'permission'], '正式辅助责任编辑仍保存')
  assert.equal(auxiliary.calls[1].body.fillAssignments.length, 125)
  assert.equal(auxiliary.calls[1].body.fillAssignments.filter((assignment) => assignment.candidateSourceIds[0] === 672).length, 1)

  for (const wholeRule of [originalRule, null]) {
    const first = await createDialog({ wholeRule, rows: [], cellRules: [rules[0]] })
    const d = first.dialog
    d.activeConfigMode.value = 'assistMapping'
    d.pendingAssistSubjectType.value = 'USERS'
    d.pendingAssistSubjectId.value = 672
    d.addAssistResponsibilitySubject()
    assert.equal(d.hasAssistFillConfiguration.value, false, '只有未映射责任主体不创建权限')
    d.mapSourceCellToSelectedAssistGridCell({ rowIndex: 0, columnIndex: 0, identity: '0:0', text: '字段0' })
    assert.equal(d.hasAssistFillConfiguration.value, true, '真实首次责任映射可选择辅助路径')
    await d.confirmAllRules()
    assert.deepEqual(first.calls.map((call) => call.name), ['cells', 'permission'])
    assert.deepEqual(first.calls[1].body.fillAssignments[0].candidateSourceIds, [672])
    assert.equal(d.loadedFillAssignmentCount.value, 1)
    assert.equal(d.hasUnsavedChanges.value, false)
  }

  for (const failedCall of ['cells', 'permission', 'readCells']) {
    const state = await createDialog()
    const d = state.dialog
    d.wholeFormFillRule.value = clone(requestedRule)
    if (failedCall !== 'readCells') d.ruleRows.value[0].label = '已编辑'
    const baseline = d.savedStateSignature.value
    const ruleBaseline = d.savedWholeFormFillRuleSignature.value
    state.fail[failedCall] = `${failedCall} failed`
    await d.confirmAllRules()
    assert.equal(d.errorMessage.value, `${failedCall} failed`)
    assert.equal(d.savedStateSignature.value, baseline, '失败不清理可编辑状态')
    assert.equal(d.savedWholeFormFillRuleSignature.value, ruleBaseline, '失败不提前更新正式责任基线')
    assert.equal(d.hasUnsavedChanges.value, true)
    assert.equal(d.saving.value, false)
    assert.ok(!state.messages.some((message) => message.success))
    assert.equal(state.events.length, 0, '失败不发出confirmed')
    if (failedCall === 'cells') assert.ok(!state.calls.some((call) => call.name === 'permission'))
    delete state.fail[failedCall]
    await d.confirmAllRules()
    assert.equal(d.hasUnsavedChanges.value, false, '失败后可按原编辑状态重试')
  }
  const incomplete = await createDialog()
  incomplete.dialog.wholeFormFillRule.value = { ...requestedRule, candidateSourceIds: [] }
  await incomplete.dialog.confirmAllRules()
  assert.match(incomplete.dialog.errorMessage.value, /缺少填写人或角色/)
  assert.equal(incomplete.calls.length, 0, '验证失败在所有写入前发生')

  const conflicting = await createDialog({ rows: [], cellRules: [rules[0]] })
  const d = conflicting.dialog
  d.wholeFormFillRule.value = clone(requestedRule)
  d.activeConfigMode.value = 'assistMapping'
  d.pendingAssistSubjectType.value = 'USERS'
  d.pendingAssistSubjectId.value = 672
  d.addAssistResponsibilitySubject()
  d.mapSourceCellToSelectedAssistGridCell({ rowIndex: 0, columnIndex: 0, identity: '0:0', text: '字段0' })
  await d.confirmAllRules()
  assert.match(d.errorMessage.value, /请分别保存整表填写责任和辅助映射修改/)
  assert.equal(conflicting.calls.length, 0, '两种责任同时编辑必须在任何写入前拒绝')
  d.removeAssistGridCellMapping('ASSIST_GRID_USERS672_R0_C0')
  assert.equal(d.hasAssistFillConfiguration.value, false, '撤销新映射不遗留辅助授权转换')
  await d.confirmAllRules()
  assert.deepEqual(conflicting.calls.map((call) => call.name), ['cells', 'permission'])
  assert.ok(!('fillAssignments' in conflicting.calls[1].body))
  console.log('PASS batch-record-whole-form-fill-rule-behavior')
})().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
