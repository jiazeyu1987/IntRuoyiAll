const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(workspaceRoot, 'yudao-ui-admin-vue3')
const backendRoot = path.join(workspaceRoot, 'ruoyi-vue-pro')

const legacyPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/batchrecordtemplate/index.vue'
)
const retainedFormListPath = path.join(
  frontendRoot,
  'src/views/mes/pro/batchrecordformlist/index.vue'
)
const sharedDesignerPath = path.join(
  frontendRoot,
  'src/views/mes/pro/batchrecord-shared/DesignerWrapper.vue'
)
const sharedRulesPath = path.join(
  frontendRoot,
  'src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts'
)
const removalMigrationPath = path.join(
  backendRoot,
  'sql/mysql/20260715_mes_edhr_template_config_menu_removal.sql'
)

assert.equal(
  fs.existsSync(legacyPagePath),
  false,
  '旧模板与配置页面必须删除，不能继续保留隐藏页面文件。'
)
assert.ok(fs.existsSync(retainedFormListPath), '批记录表单页面必须保留。')
assert.ok(fs.existsSync(sharedDesignerPath), 'DesignerWrapper 必须迁移到批记录共享目录。')
assert.ok(fs.existsSync(sharedRulesPath), '批记录模板规则必须迁移到批记录共享目录。')
assert.ok(fs.existsSync(removalMigrationPath), '必须新增菜单删除迁移 SQL。')

const removalSql = fs.readFileSync(removalMigrationPath, 'utf8')
assert.match(removalSql, /SIGNAL SQLSTATE '45000'/, '菜单删除迁移必须 fail fast 校验目标身份。')
assert.match(removalSql, /WHERE\s+`id`\s*=\s*900002/, '迁移必须只处理固定菜单 900002。')
assert.match(removalSql, /`type`\s*=\s*3/, '900002 必须转为按钮权限行，从菜单树移除。')
assert.match(removalSql, /`path`\s*=\s*''/, '900002 必须清空旧前端路径。')
assert.match(removalSql, /`component`\s*=\s*''/, '900002 必须清空旧前端组件。')
assert.match(removalSql, /`component_name`\s*=\s*''/, '900002 必须清空旧路由组件名。')
assert.match(removalSql, /`visible`\s*=\s*b'0'/, '900002 必须不可见。')
assert.match(removalSql, /WHEN\s+900365\s+THEN\s+0/, '批记录表单必须成为 eDHR 首个可见页签。')
assert.match(removalSql, /WHEN\s+900033\s+THEN\s+1/, '批次执行必须顺延为第二个可见页签。')
assert.match(removalSql, /WHEN\s+900025\s+THEN\s+2/, '表单追溯必须顺延为第三个可见页签。')
assert.doesNotMatch(removalSql, /WHEN\s+900235\s+THEN\s+'变更与异常'/, '变更与异常不得恢复为独立可见页签。')
assert.doesNotMatch(removalSql, /WHEN\s+900260\s+THEN\s+3/, '放行与归档不得恢复为独立可见页签排序。')
assert.doesNotMatch(removalSql, /WHEN\s+900260\s+THEN\s+'放行与归档'/, '放行与归档不得恢复为独立可见菜单名称。')
assert.match(removalSql, /WHEN\s+900432\s+THEN\s+3/, '表单日志必须顺延为第四个可见页签。')
assert.match(removalSql, /WHERE\s+`id`\s+IN\s+\(900235,\s*900260\)/, '变更与异常与放行与归档旧独立标签必须统一隐藏为按钮权限行。')
assert.match(removalSql, /WHEN\s+900260\s+THEN\s+'eDHR放行查询'/, '放行与归档必须转为隐藏放行查询权限。')
assert.match(removalSql, /WHEN\s+900260\s+THEN\s+'mes:pro-edhr-release:query'/, '放行权限标识必须保留。')
assert.doesNotMatch(removalSql, /WHEN\s+900260\s+THEN\s+'\/mes\/pro\/feedback\/edhr-release'/, '放行与归档不得保留独立前端页面路径。')
assert.doesNotMatch(removalSql, /WHEN\s+900260\s+THEN\s+'mes\/pro\/edhr-release\/ReleasePage'/, '放行与归档不得保留独立前端组件。')
assert.doesNotMatch(
  removalSql,
  /DELETE\s+FROM\s+`?system_(menu|role_menu|tenant_package)`?/i,
  '删除旧入口不得删除菜单、角色菜单或租户套餐数据。'
)

const sourceFilesToCheck = [
  'src/router/modules/remaining.ts',
  'src/views/mes/pro/process/index.vue',
  'src/views/mes/pro/batchrecordcelllink/index.vue',
  'src/views/mes/pro/batchrecordformlist/index.vue'
]

for (const relativePath of sourceFilesToCheck) {
  const source = fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')
  assert.doesNotMatch(
    source,
    /\/mes\/pro\/batch-record-template/,
    `${relativePath} 不得再指向旧模板与配置 URL。`
  )
}

const srcRoot = path.join(frontendRoot, 'src')
const stack = [srcRoot]
while (stack.length > 0) {
  const current = stack.pop()
  for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
    const fullPath = path.join(current, entry.name)
    if (entry.isDirectory()) {
      stack.push(fullPath)
      continue
    }
    if (!/\.(vue|ts|js)$/.test(entry.name)) {
      continue
    }
    const source = fs.readFileSync(fullPath, 'utf8')
    assert.doesNotMatch(
      source,
      /batchrecordtemplate\/(DesignerWrapper|batchRecordTemplateRules)/,
      `${path.relative(frontendRoot, fullPath)} 必须改用 batchrecord-shared 共享模块。`
    )
  }
}

console.log('PASS: eDHR 模板与配置旧入口删除静态合同')
