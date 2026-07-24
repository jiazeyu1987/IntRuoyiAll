const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260615_mes_scheduler_workbench_only_summary.sql'
)

assert(fs.existsSync(migrationPath), '排产员工作台概览收敛和排产看板隐藏迁移 SQL 必须存在。')

const migrationSql = fs.readFileSync(migrationPath, 'utf8')

assert.match(migrationSql, /SIGNAL SQLSTATE '45000'/, '迁移缺少 fail-fast 前置条件。')
assert.match(migrationSql, /`id`\s*=\s*900120/, '迁移必须依赖智能排产父菜单 900120。')
assert.match(migrationSql, /`id`\s*=\s*5590/, '迁移必须依赖排产员工作台菜单 5590。')
assert.match(
  migrationSql,
  /UPDATE `system_menu`[\s\S]*?`name` = '排产看板'[\s\S]*?`visible` = b'0'[\s\S]*?WHERE `id` = 5985;/,
  '排产看板必须隐藏但保留受控菜单记录。'
)

const expectedVisibleMenus = [
  { id: 5590, name: '排产员工作台', sort: 0 },
  { id: 5580, name: '排产工单', sort: 1 },
  { id: 5550, name: '报工', sort: 2 },
  { id: 5262, name: '排程日历', sort: 3 },
  { id: 900121, name: '工艺流程排产配置', sort: 4 },
  { id: 5540, name: '生产排产', sort: 5 },
  { id: 900104, name: '璞慧排产', sort: 6 }
]

for (const menu of expectedVisibleMenus) {
  assert.match(
    migrationSql,
    new RegExp(
      `UPDATE \`system_menu\`[\\s\\S]*?\`name\` = '${menu.name}'[\\s\\S]*?\`sort\` = ${menu.sort}[\\s\\S]*?\`parent_id\` = 900120[\\s\\S]*?\`visible\` = b'1'[\\s\\S]*?WHERE \`id\` = ${menu.id};`
    ),
    `菜单 ${menu.name} 必须保持可见并调整为隐藏排产看板后的顺序。`
  )
}

console.log('PASS: MES scheduler workbench only summary static contract')
