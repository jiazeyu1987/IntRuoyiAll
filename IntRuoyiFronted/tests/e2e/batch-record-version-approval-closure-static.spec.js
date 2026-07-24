const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const approvalCenterApi = read('src/api/approval-center/index.ts')
const approvalCenterPage = read('src/views/approval-center/index.vue')
const versionGovernancePagePath = path.join(
  repoRoot,
  'src/views/mes/pro/edhr-version-governance/VersionGovernancePage.vue'
)
const batchRecordFormListPage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const batchRecordTemplatePage = read('src/views/mes/pro/batchrecordformlist/index.vue')
const batchRecordReportApi = read('src/api/mes/pro/batchrecordreport/index.ts')
const wordImportRealFlow = read('tests/e2e/edhr-word-import-upgrade-action-real-flow.e2e.js')

assert.match(
  approvalCenterApi,
  /\/approval-center\/tasks\/page/,
  '批记录升版待审必须走统一审批中心待办列表。'
)
assert.match(
  approvalCenterApi,
  /\/approval-center\/tasks\/review/,
  '批记录升版审核必须走统一审批中心审核接口。'
)
assert.match(
  approvalCenterPage,
  /actions\.includes\('APPROVE'\)/,
  '审批中心必须按任务动作展示审核按钮。'
)
assert.match(
  approvalCenterPage,
  /actions\.includes\('REJECT'\)/,
  '审批中心必须按任务动作展示驳回能力。'
)
assert.doesNotMatch(
  approvalCenterPage,
  /row\.moduleCode\s*!==\s*'EDHR'/,
  '审批中心不能按 EDHR 模块一刀切禁止直接审核，应由 availableActions 决定。'
)
assert.equal(
  fs.existsSync(versionGovernancePagePath),
  false,
  'eDHR 版本治理页面已下线，升版审批审核入口必须统一放在审批中心待办。'
)
assert.match(
  approvalCenterPage,
  /审核不通过必须填写原因/,
  '审批中心驳回升版必须要求填写原因，不得静默驳回。'
)
assert.match(
  wordImportRealFlow,
  /completeUpgradeApprovalFromTodo/,
  '批记录 Word 升版真实 E2E 必须从审批中心待办完成审核闭环。'
)
assert.match(
  wordImportRealFlow,
  /queryApprovalTodoByProcess/,
  '批记录 Word 升版真实 E2E 必须按 processInstanceId 精确锁定审批中心待办，不能依赖可变业务标题。'
)
assert.match(
  wordImportRealFlow,
  /requestPayload\?\.processInstanceId/,
  '批记录 Word 升版真实 E2E 提交审核后必须核对请求体 processInstanceId，防止点错审批行。'
)
assert.match(
  wordImportRealFlow,
  /getByText\('审核通过',\s*\{\s*exact:\s*true\s*\}\)/,
  '批记录 Word 升版真实 E2E 审核弹窗必须精确点击“审核通过”选项，避免匹配到电子签名提示文案。'
)
assert.match(
  wordImportRealFlow,
  /findApprovalRowByTask/,
  '批记录 Word 升版真实 E2E 必须把后端 processInstanceId 锁定结果转换为前端行定位，不能直接依赖模板化 businessTitle。'
)
assert.doesNotMatch(
  wordImportRealFlow,
  /filter\(\{\s*hasText:\s*approvalTask\.businessTitle\s*\|\|/,
  '批记录 Word 升版真实 E2E 不得用 approvalTask.businessTitle 模板文本直接定位前端行。'
)
assert.match(
  wordImportRealFlow,
  /\/admin-api\/approval-center\/tasks\/review/,
  '批记录 Word 升版真实 E2E 必须通过统一审批中心审核接口。'
)
assert.doesNotMatch(
  wordImportRealFlow,
  /\/admin-api\/mes\/pro\/batch-record-report\/version-approval\/pending/,
  '批记录 Word 升版真实 E2E 不得再依赖旧私有待审列表。'
)
assert.match(
  wordImportRealFlow,
  /versionStatus['"]?\s*:\s*['"]APPROVED|versionStatus\)\s*,\s*['"]APPROVED/,
  '批记录 Word 升版真实 E2E 必须断言审核后版本状态为 APPROVED。'
)
assert.match(
  wordImportRealFlow,
  /EDHR_WORD_UPGRADE_APPROVAL_MODE/,
  '批记录 Word 升版真实 E2E 必须支持 DIRECT / BPM_REQUIRED 两种审批模式。'
)
assert.match(
  wordImportRealFlow,
  /verifyUpgradeDirectPublished/,
  '批记录 Word 升版 DIRECT 真实 E2E 必须验证版本直接生效。'
)
assert.match(
  wordImportRealFlow,
  /approvalInstanceId\s*==\s*null/,
  '批记录 Word 升版 DIRECT 真实 E2E 必须验证不生成审批实例，接口返回 null 或 undefined 都应视为无 BPM 实例。'
)
assert.match(
  wordImportRealFlow,
  /EDHR_WORD_UPGRADE_ARTIFACT_DIR/,
  '批记录 Word 升版真实 E2E 必须写入可追溯证据 JSON。'
)
assert.match(
  wordImportRealFlow,
  /EDHR_WORD_UPGRADE_SINGLE_UPGRADE_ONLY/,
  '批记录 Word 升版真实 E2E 必须支持对已有已生效版本直接执行单次升版，避免审批开启时先制造一个待审批前置版本。'
)
assert.match(
  wordImportRealFlow,
  /EDHR_WORD_UPGRADE_RESUME_APPROVAL_INSTANCE_ID/,
  '批记录 Word 升版真实 E2E 必须支持从已形成的审批实例续跑审批，避免中断后重复制造升版版本。'
)
assert.match(
  wordImportRealFlow,
  /EDHR_WORD_UPGRADE_APPROVER_USERNAME/,
  '批记录 Word 升版真实 E2E 必须支持提交人和实际审批人分账号完成，不能让 aoteman 代审已分派给 admin 的 BPM 待办。'
)
assert.match(
  wordImportRealFlow,
  /loginWithCredentials/,
  '批记录 Word 升版真实 E2E 多角色审批必须通过真实登录切换审批人账号。'
)
assert.match(
  wordImportRealFlow,
  /EDHR_WORD_UPGRADE_CREATE_ROUTE_FIXTURE/,
  '批记录 Word 升版真实 E2E 缺少当前工艺流程版本时，必须支持通过真实页面创建路线夹具。'
)
assert.match(
  wordImportRealFlow,
  /EDHR_WORD_UPGRADE_FIXTURE_SOURCE_ROUTE_ID/,
  '批记录 Word 升版路线夹具必须显式指定安全源路线，不能默认复制共享路线。'
)
assert.match(
  wordImportRealFlow,
  /\/admin-api\/mes\/pro\/route\/copy/,
  '批记录 Word 升版路线夹具必须走前端路线复制接口响应，不得用 SQL 直写路线。'
)
assert.match(
  wordImportRealFlow,
  /routeFixtureSummary/,
  '批记录 Word 升版真实 E2E 证据 JSON 必须记录路线夹具创建或复用结果。'
)
assert.match(
  wordImportRealFlow,
  /login\?redirect=.*\/index/,
  '批记录 Word 升版真实 E2E 登录必须对齐官方 preflight，从 /login?redirect=/index 进入。'
)
assert.match(
  wordImportRealFlow,
  /input\.el-input__inner:not\(\[role="combobox"\]\):visible/,
  '批记录 Word 升版真实 E2E 登录用户名输入必须排除租户 combobox，避免误填租户框。'
)
assert.match(
  wordImportRealFlow,
  /response\.request\(\)\.method\(\) === 'POST'/,
  '批记录 Word 升版真实 E2E 登录响应等待必须按 POST 登录接口判断。'
)

assert.match(
  batchRecordReportApi,
  /expectedTargetVersionNo\?: string/,
  '批记录 Word 导入写接口必须传递预检目标版本号，避免前端显示 V3.0 但后端复用旧 V2.0 快照。'
)
assert.match(
  batchRecordReportApi,
  /data\.append\('expectedTargetVersionNo', expectedTargetVersionNo\)/,
  '批记录 Word 导入写接口必须把 expectedTargetVersionNo 写入 multipart 参数。'
)
assert.match(
  batchRecordReportApi,
  /approvalInstanceId\?: string/,
  '批记录 Word 升版导入响应必须暴露审批实例 ID，前端才能确认已形成审批单。'
)

for (const [label, source] of [
  ['批记录表单列表页', batchRecordFormListPage],
  ['批记录模板页', batchRecordTemplatePage]
]) {
  assert.match(
    source,
    /expectedTargetVersionNo:\s*wordImportDialog\.selectedAction === 'UPGRADE'[\s\S]*?\? wordImportDialog\.preflight\?\.nextVersionNo/,
    `${label} 升版导入必须把预检目标版本号随写入请求提交。`
  )
  assert.match(
    source,
    /submitImportedVersionApproval\(result\)/,
    `${label} 升版导入生成快照后必须立即提交升版审批，不能只停留在预检通过。`
  )
  assert.match(
    source,
    /自动提交升版审批/,
    `${label} 导入结果提示必须说明已自动提交升版审批。`
  )
  const submitApprovalBlock = source.match(
    /const submitImportedVersionApproval = \([\s\S]*?result: BatchRecordReportImportResultVO[\s\S]*?\) => \{[\s\S]*?\n\}/
  )?.[0] || ''
  assert.match(
    submitApprovalBlock,
    /result\.versionStatus\s*===\s*'APPROVED'/,
    `${label} 已生效 APPROVED 导入结果必须按成功闭环处理。`
  )
  assert.ok(
    submitApprovalBlock.indexOf("result.versionStatus === 'APPROVED'") <
      submitApprovalBlock.indexOf("result.versionStatus !== 'PENDING_APPROVAL'"),
    `${label} 必须先处理 APPROVED 已生效结果，再对其他异常状态 fail fast。`
  )
}

console.log('PASS: batch record version approval closure static contract')
