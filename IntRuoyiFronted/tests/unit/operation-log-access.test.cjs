const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')
const path = require('node:path')
const filename = path.resolve(__dirname, '../../src/utils/operationLogAccess.ts')
const context = { exports: {}, Set }
vm.runInNewContext(ts.transpileModule(fs.readFileSync(filename, 'utf8'), {
  compilerOptions: { module: ts.ModuleKind.CommonJS }
}).outputText, context, { filename })
const { canAccessOperationLogs: allowed, isOperationLogPath, filterOperationLogMenus: filter } = context.exports
for (const role of ['super_admin', 'tenant_admin', 'audit_admin', 'edhr_auditor']) assert.equal(allowed([role]), true)
for (const role of ['production_leader_permission', 'pqc_leader_permission', 'production_permission', 'dcc_project_reviewer', 'qa']) assert.equal(allowed([role]), false)
assert.equal(allowed([]), false)
assert.equal(allowed(['production_leader_permission', 'edhr_auditor']), true)
for (const route of ['/system/log/operate-log', '/system/operate-log/']) assert.equal(isOperationLogPath(route), true)
assert.equal(isOperationLogPath('/system/user'), false)
const menus = [{ path: '/system', children: [{ path: 'log', children: [
  { path: 'operate-log', name: 'SystemOperateLog' }, { path: 'other' }
] }, { path: 'user' }] }]
const result = filter(menus, ['production_leader_permission'])
assert.equal(result[0].children[0].children.length, 1)
assert.equal(result[0].children[0].children[0].path, 'other')
assert.equal(result[0].children[1].path, 'user')
assert.equal(filter(menus, ['edhr_auditor'])[0].children[0].children.length, 2)
assert.equal(menus[0].children[0].children.length, 2, 'Filtering must not mutate cached menus')
console.log('operation log role boundary: passed')
