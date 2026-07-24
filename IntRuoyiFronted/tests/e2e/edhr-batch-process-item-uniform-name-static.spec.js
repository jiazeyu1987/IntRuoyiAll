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

const readStyleBlock = (selector) => {
  const start = detail.indexOf(`${selector} {`)
  assert.notEqual(start, -1, `必须存在样式块：${selector}`)
  const end = detail.indexOf('}', start)
  assert.notEqual(end, -1, `样式块必须正确闭合：${selector}`)
  return detail.slice(start, end)
}

assert(
  detail.includes(':title="processGroup.processName || \'--\'"') &&
    detail.includes("{{ processGroup.processName || '--' }}"),
  '普通工序卡片必须显示工序名称，不得把工序编码作为主文本。'
)
assert(
  !detail.includes("{{ processGroup.processCode || '--' }} {{ processGroup.processName || '--' }}"),
  '普通工序卡片不得继续拼接显示工序编码。'
)

const reviewListStyle = readStyleBlock('.edhr-batch-detail__review-list')
assert(
  reviewListStyle.includes('--edhr-process-item-height: 48px'),
  '左侧工序列表必须统一定义 48px 卡片高度。'
)

const workbenchStyle = readStyleBlock('.edhr-batch-detail__review-workbench')
assert(
  workbenchStyle.includes('grid-template-columns: 240px minmax(0, 1fr) 260px'),
  '左侧工序栏必须提供 240px 宽度，确保当前工序名称和状态完整显示。'
)

const pendingListStyle = readStyleBlock('.edhr-batch-detail__pending-task-list')
assert(
  pendingListStyle.includes('flex: 0 0 auto'),
  '特殊节点分组不得在滚动列表中被压缩。'
)

const pendingItemStyle = readStyleBlock('.edhr-batch-detail__pending-task-item')
for (const declaration of [
  'height: var(--edhr-process-item-height)',
  'min-height: var(--edhr-process-item-height)',
  'box-sizing: border-box'
]) {
  assert(
    pendingItemStyle.includes(declaration),
    `特殊节点卡片必须使用统一高度且完整显示：${declaration}`
  )
}

const processGroupStyle = readStyleBlock('.edhr-batch-detail__process-task-group')
for (const declaration of [
  'flex: 0 0 var(--edhr-process-item-height)',
  'height: var(--edhr-process-item-height)',
  'box-sizing: border-box'
]) {
  assert(
    processGroupStyle.includes(declaration),
    `普通工序卡片不得被 flex 压缩：${declaration}`
  )
}

const processGroupHeadStyle = readStyleBlock('.edhr-batch-detail__process-task-group-head')
for (const declaration of ['height: 100%', 'min-height: 0', 'box-sizing: border-box']) {
  assert(
    processGroupHeadStyle.includes(declaration),
    `普通工序按钮必须完整填满统一卡片高度：${declaration}`
  )
}

const releaseItemStyle = readStyleBlock('.edhr-batch-detail__review-item')
for (const declaration of [
  'flex: 0 0 var(--edhr-process-item-height)',
  'height: var(--edhr-process-item-height)',
  'min-height: var(--edhr-process-item-height)',
  'box-sizing: border-box'
]) {
  assert(
    releaseItemStyle.includes(declaration),
    `放行卡片必须使用统一高度且不得被压缩：${declaration}`
  )
}

assert(
  !detail.includes('edhr-batch-detail__process-report edhr-batch-detail__review-report'),
  '放行卡片不得保留导致高度不一致的第二行说明。'
)

console.log('PASS: eDHR batch process items use uniform height and process names')
