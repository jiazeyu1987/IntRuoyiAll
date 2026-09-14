const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '..', '..')
const pagePath = path.join(
  root,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const source = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const reportMarker = 'data-team-leader-report-workbench'
const reportStart = source.indexOf(reportMarker)
assert.ok(reportStart >= 0, '班组长报工列表必须保留稳定区域标记。')
const reportEnd = source.indexOf('<ContentWrap', reportStart + reportMarker.length)
assert.ok(reportEnd > reportStart, '必须能定位班组长报工列表区域边界。')
const reportBlock = source.slice(reportStart, reportEnd)

assert.match(
  source,
  /const\s*\{[\s\S]*state:\s*submissionMultiFilterState[\s\S]*updateState:\s*updateSubmissionMultiFilterState[\s\S]*removeCondition:\s*removeSubmissionMultiFilterCondition[\s\S]*\}\s*=\s*useTableMultiFilter\(\s*'mes\.processPool\.teamLeader\.submissions'/,
  '班组长报工筛选必须把 hook 状态和模板事件方法暴露为稳定顶层绑定。'
)
assert.match(
  reportBlock,
  /:multi-filter-state="submissionMultiFilterState"/,
  '班组长报工列表必须直接绑定稳定的多维筛选状态。'
)
assert.match(
  reportBlock,
  /@update:multi-filter-state="updateSubmissionMultiFilterState"[\s\S]*@multi-filter-remove="removeSubmissionMultiFilterCondition"/,
  '班组长报工列表必须直接绑定稳定的多维筛选事件方法。'
)
assert.doesNotMatch(
  reportBlock,
  /submissionMultiFilter\.(?:state|updateState|removeCondition)/,
  '模板渲染阶段不得解引用可能与热更新实例不同步的 hook 包装对象。'
)

console.log('PASS: team leader multi-filter render state static contract')
