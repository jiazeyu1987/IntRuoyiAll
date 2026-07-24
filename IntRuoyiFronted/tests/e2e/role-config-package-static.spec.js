const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const rolePageSource = read('src/views/system/role/index.vue')
const roleApiSource = read('src/api/system/role/index.ts')
const postPageSource = read('src/views/system/post/index.vue')
const postApiSource = read('src/api/system/post/index.ts')
const approvalRolePageSource = read('src/views/dcc/controlled-file/positions/index.vue')
const approvalRoleApiSource = read('src/api/dcc/controlledFile/approvalPositions.ts')

for (const token of [
  '导出配置包',
  '导入配置包',
  '权限角色配置包.json',
  'accept=".json"',
  'exportRoleConfigPackage',
  'importRoleConfigPackage'
]) {
  assert.ok(rolePageSource.includes(token) || roleApiSource.includes(token), `权限角色配置包契约缺失：${token}`)
}

for (const token of [
  '导出配置包',
  '导入配置包',
  '组织角色配置包.json',
  'accept=".json"',
  'exportPostConfigPackage',
  'importPostConfigPackage'
]) {
  assert.ok(postPageSource.includes(token) || postApiSource.includes(token), `组织角色配置包契约缺失：${token}`)
}

for (const token of [
  '导出配置包',
  '导入配置包',
  '审批角色配置包.json',
  'accept=".json"',
  'exportApprovalPositionConfigPackage',
  'importApprovalPositionConfigPackage'
]) {
  assert.ok(
    approvalRolePageSource.includes(token) || approvalRoleApiSource.includes(token),
    `审批角色配置包契约缺失：${token}`
  )
}

console.log('PASS: role config package static contract')
