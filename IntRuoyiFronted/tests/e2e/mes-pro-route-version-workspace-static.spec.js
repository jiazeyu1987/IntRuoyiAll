const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeIndex = read('src/views/mes/pro/route/index.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeApi = read('src/api/mes/pro/route/index.ts')

assert.match(
  routeApi,
  /export interface ProRouteVersionBlockerVO[\s\S]*publishable:\s*boolean[\s\S]*blockers:\s*string\[\]/,
  '路线版本 blocker API 必须暴露 publishable 和 blockers。'
)

assert.match(
  routeIndex,
  /data-testid="route-version-workspace"/,
  '工艺路线列表必须提供路线版本工作区入口。'
)
for (const permission of [
  'mes:pro-route:version-query',
  'mes:pro-route:version-create',
  'mes:pro-route:version-submit',
  'mes:pro-route:version-withdraw',
  'mes:pro-route:version-cancel',
  'mes:pro-route:version-reopen'
]) {
  assert.match(
    routeIndex,
    new RegExp(`v-hasPermi="\\['${permission}'\\]"`),
    `版本工作区按钮必须使用后端声明的权限：${permission}`
  )
}
assert.match(
  routeIndex,
  /route-version-workspace__active-version/,
  '版本工作区必须显示当前 ACTIVE 版本。'
)
assert.match(
  routeIndex,
  /route-version-workspace__candidate-list/,
  '版本工作区必须显示候选版本列表。'
)
assert.match(
  routeIndex,
  /resolveRouteVersionStatusLabel\(version\.lifecycleStatus\)/,
  '版本工作区必须把 lifecycleStatus 转成人可读状态。'
)
assert.match(
  routeIndex,
  /loadRouteVersionBlockers\(version\.id\)/,
  '版本工作区必须提供发布阻断项查询入口。'
)
assert.match(
  routeIndex,
  /submitRouteCandidateVersion\(version\)/,
  '版本工作区必须把完整候选版本对象传给提交入口，提交后按策略自动继续执行。'
)
assert.match(
  routeIndex,
  /v-if="canEditRouteCandidateVersion\(version\)"[\s\S]*>\s*编辑\s*<\/el-button>/,
  'DRAFT 候选版本必须只显示编辑动作，而不是把非法动作显示为禁用按钮。'
)
assert.match(
  routeIndex,
  /v-if="canViewRouteVersion\(version\)"[\s\S]*openRouteVersionViewer\(version\)[\s\S]*>\s*查看\s*<\/el-button>/,
  'PENDING_APPROVAL / READY_TO_PUBLISH / REJECTED 等非草稿版本必须提供只读查看入口。'
)
assert.match(
  routeIndex,
  /withdrawRouteCandidateVersion\(version\.id\)/,
  '版本工作区必须提供审核中撤回入口。'
)
assert.match(
  routeIndex,
  /v-if="canWithdrawRouteVersion\(version\)"[\s\S]*withdrawRouteCandidateVersion\(version\.id\)/,
  'PENDING_APPROVAL 只能显示撤回动作，不能通过禁用按钮混淆状态。'
)
assert.doesNotMatch(
  routeIndex,
  /promptRouteVersionSignaturePassword|电子签名发布|signaturePassword/,
  '提交发布不应要求提交者电子签名；审批签名由审批中心同意/驳回动作完成。'
)
assert.match(
  routeIndex,
  /const submitRouteCandidateVersion = async \(version: ProRouteVersionVO\)[\s\S]*ProRouteApi\.submitAndPublishRouteCandidateVersion\(\{[\s\S]*id:\s*latestVersion\.id[\s\S]*\}\)[\s\S]*resolveRoutePublishSuccessMessage\(submittedVersion,\s*true\)/,
  '草稿“提交发布”必须一键提交审批，不得携带提交者签名密码。'
)
assert.match(
  routeIndex,
  /const resolveLatestRouteVersionForSubmit = async \(version: ProRouteVersionVO\)[\s\S]*ProRouteApi\.getRouteVersionList\(currentRoute\.id\)[\s\S]*latestVersion[\s\S]*latestVersion\.lifecycleStatus !== 'DRAFT'[\s\S]*return undefined[\s\S]*const submitRouteCandidateVersion = async \(version: ProRouteVersionVO\)[\s\S]*const latestVersion = await resolveLatestRouteVersionForSubmit\(version\)[\s\S]*if \(!latestVersion\) return[\s\S]*ProRouteApi\.submitAndPublishRouteCandidateVersion\(\{/,
  '版本工作区提交发布前必须刷新最新版本列表；若目标版本已不是 DRAFT，必须返回且不得继续调用提交发布接口。'
)
assert.match(
  routeEditPage,
  /const submitRouteCandidateVersion = async \(options: SubmitRouteCandidateVersionOptions = \{\}\)[\s\S]*ProRouteApi\.getRouteVersion\(context\.routeVersionId\)[\s\S]*latestVersion\.lifecycleStatus !== 'DRAFT'[\s\S]*return[\s\S]*ProRouteApi\.submitAndPublishRouteCandidateVersion\(\{/,
  '编辑页提交发布前必须读取最新 routeVersion 状态；URL 中旧 DRAFT 不得导致 ACTIVE 版本重复提交。'
)
assert.match(
  routeIndex,
  /cancelRouteCandidateVersion\(version\.id\)/,
  '版本工作区必须提供候选取消入口。'
)
assert.match(
  routeIndex,
  /v-if="canCancelRouteVersion\(version\)"[\s\S]*cancelRouteCandidateVersion\(version\.id\)/,
  'DRAFT / READY_TO_PUBLISH / REJECTED 才显示取消动作。'
)
assert.match(
  routeIndex,
  /reopenRouteCandidateVersion\(version\.id\)/,
  '版本工作区必须提供驳回后按意见修改入口。'
)
assert.match(
  routeIndex,
  /v-if="canReopenRouteVersion\(version\)"[\s\S]*reopenRouteCandidateVersion\(version\.id\)/,
  'REJECTED 必须显示按意见修改动作。'
)
assert.match(
  routeIndex,
  /canWithdrawRouteVersion[\s\S]*PENDING_APPROVAL/,
  'PENDING_APPROVAL 必须只允许查看和撤回，不允许编辑或新建替代草稿。'
)
assert.doesNotMatch(
  routeIndex,
  /canPublishRouteVersion[\s\S]*READY_TO_PUBLISH/,
  'READY_TO_PUBLISH 不再作为人工恢复发布入口。'
)
assert.match(
  routeIndex,
  /resolveRouteVersionErrorMessage/,
  '版本工作区必须显式展示后端错误，不得静默失败。'
)
assert.doesNotMatch(
  routeIndex,
  /catch\s*\(\s*\)\s*\{\s*\}/,
  '工艺路线页面不得保留空 catch 掩盖 route version 后端错误。'
)

console.log('mes-pro-route-version-workspace-static PASS')
