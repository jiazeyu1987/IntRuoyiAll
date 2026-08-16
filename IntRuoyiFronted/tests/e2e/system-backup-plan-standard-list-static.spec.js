const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/system/backup-plan/index.vue')
const apiPath = path.join(root, 'src/api/system/backupPlan/index.ts')
const realE2ePath = path.join(root, 'tests/e2e/system-backup-plan-real-readonly.e2e.js')
const queryOnlyE2ePath = path.join(root, 'tests/e2e/system-backup-plan-query-only-permission.e2e.js')

assert.ok(fs.existsSync(sourcePath), '系统管理备份计划页面必须存在。')
assert.ok(fs.existsSync(apiPath), '系统管理备份计划 API 包装必须存在。')
assert.ok(fs.existsSync(realE2ePath), '系统管理备份计划真实只读 E2E 必须存在。')
assert.ok(fs.existsSync(queryOnlyE2ePath), '系统管理备份计划 query-only 权限 E2E 必须存在。')

const source = fs.readFileSync(sourcePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')
const realE2e = fs.readFileSync(realE2ePath, 'utf8')
const queryOnlyE2e = fs.readFileSync(queryOnlyE2ePath, 'utf8')

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
assert.match(source, /备份仓库/, '页面必须只读展示后端返回的备份仓库环境。')
assert.match(source, /新鲜度阈值/, '页面必须只读展示已批准 RPO 对应的新鲜度阈值。')
assert.match(source, /最新成功备份点/, '页面必须只读展示 latestBackupPoint 的最新成功备份时间。')
assert.match(source, /formatRepositoryEnvironment/, '备份仓库环境必须通过白话格式化函数展示。')
assert.match(source, /formatFreshnessThreshold/, '新鲜度阈值必须通过白话格式化函数展示。')
assert.match(source, /formatLatestBackupPoint/, '最新备份点必须通过白话格式化函数展示。')
assert.match(source, /import \{ checkPermi \} from '@\/utils\/permission'/, '页面必须显式计算 query-only 权限状态。')
assert.match(source, /canUpdateBackupPlan/, '页面必须有统一的备份计划更新权限开关。')
assert.match(
  source,
  /<el-form[\s\S]*v-if="canUpdateBackupPlan"[\s\S]*class="backup-plan-form"/,
  'query-only 权限下不得显示备份频率、星期、时间等可编辑计划控件。'
)
assert.match(
  source,
  /当前账号只有查询权限/,
  'query-only 权限下必须明确展示只读提示，而不是保留不可保存的编辑控件。'
)
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
assert.match(api, /repositoryEnvironment\?:/, '状态 API 类型必须暴露 repositoryEnvironment。')
assert.match(api, /maxFreshnessHours\?:/, '状态 API 类型必须暴露 maxFreshnessHours。')
assert.match(api, /completedAt\?:/, '备份点 API 类型必须暴露 manifest completedAt。')
assert.match(realE2e, /SYSTEM_BACKUP_PLAN_E2E_TENANT/, '真实只读 E2E 必须允许显式指定测试租户。')
assert.match(realE2e, /SYSTEM_BACKUP_PLAN_E2E_USERNAME/, '真实只读 E2E 必须允许显式指定测试账号。')
assert.match(realE2e, /SYSTEM_BACKUP_PLAN_E2E_PASSWORD/, '真实只读 E2E 必须允许通过进程环境传入密码。')
assert.match(
  realE2e,
  /\['备份仓库', '新鲜度阈值', '最新成功备份点'\]/,
  '真实只读 E2E 必须验证新增运维状态字段可见。'
)
assert.match(queryOnlyE2e, /SYSTEM_BACKUP_PLAN_QUERY_ONLY_PASSWORD/, 'query-only E2E 必须要求显式凭据输入。')
assert.match(queryOnlyE2e, /system:backup-plan:query/, 'query-only E2E 必须先确认查询权限。')
assert.match(queryOnlyE2e, /system:backup-plan:update/, 'query-only E2E 必须确认不存在更新权限。')
assert.match(queryOnlyE2e, /system:backup-plan:execute/, 'query-only E2E 必须确认不存在执行权限。')
assert.match(queryOnlyE2e, /当前账号只有查询权限/, 'query-only E2E 必须验证页面只读提示。')
assert.match(
  queryOnlyE2e,
  /getByText\('备份包历史', \{ exact: true \}\)/,
  'query-only E2E 必须精确定位备份包历史标题，避免与只读提示文案冲突。'
)
assert.match(
  queryOnlyE2e,
  /!document\.querySelector\('\.backup-plan-page \.el-loading-mask'\)/,
  'query-only E2E 必须等待页面加载遮罩消失后再生成截图证据。'
)
assert.match(
  queryOnlyE2e,
  /const permissionResponseResultPromise = permissionResponsePromise\.then\(/,
  'query-only E2E 必须立即接管权限响应拒绝，避免它覆盖更早的登录根因。'
)
assert.match(
  queryOnlyE2e,
  /if \(permissionResponseResult\.error\) \{[\s\S]*throw permissionResponseResult\.error/,
  'query-only E2E 后续仍必须重新抛出权限响应失败，不得吞掉错误。'
)
assert.match(
  queryOnlyE2e,
  /await statusResponse\.request\(\)\.allHeaders\(\)/,
  'query-only E2E 的最终 403 比对必须读取包含安全相关字段的完整请求头。'
)
assert.match(
  queryOnlyE2e,
  /permission envelope expected HTTP 200/,
  'query-only E2E 必须按项目异常封装合同验证权限拒绝的 HTTP 状态。'
)
assert.match(
  queryOnlyE2e,
  /expected business code 403/,
  'query-only E2E 必须最终比对四个写接口的业务码 403。'
)
