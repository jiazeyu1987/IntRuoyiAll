const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeGraph = read('src/views/mes/pro/route/RouteFlowGraphDesigner.vue')
const routeCandidateEntry = fs.existsSync(path.join(root, 'src/views/mes/pro/route/routeCandidateEntry.ts'))
  ? read('src/views/mes/pro/route/routeCandidateEntry.ts')
  : ''

function assertIncludes(source, expected, label) {
  assert.ok(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

function assertNotIncludes(source, unexpected, label) {
  assert.ok(!source.includes(unexpected), `${label}: unexpected ${JSON.stringify(unexpected)}`)
}

assertIncludes(routeGraph, 'data-flow-action="open-capacity-override-dialog"', '工作站卡片必须提供产能覆盖按钮')
assertNotIncludes(
  routeGraph,
  '<div v-if="isEditable" class="route-flow-graph-designer__workstation-capacity-actions">',
  '工作站卡片产能覆盖按钮不得被 isEditable 隐藏，生效版本也要展示入口'
)
assertNotIncludes(
  routeGraph,
  ':disabled="!isEditable || capacityOverrideButtonDisabled"',
  '覆盖产能链接不得因生效版本只读上下文置灰，点击入口应进入候选版本流程'
)
assertNotIncludes(
  routeGraph,
  `<el-button
                      :disabled="capacityOverrideButtonDisabled"
                      :loading="capacityOverrideSaving"
                      :title="capacityOverrideButtonTitle"
                      data-flow-action="open-capacity-override-dialog"`,
  '产能覆盖入口不得使用 Element Plus el-button，避免继承只读 el-form 的禁用态'
)
assertIncludes(
  routeGraph,
  'class="route-flow-graph-designer__capacity-override-button"',
  '产能覆盖入口必须使用不继承 el-form disabled 的原生按钮样式'
)
assertIncludes(routeGraph, 'data-testid="route-flow-capacity-override-dialog"', '产能覆盖弹框必须有稳定测试标识')
assertIncludes(routeGraph, 'capacityOverrideDialogVisible', '产能覆盖弹框必须有显式打开状态')
assertIncludes(routeGraph, 'capacityOverrideForm.hourlyCapacity', '产能覆盖弹框必须输入产能/h')
assertIncludes(routeGraph, 'submitCapacityOverride', '产能覆盖弹框确认必须立即保存')
assertIncludes(routeGraph, 'capacityMode: capacityModeToSave', '确认后必须按差异判定保存产能模式')
assertIncludes(routeGraph, "capacityModeToSave === 'MANUAL_OVERRIDE'", '只有差异化覆盖才写入 MANUAL_OVERRIDE 的产能/h')
assertIncludes(routeGraph, 'ProRouteApi.saveScheduleConfig(payload)', '产能覆盖必须复用现有排产配置保存接口')
assertIncludes(routeGraph, 'ProRouteApi.getScheduleConfigListByRouteVersion(routeVersionId)', '保存后必须回读排产配置')
assertIncludes(routeGraph, 'syncCapacityOverrideDraftBaseline', '保存后必须同步草稿和 baseline，避免顶部保存重复提交')
const disabledStart = routeGraph.indexOf('const capacityOverrideButtonDisabled = computed(')
assert.ok(disabledStart >= 0, '必须定义产能覆盖按钮禁用条件')
const disabledEnd = routeGraph.indexOf('\n)', disabledStart)
const disabledBlock = routeGraph.slice(disabledStart, disabledEnd >= 0 ? disabledEnd : routeGraph.length)
assertNotIncludes(
  disabledBlock,
  '!isDraftCandidateEdit.value',
  '产能覆盖按钮不得因非草稿候选上下文直接置灰，点击入口应显式提示前置条件'
)
assertNotIncludes(
  disabledBlock,
  '!isEditable.value',
  '产能覆盖按钮不得因生效版本只读上下文置灰，点击入口应进入候选版本流程'
)
assertNotIncludes(
  disabledBlock,
  'selectedProcessAttributesLoading.value ||',
  '生效版本产能覆盖入口不得因工序属性加载中置灰，创建/进入候选版本只需要当前选中工序'
)
assertIncludes(
  disabledBlock,
  '(isDraftCandidateEdit.value &&\n      selectedProcessAttributesLoading.value)',
  '只有草稿候选版本真正打开弹框前才需要等待工序属性加载完成'
)
assertIncludes(
  routeGraph,
  "requireCandidateRouteVersionId('产能覆盖打开')",
  '产能覆盖弹框真正打开前必须先校验草稿候选版本上下文'
)
assertIncludes(
  routeGraph,
  'const capacityOverrideRouteVersionId = ref<number | null>(null)',
  '产能覆盖弹框必须固定打开时确认过的候选 routeVersionId，避免保存时受路由 query 瞬时变化影响'
)
assertIncludes(
  routeGraph,
  "capacityOverrideRouteVersionId.value = requireCandidateRouteVersionId('产能覆盖打开')",
  '产能覆盖弹框打开时必须记录已校验的候选版本编号'
)
assertIncludes(
  routeGraph,
  'const routeVersionId = capacityOverrideRouteVersionId.value',
  '产能覆盖保存必须使用弹框打开时固定的候选版本编号'
)
assertIncludes(
  routeGraph,
  'routeScheduleConfig?.shiftHours ?? routeProcess?.shiftHours',
  '候选版本排产配置尚未落库时，产能覆盖必须从路线工序行读取班次小时，不能把 DRAFT 快照读取缺口误判为缺少班次小时'
)
assertIncludes(
  routeGraph,
  'normalizeRouteQueryText(route.query.capacityOverride),\n    props.routeVersionEditContext?.routeVersionId',
  '创建候选版本并切换路由后，产能覆盖自动打开必须监听 capacityOverride 与候选版本上下文变化'
)
assertIncludes(
  routeGraph,
  'ensureCapacityOverrideCandidateContext',
  '产能覆盖点击入口必须先确保候选版本上下文，而不是直接抛候选版本错误'
)
assertIncludes(
  routeGraph,
  'ensureSameSourceDraftCandidateForProductionConfig',
  '缺少草稿候选上下文时，产能覆盖入口必须复用统一候选版本入口'
)
assertIncludes(
  routeCandidateEntry,
  'ProRouteApi.createRouteCandidateVersion',
  '统一候选版本入口必须走正式候选版本创建接口'
)
assertIncludes(
  routeCandidateEntry,
  'ProRouteApi.getRouteVersionList(routeId)',
  '统一候选版本入口必须先查询现有路线版本'
)
assertIncludes(
  routeCandidateEntry,
  'resolveSameSourceDraftCandidateForProductionConfig',
  '统一候选版本入口必须复用当前激活版本已有 DRAFT 候选版本，不能只尝试新建'
)
assertIncludes(
  routeCandidateEntry,
  'draftCandidates.length > 1',
  '存在多个 DRAFT 候选版本时必须显式阻塞，不能猜测写入目标版本'
)
assertIncludes(
  routeCandidateEntry,
  'currentDraftCandidate.sourceRouteVersionId !== routeInfo.activeRouteVersionId',
  '复用 DRAFT 候选版本时必须限定来源为当前 active 版本'
)
assertIncludes(
  routeCandidateEntry,
  'READY_TO_PUBLISH_ROUTE_VERSION_STATUS',
  'READY_TO_PUBLISH 必须作为未完成候选阻断新建替代草稿'
)
assertIncludes(
  routeCandidateEntry,
  'buildRouteCandidateEditQuery',
  '统一候选版本入口必须提供候选编辑路由参数构造函数'
)
assertIncludes(
  routeGraph,
  'capacityOverride: CAPACITY_OVERRIDE_AUTO_OPEN_QUERY_VALUE',
  '创建候选版本后必须携带自动打开产能覆盖的路由标记'
)
assertIncludes(
  routeGraph,
  'tryOpenCapacityOverrideFromRouteQuery',
  '进入候选版本编辑页后必须根据路由标记自动打开产能覆盖弹框'
)
const autoOpenStart = routeGraph.indexOf('const tryOpenCapacityOverrideFromRouteQuery = async () => {')
assert.ok(autoOpenStart >= 0, '必须定义产能覆盖路由标记自动打开函数')
const autoOpenEnd = routeGraph.indexOf('\nconst syncCapacityOverrideDraftBaseline', autoOpenStart)
const autoOpenBlock = routeGraph.slice(autoOpenStart, autoOpenEnd >= 0 ? autoOpenEnd : routeGraph.length)
assertNotIncludes(
  autoOpenBlock,
  'clearCapacityOverrideAutoOpenQuery',
  '自动打开产能覆盖弹框时不得清除 capacityOverride 路由标记；标记必须在取消或保存成功关闭弹框后清理'
)
assertIncludes(
  routeGraph,
  'const closeCapacityOverrideDialog = async () => {',
  '产能覆盖弹框必须提供统一关闭函数，用于关闭后清理自动打开路由标记'
)
assertIncludes(
  routeGraph,
  '@click="closeCapacityOverrideDialog"',
  '产能覆盖弹框取消按钮必须走统一关闭函数'
)
assertNotIncludes(
  routeGraph,
  "message.error(resolveErrorMessage(error, '产能覆盖打开失败'))",
  '缺少候选版本时不得只弹产能覆盖打开失败，必须先走候选版本创建/进入路径'
)

assertIncludes(routeGraph, 'route-flow-graph-designer__workstation-capacity-original', '覆盖模式必须保留原班次产能')
assertIncludes(routeGraph, 'text-decoration: line-through', '原班次产能必须以删除线展示')
assertIncludes(routeGraph, '覆盖产能：', '覆盖产能/h 必须可见')
assertIncludes(routeGraph, '覆盖班次产能：', '覆盖班次产能必须可见')
assertIncludes(routeGraph, 'calculateCapacityOverrideShiftCapacity', '覆盖班次产能必须按产能/h和班次小时计算')

assertIncludes(routeGraph, 'PROCESS_DETAIL_HIDDEN_FIELD_KEYS', '流转图关注列必须支持隐藏字段集合')
assertIncludes(routeGraph, "'shiftCapacity'", '排产策略必须被隐藏字段集合覆盖')
assertIncludes(routeGraph, "'resourceStatus'", '资源状态必须被隐藏字段集合覆盖')
assertIncludes(routeGraph, "schedule: ['workstation']", '排产来源焦点必须回到工作站卡片')
assertNotIncludes(routeGraph, "schedule: ['shiftCapacity', 'productionQuantityFactor']", '排产来源焦点不得继续依赖隐藏的排产策略行')
assertNotIncludes(routeGraph, 'catch {}', '产能覆盖流程不得静默吞错')

console.log('mes-route-flow-capacity-override-button-static PASS')
