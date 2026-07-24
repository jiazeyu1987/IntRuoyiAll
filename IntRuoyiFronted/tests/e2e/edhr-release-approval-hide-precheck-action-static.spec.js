const assert = require('assert')
const fs = require('fs')
const path = require('path')

const detailPath = path.join(
  process.cwd(),
  'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')

const actionItemsStart = detail.indexOf('const buildReleaseStageActionItems = (stageKey: ReleaseStageKey)')
const actionItemsEnd = detail.indexOf('const releaseStageActionItems', actionItemsStart)
assert.ok(actionItemsStart >= 0 && actionItemsEnd > actionItemsStart, '必须能定位右侧阶段动作构建函数。')
const actionItemsSource = detail.slice(actionItemsStart, actionItemsEnd)

const extractStageActions = (stageKey) => {
  const marker = `if (stageKey === '${stageKey}') {`
  const start = actionItemsSource.indexOf(marker)
  assert.ok(start >= 0, `必须能定位 ${stageKey} 阶段动作列表。`)
  let depth = 0
  let bodyStart = -1
  for (let index = start; index < actionItemsSource.length; index += 1) {
    const char = actionItemsSource[index]
    if (char === '{') {
      depth += 1
      if (bodyStart < 0) bodyStart = index + 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0 && bodyStart >= 0) {
        return actionItemsSource.slice(bodyStart, index)
      }
    }
  }
  throw new Error(`无法截取 ${stageKey} 阶段动作列表。`)
}

const releaseApprovalActionsMatch = detail.match(
  /const buildReleaseStageActionItems = \(stageKey: ReleaseStageKey\): ReleaseStageActionItem\[] =>/
)
assert.ok(releaseApprovalActionsMatch, '右侧动作列表必须由 buildReleaseStageActionItems 按阶段生成。')

const releaseApprovalActions = extractStageActions('release-approval')
assert.match(
  releaseApprovalActions,
  /key: 'release-approval-signature'[\s\S]*label: '放行审批'[\s\S]*disabled: !canSubmitRelease\.value[\s\S]*onClick: openReleaseSignatureConfirmDialog/,
  '放行审批阶段右侧只保留电子签名确认放行动作。'
)
assert.doesNotMatch(
  releaseApprovalActions,
  /onClick: openPrimaryReleaseAction|openReleaseTransactionDialog|releaseApprovalDrawerVisible\.value = true/,
  '放行审批阶段右侧不得继续走旧提交弹框或审批抽屉。'
)
assert.doesNotMatch(releaseApprovalActions, /traceAction|追溯记录/, '放行审批阶段不得重复展示追溯记录入口。')
assert.doesNotMatch(
  releaseApprovalActions,
  /label: '放行预检'|key: 'release-check'/,
  '放行预检完成后进入审批阶段，右侧当前阶段操作区不得继续显示放行预检动作。'
)

const precheckActions = extractStageActions('precheck')
assert.match(
  precheckActions,
  /label: '放行预检'[\s\S]*onClick: openReleaseCheckGroup/,
  '放行预检阶段必须继续显示并绑定放行预检入口。'
)

console.log('PASS edhr release approval hide precheck action static contract')
