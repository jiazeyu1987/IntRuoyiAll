const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPath = path.join(
  repoRoot,
  'src',
  'views',
  'mes',
  'pro',
  'edhr-batch',
  'BatchExecutionDetailPage.vue'
)
const detail = fs.readFileSync(detailPath, 'utf8')

const preSpecialIndex = detail.indexOf('v-for="task in preProcessSpecialTaskEntries"')
const processGroupIndex = detail.indexOf('v-for="processGroup in processTaskGroups"')
const postSpecialIndex = detail.indexOf('v-for="task in postProcessSpecialTaskEntries"')
const releaseIndex = detail.indexOf('class="edhr-batch-detail__review-item edhr-batch-detail__release-process-item"')

assert.notEqual(preSpecialIndex, -1, '来料检等前置特殊节点必须使用独立展示集合。')
assert.notEqual(processGroupIndex, -1, '普通工序组必须保留在左侧工序列表。')
assert.notEqual(postSpecialIndex, -1, '灭菌和成品检等收尾特殊节点必须使用独立展示集合。')
assert.notEqual(releaseIndex, -1, '放行虚拟工序必须保留在列表最后。')
assert(
  preSpecialIndex < processGroupIndex &&
    processGroupIndex < postSpecialIndex &&
    postSpecialIndex < releaseIndex,
  '左侧顺序必须为前置特殊节点、普通工序、收尾特殊节点、放行。'
)

assert(
  detail.includes(
    'task.nodeType === EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT'
  ),
  '前置特殊节点集合必须只包含来料检报告。'
)
assert(
  detail.includes('const postProcessSpecialTaskEntries = computed(() =>') &&
    detail.includes('task.nodeType !== EDHR_BATCH_NODE_INCOMING_INSPECTION_REPORT'),
  '收尾特殊节点集合必须排除来料检报告。'
)
assert(
  !detail.includes('v-for="task in specialTaskEntries"'),
  '模板不得继续把全部特殊节点整体渲染在普通工序之前。'
)

const readStyleBlock = (selector) => {
  const start = detail.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = detail.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return detail.slice(start, end)
}

const processNameStyle = readStyleBlock('.edhr-batch-detail__review-process-name')
for (const declaration of [
  'overflow: hidden',
  'text-overflow: ellipsis',
  'white-space: nowrap'
]) {
  assert(
    processNameStyle.includes(declaration),
    `工序编码和名称必须保持单行省略：${declaration}`
  )
}

assert(
  detail.includes(':title="processGroup.processName || \'--\'"') &&
    detail.includes("{{ processGroup.processName || '--' }}"),
  '普通工序必须显示工序名称并保留完整 title 提示。'
)
assert(
  !detail.includes("{{ processGroup.processCode || '--' }} {{ processGroup.processName || '--' }}"),
  '普通工序不得继续拼接显示工序编码。'
)
assert(
  !detail.includes('class="edhr-batch-detail__process-task-form-list"'),
  '左侧工序列表不得重新堆叠辅助表单，辅助表单继续在右侧面板展示。'
)

console.log('PASS: eDHR batch process order and compact text layout static contract')
