const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')

const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260611_mes_edhr_batch_processing_tabs.sql'
)
const retireExecutionListMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260721_mes_edhr_execution_list_retire.sql'
)
const visibleTabsMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260702_mes_edhr_seven_visible_tabs.sql'
)
const formTraceMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260714_mes_edhr_form_trace_menu.sql'
)
const formFillLogMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260713_mes_edhr_form_fill_log_menu.sql'
)
const templateConfigRemovalMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260715_mes_edhr_template_config_menu_removal.sql'
)

for (const requiredPath of [
  migrationPath,
  retireExecutionListMigrationPath,
  visibleTabsMigrationPath,
  formTraceMigrationPath,
  formFillLogMigrationPath,
  templateConfigRemovalMigrationPath
]) {
  assert(fs.existsSync(requiredPath), `${path.basename(requiredPath)} 必须存在。`)
}

const migrationSql = fs.readFileSync(migrationPath, 'utf8')
const retireExecutionListSql = fs.readFileSync(retireExecutionListMigrationPath, 'utf8')
const visibleTabsSql = fs.readFileSync(visibleTabsMigrationPath, 'utf8')
const formTraceSql = fs.readFileSync(formTraceMigrationPath, 'utf8')
const formFillLogSql = fs.readFileSync(formFillLogMigrationPath, 'utf8')
const removalSql = fs.readFileSync(templateConfigRemovalMigrationPath, 'utf8')

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

assert.match(migrationSql, /SIGNAL SQLSTATE '45000'/, 'eDHR批处理迁移缺少 fail-fast 前置条件。')
assert.match(migrationSql, /900220,\s*'eDHR批处理'/, '必须创建 eDHR批处理父菜单 900220。')
assert.match(migrationSql, /'mes:pro-edhr-batch-processing:query'/, 'eDHR批处理父菜单必须有权限标识。')
assert.match(
  migrationSql,
  /CREATE TEMPORARY TABLE `tmp_mes_edhr_batch_processing_target_packages`/,
  '迁移必须先识别已有 eDHR 菜单套餐，避免无差别扩权。'
)
assert.match(
  migrationSql,
  /JOIN `system_menu` m ON m\.`id` IN \(900220, 900002, 900024, 900025, 900026, 900033\)/,
  '租户管理员角色必须补齐 eDHR批处理父菜单和初始子页签绑定。'
)

assert.match(
  retireExecutionListSql,
  /ensure_mes_edhr_execution_list_retired/,
  'retire migration must declare the obsolete execution list retirement procedure.'
)
assert.match(
  retireExecutionListSql,
  /RETIRED_EDHR_EXECUTION_LIST/,
  'retire migration must replace the old menu contract.'
)
assert.match(
  retireExecutionListSql,
  /DELETE FROM `system_role_menu`[\s\S]*?WHERE `menu_id` = 900023/,
  'retire migration must clear role bindings for the obsolete menu.'
)
assert.match(
  retireExecutionListSql,
  /`existing_menu`.`menu_id` <> 900023/,
  'retire migration must remove the obsolete menu from tenant packages.'
)
assert.doesNotMatch(
  retireExecutionListSql,
  new RegExp('/mes/pro/feedback/' + 'edhr-execution'),
  'retire migration must not keep the old bare route.'
)
assert.doesNotMatch(
  retireExecutionListSql,
  new RegExp('mes/pro/edhr/' + 'ExecutionListPage'),
  'retire migration must not keep the old component contract.'
)

assert.match(
  visibleTabsSql,
  /SELECT\s+900365\s+AS\s+`id`,\s*'批记录表单'\s+AS\s+`name`/,
  '批记录表单必须由可见页签收敛迁移声明固定菜单 ID。'
)
assert.match(
  formTraceSql,
  /SET `name` = '表单追溯'[\s\S]*?`path` = '\/mes\/pro\/feedback\/edhr-form-trace'/,
  '表单追溯必须由专用迁移承接审计、追溯与放行权限。'
)
assert.match(
  formFillLogSql,
  /SELECT 900432, '表单日志', 'mes:pro-edhr-form-fill-log:query', 2, 6, 900220/,
  '表单日志必须由专用迁移声明固定菜单 ID。'
)

assert.match(removalSql, /ensure_mes_edhr_template_config_menu_removed/, '删除迁移必须使用可重复执行的命名过程。')
assert.match(removalSql, /SIGNAL SQLSTATE '45000'/, '删除迁移必须 fail fast 校验目标身份。')
assert.match(removalSql, /WHERE `id` = 900002/, '删除迁移必须只处理固定旧入口 900002。')
assert.match(removalSql, /`name` = 'eDHR批记录'/, '最终迁移必须把父菜单 900220 对齐为 eDHR批记录。')
assert.match(removalSql, /`type` = 3/, '旧模板与配置入口必须改为 BUTTON 权限行。')
assert.match(removalSql, /`path` = ''/, '旧模板与配置入口必须清空路径。')
assert.match(removalSql, /`component` = ''/, '旧模板与配置入口必须清空组件。')
assert.match(removalSql, /`component_name` = ''/, '旧模板与配置入口必须清空组件名。')
assert.match(removalSql, /`visible` = b'0'/, '旧模板与配置入口必须从菜单树隐藏。')
assert.doesNotMatch(
  removalSql,
  /WHEN\s+900235\s+THEN\s+'变更与异常'/,
  '变更与异常不得恢复为独立可见页签。'
)
assert.match(
  removalSql,
  /WHERE\s+`id`\s+IN\s+\(900235,\s*900260\)/,
  '变更与异常与放行与归档旧独立标签必须统一隐藏为按钮权限行。'
)
assert.match(removalSql, /WHEN\s+900260\s+THEN\s+'eDHR放行查询'/, '放行与归档必须转为隐藏放行查询权限。')
assert.match(removalSql, /WHEN\s+900260\s+THEN\s+'mes:pro-edhr-release:query'/, '放行权限标识必须保留。')
assert.doesNotMatch(removalSql, /WHEN\s+900260\s+THEN\s+'放行与归档'/, '放行与归档不得恢复为独立可见页签。')
assert.doesNotMatch(
  removalSql,
  /DELETE\s+FROM\s+`?system_(menu|role_menu|tenant_package)`?/i,
  '删除旧入口不得删除菜单、角色菜单或租户套餐数据。'
)

const finalVisibleMenus = [
  {
    id: 900365,
    name: '批记录表单',
    permission: 'mes:pro-batch-record-template:query',
    sort: 0,
    path: '/mes/pro/batch-record-form-list',
    component: 'mes/pro/batchrecordformlist/index',
    componentName: 'MesProBatchRecordFormList'
  },
  {
    id: 900033,
    name: '批次执行',
    permission: 'mes:pro-edhr-batch-execution:query',
    sort: 1,
    path: '/mes/pro/feedback/edhr-batch-execution',
    component: 'mes/pro/edhr-batch/BatchExecutionListPage',
    componentName: 'MesProEdhrBatchExecutionListPage'
  },
  {
    id: 900025,
    name: '表单追溯',
    permission: 'mes:pro-batch-record-execution:track',
    sort: 2,
    path: '/mes/pro/feedback/edhr-form-trace',
    component: 'mes/pro/edhr/FormTracePage',
    componentName: 'MesProFeedbackEdhrFormTrace'
  },
  {
    id: 900432,
    name: '表单日志',
    permission: 'mes:pro-edhr-form-fill-log:query',
    sort: 3,
    path: '/mes/pro/feedback/edhr-form-fill-log',
    component: 'mes/pro/edhr/FormFillLogPage',
    componentName: 'MesProEdhrFormFillLogPage'
  }
]

for (const menu of finalVisibleMenus) {
  assert.match(removalSql, new RegExp(`WHEN\\s+${menu.id}\\s+THEN\\s+'${escapeRegExp(menu.name)}'`))
  assert.match(removalSql, new RegExp(`WHEN\\s+${menu.id}\\s+THEN\\s+'${escapeRegExp(menu.permission)}'`))
  assert.match(removalSql, new RegExp(`WHEN\\s+${menu.id}\\s+THEN\\s+${menu.sort}`))
  assert.match(removalSql, new RegExp(`WHEN\\s+${menu.id}\\s+THEN\\s+'${escapeRegExp(menu.path)}'`))
  assert.match(removalSql, new RegExp(`WHEN\\s+${menu.id}\\s+THEN\\s+'${escapeRegExp(menu.component)}'`))
  assert.match(removalSql, new RegExp(`WHEN\\s+${menu.id}\\s+THEN\\s+'${escapeRegExp(menu.componentName)}'`))
}

console.log('PASS: MES eDHR batch processing tabs static contract')
