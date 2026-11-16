const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')
const source = fs.readFileSync(path.resolve(__dirname, '../../src/store/modules/permission.ts'), 'utf8')
const code = source.slice(0, source.indexOf('export const usePermissionStore'))
  .replace(/^import .*from .*$/gm, '')
  .replace('const { wsCache } = useCache()', '')
const js = ts.transpileModule(code, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS } }).outputText
const merge = new Function('cloneDeep', 'showroomRoutes', 'exports', js + '\nreturn mergeApprovalCenterRoute')(
  (value) => structuredClone(value), [], {}
)
const policy = { path: 'business-approval-policy', name: 'ApprovalCenterBpmBusinessApprovalPolicy', meta: { hidden: false, permission: ['bpm:business-approval-policy:query'] } }
const model = { path: 'model', name: 'ApprovalCenterBpmModel', meta: { hidden: false } }
const staticRoute = { path: '/approval-center', name: 'ApprovalCenter', children: [
  { path: 'cc', name: 'ApprovalCenterCc' },
  { path: 'manager', name: 'ApprovalCenterWorkflowManagement', children: [model, policy,
    { path: 'form', name: 'ApprovalCenterBpmForm', meta: { hidden: true } }] },
  { path: 'oa', name: 'ApprovalCenterOaExample', meta: { hidden: true } }
] }
const dynamic = (children) => ({ path: '/approval-center', children: [
  { path: 'cc', name: 'ApprovalCenterCc' },
  { path: 'manager', name: 'ApprovalCenterWorkflowManagement', children }
] })

test('管理员已有策略授权时，在流程管理内显示模型和业务审批策略，其他管理入口保持隐藏', () => {
  const before = structuredClone(staticRoute)
  const result = merge(staticRoute, dynamic([model, policy, { path: 'form', meta: { hidden: false } }]))
  const manager = result.children.find((r) => r.name === 'ApprovalCenterWorkflowManagement')
  assert.equal(manager.meta.hidden, false)
  assert.equal(manager.meta.alwaysShow, true)
  assert.deepEqual(manager.children.filter((r) => !r.meta?.hidden).map((r) => r.path), ['model', 'business-approval-policy'])
  assert.equal(result.children[1], manager)
  assert.deepEqual(staticRoute, before)
})

test('只有策略查询授权时仍能进入流程管理，重定向至策略页', () => {
  const result = merge(staticRoute, dynamic([policy]))
  const manager = result.children.find((r) => r.name === 'ApprovalCenterWorkflowManagement')
  assert.equal(manager.meta.hidden, false)
  assert.equal(manager.redirect, '/approval-center/manager/business-approval-policy')
})

test('普通用户没有策略授权时不新增策略入口，流程模型保留原位置', () => {
  const result = merge(staticRoute, dynamic([model]))
  const manager = result.children.find((r) => r.name === 'ApprovalCenterWorkflowManagement')
  assert.equal(manager.meta.hidden, true)
  assert.equal(manager.children.some((r) => r.name === policy.name), false)
  assert.equal(result.children.find((r) => r.name === model.name).path, 'manager/model')
})
