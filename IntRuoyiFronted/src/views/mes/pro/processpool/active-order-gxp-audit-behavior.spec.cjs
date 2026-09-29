const assert = require('node:assert/strict')
const { test } = require('node:test')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const vue = require('vue')

// Execute the component's actual audit declarations and watchers, not a copied implementation.
const panel = fs.readFileSync(path.join(__dirname, 'components/ActiveOrderSubmissionDetailPanel.vue'), 'utf8')
const script = panel.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
const ast = ts.createSourceFile('panel.ts', script, ts.ScriptTarget.Latest, true, ts.ScriptKind.TS)
const declarations = ast.statements.filter((s) => ts.isVariableStatement(s) &&
  s.declarationList.declarations.every((d) => /^(gxpAudit|selectedGxpAuditEvent|formatGxpAudit|loadGxpAudit$|openGxpAuditEvent$|resetGxpAudit)/.test(d.name.getText(ast))))
const watchers = ast.statements.filter((s) => ts.isExpressionStatement(s) && ts.isCallExpression(s.expression) &&
  s.expression.expression.getText(ast) === 'watch' &&
  /auditScopeTypeValue|gxpAuditDetailVisible/.test(s.expression.arguments[0].getText(ast)))
const names = declarations.flatMap((s) => s.declarationList.declarations.map((d) => d.name.getText(ast)))
const executable = ts.transpileModule([...declarations, ...watchers].map((s) => s.getText(ast)).join('\n') +
  `\nreturn {${names.join(',')}}`, { compilerOptions: { target: ts.ScriptTarget.ES2020 } }).outputText

function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function harness(t) {
  const scope = vue.effectScope()
  t.after(() => scope.stop())
  const auditScopeTypeValue = vue.ref('TEAM')
  const auditScopeIdValue = vue.ref(undefined)
  const showSummaryTab = vue.ref(true)
  const props = vue.reactive({ loading: false, error: '' })
  const pages = [], details = []
  const request = (queue) => (...args) => {
    const pending = deferred()
    queue.push({ ...pending, args })
    return pending.promise
  }
  const dependencies = { ref: vue.ref, computed: vue.computed, watch: vue.watch,
    auditScopeTypeValue, auditScopeIdValue, showSummaryTab, props,
    getActiveOrderGxpAuditPage: request(pages), getActiveOrderGxpAuditEvent: request(details) }
  const state = scope.run(() => new Function(...Object.keys(dependencies), executable)(...Object.values(dependencies)))
  return { ...state, pages, details, auditScopeIdValue, auditScopeTypeValue, showSummaryTab, props,
    async select(id, type = 'TEAM') { auditScopeTypeValue.value = type; auditScopeIdValue.value = id; await vue.nextTick() } }
}

for (const state of ['loading', 'error']) {
  test(`parent ${state} invalidates audit while panel instance remains alive`, async (t) => {
    const h = harness(t); await h.select(1)
    const a = h.openGxpAuditEvent({ id: 11 })
    h.props[state] = state === 'loading' ? true : 'parent error'
    await vue.nextTick()
    h.pages[0].resolve(page(1)); h.details[0].resolve({ id: 11 }); await a; await settle()
    assert.deepEqual(h.gxpAuditEvents.value, [])
    assert.equal(h.selectedGxpAuditEvent.value, undefined)
    assert.equal(h.gxpAuditDetailVisible.value, false)
    assert.equal(h.gxpAuditLoading.value, false)
    h.props[state] = state === 'loading' ? false : ''
    await vue.nextTick()
    assert.equal(h.pages.length, 2)
  })
}
test('close and reopen same detail in one tick invalidates first generation', async (t) => {
  const h = harness(t); await h.select(1)
  const a = h.openGxpAuditEvent({ id: 11 })
  h.gxpAuditDetailVisible.value = false
  const b = h.openGxpAuditEvent({ id: 11 })
  h.details[0].resolve({ id: 11, reason: 'old' }); await a
  assert.equal(h.selectedGxpAuditEvent.value, undefined)
  assert.equal(h.gxpAuditDetailLoading.value, true)
  h.details[1].resolve({ id: 11, reason: 'new' }); await b
  assert.equal(h.selectedGxpAuditEvent.value.reason, 'new')
})
test('scope change guards response before next Vue tick', async (t) => {
  const h = harness(t); await h.select(1)
  h.auditScopeIdValue.value = 2
  h.pages[0].resolve(page(1)); await Promise.resolve()
  assert.deepEqual(h.gxpAuditEvents.value, [])
  assert.equal(h.gxpAuditLoading.value, true)
})
const page = (id) => ({ list: [{ id }], total: 120 })
test('BATCH identity kind changes with same ID invalidate both pending list and detail', async (t) => {
  const h = harness(t)
  await h.select({ batchExecutionId: '77' }, 'BATCH')
  const oldDetail = h.openGxpAuditEvent({ id: 11 })
  await h.select({ activeOrderId: '77' }, 'BATCH')
  assert.deepEqual(h.pages.at(-1).args.slice(0, 2), ['BATCH', { activeOrderId: '77' }])
  h.pages[0].resolve(page(11)); h.details[0].resolve({ id: 11 }); await oldDetail; await settle()
  assert.deepEqual(h.gxpAuditEvents.value, [])
  assert.equal(h.selectedGxpAuditEvent.value, undefined)
  h.pages.at(-1).resolve(page(22)); await settle()
  assert.equal(h.gxpAuditEvents.value[0].id, 22)
})
async function settle() { await Promise.resolve(); await vue.nextTick() }

test('order B resolves before A: A cannot overwrite B', async (t) => {
  const h = harness(t)
  await h.select(1); await h.select(2)
  h.pages[1].resolve(page(2)); await settle()
  h.pages[0].resolve(page(1)); await settle()
  assert.equal(h.gxpAuditEvents.value[0].id, 2)
})
test('page 2 resolves before page 1: displayed page remains 2', async (t) => {
  const h = harness(t)
  await h.select(1)
  h.gxpAuditPageNo.value = 2
  const second = h.loadGxpAudit()
  assert.equal(h.pages[1].args[2].pageNo, 2)
  h.pages[1].resolve(page(22)); await second
  h.pages[0].resolve(page(11)); await settle()
  assert.equal(h.gxpAuditEvents.value[0].id, 22)
})
for (const outcome of ['success', 'failure']) {
  test(`stale list ${outcome} cannot finish newer loading or replace error`, async (t) => {
    const h = harness(t)
    await h.select(1); await h.select(2)
    if (outcome === 'success') h.pages[0].resolve(page(1))
    else h.pages[0].reject(new Error('old list error'))
    await settle()
    assert.equal(h.gxpAuditLoading.value, true)
    assert.equal(h.gxpAuditError.value, '')
    h.pages[1].reject(new Error('current list error')); await settle()
    assert.equal(h.gxpAuditError.value, 'current list error')
    assert.equal(h.gxpAuditLoading.value, false)
  })
}

for (const invalidId of [null, '', ' ', 0, -1, NaN]) {
  test(`invalid scope ${String(invalidId)} clears state without querying`, async (t) => {
    const h = harness(t); await h.select(1)
    await h.select(invalidId)
    assert.equal(h.pages.length, 1)
    assert.equal(h.gxpAuditLoading.value, false)
    h.pages[0].resolve(page(1)); await settle()
    assert.deepEqual(h.gxpAuditEvents.value, [])
  })
}

function renderOperator(h, row) {
  const column = panel.match(/<el-table-column label="(?:操作人|实际执行人(?: \/ 认证账号)?)"[\s\S]*?<\/el-table-column>/)[0]
  const expression = column.match(/\{\{([\s\S]*?)\}\}/)[1]
  return new Function('row', ...Object.keys(h), `return (${expression})`)(row, ...Object.values(h))
}
test('operator cell reads performedBy snapshot, never authenticated account', (t) => {
  const h = harness(t)
  const row = { actorId: 900, actorDisplayName: '设备账号',
    performedByJson: JSON.stringify({ actorId: 12, displayName: '员工甲' }) }
  assert.equal(renderOperator(h, row), '员工甲')
})
for (const actorType of ['SYSTEM_USER', 'MES_EMPLOYEE_PROFILE']) {
  test(`formal ${actorType} string identity is not confused with authentication ID`, (t) => {
    const h = harness(t)
    const snapshot = { actorId: '9223372036854775806', actorType,
      displayName: '正式执行员工', username: 'employee-001' }
    const row = Object.freeze({ actorId: 900, actorDisplayName: '设备认证账号',
      performedByJson: JSON.stringify(snapshot),
      authenticatedActorJson: JSON.stringify({ actorId: 900, displayName: '设备认证账号',
        username: 'device-001', tenantId: 1, userType: 2 }) })
    assert.equal(renderOperator(h, row), '正式执行员工')
    assert.equal(h.formatGxpAuditActorSnapshot(row.authenticatedActorJson, '认证账号'), '设备认证账号')
    assert.deepEqual(JSON.parse(row.performedByJson), snapshot)
    assert.equal(renderOperator(h, { ...row, performedByJson: JSON.stringify({
      actorId: snapshot.actorId, actorType, username: 'employee-001' }) }), '未记录')
  })
}
test('list and detail distinguish employee and authentication snapshots without mutating history', (t) => {
  const h = harness(t)
  const row = Object.freeze({ actorDisplayName: '错误旧认证显示',
    performedByJson: JSON.stringify({ actorId: 12, displayName: '员工甲' }),
    authenticatedActorJson: JSON.stringify({ actorId: 900, displayName: '设备账号' }) })
  const column = panel.match(/<el-table-column label="实际执行人 \/ 认证账号"[\s\S]*?<\/el-table-column>/)[0]
  const listValues = [...column.matchAll(/\{\{([\s\S]*?)\}\}/g)].map((m) =>
    new Function('row', ...Object.keys(h), `return (${m[1]})`)(row, ...Object.values(h)))
  assert.deepEqual(listValues, ['员工甲', '设备账号'])
  for (const [label, expected] of [['实际执行人', '员工甲'], ['认证账号', '设备账号']]) {
    const item = panel.match(new RegExp(`<el-descriptions-item label="${label}">([\\s\\S]*?)<\\/el-descriptions-item>`))[1]
    const expression = item.match(/\{\{([\s\S]*?)\}\}/)[1]
    assert.equal(new Function('selectedGxpAuditEvent', 'formatGxpAuditActorSnapshot', `return (${expression})`)(
      row, h.formatGxpAuditActorSnapshot), expected)
  }
  assert.equal(h.formatGxpAuditActorSnapshot('{bad', '认证账号'), '认证账号快照损坏')
  assert.equal(h.formatGxpAuditActorSnapshot(undefined, '认证账号'), '未记录')
})
for (const [snapshot, expected] of [[undefined, '未记录'], ['', '未记录'],
  ['{broken', '执行人快照损坏'], ['null', '执行人快照损坏'], ['[]', '执行人快照损坏'],
  ['{}', '未记录'], ['{"displayName":42}', '执行人快照损坏']]) {
  test(`operator snapshot ${snapshot} exposes absence/corruption`, (t) => {
    const h = harness(t)
    assert.equal(renderOperator(h, { actorDisplayName: '设备账号', performedByJson: snapshot }), expected)
  })
}
test('scope change clears previously loaded list and detail immediately', async (t) => {
  const h = harness(t)
  await h.select(1)
  h.pages[0].resolve(page(1)); await settle()
  const detail = h.openGxpAuditEvent({ id: 11 })
  h.details[0].resolve({ id: 11 }); await detail
  await h.select(2)
  assert.deepEqual(h.gxpAuditEvents.value, [])
  assert.equal(h.gxpAuditTotal.value, 0)
  assert.equal(h.selectedGxpAuditEvent.value, undefined)
  assert.equal(h.gxpAuditDetailVisible.value, false)
})
for (const invalidate of ['missing', 'hidden', 'type']) {
  test(`${invalidate} scope invalidates pending list and detail`, async (t) => {
    const h = harness(t)
    await h.select(1)
    const detail = h.openGxpAuditEvent({ id: 11 })
    if (invalidate === 'missing') await h.select(undefined)
    if (invalidate === 'type') await h.select(1, 'PQC')
    if (invalidate === 'hidden') { h.showSummaryTab.value = false; await vue.nextTick() }
    h.pages[0].resolve(page(1)); h.details[0].resolve({ id: 11 }); await detail; await settle()
    assert.deepEqual(h.gxpAuditEvents.value, [])
    assert.equal(h.selectedGxpAuditEvent.value, undefined)
    assert.equal(h.gxpAuditDetailVisible.value, false)
    assert.equal(h.gxpAuditLoading.value, invalidate === 'type')
  })
}
test('detail B resolves before A: only B stays selected', async (t) => {
  const h = harness(t); await h.select(1)
  const a = h.openGxpAuditEvent({ id: 11 })
  const b = h.openGxpAuditEvent({ id: 22 })
  h.details[1].resolve({ id: 22 }); await b
  h.details[0].resolve({ id: 11 }); await a
  assert.equal(h.selectedGxpAuditEvent.value.id, 22)
})
test('old detail failure does not overwrite new loading/error', async (t) => {
  const h = harness(t); await h.select(1)
  const a = h.openGxpAuditEvent({ id: 11 })
  const b = h.openGxpAuditEvent({ id: 22 })
  h.details[0].reject(new Error('old detail error')); await a
  assert.equal(h.gxpAuditDetailLoading.value, true)
  assert.equal(h.gxpAuditDetailError.value, '')
  h.details[1].reject(new Error('current detail error')); await b
  assert.equal(h.gxpAuditDetailError.value, 'current detail error')
  assert.equal(h.gxpAuditDetailLoading.value, false)
  assert.equal(h.selectedGxpAuditEvent.value, undefined)
})
test('current missing detail reports failure; stale success cannot erase it', async (t) => {
  const h = harness(t); await h.select(1)
  const a = h.openGxpAuditEvent({ id: 11 })
  const b = h.openGxpAuditEvent({ id: 22 })
  h.details[1].resolve(undefined); await b
  assert.equal(h.gxpAuditDetailError.value, '统一 GxP 审计事件不存在或不属于当前对象。')
  h.details[0].resolve({ id: 11 }); await a
  assert.equal(h.selectedGxpAuditEvent.value, undefined)
  assert.equal(h.gxpAuditDetailError.value, '统一 GxP 审计事件不存在或不属于当前对象。')
})
test('stale list failure cannot overwrite current failure', async (t) => {
  const h = harness(t); await h.select(1); await h.select(2)
  h.pages[1].reject(new Error('current')); await settle()
  h.pages[0].reject(new Error('old')); await settle()
  assert.equal(h.gxpAuditError.value, 'current')
})
for (const outcome of ['success', 'failure']) {
  test(`closing dialog invalidates pending detail ${outcome}`, async (t) => {
    const h = harness(t); await h.select(1)
    const a = h.openGxpAuditEvent({ id: 11 })
    h.gxpAuditDetailVisible.value = false; await vue.nextTick()
    if (outcome === 'success') h.details[0].resolve({ id: 11 })
    else h.details[0].reject(new Error('closed detail error'))
    await a
    assert.equal(h.selectedGxpAuditEvent.value, undefined)
    assert.equal(h.gxpAuditDetailError.value, '')
    assert.equal(h.gxpAuditDetailLoading.value, false)
    assert.equal(h.gxpAuditDetailVisible.value, false)
  })
}
