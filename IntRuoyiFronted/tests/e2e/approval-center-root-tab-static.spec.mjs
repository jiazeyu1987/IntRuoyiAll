import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const routes = read('src/router/modules/remaining.ts')
const workflowMenuSql = read('../ruoyi-vue-pro/sql/mysql/20260714_approval_center_workflow_menu_consolidation.sql')
const page = read('src/views/approval-center/index.vue')
const tabMenu = read('src/layout/components/TabMenu/src/TabMenu.vue')
const tagsViewStore = read('src/store/modules/tagsView.ts')

assert.match(routes, /path:\s*'\/approval-center'/, 'approval center root route must exist')
assert.match(routes, /redirect:\s*'\/approval-center\/todo'/, 'approval center root route must redirect to todo child route')
assert.match(routes, /title:\s*'审批中心'/, 'approval center root route title must be 审批中心')
assert.doesNotMatch(routes, /path:\s*'\/approval-center'[\s\S]{0,120}hidden:\s*true/, 'approval center root route must be visible in first-level menu')
assert.match(routes, /path:\s*'\/approval-center'[\s\S]{0,260}alwaysShow:\s*true/, 'approval center root route must stay visible as a first-level menu shell')

for (const routePath of ['todo', 'done', 'my-initiated', 'cc']) {
  assert.match(routes, new RegExp(`path:\\s*'${routePath}'`), `approval center must declare child route ${routePath}`)
  assert.doesNotMatch(
    routes,
    new RegExp(`path:\\s*'${routePath}'[\\s\\S]{0,140}hidden:\\s*true`),
    `approval center child route ${routePath} must be visible in second-level menu`
  )
}
assert.doesNotMatch(routes, /path:\s*'signature-pending'|title:\s*'签名待处理'/, 'approval center must not expose a separate signature pending child route')
assert.match(routes, /tagsViewKey:\s*'\/approval-center'/, 'approval center child routes must share one unified tags view key')
assert.match(routes, /tagsViewTitle:\s*'审批中心'/, 'approval center child routes must render unified tags view title')

const expectedApprovalChildOrder = [
  "title: '流程管理'",
  "title: '待办'",
  "title: '已办'",
  "title: '我发起的'",
  "title: '抄送我的'"
]
let previousApprovalChildIndex = -1
for (const childTitle of expectedApprovalChildOrder) {
  const currentIndex = routes.indexOf(childTitle)
  assert.ok(currentIndex > previousApprovalChildIndex, `approval center child order must include ${childTitle}`)
  previousApprovalChildIndex = currentIndex
}

for (const [routePath, activePath, routeTitle, componentName] of [
  ['model', 'manager/model', '流程模型', 'ApprovalCenterBpmModel'],
  ['form', 'manager/form', '流程表单', 'ApprovalCenterBpmForm'],
  ['category', 'manager/category', '流程分类', 'ApprovalCenterBpmCategory'],
  ['user-group', 'manager/user-group', '用户分组', 'ApprovalCenterBpmUserGroup'],
  ['process-expression', 'manager/process-expression', '流程表达式', 'ApprovalCenterBpmProcessExpression']
]) {
  assert.match(routes, new RegExp(`path:\\s*'${routePath}'[\\s\\S]{0,260}name:\\s*'${componentName}'[\\s\\S]{0,260}title:\\s*'${routeTitle}'`), `approval center workflow management route ${routePath} must exist`)
  assert.match(routes, new RegExp(`path:\\s*'${routePath}'[\\s\\S]{0,360}activeMenu:\\s*'/approval-center/${activePath}'`), `approval center workflow management route ${routePath} must highlight its new menu path`)
}

assert.match(routes, /path:\s*'oa'[\s\S]{0,260}title:\s*'OA 示例'/, 'approval center may keep OA example route for deep-link compatibility')
assert.match(routes, /path:\s*'oa'[\s\S]{0,260}hidden:\s*true/, 'approval center OA example group must be hidden from the side menu')
assert.doesNotMatch(routes, /path:\s*'oa'[\s\S]{0,260}alwaysShow:\s*true/, 'approval center OA example group must not force a visible menu shell')
assert.match(routes, /path:\s*'leave'[\s\S]{0,260}name:\s*'ApprovalCenterOALeave'[\s\S]{0,260}title:\s*'请假查询'/, 'approval center OA leave route must remain for deep-link compatibility')
assert.match(routes, /path:\s*'leave'[\s\S]{0,320}hidden:\s*true/, 'approval center OA leave child route must be hidden from the side menu')
assert.doesNotMatch(routes, /activeMenu:\s*'\/approval-center\/oa\/leave'/, 'hidden OA routes must not point active menu back to the deleted side tab')
assert.match(routes, /path:\s*'\/bpm'[\s\S]{0,120}hidden:\s*true/, 'legacy /bpm shell must remain hidden for deep-link compatibility')
assert.doesNotMatch(routes, /path:\s*'\/bpm'[\s\S]{0,220}alwaysShow:\s*true/, 'legacy /bpm shell must not remain as a visible first-level menu')

for (const [legacyPath, activeMenu] of [
  ['manager/model', '/approval-center/manager/model'],
  ['manager/form', '/approval-center/manager/form'],
  ['manager/category', '/approval-center/manager/category'],
  ['manager/user-group', '/approval-center/manager/user-group'],
  ['manager/process-expression', '/approval-center/manager/process-expression'],
  ['task/todo', '/approval-center/todo'],
  ['task/done', '/approval-center/done'],
  ['task/copy', '/approval-center/cc'],
  ['process-instance/my', '/approval-center/my-initiated']
]) {
  assert.match(routes, new RegExp(`path:\\s*'${legacyPath}'[\\s\\S]{0,420}activeMenu:\\s*'${activeMenu}'`), `legacy /bpm deep link ${legacyPath} must highlight ${activeMenu}`)
}

for (const legacyOaPath of ['oa/leave', 'oa/leave/create', 'oa/leave/detail']) {
  assert.match(routes, new RegExp(`path:\\s*'${legacyOaPath}'[\\s\\S]{0,220}hidden:\\s*true`), `legacy /bpm deep link ${legacyOaPath} must stay hidden`)
}

assert.match(workflowMenuSql, /SET `name` = 'OA 示例'[\s\S]{0,260}`visible` = b'0'[\s\S]{0,120}`always_show` = b'0'[\s\S]{0,160}WHERE `id` = 5;/, 'approval center SQL must hide OA example menu id 5')
assert.match(workflowMenuSql, /SET `name` = '请假查询'[\s\S]{0,260}`visible` = b'0'[\s\S]{0,160}WHERE `id` = 1118;/, 'approval center SQL must hide OA leave child menu id 1118')

assert.match(page, /approvalTabNames/, 'approval center page must define child tab names')
assert.match(page, /approvalTabRoutes/, 'approval center page must define child tab route map')
assert.match(page, /resolveRouteTab/, 'approval center page must resolve child tab from route path')
assert.match(page, /syncRouteToCanonicalPath/, 'approval center page must normalize legacy query route to child path')
assert.match(page, /未知审批中心子页签/, 'approval center page must fail fast on unknown child tab')
assert.match(tabMenu, /syncMenuTabRoutersByRoute/, 'cut menu must define route-driven submenu sync logic')
assert.match(
  tabMenu,
  /watch\(\s*\[\(\) => unref\(currentRoute\)\.path, tabRouters, fixedMenu\]/,
  'cut menu must resync submenu when current route changes'
)
assert.match(
  tabMenu,
  /permissionStore\.setMenuTabRouters\(/,
  'cut menu must rebuild second-level menu entries from current top-level route'
)
assert.match(tagsViewStore, /getViewIdentity\(view: RouteLocationNormalizedLoaded\)/, 'tags view store must support unified tag identity')
assert.match(tagsViewStore, /tagsViewKey \|\| view\.fullPath/, 'tags view store must dedupe approval center child routes by shared tag key')
assert.match(tagsViewStore, /tagsViewTitle/, 'tags view store must support unified tag title rendering')

process.stdout.write('approval-center root tab static contract passed\n')
