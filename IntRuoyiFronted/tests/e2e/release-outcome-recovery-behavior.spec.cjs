const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const { parse, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const moduleObject = { exports: {} }
const source = fs.readFileSync(path.join(root, 'src/views/system/notify/my/notifyInbox.ts'), 'utf8')
const code = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
Function('exports', code)(moduleObject.exports)
const { loadInboxPage, toInboxRecoveries, toInboxMessages } = moduleObject.exports
const own = { id: '453', workTaskId: '2883', workOrderCode: '原订单', batchCode: '原批号', eventType: 'MARKET_APPROVED' }
const messages = Array.from({ length: 27 }, (_, i) => ({ id: i + 1, readStatus: i % 2 === 0 }))
const loader = (rows, calls = []) => async params => {
  calls.push(params)
  const items = params.readStatus === undefined ? rows : rows.filter(row => String(row.readStatus) === String(params.readStatus))
  return { list: items.slice((params.pageNo - 1) * params.pageSize, params.pageNo * params.pageSize), total: items.length }
}

test('站内信和补发入口使用同一真实列表，个人中心不再渲染底部独立区域', () => {
  const profile = fs.readFileSync(path.join(root, 'src/views/Profile/Index.vue'), 'utf8')
  assert.doesNotMatch(profile, /ReleaseOutcomeRecovery/)
  for (const file of ['src/views/system/notify/my/components/MyNotifyMessageList.vue', 'src/views/mes/pro/production-release/components/ReleaseTaskNotificationEntry.vue']) {
    const { descriptor, errors } = parse(fs.readFileSync(path.join(root, file), 'utf8'))
    assert.deepEqual(errors, [])
    assert.ok(compileScript(descriptor, { id: file, inlineTemplate: true }).content)
  }
})

test('跨页合并时无漏项、重复或错误总数，待补发优先且每页不超出容量', async () => {
  for (const count of [0, 1, 9, 10, 12, 20, 35]) {
    const pending = toInboxRecoveries(Array.from({ length: count }, (_, i) => ({ ...own, id: String(453 + i) })))
    const expected = [...pending, ...toInboxMessages(messages)]
    const actual = []
    for (let pageNo = 1; pageNo <= Math.ceil(expected.length / 10); pageNo++) {
      const calls = []
      const page = await loadInboxPage(pending, { pageNo, pageSize: 10 }, loader(messages, calls))
      assert.equal(page.total, expected.length)
      assert.deepEqual(page.list, expected.slice((pageNo - 1) * 10, pageNo * 10))
      assert.ok(calls.length <= 2)
      actual.push(...page.list)
    }
    assert.deepEqual(actual, expected)
  }
})

test('已读未读筛选只包含真实站内信，待补发筛选不向站内信接口发送非法状态', async () => {
  const pending = toInboxRecoveries([own])
  for (const readStatus of [true, false, 'true', 'false']) {
    const page = await loadInboxPage(pending, { pageNo: 1, pageSize: 10, readStatus }, loader(messages))
    assert.ok(page.list.every(row => row.kind === 'message' && String(row.readStatus) === String(readStatus)))
    assert.equal(page.total, messages.filter(row => String(row.readStatus) === String(readStatus)).length)
  }
  const page = await loadInboxPage(pending, { pageNo: 1, pageSize: 10, readStatus: 'pending' }, () => assert.fail('待补发不能调用已送达接口'))
  assert.deepEqual(page, { list: pending, total: 1 })
})

test('两种来源的相同编号不会冲突，待补发不伪造发送时间或已读状态', () => {
  const [pending] = toInboxRecoveries([own])
  const [message] = toInboxMessages([{ id: 453 }])
  assert.notEqual(pending.rowKey, message.rowKey)
  assert.equal(pending.readStatus, undefined)
  assert.equal(pending.createTime, undefined)
  assert.match(pending.templateContent, /上市放行通过.*原订单.*原批号/)
})

test('畸形身份、未知事件与接口失败明确报错', async () => {
  for (const row of [null, { ...own, workTaskId: '0' }, { ...own, eventType: 'ASSIGNED' }, { ...own, eventType: 'toString' }]) {
    assert.throws(() => toInboxRecoveries([row]), /放行结果通知身份或内容无效/)
  }
  await assert.rejects(loadInboxPage([], { pageNo: 1, pageSize: 10 }, async () => { throw new Error('查询失败') }), /查询失败/)
})
const descriptor = parse(fs.readFileSync(path.join(root, 'src/views/system/notify/my/components/MyNotifyMessageList.vue'), 'utf8')).descriptor
const ast = ts.createSourceFile('list.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
const getListStatement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'getList')).getText(ast)
const getListCode = ts.transpileModule('let loadSequence = 0;\n' + getListStatement, { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
const ref = value => ({ value })
function listHarness(loadPending = async () => [own]) {
  let pendingCalls = 0
  const context = { props: { active: true }, canReadReleaseOutcomes: ref(true),
    loading: ref(false), loadError: ref(''), list: ref([]), total: ref(0),
    queryParams: { pageNo: 1, pageSize: 10 }, toInboxRecoveries, loadInboxPage,
    listOwnPendingReleaseOutcomes: () => { pendingCalls++; return loadPending() },
    NotifyMessageApi: { getMyNotifyMessagePage: loader(messages) },
    mergeReadTimeOverrides: rows => rows, refreshTableLayout: async () => {} }
  return { ...context, calls: () => pendingCalls,
    getList: Function(...Object.keys(context), getListCode + '; return getList')(...Object.values(context)) }
}

test('真实列表在无权限、已读筛选或未展示时不查询补发接口', async () => {
  for (const key of ['permission', 'filter', 'inactive']) {
    const h = listHarness()
    if (key === 'permission') h.canReadReleaseOutcomes.value = false
    if (key === 'filter') h.queryParams.readStatus = true
    if (key === 'inactive') h.props.active = false
    await h.getList()
    assert.equal(h.calls(), 0)
    assert.ok(h.list.value.every(row => row.kind === 'message'))
  }
})

test('真实列表补发查询失败时展示错误，清除旧数据，不伪造成功', async () => {
  const h = listHarness(async () => { throw new Error('本人结果通知读取失败') })
  h.list.value = [{ id: 'old' }]
  await h.getList()
  assert.equal(h.loadError.value, '本人结果通知读取失败')
  assert.deepEqual(h.list.value, [])
  assert.equal(h.loading.value, false)
})

test('离开站内信后，旧补发请求的迟到响应不会恢复记录', async () => {
  let finish
  const h = listHarness(() => new Promise(resolve => { finish = resolve }))
  const request = h.getList()
  h.props.active = false
  await h.getList()
  finish([own])
  await request
  assert.deepEqual(h.list.value, [])
})
