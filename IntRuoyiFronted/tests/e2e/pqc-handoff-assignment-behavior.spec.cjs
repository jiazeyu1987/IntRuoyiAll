const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { test } = require('node:test')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileTemplate, compileScript } = require('vue/compiler-sfc')
const root = path.resolve(__dirname, '../..')
const file = 'src/views/mes/pro/handoff/PqcHandoffAssignmentConfig.vue'
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8')
const descriptor = parse(read(file)).descriptor
const queryPermission = 'mes:pro-edhr-work-task-rule:query'
const updatePermission = 'mes:pro-edhr-work-task-rule:update'
const deferred = () => {
  let resolve, reject
  const promise = new Promise((ok, bad) => { resolve = ok; reject = bad })
  return { promise, resolve, reject }
}
const rule = (id = 10, owner = 344) => ({ id, candidateSourceType: 'USER', candidateSourceId: owner, candidateLabel: '正式检验员 ' + owner, enabled: true, remark: '原配置', candidateUserSnapshot: String(owner), handlerLeaderUserIds: [343] })
function panel(overrides = {}, permissions = [queryPermission, updatePermission]) {
  const mounted = [], stops = [], messages = [], writes = [], optionReads = []
  const api = {
    pqcRouteOptions: async () => { optionReads.push('route'); return [{ id: 98, label: '正式路线' }] },
    pqcRoleOptions: async () => { optionReads.push('role'); return [{ id: 27, label: '正式检验角色' }] },
    pqcUserOptions: async () => [{ id: 344, label: '正式检验员 344' }],
    pqcAssignment: async () => rule(),
    savePqcAssignment: async data => { writes.push(data); return rule() },
    ...overrides
  }
  const bindings = {
    ...vue, api, useMessage: () => ({ success: text => messages.push(text) }),
    hasPermission: requested => requested.some(permission => permissions.includes(permission)),
    onMounted: callback => mounted.push(callback),
    watch: (...args) => { const stop = vue.watch(...args); stops.push(stop); return stop }
  }
  const ast = ts.createSourceFile('actual.ts', descriptor.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const script = ast.statements.filter(s => !ts.isImportDeclaration(s)).map(s => s.getText(ast)).join('\n')
  const code = ts.transpileModule(script, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  const names = ['routes', 'roles', 'routeId', 'sourceId', 'sourceType', 'reason', 'enabled', 'ruleId', 'userOptions', 'readyToSave', 'error', 'loading', 'saving', 'searching', 'save', 'loadRule', 'searchUsers', 'changeSource']
  const values = Function(...Object.keys(bindings), code + '; return {' + names.join(',') + '}')(...Object.values(bindings))
  return { ...values, writes, messages, optionReads, mount: async () => { for (const callback of mounted) await callback() }, selectRoute: async id => { values.routeId.value = id; await vue.nextTick(); await values.loadRule() }, close: () => stops.forEach(stop => stop()) }
}
test('实际独立 PQC SFC、父页和响应式并列入口均可编译', () => {
  for (const relative of [file, 'src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue']) {
    const parsed = parse(read(relative)).descriptor
    assert.deepEqual(compileTemplate({ source: parsed.template.content, filename: relative, id: 'pqc-config' }).errors, [])
    assert.doesNotThrow(() => compileScript(parsed, { id: 'pqc-config' }))
  }
  assert.match(descriptor.template.content, /PQC 检验接手人员配置/)
  assert.match(descriptor.template.content, /PQC 组长由正式人员关系确定/)
  assert.match(descriptor.template.content, /已创建的任务保留原责任快照/)
  const parent = read('src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')
  assert.match(parent, /edhr-ncr__assignments/)
  assert.match(parent, /grid-template-columns: repeat\(2, minmax\(0, 1fr\)\)/)
  assert.match(parent, /@media[^{]*\{\s*\.edhr-ncr__assignments\s*\{\s*grid-template-columns: 1fr/)
})
test('规则查询权限才能渲染 PQC 卡，原 QA 卡仍使用其原守卫', () => {
  const parent = parse(read('src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue')).descriptor
  const find = (node, tag) => {
    if (node.type === 1 && node.tag === tag) return node
    for (const child of node.children || []) { const found = find(child, tag); if (found) return found }
  }
  const node = find(parent.template.ast, 'PqcHandoffAssignmentConfig')
  const ast = ts.createSourceFile('parent.ts', parent.scriptSetup.content, ts.ScriptTarget.Latest, true)
  const statement = ast.statements.find(s => ts.isVariableStatement(s) && s.declarationList.declarations.some(d => d.name.getText(ast) === 'canConfigurePqcAssignment'))
  assert.ok(node); assert.ok(statement)
  const code = ts.transpileModule(statement.getText(ast), { compilerOptions: { target: ts.ScriptTarget.ES2022 } }).outputText
  const rendered = compileTemplate({ source: node.loc.source, filename: 'actual-entry.vue', id: 'actual-entry', compilerOptions: { mode: 'function', prefixIdentifiers: false, cacheHandlers: false } })
  assert.deepEqual(rendered.errors, [])
  const render = Function('Vue', rendered.code)({ ...vue, resolveComponent: () => 'pqc-config' })
  for (const permissions of [[], [updatePermission], ['mes:pro-edhr-nonconformance-review:dispose'], [queryPermission]]) {
    const canConfigurePqcAssignment = Function('computed', 'hasPermission', code + ';return canConfigurePqcAssignment')(vue.computed, requested => requested.some(p => permissions.includes(p))).value
    assert.equal(render({ canConfigurePqcAssignment }, []).type, permissions.includes(queryPermission) ? 'pqc-config' : vue.Comment)
  }
  assert.match(find(parent.template.ast, 'QaHandoffAssignmentConfig').loc.source, /v-if="canConfigureQaAssignment"/)
})
test('实际 API 五端点严格使用 PQC URL/参数且不调用 QA 保存', async () => {
  const exports = {}, calls = [], module = { exports }
  const request = { get: async data => { calls.push({ method: 'GET', ...data }); return null }, post: async data => { calls.push({ method: 'POST', ...data }); return rule() } }
  const code = ts.transpileModule(read('src/api/mes/pro/handoff/index.ts'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
  Function('exports', 'module', 'require', code)(exports, module, name => { assert.equal(name, '@/config/axios'); return { default: request } })
  const api = module.exports, data = { routeId: 98, candidateSourceType: 'USER', candidateSourceId: 344, enabled: true, reason: '本路线检验接手', expectedRuleId: 10 }
  await api.pqcRouteOptions(); await api.pqcUserOptions('检验员'); await api.pqcRoleOptions(); await api.pqcAssignment(98); await api.savePqcAssignment(data)
  const base = '/mes/pro/active-order-handoff/'
  assert.deepEqual(calls, [
    { method: 'GET', url: base + 'pqc-route-options' },
    { method: 'GET', url: base + 'pqc-user-options', params: { keyword: '检验员' } },
    { method: 'GET', url: base + 'pqc-role-options' },
    { method: 'GET', url: base + 'pqc-assignment', params: { routeId: 98 } },
    { method: 'POST', url: base + 'pqc-assignment', data }
  ])
})
test('正式选项初始化、原用户姓名回显、精确旧版本保存且不发送只读责任摘要', async () => {
  const p = panel(); await p.mount(); await p.selectRoute(98)
  assert.deepEqual(p.optionReads, ['route', 'role']); assert.deepEqual(p.userOptions.value, [{ id: 344, label: '正式检验员 344' }])
  p.reason.value = '  明确本路线的检验接手人员  '; await p.save()
  assert.deepEqual(p.writes, [{ routeId: 98, candidateSourceType: 'USER', candidateSourceId: 344, enabled: true, reason: '明确本路线的检验接手人员', expectedRuleId: 10 }])
  assert.deepEqual(p.messages, ['PQC 接手人员已保存']); assert.equal(p.reason.value, ''); p.close()
})
test('未配置路线新增使用 null expectedRuleId，禁用规则仍按原版本保存', async () => {
  const p = panel({ pqcAssignment: async () => null }); await p.mount(); await p.selectRoute(98)
  p.sourceId.value = 344; p.reason.value = '首次配置'; p.enabled.value = false; await p.save()
  assert.equal(p.writes[0].expectedRuleId, null); assert.equal(p.writes[0].enabled, false); p.close()
})
test('直接保存处理器在没有更新权限时禁止 POST', async () => {
  const p = panel({}, [queryPermission]); await p.mount(); await p.selectRoute(98); p.reason.value = '不可越权'; await p.save()
  assert.equal(p.writes.length, 0); assert.match(p.error.value, /没有配置更新权限/)
  assert.match(descriptor.template.content, /v-hasPermi="\['mes:pro-edhr-work-task-rule:update'\]"/); p.close()
})
test('正式配置读取失败不能通过已填写人员和原因绕过', async () => {
  const p = panel({ pqcAssignment: async () => { throw Error('正式配置读取失败') } }); await p.mount(); await p.selectRoute(98)
  assert.equal(p.error.value, '正式配置读取失败'); assert.equal(p.readyToSave.value, false)
  p.sourceId.value = 344; p.reason.value = '无效重试'; await p.save(); assert.equal(p.writes.length, 0); p.close()
})
test('正式路线或角色选项初始化失败保持显式错误并阻止保存', async () => {
  const p = panel({ pqcRoleOptions: async () => { throw Error('正式角色选项读取失败') } }); await p.mount()
  assert.equal(p.error.value, '正式角色选项读取失败'); await p.selectRoute(98); p.reason.value = '不能跳过前置'; await p.save()
  assert.equal(p.readyToSave.value, false); assert.equal(p.writes.length, 0); p.close()
})
test('空负责人、空原因或未完成路线加载不发送 POST', async () => {
  const p = panel(); await p.mount(); await p.save(); assert.equal(p.writes.length, 0); await p.selectRoute(98)
  p.reason.value = '   '; await p.save(); p.sourceId.value = undefined; p.reason.value = '需要负责人'; await p.save()
  assert.equal(p.writes.length, 0); p.close()
})
test('迟到 A 路线读取不覆盖 B 路线的负责人和 expectedRuleId', async () => {
  const a = deferred(), b = deferred(), p = panel({ pqcAssignment: id => id === 98 ? a.promise : b.promise }); await p.mount()
  p.routeId.value = 98; const first = p.loadRule(); p.routeId.value = 99; const second = p.loadRule()
  b.resolve(rule(11, 349)); await second; a.resolve(rule(10, 344)); await first
  assert.equal(p.ruleId.value, 11); assert.equal(p.sourceId.value, 349); assert.deepEqual(p.userOptions.value, [{ id: 349, label: '正式检验员 349' }]); p.close()
})
test('责任来源切换清空旧候选并拒绝旧 USER 查询响应，角色保存保持正式来源', async () => {
  const result = deferred(), p = panel({ pqcUserOptions: () => result.promise }); await p.mount(); await p.selectRoute(98)
  const searched = p.searchUsers('A'); p.sourceType.value = 'ROLE_GROUP'; p.changeSource()
  assert.equal(p.sourceId.value, undefined); result.resolve([{ id: 350, label: '迟到检验员' }]); await searched
  assert.deepEqual(p.userOptions.value, []); assert.equal(p.searching.value, false)
  p.sourceId.value = 27; p.reason.value = '由正式检验角色接手'; await p.save(); assert.equal(p.writes[0].candidateSourceType, 'ROLE_GROUP'); assert.equal(p.writes[0].candidateSourceId, 27); p.close()
})
test('查询失败保持原已选人员并显示错误，不伪装查询成功', async () => {
  const p = panel({ pqcUserOptions: async () => { throw Error('正式用户查询失败') } }); await p.mount(); await p.selectRoute(98); await p.searchUsers('未知')
  assert.equal(p.error.value, '正式用户查询失败'); assert.deepEqual(p.userOptions.value, [{ id: 344, label: '正式检验员 344' }]); p.close()
})
test('连续保存只能一次 POST；迟到 A 保存/用户搜索不污染 B 配置或显示成功', async () => {
  const saved = deferred(), searched = deferred(), requests = []
  const p = panel({ pqcAssignment: async id => rule(id, id + 200), savePqcAssignment: data => { requests.push(data); return saved.promise }, pqcUserOptions: () => searched.promise })
  await p.mount(); await p.selectRoute(98); p.reason.value = '保存 A'; const saving = p.save(); await p.save(); const search = p.searchUsers('A')
  await p.selectRoute(99); assert.equal(p.readyToSave.value, false); p.reason.value = '保存 B'; await p.save(); assert.equal(requests.length, 1)
  saved.resolve(rule(500, 344)); searched.resolve([{ id: 344, label: '迟到 A' }]); await Promise.all([saving, search])
  assert.equal(p.ruleId.value, 99); assert.equal(p.sourceId.value, 299); assert.deepEqual(p.messages, []); assert.equal(p.saving.value, false); p.close()
})
test('保存冲突/失败保留原版本和用户输入，显示正式错误且不报成功', async () => {
  const p = panel({ savePqcAssignment: async () => { throw Error('配置已被其他管理员修改') } }); await p.mount(); await p.selectRoute(98); p.reason.value = '保留本次原因'; await p.save()
  assert.equal(p.ruleId.value, 10); assert.equal(p.reason.value, '保留本次原因'); assert.equal(p.error.value, '配置已被其他管理员修改'); assert.deepEqual(p.messages, []); assert.equal(p.saving.value, false); p.close()
})
