const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(repoRoot, 'src', 'views', 'mes', 'pro', 'edhr-batch', 'BatchExecutionDetailPage.vue')
const detail = fs.readFileSync(detailPath, 'utf8')

const sliceBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.ok(start >= 0, `必须能定位 ${label} 起点。`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `必须能定位 ${label} 终点。`)
  return source.slice(start, end)
}

const forbiddenDisplayPredicates = [
  'hasActiveWorkTask',
  'activeWorkTaskId',
  'canOpenTask',
  'canHandlePendingTask',
  'canOperateSpecialNode',
  'allowedActions',
  'EDHR_BATCH_TASK_STATUS_APPROVED',
  'EDHR_BATCH_TASK_STATUS_SKIPPED',
  'isOptionalTask'
]

const assertDisplayBlockDoesNotUseActionPredicates = (block, label, exceptions = []) => {
  for (const forbidden of forbiddenDisplayPredicates) {
    if (exceptions.includes(forbidden)) continue
    assert.doesNotMatch(
      block,
      new RegExp(forbidden),
      `${label} 是流程展示/追溯信息，不得依赖当前用户动作、活跃待办、完成/跳过状态或可选表单条件。`
    )
  }
}

const processTaskGroupsBlock = sliceBetween(
  detail,
  'const processTaskGroups = computed',
  'const selectedProcessTaskGroup = computed',
  '普通工序导航分组'
)
assert.match(processTaskGroupsBlock, /if \(isSpecialNode\(task\)\) continue/, '普通工序导航分组只能排除特殊节点。')
assertDisplayBlockDoesNotUseActionPredicates(processTaskGroupsBlock, '普通工序导航分组')

const specialTaskEntriesBlock = sliceBetween(
  detail,
  'const specialTaskEntries = computed',
  'const preProcessSpecialTaskEntries = computed',
  '特殊节点导航列表'
)
assert.match(specialTaskEntriesBlock, /isSpecialNode\(task\)/, '特殊节点导航列表必须只按特殊节点身份识别。')
assertDisplayBlockDoesNotUseActionPredicates(specialTaskEntriesBlock, '特殊节点导航列表')

const preProcessSpecialTaskEntriesBlock = sliceBetween(
  detail,
  'const preProcessSpecialTaskEntries = computed',
  'const postProcessSpecialTaskEntries = computed',
  '前置特殊节点导航列表'
)
assert.match(preProcessSpecialTaskEntriesBlock, /nodeType === EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT/, '来料检报告只按节点类型归位。')
assertDisplayBlockDoesNotUseActionPredicates(preProcessSpecialTaskEntriesBlock, '前置特殊节点导航列表')

const postProcessSpecialTaskEntriesBlock = sliceBetween(
  detail,
  'const postProcessSpecialTaskEntries = computed',
  'const selectedOpenableTask = computed',
  '后置特殊节点导航列表'
)
assert.match(postProcessSpecialTaskEntriesBlock, /nodeType !== EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT/, '灭菌/成品检特殊节点只按节点类型归位。')
assertDisplayBlockDoesNotUseActionPredicates(postProcessSpecialTaskEntriesBlock, '后置特殊节点导航列表')

const navBlock = sliceBetween(
  detail,
  '<nav class="edhr-batch-detail__process-panel edhr-batch-detail__process-list edhr-batch-detail__review-list"',
  '</nav>',
  '左侧流程导航模板'
)
assert.match(navBlock, /v-for="processGroup in processTaskGroups"/, '左侧流程导航必须展示普通工序分组。')
assert.match(navBlock, /v-for="task in preProcessSpecialTaskEntries"/, '左侧流程导航必须展示前置特殊节点。')
assert.match(navBlock, /v-for="task in postProcessSpecialTaskEntries"/, '左侧流程导航必须展示后置特殊节点。')
assert.match(navBlock, /RELEASE_VIRTUAL_PROCESS\.label/, '左侧流程导航必须展示放行虚拟节点。')
assertDisplayBlockDoesNotUseActionPredicates(navBlock, '左侧流程导航模板', ['activeWorkTaskId'])
assert.doesNotMatch(navBlock, /v-if="[^"]*(can|allowed|activeWorkTask|APPROVED|SKIPPED|isOptionalTask)/i, '左侧流程导航不得用可操作性条件控制节点是否存在。')

const evidenceGroupsBlock = sliceBetween(
  detail,
  'const selectedProcessEvidenceGroups = computed',
  'type ReleaseTransactionMode',
  '当前工序证据分组'
)
assert.match(evidenceGroupsBlock, /\.filter\(\(item\): item is ProcessEvidenceItem => Boolean\(item\)\)/, '证据分组只能剔除不存在的定义项。')
assert.doesNotMatch(
  evidenceGroupsBlock,
  /\.filter\([\s\S]*(disabled|canOpenTask|canHandlePendingTask|allowedActions|activeWorkTaskId)/,
  '历史、追溯和证据入口不得因为当前不可操作而被过滤消失，只能在条目上 disabled。'
)

const releaseFlowBlock = sliceBetween(
  detail,
  'const releaseFlowStepsViewModel = computed',
  'const terminalReleaseActionItems',
  '放行流程指示图模型'
)
assertDisplayBlockDoesNotUseActionPredicates(releaseFlowBlock, '放行流程指示图模型')
assert.doesNotMatch(
  releaseFlowBlock,
  /canAttemptClose|canRunReleasePrecheck|canOpenArchivePrintDrawer|releaseStageActionItems/,
  '放行流程指示图只能说明流程位置，不能依赖按钮可操作性或当前动作集合。'
)

console.log('PASS: eDHR flow navigation filter static contract')
