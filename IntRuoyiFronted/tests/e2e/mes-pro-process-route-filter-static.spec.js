const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const processPageSource = readText('src/views/mes/pro/process/index.vue')
const processApiSource = readText('src/api/mes/pro/process/index.ts')
const routeCandidateEntrySource = fs.existsSync(
  path.join(root, 'src/views/mes/pro/route/routeCandidateEntry.ts')
)
  ? readText('src/views/mes/pro/route/routeCandidateEntry.ts')
  : ''

assert.match(
  processApiSource,
  /export interface ProProcessRouteVO \{[\s\S]*id: number[\s\S]*code: string[\s\S]*name: string[\s\S]*\}/,
  '工序分页类型必须包含所属工艺路线精简 VO。'
)
assert.match(
  processApiSource,
  /routeList\?: ProProcessRouteVO\[\]/,
  '工序分页 VO 必须返回 routeList。'
)
assert.match(
  processApiSource,
  /routeProcessId\?: number/,
  '所属工艺路线 VO 必须包含路线工序编号，用于直达工艺流程节点。'
)
assert.match(
  processApiSource,
  /shiftCapacity\?: number/,
  '所属工艺路线 VO 必须包含该路线工序的排产产能。'
)
assert.match(
  processApiSource,
  /routeCapacityConflict\?: boolean/,
  '工序分页 VO 必须返回多路线排产产能是否不一致。'
)
assert.match(
  processApiSource,
  /routeCapacityConflictMessage\?: string/,
  '工序分页 VO 必须返回多路线产能差异的用户提示。'
)
assert.match(
  processPageSource,
  /import \{[\s\S]*\bProRouteApi\b[\s\S]*\} from '@\/api\/mes\/pro\/route'/,
  '工序设置列表必须复用工艺路线精简列表接口作为筛选选项。'
)
assert.match(
  processPageSource,
  /routeOptions\.value = await ProRouteApi\.getRouteSimpleList\(\)/,
  '工序设置列表必须调用工艺路线精简列表接口加载筛选选项。'
)
assert.match(
  processPageSource,
  /const routeOptions = ref<[\s\S]*ProRouteVO\[\]>\(\[\]\)/,
  '工序设置列表必须维护工艺路线筛选选项。'
)
assert.match(
  processPageSource,
  /const routeQuickFilterOptions = computed<[\s\S]*TableQuickFilterOption\[\]>\(\(\) =>[\s\S]*label: item\.name[\s\S]*value: item\.id!/,
  '工序设置列表必须把工艺路线转换成快速筛选选项数组。'
)
assert.match(
  processPageSource,
  /key: 'routeId'[\s\S]*label: '工艺路线'[\s\S]*type: 'select'[\s\S]*options: routeQuickFilterOptions\.value/,
  '快速筛选必须包含工艺路线下拉项。'
)
assert.match(
  processPageSource,
  /routeId: undefined as number \| undefined/,
  '查询参数必须包含 routeId。'
)
assert.match(
  processPageSource,
  /queryParams\.routeId = parsed/,
  '快速筛选选择工艺路线后必须写入 routeId。'
)
assert.match(
  processPageSource,
  /routeId: queryParams\.routeId/,
  '分页请求必须传递 routeId。'
)
assert.match(
  processPageSource,
  /key: 'routeList', label: '所属工艺路线'/,
  '列配置必须包含所属工艺路线。'
)
assert.match(
  processPageSource,
  /label="所属工艺路线"[\s\S]*prop="routeList"/,
  '表格必须展示所属工艺路线列。'
)
assert.match(
  processPageSource,
  /const resolveProcessRouteTags = \(routeList\?: ProProcessVO\['routeList'\]\) =>[\s\S]*routeList\?\.filter/,
  '所属工艺路线列必须解析后端返回的多个路线。'
)

const routeListColumnMarker = `v-if="isProcessColumnVisible('routeList')"`
const routeListColumnMarkerIndex = processPageSource.indexOf(routeListColumnMarker)
assert.notEqual(routeListColumnMarkerIndex, -1, '必须能定位所属工艺路线列标记。')
const routeListColumnStart = processPageSource.lastIndexOf('<el-table-column', routeListColumnMarkerIndex)
const routeListColumnEnd =
  processPageSource.indexOf('</el-table-column>', routeListColumnMarkerIndex) + '</el-table-column>'.length
const routeListColumnSource = processPageSource.slice(routeListColumnStart, routeListColumnEnd)
assert.ok(routeListColumnSource, '必须能定位所属工艺路线列源码。')
assert.doesNotMatch(
  routeListColumnSource,
  /show-overflow-tooltip/,
  '所属工艺路线列不能启用整列 overflow tooltip，避免把 routeList 原始对象或长编码尾部显示出来。'
)
assert.match(
  routeListColumnSource,
  /v-for=/,
  '所属工艺路线列必须循环展示全部有效路线。'
)
assert.match(
  routeListColumnSource,
  /resolveProcessRouteTags\(scope\.row\.routeList\)/,
  '所属工艺路线列必须通过多路线标签渲染。'
)
assert.match(
  routeListColumnSource,
  /routeCapacityConflict/,
  '所属工艺路线列必须展示多路线产能不一致提示。'
)
assert.match(
  processPageSource,
  /const openRouteCapacityOverride = async \(row: ProProcessVO\) =>[\s\S]*name: 'MesProRouteEdit'[\s\S]*tab: 'flow'[\s\S]*routeProcessId/,
  '产能不一致时必须能跳转到工艺流程并定位路线工序，供用户使用覆盖产能。'
)
assert.match(
  processPageSource,
  /ensureSameSourceDraftCandidateForProductionConfig\(/,
  '产能覆盖外部入口必须复用统一候选版本入口，避免各页面复制不同候选版本逻辑。'
)
assert.match(
  processPageSource,
  /buildRouteCandidateEditQuery\(candidateResult\.candidate,[\s\S]*capacityOverride:\s*'1'/,
  '产能覆盖外部入口跳转候选版本后必须自动打开产能覆盖弹框。'
)
assert.match(
  routeCandidateEntrySource,
  /export const ensureSameSourceDraftCandidateForProductionConfig/,
  '统一候选版本入口必须导出 ensureSameSourceDraftCandidateForProductionConfig。'
)
assert.match(
  routeCandidateEntrySource,
  /ProRouteApi\.getRouteVersionList\(routeId\)/,
  '统一候选版本入口必须先查找当前路线已有 DRAFT 候选版本。'
)
assert.match(
  routeCandidateEntrySource,
  /ProRouteApi\.createRouteCandidateVersion\(\{[\s\S]*sourceRouteVersionId:\s*routeInfo\.activeRouteVersionId/,
  '统一候选版本入口没有可复用 DRAFT 候选版本时必须从当前 active 版本创建候选版本。'
)
assert.match(
  routeCandidateEntrySource,
  /export const buildRouteCandidateEditQuery[\s\S]*routeVersionId:\s*String\(candidate\.id\)[\s\S]*routeVersionNo:\s*candidate\.versionNo[\s\S]*routeVersionStatus:\s*candidate\.lifecycleStatus/,
  '统一候选版本入口必须构造候选版本三要素路由参数。'
)
assert.match(
  processPageSource,
  /\.process-route-tag[\s\S]*overflow: hidden;[\s\S]*text-overflow: ellipsis;[\s\S]*white-space: nowrap;/,
  '所属工艺路线标签必须在标签内部裁剪长文本，不能让编码尾部溢出到红框区域。'
)
assert.match(
  processPageSource,
  /\.process-route-tag :deep\(\.el-tag__content\)[\s\S]*overflow: hidden;[\s\S]*text-overflow: ellipsis;[\s\S]*white-space: nowrap;/,
  'Element Plus 标签内容区域必须同步裁剪长路线名称。'
)

console.log('PASS: mes pro process route filter static contract')
