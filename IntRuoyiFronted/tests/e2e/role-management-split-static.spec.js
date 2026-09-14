const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const remainingSource = read('src/router/modules/remaining.ts')
const breadcrumbSource = read('src/layout/components/Breadcrumb/src/Breadcrumb.vue')
const rolePageSource = read('src/views/system/role/index.vue')
const roleFormSource = read('src/views/system/role/RoleForm.vue')
const roleAssignMenuSource = read('src/views/system/role/RoleAssignMenuForm.vue')
const roleDataPermissionSource = read('src/views/system/role/RoleDataPermissionForm.vue')
const postPageSource = read('src/views/system/post/index.vue')
const postFormSource = read('src/views/system/post/PostForm.vue')
const approvalRolePageSource = read('src/views/dcc/controlled-file/positions/index.vue')

for (const token of [
  "path: '/system/post'",
  "path: 'controlled-file/positions'",
  "title: '组织角色'",
  "title: '审批角色'",
  "activeMenu: '/system/role/organization-role'",
  "activeMenu: '/system/role/approval-role'"
]) {
  assert.ok(remainingSource.includes(token), `remaining.ts 必须提供新角色管理兼容路由契约：${token}`)
}

assert.ok(
  remainingSource.includes("path: '/system/post'"),
  'remaining.ts 必须保留旧地址兼容入口：/system/post'
)
assert.ok(
  remainingSource.includes("path: 'controlled-file/positions'"),
  'remaining.ts 必须保留旧地址兼容入口：/dcc/controlled-file/positions'
)

for (const token of [
  'currentRoute.value.meta?.activeMenu',
  'currentRoute.value.matched.slice(-1)[0].path'
]) {
  assert.ok(breadcrumbSource.includes(token), `Breadcrumb 必须优先使用 activeMenu 定位面包屑：${token}`)
}

for (const forbiddenToken of [
  'label="角色名称"',
  'placeholder="请输入角色名称"',
  'label="角色标识"',
  'placeholder="请输入角色标识"',
  'label="角色编号"',
  "'角色数据.xls'"
]) {
  assert.ok(!rolePageSource.includes(forbiddenToken), `权限角色页不得继续展示旧角色管理文案：${forbiddenToken}`)
}

for (const requiredToken of [
  '权限角色',
  '控制菜单与数据权限',
  '权限角色名称',
  '权限角色标识',
  '权限角色编号',
  '权限角色配置包.json',
  '菜单权限',
  '数据权限'
]) {
  assert.ok(rolePageSource.includes(requiredToken), `权限角色页必须展示：${requiredToken}`)
}

for (const requiredToken of [
  '权限角色名称',
  '请输入权限角色名称',
  '权限角色标识',
  '请输入权限角色标识'
]) {
  assert.ok(roleFormSource.includes(requiredToken), `权限角色表单必须展示：${requiredToken}`)
}

for (const token of ['权限角色名称', '权限角色标识']) {
  assert.ok(roleAssignMenuSource.includes(token), `菜单权限弹窗必须改名：${token}`)
  assert.ok(roleDataPermissionSource.includes(token), `数据权限弹窗必须改名：${token}`)
}

for (const token of [
  '父子联动(选中父节点，自动选择子节点):',
  ':check-strictly="!checkStrictly"',
  'const checkStrictly = ref(true)',
  'checkStrictly.value = true'
]) {
  assert.ok(roleAssignMenuSource.includes(token), `菜单权限弹窗必须支持最小权限父子联动控制：${token}`)
}

for (const forbiddenToken of [
  'label="岗位名称"',
  'placeholder="请输入岗位名称"',
  'label="岗位编码"',
  'placeholder="请输入岗位编码"',
  'label="岗位编号"',
  "'岗位列表.xls'"
]) {
  assert.ok(!postPageSource.includes(forbiddenToken), `组织角色页不得继续展示旧岗位管理文案：${forbiddenToken}`)
}

for (const requiredToken of [
  '组织角色',
  '控制组织岗位归属',
  '组织角色名称',
  '组织角色编码',
  '组织角色编号',
  '组织角色配置包.json'
]) {
  assert.ok(postPageSource.includes(requiredToken), `组织角色页必须展示：${requiredToken}`)
}

for (const requiredToken of [
  '组织角色名称',
  '请输入组织角色名称',
  '组织角色编码',
  '请输入组织角色编码'
]) {
  assert.ok(postFormSource.includes(requiredToken), `组织角色表单必须展示：${requiredToken}`)
}

for (const forbiddenToken of ['新增岗位', '岗位编码', '岗位名称', '当前岗位', '岗位分配']) {
  assert.ok(!approvalRolePageSource.includes(forbiddenToken), `审批角色页不得继续展示旧 DCC岗位分配语义：${forbiddenToken}`)
}

for (const requiredToken of [
  '审批角色',
  '控制 DCC 审批语义与分配',
  '审批角色编码',
  '审批角色名称',
  '新增审批角色',
  '保存审批角色',
  '当前审批角色',
  '审批角色分配'
]) {
  assert.ok(approvalRolePageSource.includes(requiredToken), `审批角色页必须展示：${requiredToken}`)
}

console.log('PASS: role management split rename static contract')
