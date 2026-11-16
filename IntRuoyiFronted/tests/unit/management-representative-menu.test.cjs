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
const filter = new Function('cloneDeep', 'showroomRoutes', 'exports', js + '\nreturn filterHiddenModuleMenus')(
  structuredClone, [], {}
)
const routes = [{ path: '/mes/pro', children: [
  { path: 'batch-record-form-list' },
  { path: 'feedback/edhr-batch-production-fill' },
  { path: '/mes/pro/feedback/edhr-batch-pqc-fill' },
  { path: 'production-release/pqc' },
  { path: 'feedback/edhr-batch-execution', meta: { title: '上市放行' } },
  { path: 'feedback/edhr-batch-history', meta: { title: '历史追溯' } }
] }]

test('管理者代表隐藏四个入口，保留上市放行、历史追溯，原始授权路由不变', () => {
  const before = structuredClone(routes)
  const result = filter(routes, ['MES_MANAGEMENT_REPRESENTATIVE'])
  assert.deepEqual(result[0].children.map(route => route.meta.title), ['上市放行', '历史追溯'])
  assert.deepEqual(routes, before)
})

test('其他岗位及管理员的四个入口保持原有可见性', () => {
  for (const roles of [[], ['MES_PQC_RELEASE_OWNER'], ['pqc_permission'], ['tenant_admin']]) {
    assert.deepEqual(filter(routes, roles), routes)
  }
})

test('管理者代表兼任其他角色时仍隐藏指定入口', () => {
  assert.equal(filter(routes, ['MES_MANAGEMENT_REPRESENTATIVE', 'edhr_auditor'])[0].children.length, 2)
})
