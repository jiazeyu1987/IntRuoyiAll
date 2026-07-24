import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const repoRoot = process.cwd()
const detailPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const listPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/batchExecution.ts')
const detailPage = fs.readFileSync(detailPath, 'utf8')
const listPage = fs.readFileSync(listPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

assert.match(
  detailPage,
  /const\s+releaseActionLocked\s*=\s*computed\([\s\S]*?!hasGoldenFingerActionBypass\.value[\s\S]*?detail\.value\?\.releaseActionLocked\s*===\s*true[\s\S]*?releaseStatus\.value\s*===\s*'PENDING_APPROVAL'[\s\S]*?\)/,
  '批次详情页必须优先映射后端 releaseActionLocked，并识别放行审批中动作锁状态；金手指用户可绕过该动作锁。'
)

assert.match(
  detailPage,
  /const\s+hasGoldenFingerActionBypass\s*=\s*computed\([\s\S]*?hasGoldenFingerPermission\.value[\s\S]*?\)/,
  '批次详情页必须显式使用金手指权限控制动作锁绕过。'
)

assert.match(
  detailPage,
  /const\s+releaseActionLockMessage\s*=\s*computed\([\s\S]*?detail\.value\?\.releaseActionLockReason\s*\|\|\s*RELEASE_ACTION_LOCKED_MESSAGE[\s\S]*?\)/,
  '批次详情页必须优先展示后端 releaseActionLockReason，不能只靠前端固定文案。'
)

for (const [computedName, projectionName] of [
  ['canGenerateArchive', 'archiveProjectionState'],
  ['canQualityReject', 'qualityRejectProjectionState'],
  ['canAttemptClose', 'closeProjectionState']
]) {
  const declaration = detailPage.match(new RegExp(`const\\s+${computedName}\\s*=\\s*computed\\([\\s\\S]*?\\n\\)`))
  assert.ok(declaration, `${computedName} 必须保留为 computed 动作门禁。`)
  assert.match(
    declaration[0],
    new RegExp(`${projectionName}\\.value\\.allowed`),
    `${computedName} 必须通过受控动作投影门禁锁定普通批次动作。`
  )
}

assert.match(
  detailPage,
  /const\s+resolveEdhrBatchActionProjection[\s\S]*?locked:\s*batchActionLocked\.value[\s\S]*?pending:\s*batchActionLocked\.value/,
  'eDHR 普通动作投影必须接入 batchActionLocked，覆盖普通用户的放行审批中与作废申请中锁定。'
)

for (const computedName of [
  'canRequestReopen',
  'canOpenArchivePrintDrawer',
  'canRunReleasePrecheck'
]) {
  const declaration = detailPage.match(new RegExp(`const\\s+${computedName}\\s*=\\s*computed\\([\\s\\S]*?\\n\\)`))
  assert.ok(declaration, `${computedName} 必须保留为 computed 动作门禁。`)
  assert.match(
    declaration[0],
    /!batchActionLocked\.value/,
    `${computedName} 必须在 PENDING_APPROVAL 或作废申请中锁定普通批次动作。`
  )
}

assert.match(
  detailPage,
  /terminalReleaseActionItems[\s\S]*?canAttemptClose\.value[\s\S]*?canQualityReject\.value[\s\S]*?canRequestReopen\.value/,
  '终态阶段动作必须复用 canAttemptClose/canQualityReject/canRequestReopen 门禁。'
)

assert.match(
  detailPage,
  /openReopenBatchDialog[\s\S]*?if\s*\(\s*!canRequestReopen\.value\s*\)/,
  '申请重开入口必须在点击处理时复核动作锁，不能只依赖按钮 disabled。'
)

assert.match(
  detailPage,
  /submitClose[\s\S]*?if\s*\(\s*!canAttemptClose\.value\s*\)/,
  '关闭提交处理必须复核动作锁，避免弹窗打开后状态进入放行审批仍可提交。'
)

assert.match(
  detailPage,
  /openArchivePrintDrawer[\s\S]*?if\s*\(\s*!canOpenArchivePrintDrawer\.value\s*\)/,
  '归档打印外层入口必须在点击处理时复核动作锁，不能只依赖抽屉内按钮 disabled。'
)

assert.match(
  detailPage,
  /handleGenerateArchive[\s\S]*?if\s*\(\s*!canGenerateArchive\.value\s*\)/,
  '归档生成处理必须复核动作锁，不能只依赖按钮 disabled。'
)

assert.match(
  detailPage,
  /handleReleasePrecheck[\s\S]*?if\s*\(\s*!canRunReleasePrecheck\.value\s*\)/,
  '放行预检执行必须在点击处理时复核动作锁，审批中只能查看检查项或处理审批。'
)

assert.match(
  detailPage,
  /submitQualityReject[\s\S]*?if\s*\(\s*!canQualityReject\.value\s*\)/,
  '质量拒收提交处理必须复核动作锁，避免审批中绕过普通动作锁。'
)

assert.match(
  apiSource,
  /releaseActionLocked\?: boolean/,
  '批次列表 API 类型必须暴露 releaseActionLocked，列表不能靠文案猜测放行审批中状态。'
)

assert.match(
  listPage,
  /row\.releaseActionLocked === true && !hasGoldenFingerActionBypass\.value[\s\S]*'release-locked'/,
  '批次列表行操作状态机必须识别放行审批中并让普通用户进入 release-locked 分支，金手指用户可绕过。'
)

const releaseLockedListBranch = listPage.match(
  /resolveBatchVoidOperationState\(row\) === 'release-locked'[\s\S]*?<div v-else class="edhr-batch-page__actions">/
)?.[0]

assert.ok(releaseLockedListBranch, '批次列表必须保留放行审批中 release-locked 行操作分支。')

assert(
  releaseLockedListBranch.includes('编辑') &&
    !releaseLockedListBranch.includes('作废') &&
    !releaseLockedListBranch.includes('releaseActionLockReason') &&
    !releaseLockedListBranch.includes('放行审批中'),
  '放行审批中列表行只能显示编辑入口，不显示操作列行内放行锁定说明，也不能落入普通作废分支。'
)

assert.match(
  listPage,
  /openVoidDialog[\s\S]*?if\s*\(row\.releaseActionLocked === true && !hasGoldenFingerActionBypass\.value\)/,
  '列表作废入口必须在点击处理时复核普通用户 releaseActionLocked，金手指用户可绕过。'
)
