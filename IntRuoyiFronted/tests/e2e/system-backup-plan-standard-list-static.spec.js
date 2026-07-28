const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/system/backup-plan/index.vue')
const apiPath = path.join(root, 'src/api/system/backupPlan/index.ts')

assert.ok(fs.existsSync(sourcePath), '系统管理备份计划页面必须存在。')
assert.ok(fs.existsSync(apiPath), '系统管理备份计划 API 包装必须存在。')

const source = fs.readFileSync(sourcePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.match(
  source,
  /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index.vue'/,
  '备份包历史必须使用标准列表模板。'
)
assert.match(
  source,
  /<UnifiedListTemplate[\s\S]*table-key="system\.backup-plan\.history"[\s\S]*<\/UnifiedListTemplate>/,
  '备份包历史必须使用稳定 tableKey 接入标准列表模板。'
)
assert.match(source, /自动备份/, '页面必须使用普通管理员能理解的“自动备份”文案。')
assert.match(source, /每天/, '页面必须提供“每天”简单频率。')
assert.match(source, /每周/, '页面必须提供“每周”简单频率。')
assert.match(source, /现在备份一次/, '页面必须提供弱化的一键立即备份入口。')
assert.match(source, /正常[\s\S]*已关闭[\s\S]*上次失败[\s\S]*配置异常/, '页面必须暴露四类简单状态。')
assert.match(source, /会立即备份正式服数据，可能需要几分钟，是否继续？/, '立即备份必须有白话确认。')
assert.doesNotMatch(source, /Crontab|cron|CRON|表达式/, '低门槛页面不得暴露 Cron 或表达式。')
assert.doesNotMatch(
  source,
  /\{\{\s*[^}]*\.(manifestPath|snapshotPath)[^}]*\}\}/,
  '低门槛页面不得直接展示 manifest 或快照技术路径。'
)
assert.match(source, /formatBackupStorageLabel/, '备份包保存位置必须通过白话格式化函数展示。')
assert.match(api, /url:\s*'\/infra\/backup-plan\/status'/, 'API 必须调用独立备份计划状态接口。')
assert.match(api, /url:\s*'\/infra\/backup-plan\/history\/page'/, 'API 必须调用独立备份历史分页接口。')
