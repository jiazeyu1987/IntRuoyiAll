const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')

const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260611_mes_smart_scheduling_tabs.sql'
)
const routerHelperPath = path.join(
  workspaceRoot,
  'yudao-ui-admin-vue3/src/utils/routerHelper.ts'
)

assert(fs.existsSync(migrationPath), '智能排产页签菜单迁移 SQL 必须存在。')
assert(fs.existsSync(routerHelperPath), '前端动态菜单路由工具必须存在。')

const migrationSql = fs.readFileSync(migrationPath, 'utf8')
const routerHelperSource = fs.readFileSync(routerHelperPath, 'utf8')

assert.match(migrationSql, /SIGNAL SQLSTATE '45000'/, '菜单迁移缺少 fail-fast 前置条件。')
assert.match(migrationSql, /900120,\s*'智能排产'/, '必须创建智能排产父菜单 900120。')
assert.match(migrationSql, /'mes:pro-smart-scheduling:query'/, '智能排产父菜单必须有权限标识。')

const requiredMenus = [
  {
    id: 5985,
    name: '排产看板',
    permission: 'mes:home:query',
    sort: 0,
    path: '/mes/home/index',
    component: 'mes/home/index',
    componentName: 'MesHome'
  },
  {
    id: 5580,
    name: '排产工单',
    permission: 'mes:pro-schedule-order:query',
    sort: 1,
    path: '/mes/pro/schedule-order',
    component: 'mes/pro/scheduleorder/index',
    componentName: 'MesProScheduleOrder'
  },
  {
    id: 5550,
    name: '报工',
    permission: 'mes:pro-feedback:query',
    sort: 2,
    path: '/mes/pro/feedback',
    component: 'mes/pro/feedback/index',
    componentName: 'MesProFeedback'
  },
  {
    id: 5262,
    name: '排程日历',
    permission: 'mes:pro-task:query',
    sort: 3,
    path: '/mes/pro/schedule-calendar',
    component: 'mes/pro/task/calendar/index',
    componentName: 'MesCalProScheduleCalendar'
  }
]

for (const menu of requiredMenus) {
  assert.match(
    migrationSql,
    new RegExp(`WHERE\\s+\`id\`\\s*=\\s*${menu.id}`),
    `菜单 ${menu.id} 必须通过固定 ID 更新，避免误改同名菜单。`
  )
  assert.match(migrationSql, new RegExp(`\`name\`\\s*=\\s*'${menu.name}'`))
  assert.match(migrationSql, new RegExp(`\`permission\`\\s*=\\s*'${menu.permission}'`))
  assert.match(migrationSql, new RegExp(`\`parent_id\`\\s*=\\s*900120`))
  assert.match(migrationSql, new RegExp(`\`sort\`\\s*=\\s*${menu.sort}`))
  assert.match(migrationSql, new RegExp(`\`path\`\\s*=\\s*'${menu.path}'`))
  assert.match(migrationSql, new RegExp(`\`component\`\\s*=\\s*'${menu.component}'`))
  assert.match(migrationSql, new RegExp(`\`component_name\`\\s*=\\s*'${menu.componentName}'`))
}

assert.match(
  migrationSql,
  /JSON_CONTAINS\(CAST\(`menu_ids` AS JSON\), CAST\('5100' AS JSON\), '\$'\)/,
  '租户套餐必须基于 MES 系统父菜单授权合并智能排产菜单。'
)
assert.match(
  migrationSql,
  /JOIN `system_menu` m ON m\.`id` IN \(900120, 5985, 5580, 5581, 5550, 5551, 5262\)/,
  '租户管理员角色必须补齐智能排产父菜单、四个子菜单和查询权限菜单绑定。'
)
assert.match(
  migrationSql,
  /`id` IN \(5985, 5580, 5550, 5262\)[\s\S]*`permission` = ''/,
  '迁移必须拒绝目标页签缺少权限标识的旧数据。'
)
assert.match(
  routerHelperSource,
  /const generateRoutePath[\s\S]*path\.startsWith\('\/'\)[\s\S]*return path/,
  '动态路由重定向必须支持智能排产子页签保留绝对路径。'
)
assert.match(
  routerHelperSource,
  /export const pathResolve[\s\S]*path\.startsWith\('\/'\)[\s\S]*return path\.replace/,
  '菜单点击路径必须支持智能排产子页签保留绝对路径。'
)

console.log('PASS: MES smart scheduling tabs static contract')
