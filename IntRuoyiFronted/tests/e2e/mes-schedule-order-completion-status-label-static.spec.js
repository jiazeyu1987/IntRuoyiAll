const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const sliceBetween = (source, startMarker, endMarker) => {
  const start = source.indexOf(startMarker)
  const end = source.indexOf(endMarker, start)
  assert.ok(start >= 0 && end > start, `无法定位合同片段：${startMarker}`)
  return source.slice(start, end)
}

const quickFilterSource = sliceBetween(
  pageSource,
  'const scheduleOrderQuickFilterDefinitions',
  'const scheduleOrderMultiFilterDefinitions'
)
const multiFilterSource = sliceBetween(
  pageSource,
  'const scheduleOrderMultiFilterDefinitions',
  'const processDialogVisible'
)

for (const [name, source] of [
  ['快速筛选', quickFilterSource],
  ['多维筛选', multiFilterSource]
]) {
  assert.ok(
    /key:\s*'completionFilter'[\s\S]*?label:\s*'完成状态'[\s\S]*?queryParamKey:\s*'completionFilter'/.test(
      source
    ),
    `${name}必须使用“完成状态”表达 completionFilter 的业务含义。`
  )
  assert.ok(
    source.includes('options: scheduleOrderCompletionFilterOptions'),
    `${name}必须继续使用正式完成状态选项。`
  )
}

assert.ok(
  !pageSource.includes("label: '完成筛选'"),
  '排产工单页面不得继续显示含义不清的“完成筛选”。'
)

console.log('PASS: MES schedule order completion status label static contract')
