const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260611_mes_scheduler_workbench_smart_scheduling_tab.sql'
)

assert(fs.existsSync(migrationPath), '排产员工作台归入智能排产菜单迁移 SQL 必须存在。')

const migrationSql = fs.readFileSync(migrationPath, 'utf8')

assert.match(migrationSql, /SIGNAL SQLSTATE '45000'/, '迁移缺少 fail-fast 前置条件。')
assert.match(migrationSql, /`id`\s*=\s*900120/, '迁移必须依赖已存在的智能排产父菜单 900120。')

const expectedMenus = [
  { id: 5985, name: '排产看板', sort: 0, path: '/mes/home/index', permission: 'mes:home:query' },
  {
    id: 5590,
    name: '排产员工作台',
    sort: 1,
    path: '/mes/pro/scheduler-workbench',
    permission: 'mes:pro-scheduler-workbench:query',
    component: 'mes/pro/scheduler-workbench/index',
    componentName: 'MesProSchedulerWorkbench'
  },
  { id: 5580, name: '排产工单', sort: 2, path: '/mes/pro/schedule-order', permission: 'mes:pro-schedule-order:query' },
  { id: 5550, name: '报工', sort: 3, path: '/mes/pro/feedback', permission: 'mes:pro-feedback:query' },
  { id: 5262, name: '排程日历', sort: 4, path: '/mes/pro/schedule-calendar', permission: 'mes:pro-task:query' },
  { id: 5540, name: '生产排产', sort: 5, path: '/mes/pro/task', permission: 'mes:pro-task:query' },
  { id: 900104, name: '璞慧排产', sort: 6, path: '/mes/pro/puhui-schedule', permission: 'mes:pro-puhui-schedule:query' }
]

for (const menu of expectedMenus) {
  assert.match(
    migrationSql,
    new RegExp(`WHERE\\s+\`id\`\\s*=\\s*${menu.id}`),
    `菜单 ${menu.id} 必须通过固定 ID 更新。`
  )
  assert.match(migrationSql, new RegExp(`\`name\`\\s*=\\s*'${menu.name}'`))
  assert.match(migrationSql, new RegExp(`\`permission\`\\s*=\\s*'${menu.permission}'`))
  assert.match(migrationSql, /`parent_id`\s*=\s*900120/)
  assert.match(migrationSql, new RegExp(`\`sort\`\\s*=\\s*${menu.sort}`))
  assert.match(migrationSql, new RegExp(`\`path\`\\s*=\\s*'${menu.path}'`))
}

assert.match(migrationSql, /`component`\s*=\s*'mes\/pro\/scheduler-workbench\/index'/)
assert.match(migrationSql, /`component_name`\s*=\s*'MesProSchedulerWorkbench'/)
assert.match(
  migrationSql,
  /JSON_CONTAINS\(CAST\(`menu_ids` AS JSON\), CAST\('900120' AS JSON\), '\$'\)/,
  '租户套餐必须基于智能排产父菜单授权合并排产员工作台。'
)
assert.match(
  migrationSql,
  /JOIN `system_menu` m ON m\.`id` IN \(900120, 5590\)/,
  '租户管理员角色必须补齐智能排产父菜单和排产员工作台绑定。'
)
assert.match(
  migrationSql,
  /`id` = 5590[\s\S]*`permission` = ''/,
  '迁移必须拒绝排产员工作台缺少权限标识的旧数据。'
)

console.log('PASS: MES scheduler workbench smart scheduling tab static contract')
