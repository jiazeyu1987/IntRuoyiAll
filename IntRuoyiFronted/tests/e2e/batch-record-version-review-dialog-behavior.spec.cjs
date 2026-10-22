const test = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs'),
  path = require('node:path'),
  ts = require('typescript')
const { ref, computed, watch } = require('vue'),
  { parse } = require('vue/compiler-sfc')
const read = (name) => fs.readFileSync(path.resolve(__dirname, '../../', name), 'utf8')
const source = read('src/views/mes/pro/batchrecordformlist/BatchRecordVersionReviewDialog.vue')
const body = parse(source).descriptor.scriptSetup.content
const ast = ts.createSourceFile('review.ts', body, ts.ScriptTarget.Latest, true)
const js = ts.transpileModule(
  ast.statements
    .filter((n) => !ts.isImportDeclaration(n))
    .map((n) => n.getText(ast))
    .join('\n'),
  { compilerOptions: { target: ts.ScriptTarget.ES2022 } }
).outputText
const permissions = [
  'mes:pro-batch-record-version:query',
  'mes:pro-batch-record-version:confirm',
  'mes:pro-batch-record-template:version-approve'
]
const row = (id = '173', status = 'PRECHECK_FAILED') => ({
  batchRecordVersionId: id,
  reportId: `report-${id}`,
  versionNo: 'V2',
  versionStatus: status,
  reportName: '总表',
  batchRecordName: '测试批记录'
})
const item = (id = 377, risk = 'CONFIRM_REQUIRED', confirmed = false) => ({
  itemId: id,
  riskLevel: risk,
  confirmed,
  businessOwnerType: 'DCC_OWNER',
  message: '核对整表责任配置'
})
const result = (id = '173', items = [item()], ready = false) => ({
  versionId: id,
  items,
  approvalReady: ready,
  blockerCount: 0,
  confirmRequiredCount: items.length,
  confirmedCount: items.filter((i) => i.confirmed).length
})
const flush = () => new Promise((r) => setImmediate(r))
const deferred = () => {
  let resolve, reject
  const promise = new Promise((r, j) => {
    resolve = r
    reject = j
  })
  return { promise, resolve, reject }
}
function setup(options = {}) {
  const calls = [],
    events = [],
    allowed = new Set(options.permissions || permissions)
  let current = row(),
    diff = result(),
    key = 0,
    unmount
  const deps = {
    ref,
    computed,
    watch,
    onBeforeUnmount(fn) {
      unmount = fn
    },
    defineExpose() {},
    defineEmits: () => (name) => events.push(name),
    crypto: { randomUUID: () => `key-${++key}` },
    hasPermission: (p) => p.some((x) => allowed.has(x)),
    parsePositiveRouteQueryId: (v) =>
      /^\d+$/.test(String(v)) && String(v) !== '0' ? String(v) : '',
    ElMessageBox: {
      confirm: async (...args) => {
        calls.push(['ask', ...args])
        if (options.cancel) throw 'cancel'
      }
    },
    BatchRecordReportApi: {
      getGeneratedReportPage: async (params, opt) => {
        calls.push(['page', params, opt])
        return options.page ? options.page(params) : { list: [current], total: 1 }
      },
      submitBatchRecordVersionApproval: async (id, opt) => {
        calls.push(['submit', id, opt])
        if (options.submit) return options.submit(id)
        current = { ...current, versionStatus: 'PENDING_APPROVAL' }
        return { versionId: id, versionStatus: 'PENDING_APPROVAL', processedResult: 'SUBMITTED' }
      }
    },
    BatchRecordVersionGovernanceApi: {
      getMigrationDiff: async (id) => {
        calls.push(['diff', id])
        return options.diff ? options.diff(id) : diff
      },
      confirmMigration: async (id, data) => {
        calls.push(['confirm', id, JSON.parse(JSON.stringify(data))])
        if (options.confirm) return options.confirm(id, data)
        diff = result(
          id,
          diff.items.map((i) =>
            data.itemIds.map(String).includes(String(i.itemId)) ? { ...i, confirmed: true } : i
          ),
          true
        )
        current = { ...current, versionStatus: 'PRECHECK_PASSED' }
        return {
          versionId: id,
          confirmedItemIds: data.itemIds,
          idempotencyKey: data.idempotencyKey
        }
      }
    }
  }
  const s = Function(
    ...Object.keys(deps),
    `${js};return {open,reload,confirmSelected,submitApproval,visible,target,version,diff,selectedIds,comment,pendingAttempt,errorMessage,notice,loading,confirming,submitting,canConfirm,canSubmit,canSelectItem,toggleItem,ownerLabel,diffLabel,statusLabel}`
  )(...Object.values(deps))
  return {
    s,
    calls,
    events,
    allowed,
    unmount: () => unmount(),
    set(rowValue, diffValue) {
      current = rowValue
      diff = diffValue
    }
  }
}
test('entry reads exact formal report and version with no automatic writes', async () => {
  const { s, calls } = setup()
  await s.open(row())
  assert.equal(s.version.value.batchRecordVersionId, '173')
  assert.deepEqual(
    calls.map((c) => c[0]),
    ['page', 'diff']
  )
  assert.deepEqual(calls[0][1], {
    reportId: 'report-173',
    pageNo: 1,
    pageSize: 100,
    latestVersionOnly: false
  })
  assert.equal(calls[0][2].ignoreErrorMessage, true)
  assert.equal(s.canSubmit.value, false)
  assert.equal(s.canConfirm.value, false)
})
test('formal read selects exact version rather than first or latest row', async () => {
  const { s } = setup({
    page: async () => ({
      list: [{ ...row('174', 'APPROVED'), reportId: 'report-173' }, row('173', 'PRECHECK_FAILED')],
      total: 2
    })
  })
  await s.open(row())
  assert.equal(s.version.value.batchRecordVersionId, '173')
  assert.equal(s.version.value.versionStatus, 'PRECHECK_FAILED')
  assert.equal(s.errorMessage.value, '')
})
test('ambiguous exact-version states fail rather than choosing one', async () => {
  const { s } = setup({
    page: async () => ({ list: [row(), row('173', 'APPROVED')], total: 2 })
  })
  await s.open(row())
  assert.match(s.errorMessage.value, /正式版本/)
  assert.equal(s.version.value, undefined)
  assert.equal(s.canConfirm.value, false)
})
test('explicit selection and comment confirm then refresh; approval is a separate click', async () => {
  const { s, calls } = setup()
  await s.open(row())
  s.toggleItem(s.diff.value.items[0], true)
  s.comment.value = '已逐项核对正式配置'
  assert.equal(s.canConfirm.value, true)
  await s.confirmSelected()
  assert.equal(calls.filter((c) => c[0] === 'confirm').length, 1)
  assert.equal(calls.filter((c) => c[0] === 'submit').length, 0)
  assert.equal(s.diff.value.items[0].confirmed, true)
  assert.equal(s.canSelectItem(s.diff.value.items[0]), false)
  assert.equal(s.canSubmit.value, true)
  await s.submitApproval()
  assert.equal(s.version.value.versionStatus, 'PENDING_APPROVAL')
  assert.equal(s.canSubmit.value, false)
  assert.equal(calls.filter((c) => c[0] === 'submit').length, 1)
})
test('risk, confirmed and permission gates prohibit inappropriate selection and submission', async () => {
  const { s } = setup({
    permissions: [permissions[0]],
    diff: async () =>
      result('173', [item(1, 'BLOCKER'), item(2, 'CONFIRM_REQUIRED', true), item(3, 'INFO')], true)
  })
  await s.open(row())
  for (const i of s.diff.value.items) {
    assert.equal(s.canSelectItem(i), false)
    s.toggleItem(i, true)
  }
  assert.deepEqual(s.selectedIds.value, [])
  s.comment.value = '意见'
  assert.equal(s.canConfirm.value, false)
  assert.equal(s.canSubmit.value, false)
})
test('comment and stale selection are validated before a write', async () => {
  const { s, calls } = setup()
  await s.open(row())
  s.selectedIds.value = ['377']
  s.comment.value = ' '
  assert.equal(s.canConfirm.value, false)
  s.comment.value = 'x'.repeat(501)
  assert.equal(s.canConfirm.value, false)
  s.comment.value = '确认'
  s.diff.value.items[0].confirmed = true
  await s.confirmSelected()
  assert.match(s.errorMessage.value, /已确认/)
  assert.equal(
    calls.some((c) => c[0] === 'confirm'),
    false
  )
})
test('cancel leaves no write or locked new request', async () => {
  const { s, calls } = setup({ cancel: true })
  await s.open(row())
  s.toggleItem(s.diff.value.items[0], true)
  s.comment.value = '确认'
  await s.confirmSelected()
  assert.equal(
    calls.some((c) => c[0] === 'confirm'),
    false
  )
  assert.equal(s.pendingAttempt.value, undefined)
})
test('unknown confirmation requires readonly refresh and preserves original payload/key across reopen', async () => {
  const { s, calls } = setup({
    confirm: async () => {
      throw new Error('回执未知')
    }
  })
  await s.open(row())
  s.toggleItem(s.diff.value.items[0], true)
  s.comment.value = '原始意见'
  await s.confirmSelected()
  const first = calls.find((c) => c[0] === 'confirm')[2]
  assert.match(s.errorMessage.value, /回执未知/)
  assert.equal(s.canConfirm.value, false)
  await s.confirmSelected()
  assert.equal(calls.filter((c) => c[0] === 'confirm').length, 1)
  await s.open(row())
  assert.equal(s.pendingAttempt.value.data.idempotencyKey, first.idempotencyKey)
  assert.equal(s.comment.value, first.comment)
  assert.deepEqual(s.selectedIds.value, first.itemIds)
  assert.equal(s.canConfirm.value, true)
  await s.confirmSelected()
  assert.deepEqual(calls.filter((c) => c[0] === 'confirm')[1][2], first)
})
test('confirmed facts after unknown response clear request without replay', async () => {
  const { s, calls, set } = setup({
    confirm: async () => {
      throw new Error('超时')
    }
  })
  await s.open(row())
  s.toggleItem(s.diff.value.items[0], true)
  s.comment.value = '确认'
  await s.confirmSelected()
  set(row('173', 'PRECHECK_PASSED'), result('173', [item(377, 'CONFIRM_REQUIRED', true)], true))
  await s.reload()
  assert.equal(s.pendingAttempt.value, undefined)
  assert.equal(s.canSubmit.value, true)
  assert.equal(calls.filter((c) => c[0] === 'confirm').length, 1)
})
test('partial prior confirmation blocks replay instead of silently shrinking original payload', async () => {
  const { s, calls, set } = setup({
    diff: undefined,
    confirm: async () => {
      throw new Error('超时')
    }
  })
  set(row(), result('173', [item(1), item(2)]))
  await s.open(row())
  s.selectedIds.value = ['1', '2']
  s.comment.value = '确认'
  await s.confirmSelected()
  set(row(), result('173', [item(1, 'CONFIRM_REQUIRED', true), item(2)]))
  await s.reload()
  assert.match(s.errorMessage.value, /部分已确认/)
  assert.equal(s.canConfirm.value, false)
  assert.equal(calls.filter((c) => c[0] === 'confirm').length, 1)
})
for (const mode of ['absent', 'wrong', 'truncated'])
  test(`formal version ${mode} fails explicitly`, async () => {
    const page =
      mode === 'absent'
        ? { list: [], total: 0 }
        : mode === 'wrong'
          ? { list: [row('174')], total: 1 }
          : { list: [row()], total: 2 }
    const { s } = setup({ page: async () => page })
    await s.open(row())
    assert.match(s.errorMessage.value, /正式版本/)
    assert.equal(s.canConfirm.value, false)
    assert.equal(s.canSubmit.value, false)
  })
test('query failure is visible and refresh can recover', async () => {
  let fail = true
  const { s } = setup({
    diff: async () => {
      if (fail) throw { response: { data: { msg: '查询无权限' } } }
      return result()
    }
  })
  await s.open(row())
  assert.equal(s.errorMessage.value, '查询无权限')
  assert.equal(s.canSubmit.value, false)
  fail = false
  await s.reload()
  assert.equal(s.errorMessage.value, '')
})
test('diff identity mismatch fails', async () => {
  const { s } = setup({ diff: async () => result('174') })
  await s.open(row())
  assert.match(s.errorMessage.value, /不一致/)
  assert.equal(s.diff.value, undefined)
})
for (const outcome of ['success', 'failure'])
  test(`late prior version ${outcome} cannot overwrite latest dialog`, async () => {
    const old = deferred()
    const { s } = setup({
      page: async (p) => ({ list: [row(p.reportId.split('-')[1])], total: 1 }),
      diff: (id) => (id === '173' ? old.promise : result(id))
    })
    const first = s.open(row())
    await s.open(row('174'))
    if (outcome === 'success') old.resolve(result())
    else old.reject(new Error('OLD'))
    await first
    assert.equal(s.target.value.versionId, '174')
    assert.equal(s.diff.value.versionId, '174')
    assert.equal(s.errorMessage.value, '')
    assert.equal(s.loading.value, false)
  })
test('approval requires both formal PRECHECK_PASSED and backend approvalReady', async () => {
  const { s, set, calls } = setup()
  set(row('173', 'PRECHECK_PASSED'), result('173', [], false))
  await s.open(row())
  assert.equal(s.canSubmit.value, false)
  set(row(), result('173', [], true))
  await s.reload()
  assert.equal(s.canSubmit.value, false)
  await s.submitApproval()
  assert.equal(
    calls.some((c) => c[0] === 'submit'),
    false
  )
})
test('approval failure visible and disables repeat until refreshed formal state', async () => {
  const { s, set, calls } = setup({
    submit: async () => {
      throw new Error('审批前置不满足')
    }
  })
  set(row('173', 'PRECHECK_PASSED'), result('173', [], true))
  await s.open(row())
  await s.submitApproval()
  assert.equal(s.errorMessage.value, '审批前置不满足')
  assert.equal(s.canSubmit.value, false)
  await s.submitApproval()
  assert.equal(calls.filter((c) => c[0] === 'submit').length, 1)
})
test('missing formal version fails before queries', async () => {
  const { s, calls } = setup()
  await s.open({ reportId: 'x' })
  assert.match(s.errorMessage.value, /缺少正式/)
  assert.deepEqual(calls, [])
})
test('entry, permissions and explicit actions remain in existing list without governance menu', () => {
  const page = read('src/views/mes/pro/batchrecordformlist/index.vue')
  assert.match(
    page,
    /data-batch-record-version-review-entry[\s\S]*versionReviewDialog\?\.open\(selectedReport\)/
  )
  assert.match(page, /v-hasPermi="\['mes:pro-batch-record-version:query'\]"/)
  assert.match(page, /<BatchRecordVersionReviewDialog ref="versionReviewDialog" @changed="getList"/)
  assert.match(source, /data-version-review-confirm/)
  assert.match(source, /data-version-review-submit/)
  assert.match(source, /责任方类型为迁移项的正式业务信息/)
  assert.doesNotMatch(source, /approval\/callback|sourceLogicalKey\s*=/)
})

test('formal API wrappers preserve backend paths, version identity, body and local errors', async () => {
  const calls = []
  const apiSource = read('src/api/mes/pro/batchrecordreport/versionGovernance.ts')
  const reportSource = read('src/api/mes/pro/batchrecordreport/index.ts')
  const extract = (text) => {
    const ast = ts.createSourceFile('api.ts', text, ts.ScriptTarget.Latest, true)
    return ts.transpileModule(
      ast.statements
        .filter((node) => !ts.isImportDeclaration(node))
        .map((node) => node.getText(ast).replace(/^export /, ''))
        .join('\n'),
      { compilerOptions: { target: ts.ScriptTarget.ES2022 } }
    ).outputText
  }
  const request = {
    get: async (config) => {
      calls.push(config)
      return { versionId: '173' }
    },
    post: async (config) => {
      calls.push(config)
      return { versionId: '173' }
    }
  }
  const governance = Function(
    'request',
    `${extract(apiSource)}; return BatchRecordVersionGovernanceApi`
  )(request)
  const report = Function(
    'request',
    `${extract(reportSource)}; return BatchRecordReportApi`
  )(request)
  const payload = { itemIds: ['377'], comment: '已核对', idempotencyKey: 'original-key' }
  await governance.getMigrationDiff('173')
  await governance.confirmMigration('173', payload)
  await report.submitBatchRecordVersionApproval('173', { ignoreErrorMessage: true })
  assert.deepEqual(
    calls.map((call) => call.params),
    [{ versionId: '173' }, { versionId: '173' }, { versionId: '173' }]
  )
  assert.deepEqual(
    calls.map((call) => call.url),
    [
      '/mes/pro/batch-record-version/governance/migration-diff',
      '/mes/pro/batch-record-version/governance/migration-confirm',
      '/mes/pro/batch-record-report/version-approval/submit'
    ]
  )
  assert.strictEqual(calls[1].data, payload)
  assert.ok(calls.every((call) => call.ignoreErrorMessage === true))
  const failure = new Error('正式拒绝')
  const failed = Function(
    'request',
    `${extract(apiSource)}; return BatchRecordVersionGovernanceApi`
  )({
    post: async () => {
      throw failure
    }
  })
  await assert.rejects(failed.confirmMigration('173', payload), (error) => error === failure)
})
for (const invalid of ['duplicate', 'missing-confirmed', 'missing-count'])
  test(`malformed ${invalid} migration facts fail explicitly`, async () => {
    const value = result()
    if (invalid === 'duplicate') value.items = [item(), item()]
    if (invalid === 'missing-confirmed') delete value.items[0].confirmed
    if (invalid === 'missing-count') delete value.blockerCount
    const { s } = setup({ diff: async () => value })
    await s.open(row())
    assert.match(s.errorMessage.value, /不一致/)
    assert.equal(s.canConfirm.value, false)
    assert.equal(s.canSubmit.value, false)
  })
test('invalid open during a previous read exposes error without stranded loading', async () => {
  const old = deferred()
  const { s } = setup({ diff: () => old.promise })
  const first = s.open(row())
  await s.open({ reportId: 'missing-version' })
  assert.equal(s.loading.value, false)
  assert.match(s.errorMessage.value, /缺少正式/)
  old.resolve(result())
  await first
  assert.equal(s.diff.value, undefined)
})

for (const status of ['APPROVED', 'OBSOLETE', 'VOIDED', 'PENDING_APPROVAL', 'UNKNOWN_STATUS'])
  test(`${status} formal version is readonly even with unconfirmed migration items`, async () => {
    const { s, calls, set } = setup()
    set(row('173', status), result('173', [item()], true))
    await s.open(row('173', status))
    assert.equal(s.canSelectItem(s.diff.value.items[0]), false)
    s.toggleItem(s.diff.value.items[0], true)
    assert.deepEqual(s.selectedIds.value, [])
    s.selectedIds.value = ['377']
    s.comment.value = '核对意见'
    assert.equal(s.canConfirm.value, false)
    assert.equal(s.canSubmit.value, false)
    await s.confirmSelected()
    await s.submitApproval()
    assert.equal(
      calls.some((c) => ['confirm', 'submit'].includes(c[0])),
      false
    )
  })

test('preserved unknown confirmation cannot replay after version enters terminal status', async () => {
  const { s, calls, set } = setup({
    confirm: async () => {
      throw new Error('回执未知')
    }
  })
  await s.open(row())
  s.toggleItem(s.diff.value.items[0], true)
  s.comment.value = '原始核对意见'
  await s.confirmSelected()
  const payload = s.pendingAttempt.value.data
  set(row('173', 'APPROVED'), result())
  await s.open(row('173', 'APPROVED'))
  assert.deepEqual(s.pendingAttempt.value.data, payload)
  assert.equal(s.canConfirm.value, false)
  assert.match(s.notice.value, /禁止重试/)
  await s.confirmSelected()
  assert.equal(calls.filter((c) => c[0] === 'confirm').length, 1)
})

test('backend responsibility and difference enums have Chinese labels; unknown codes stay visible', () => {
  const { s } = setup()
  assert.equal(s.ownerLabel('DCC_OWNER'), 'DCC文控责任方')
  assert.equal(s.ownerLabel('PROCESS_OWNER'), '工艺责任方')
  assert.equal(s.ownerLabel('QUALITY_OWNER'), '质量责任方')
  assert.equal(s.ownerLabel('SYSTEM'), '系统')
  for (const code of [
    'TABLE',
    'PROCESS',
    'FIELD',
    'SIGNATURE_CELL',
    'ATTACHMENT_RULE',
    'CELL_RULE',
    'FIRST_IMPORT',
    'TABLE_STRUCTURE_RECONCILED',
    'PROCESS_ROUTE_REBOUND',
    'FIELD_MAPPING_REVIEWED',
    'SIGNATURE_CELL_REVIEW_REQUIRED',
    'ATTACHMENT_RULE_RECONCILED',
    'CELL_RULE_RECONCILED'
  ]) {
    assert.notEqual(s.diffLabel(code), code)
  }
  assert.equal(s.ownerLabel('UNKNOWN_OWNER'), 'UNKNOWN_OWNER')
  assert.equal(s.diffLabel('UNKNOWN_DIFF'), 'UNKNOWN_DIFF')
  assert.equal(s.statusLabel('UNKNOWN_STATUS'), 'UNKNOWN_STATUS')
})
