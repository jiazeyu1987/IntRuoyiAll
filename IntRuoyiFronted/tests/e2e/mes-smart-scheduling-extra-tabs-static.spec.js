const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260611_mes_smart_scheduling_extra_tabs.sql'
)

assert(fs.existsSync(migrationPath), '智能排产追加页签菜单迁移 SQL 必须存在。')

const migrationSql = fs.readFileSync(migrationPath, 'utf8')

assert.match(migrationSql, /SIGNAL SQLSTATE '45000'/, '追加页签迁移缺少 fail-fast 前置条件。')
assert.match(migrationSql, /`id`\s*=\s*900120/, '迁移必须依赖已存在的智能排产父菜单 900120。')

const requiredMenus = [
  {
    id: 5540,
    name: '生产排产',
    permission: 'mes:pro-task:query',
    sort: 4,
    path: '/mes/pro/task',
    component: 'mes/pro/task/index',
    componentName: 'MesProTask'
  },
  {
    id: 900104,
    name: '璞慧排产',
    permission: 'mes:pro-puhui-schedule:query',
    sort: 5,
    path: '/mes/pro/puhui-schedule',
    component: 'mes/pro/puhui-schedule/index',
    componentName: 'MesProPuhuiSchedule'
  }
]

for (const menu of requiredMenus) {
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
  assert.match(migrationSql, new RegExp(`\`component\`\\s*=\\s*'${menu.component}'`))
  assert.match(migrationSql, new RegExp(`\`component_name\`\\s*=\\s*'${menu.componentName}'`))
}

assert.match(
  migrationSql,
  /JSON_CONTAINS\(CAST\(`menu_ids` AS JSON\), CAST\('900120' AS JSON\), '\$'\)/,
  '租户套餐必须基于智能排产父菜单授权合并追加页签。'
)
assert.match(
  migrationSql,
  /JOIN `system_menu` m ON m\.`id` IN \(900120, 5540, 5541, 900104\)/,
  '租户管理员角色必须补齐智能排产父菜单、生产排产、生产排产查询和璞慧排产绑定。'
)
assert.match(
  migrationSql,
  /`id` IN \(5540, 900104\)[\s\S]*`permission` = ''/,
  '迁移必须拒绝追加页签缺少权限标识的旧数据。'
)

console.log('PASS: MES smart scheduling extra tabs static contract')
