const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const flowInterventionApiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/flowIntervention.ts')

const detail = fs.readFileSync(detailPath, 'utf8')
const flowInterventionApi = fs.readFileSync(flowInterventionApiPath, 'utf8')

assert.match(
  detail,
  /submitTransferIntervention/,
  '批次详情必须复用现有流程干预转办接口，不得新增隐藏状态写入。'
)
assert.match(
  flowInterventionApi,
  /url:\s*'\/mes\/pro\/edhr-flow-intervention\/transfer'/,
  '流程干预 API 必须仍指向正式 transfer 端点。'
)

const railStart = detail.indexOf('<aside class="edhr-batch-detail__review-rail"')
const railEnd = detail.indexOf('</aside>', railStart)
assert.ok(railStart >= 0 && railEnd > railStart, '批次详情必须保留右侧一级操作栏。')
const rail = detail.slice(railStart, railEnd)

assert.doesNotMatch(
  rail,
  /v-hasPermi="\['mes:pro-edhr-flow-intervention:transfer'\]"/,
  '管理员接管入口不能被 v-hasPermi 静态指令误删；芋道 admin 后端 super_admin 可执行，但前端权限集合不含 transfer 权限码。'
)
assert.match(
  rail,
  /管理员接管并填写/,
  '右侧一级操作栏必须为低熟练用户提供明确的“管理员接管并填写”入口。'
)
assert.match(
  detail,
  /管理员接管放行审批/,
  '放行审批抽屉必须为 admin 提供明确的“管理员接管放行审批”入口，避免审批中心可见但审核失败。'
)
const releaseStageActionBuilderStart = detail.indexOf('const buildReleaseStageActionItems =')
const releaseStageActionBuilderEnd = detail.indexOf('const releaseStageActionItems = computed', releaseStageActionBuilderStart)
assert.ok(
  releaseStageActionBuilderStart >= 0 && releaseStageActionBuilderEnd > releaseStageActionBuilderStart,
  '批次详情必须保留放行阶段右侧动作构建逻辑。'
)
const releaseStageActionBuilder = detail.slice(releaseStageActionBuilderStart, releaseStageActionBuilderEnd)
const releaseApprovalActionBody = releaseStageActionBuilder.slice(
  releaseStageActionBuilder.indexOf("if (stageKey === 'release-approval')"),
  releaseStageActionBuilder.indexOf("if (stageKey === 'archive')")
)
assert.match(
  releaseApprovalActionBody,
  /key:\s*'release-approval'/,
  '放行审批阶段右侧动作区必须提供可点击的“放行审批”入口。'
)
assert.match(
  releaseApprovalActionBody,
  /label:\s*'放行审批'/,
  '放行审批中不能只显示“提交放行”等失效动作，必须能打开放行审批抽屉。'
)
assert.match(
  releaseApprovalActionBody,
  /openReleaseApprovalGroup/,
  '放行审批阶段右侧动作必须打开本页放行审批抽屉，承载管理员接管入口。'
)
const applyRouteFocusBody = detail.slice(
  detail.indexOf('const applyRouteFocus ='),
  detail.indexOf('const resolveDefaultTaskSelection =')
)
assert.match(
  applyRouteFocusBody,
  /openReleaseApprovalGroup\(\)/,
  'focus=approval 深链必须进入放行审批动作组，避免默认停留在放行预检。'
)
const openReleaseApprovalGroupBody = detail.slice(
  detail.indexOf('const openReleaseApprovalGroup ='),
  detail.indexOf('watch(actualReleaseStageKey')
)
assert.match(
  openReleaseApprovalGroupBody,
  /selectReleaseProcess\(\)[\s\S]*viewedReleaseStageKey\.value\s*=\s*'release-approval'[\s\S]*releaseApprovalDrawerVisible\.value\s*=\s*true/,
  '放行审批动作组必须选中放行流程、定位放行审批阶段并打开抽屉。'
)
const applyInitialBatchTaskSelectionBody = detail.slice(
  detail.indexOf('const applyInitialBatchTaskSelection ='),
  detail.indexOf('const loadReviewTimeline = async')
)
assert.match(
  applyInitialBatchTaskSelectionBody,
  /resolveDetailFocus\(\)\s*===\s*'approval'/,
  '批次详情首屏选择必须识别 focus=approval，避免进入默认工序。'
)
assert.match(
  applyInitialBatchTaskSelectionBody,
  /selectReleaseProcess\(\)[\s\S]*viewedReleaseStageKey\.value\s*=\s*'release-approval'/,
  '批次详情首屏 focus=approval 必须立即选中放行审批阶段。'
)
const loadReviewTimelineBody = detail.slice(
  detail.indexOf('const loadReviewTimeline = async'),
  detail.indexOf('const cancelDeferredBatchDetailSecondaryLoad =')
)
assert.match(
  loadReviewTimelineBody,
  /resolveDetailFocus\(\)\s*===\s*'approval'/,
  '复盘时间线加载后必须识别 focus=approval，不能用默认工序选择覆盖放行深链。'
)
assert.match(
  loadReviewTimelineBody,
  /selectReleaseProcess\(\)[\s\S]*viewedReleaseStageKey\.value\s*=\s*'release-approval'/,
  'focus=approval 深链在复盘加载后必须保持放行流程选中并定位到放行审批阶段。'
)
assert.match(
  rail,
  /canTakeOverFillTask\(task\)/,
  '只有存在可接管填写任务时才显示接管入口。'
)

const canTakeOverBody = detail.slice(
  detail.indexOf('const canTakeOverFillTask ='),
  detail.indexOf('const resolveTakeoverFillCarrier =')
)
assert.match(
  detail,
  /const FLOW_TRANSFER_PERMISSION = 'mes:pro-edhr-flow-intervention:transfer'/,
  '批次详情必须显式声明正式流程干预转办权限码。'
)
assert.match(
  detail,
  /const FLOW_TRANSFER_ADMIN_ROLES = \['super_admin'\]/,
  '批次详情必须显式声明 super_admin 角色可见接管入口，和后端 @ss.hasPermission 管理员放行语义一致。'
)
assert.match(
  detail,
  /const canUseFlowTransferIntervention = computed\(\s*\(\) =>[\s\S]*userStore\.permissions\.has\(FLOW_TRANSFER_PERMISSION\)[\s\S]*userStore\.permissions\.has\('\*:\*:\*'\)[\s\S]*FLOW_TRANSFER_ADMIN_ROLES\.some\(\(role\) => userStore\.roles\.includes\(role\)\)[\s\S]*\)/,
  '接管入口可见性必须使用页面级 gate：精确权限、通配权限或 super_admin 角色，不能只依赖静态指令。'
)
assert.match(canTakeOverBody, /row\.activeWorkTaskId/, '接管入口必须要求当前工序存在活动 work task。')
assert.match(canTakeOverBody, /!hasAllowedTaskAction\(row,\s*'OPEN_FORM'\)/, '接管入口只处理当前用户不能直接填写的任务。')
assert.match(canTakeOverBody, /row\.available !== false/, '接管入口不得绕过后端 available=false 的任务。')
assert.match(canTakeOverBody, /canUseFlowTransferIntervention\.value/, '接管入口必须先通过流程转办权限/角色 gate。')
assert.doesNotMatch(canTakeOverBody, /SQL|DIRECT_STATUS_UPDATE|mock|fixture/i, '接管入口不得包含 SQL、直写状态或 mock 逻辑。')

const takeoverBody = detail.slice(
  detail.indexOf('const handleTakeOverFillTask = async'),
  detail.indexOf('const openSkipTaskDialog =')
)
for (const token of [
  "businessObjectType: 'WORK_TASK'",
  'businessObjectId: String(workTaskId)',
  'taskId: String(workTaskId)',
  "fromStatus: 'TODO'",
  "toStatus: 'TODO'",
  "reasonCategory: 'ADMIN_BATCH_DETAIL_TAKEOVER'",
  'targetUserId',
  'signoffEvidenceHash',
  'idempotencyKey',
  'submitTransferIntervention',
  'await loadDetail()',
  'await handleOpenTask('
]) {
  assert.match(takeoverBody, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `接管动作缺少正式流程字段或连续打开逻辑：${token}`)
}
assert.match(
  takeoverBody,
  /message\.confirm\(/,
  '接管前必须通过页面确认说明转办结果，不能静默替用户接管。'
)
assert.match(
  takeoverBody,
  /buildTakeoverSignoffEvidenceHash/,
  '接管动作必须生成可追溯签核证据哈希。'
)

const releaseTakeoverBody = detail.slice(
  detail.indexOf('const handleTakeOverReleaseApprovalTask = async'),
  detail.indexOf('const handleReleaseTransactionConfirm =')
)
assert.ok(
  releaseTakeoverBody.length > 0 && releaseTakeoverBody.includes('handleTakeOverReleaseApprovalTask'),
  '批次详情必须实现放行审批接管动作。'
)
for (const token of [
  "businessObjectType: 'WORK_TASK'",
  'businessObjectId: String(workTaskId)',
  'taskId: String(workTaskId)',
  "fromStatus: 'TODO'",
  "toStatus: 'TODO'",
  "reasonCategory: 'ADMIN_RELEASE_APPROVAL_TAKEOVER'",
  'targetUserId',
  'signoffEvidenceHash',
  'idempotencyKey',
  'submitTransferIntervention',
  'await loadDetail()'
]) {
  assert.match(releaseTakeoverBody, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `放行审批接管动作缺少正式流程字段：${token}`)
}
assert.match(
  releaseTakeoverBody,
  /message\.confirm\(/,
  '放行审批接管前必须通过页面确认说明转办结果，不能静默替用户接管。'
)
assert.match(
  releaseTakeoverBody,
  /buildTakeoverSignoffEvidenceHash/,
  '放行审批接管动作必须生成可追溯签核证据哈希。'
)
assert.doesNotMatch(releaseTakeoverBody, /SQL|DIRECT_STATUS_UPDATE|mock|fixture/i, '放行审批接管不得包含 SQL、直写状态或 mock 逻辑。')

const hashBody = detail.slice(
  detail.indexOf('const buildSha256Hex = async'),
  detail.indexOf('const buildTakeoverSignoffEvidenceHash =')
)
assert.match(hashBody, /window\.crypto\?\.subtle/, '签核证据必须使用浏览器 SHA-256 能力生成。')
assert.doesNotMatch(hashBody, /catch\s*\{\s*\}|fallback|DEFAULT/i, '签核证据生成不得静默降级或伪造默认成功。')

console.log('PASS: eDHR batch detail exposes auditable admin takeover for assigned-away fill tasks.')
