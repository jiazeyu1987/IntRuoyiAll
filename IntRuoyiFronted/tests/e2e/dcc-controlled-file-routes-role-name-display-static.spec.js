const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routePage = readSource('src/views/dcc/controlled-file/routes/index.vue')
const sharedUtils = readSource('src/views/dcc/controlled-file/shared/utils.ts')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.match(sharedUtils, /\[900332,\s*'文控'\]/, '固定审批角色 900332 必须显示为文控。')
assert.match(sharedUtils, /\[900333,\s*'部门负责人'\]/, '固定审批角色 900333 必须显示为部门负责人。')
assert.match(sharedUtils, /\[900334,\s*'部门授权代表'\]/, '固定审批角色 900334 必须显示为部门授权代表。')
assert.match(sharedUtils, /\[900335,\s*'编制部门负责人'\]/, '固定审批角色 900335 必须显示为编制部门负责人。')
assert.match(sharedUtils, /\[900336,\s*'授权代表'\]/, '固定审批角色 900336 必须显示为授权代表。')

const loadRouteSubjectLookups = extractBetween(
  routePage,
  'const loadRouteSubjectLookups = async () => {',
  'const handleQuery = async'
)
assert.match(
  loadRouteSubjectLookups,
  /positions\.value = positionList\s*(?:\r?\n|\s)/,
  '路线节点显示必须保留全部审批角色用于历史路线名称解析。'
)
assert.doesNotMatch(
  loadRouteSubjectLookups,
  /positions\.value = positionList\.filter\(\(item\) => item\.active\)/,
  '路线节点显示不得只保留 active 审批角色，否则历史路线会回退成 审批角色#ID。'
)

assert.ok(routePage.includes('const activePositions = computed(() => positions.value.filter((item) => item.active))'))
assert.ok(routePage.includes('positions: activePositions.value'), '路线编辑弹窗仍应只使用启用审批角色选项。')

const handleQuery = extractBetween(
  routePage,
  'const handleQuery = async (resetPage = true) => {',
  'const handleRoutePagination ='
)
const lookupLoadIndex = handleQuery.indexOf('await loadRouteSubjectLookups()')
const routePageLoadIndex = handleQuery.indexOf('getApprovalRoutePage(queryParams)')
assert.ok(
  lookupLoadIndex >= 0,
  '审批路线首屏查询必须先加载审批角色/用户字典，否则 POSITION 节点会被渲染为 -。'
)
assert.ok(
  lookupLoadIndex < routePageLoadIndex,
  '审批路线列表赋值前必须完成审批角色/用户字典加载，避免节点2等历史角色短暂或稳定显示为空。'
)

const formatRouteNodeSubject = extractBetween(
  routePage,
  'const formatRouteNodeSubject = (node: ControlledFileApprovalRouteNodeVO) => {',
  'const formatRouteNodeAssignees ='
)
const positionNameIndex = formatRouteNodeSubject.indexOf('resolveRouteNodePositionNames(node)')
const explicitLabelIndex = formatRouteNodeSubject.indexOf('const explicitLabel =')
assert.ok(positionNameIndex >= 0, 'POSITION 节点必须通过 resolveRouteNodePositionNames(node) 解析审批角色名称。')
assert.ok(explicitLabelIndex >= 0, '函数仍需保留非 POSITION 节点的显式标签解析。')
assert.ok(
  positionNameIndex < explicitLabelIndex,
  'POSITION 节点必须先按 candidateSourceIds 解析审批角色名称，不能优先显示 subjectLabel/subjectName 中的权限编码。'
)
assert.match(
  routePage,
  /const isRouteNodeTechnicalLabel = \(value: string\) =>/,
  '路线节点必须识别并过滤 doc-control-review、matrix-review 和 审批角色#ID 等技术标签。'
)
assert.doesNotMatch(
  formatRouteNodeSubject,
  /const explicitLabel =[\s\S]*if \(explicitLabel\) \{[\s\S]*return explicitLabel[\s\S]*\}[\s\S]*if \(node\.candidateSourceType === 'POSITION'\)/,
  '不得在 POSITION 解析前直接返回 subjectLabel/subjectName。'
)

console.log('PASS: DCC controlled file routes role-name display static contract')
