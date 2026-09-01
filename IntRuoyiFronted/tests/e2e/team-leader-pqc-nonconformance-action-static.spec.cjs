const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const read = (relativePath) => fs.readFileSync(path.resolve(workspaceRoot, relativePath), 'utf8')

const page = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')

assert.match(
  page,
  /v-if="canOpenPqcSubmissionNonconformanceReview\(row\)"[\s\S]*v-hasPermi="\['mes:pro-edhr-nonconformance-review:create'\]"[\s\S]*:data-pqc-submission-nonconformance-review-event-id="String\(row\.id\)"[\s\S]*>\s*不合格审查\s*<\/el-button>/,
  'PQC管理行操作必须保留不合格审查按钮，并由正式创建权限控制。'
)

assert.match(
  page,
  /const canOpenPqcSubmissionNonconformanceReview = \(row: ProcessPoolTimelineEventVO\) =>[\s\S]*canReviewSubmission\(row\)[\s\S]*Boolean\(row\.batchExecutionId\)/,
  '不合格审查按钮必须只对待复核且已带出正式批次上下文的PQC提交行开放。'
)

assert.match(
  page,
  /sourceType:\s*SOURCE_TYPE_PQC_SUBMISSION[\s\S]*sourceId:\s*String\(row\.id\)[\s\S]*batchExecutionId:\s*String\(row\.batchExecutionId\)/,
  'PQC管理入口跳转必须携带PQC提交来源ID和批次上下文。'
)

const frontlinePanel = read(
  'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
assert.doesNotMatch(
  frontlinePanel,
  /不合格审查|data-pqc-submission-nonconformance-review-event-id|SOURCE_TYPE_PQC_SUBMISSION/,
  '一线PQC填写页面不能保留提交类不合格审查入口。'
)

const roleGrantSql = read(
  'IntRuoyiBackend/sql/mysql/20260901_mes_pqc_leader_nonconformance_review_permission.sql'
)

assert.match(
  roleGrantSql,
  /dependsOn=20260830_mes_edhr_nonconformance_review_mvp/,
  'PQC组长授权脚本必须依赖不合格评审菜单权限已创建。'
)

assert.match(
  roleGrantSql,
  /SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;/,
  'PQC组长授权脚本必须显式对齐系统菜单权限字段的字符集和排序规则。'
)

assert.match(
  roleGrantSql,
  /`permission` varchar\(128\) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL/,
  'PQC组长授权脚本的临时权限列必须与system_menu.permission使用相同排序规则。'
)

assert.match(
  roleGrantSql,
  /`role`\.`code`\s*=\s*'pqc_leader_permission'/,
  '授权脚本必须精确限定PQC组长角色。'
)

for (const permission of [
  'mes:pro-edhr-nonconformance-review:query',
  'mes:pro-edhr-nonconformance-review:create'
]) {
  assert.match(roleGrantSql, new RegExp(`'${permission}'`), `PQC组长角色必须具备${permission}。`)
}

assert.doesNotMatch(
  roleGrantSql.match(/INSERT INTO `tmp_mes_pqc_leader_nonconformance_menu`[\s\S]*?;/)?.[0] || '',
  /9008300|9008303|mes:pro-edhr-nonconformance-review:dispose/,
  'PQC组长只负责从PQC管理行操作发起不合格审查，授权集合不应包含独立页面菜单或QA处置权限。'
)

assert.match(
  roleGrantSql.match(/INSERT INTO `tmp_mes_pqc_leader_nonconformance_menu`[\s\S]*?;/)?.[0] || '',
  /\(9008301,\s*'mes:pro-edhr-nonconformance-review:query'\)[\s\S]*\(9008302,\s*'mes:pro-edhr-nonconformance-review:create'\)/,
  'PQC组长授权集合必须只包含不合格审查行入口所需的隐藏查询和创建按钮。'
)

assert.match(
  roleGrantSql,
  /PQC leader role must not own nonconformance dispose permission/,
  '授权脚本必须显式阻断PQC组长误拥有QA处置权限。'
)

assert.match(
  roleGrantSql,
  /permission`\s*=\s*'mes:pro-edhr-nonconformance-review:create'[\s\S]*HAVING COUNT\(\*\) > 1/,
  '创建按钮权限必须阻断重复活动菜单节点。'
)

const roleSyncSql = read('IntRuoyiBackend/sql/mysql/20260807_test_tenant1_all_role_permission_sync.sql')

assert.match(
  roleSyncSql,
  /\('pqc_leader_permission', 'mes:pro-edhr-nonconformance-review:query', 3, '', '', ''\)/,
  '测试租户PQC组长同步数据必须把不合格审查查询权限作为隐藏按钮授权。'
)

assert.match(
  roleSyncSql,
  /\('pqc_leader_permission', 'mes:pro-edhr-nonconformance-review:create', 3, '', '', ''\)/,
  '测试租户PQC组长同步数据必须包含不合格审查创建按钮授权。'
)

assert.doesNotMatch(
  roleSyncSql,
  /\('pqc_leader_permission', 'mes:pro-edhr-nonconformance-review:dispose'/,
  '测试租户PQC组长同步数据不能包含QA处置权限。'
)

console.log('PASS: team-leader-pqc-nonconformance-action-static')
