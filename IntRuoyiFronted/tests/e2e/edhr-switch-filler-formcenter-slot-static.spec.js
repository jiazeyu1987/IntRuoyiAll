const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr/ExecutionPage.vue'),
  'utf8'
)
const detailSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)
const actionPanelSource = fs.readFileSync(
  path.join(root, 'src/views/form-center/business-action/ActionFormPanel.vue'),
  'utf8'
)

const navigateStart = source.indexOf('const navigateToAssistBatchTask = async (')
const navigateEnd = source.indexOf('const handleSelectAssistFillerSwitchItem', navigateStart)
assert.ok(navigateStart >= 0 && navigateEnd > navigateStart, '切换填写人批次任务导航函数必须存在。')

const navigateBlock = source.slice(navigateStart, navigateEnd)
const openTaskIndex = navigateBlock.indexOf('const opened = await openEdhrBatchTask')
const formCenterBranchIndex = navigateBlock.indexOf('opened.formCenterInstanceId && opened.formTemplateId')
const executionGuardIndex = navigateBlock.indexOf('if (!opened.executionId)')

assert.ok(openTaskIndex >= 0, '切换填写人必须调用正式 openTask 接口。')
assert.ok(
  formCenterBranchIndex > openTaskIndex && formCenterBranchIndex < executionGuardIndex,
  '切换到损耗单等 FormCenter 表单槽位时，必须先走表单槽位详情页分支，不能先要求 executionId。'
)
assert.match(
  navigateBlock,
  /path:\s*['"]\/mes\/pro\/feedback\/edhr-batch-execution\/detail['"]/,
  'FormCenter 表单槽位候选必须跳转批次详情页承载路线表单抽屉。'
)
assert.match(
  navigateBlock,
  /openRouteForm:\s*['"]1['"]/,
  'FormCenter 表单槽位候选必须携带 openRouteForm=1 自动打开损耗单。'
)
assert.match(
  navigateBlock,
  /batchTaskId:\s*String\(row\.id\)/,
  'FormCenter 表单槽位候选必须把所选批次任务写入 route query。'
)
assert.match(
  navigateBlock,
  /workTaskId:\s*String\(openedWorkTaskId\)/,
  'FormCenter 表单槽位候选必须把后端确认的工作任务写入 route query。'
)
assert.match(
  navigateBlock,
  /assistUserId:\s*String\(openedAssistUserId\)/,
  'FormCenter 表单槽位候选必须保留所选填写人，避免回到当前登录人上下文。'
)

assert.match(
  detailSource,
  /const\s+resolveRouteFormAssistUserId\s*=\s*\(\s*row:\s*EdhrBatchExecutionTaskRespVO\s*\)[\s\S]*parsePositiveRouteQueryId\(route\.query\.assistUserId\)/,
  '批次详情自动打开路线表单时必须从 route query 解析 assistUserId。'
)
const autoOpenStart = detailSource.indexOf('const autoOpenRouteFormFromRoute = async () =>')
const autoOpenEnd = detailSource.indexOf('const loadReviewTimeline = async', autoOpenStart)
assert.ok(autoOpenStart >= 0 && autoOpenEnd > autoOpenStart, '批次详情 openRouteForm 自动打开函数必须存在。')
const autoOpenBlock = detailSource.slice(autoOpenStart, autoOpenEnd)
assert.ok(
  !/canOpenTask\(routeQueryTask\)/.test(autoOpenBlock),
  'openRouteForm=1 来自已授权切换结果，批次详情二次自动打开不得用当前用户 allowedActions 预拦截，应交给 task/open 最终授权。'
)
assert.match(
  autoOpenBlock,
  /canAutoOpenRouteFormTask\(routeQueryTask\)/,
  '批次详情自动打开路线表单只能做槽位、状态和 activeWorkTask 基础校验。'
)
assert.match(
  detailSource,
  /openEdhrBatchTask\(\{[\s\S]*assistUserId:\s*resolveRouteFormAssistUserId\(row\)[\s\S]*\}\)/,
  '批次详情二次 openTask 必须继续传递所选填写人，避免张可莹上下文丢失。'
)
assert.match(
  detailSource,
  /formTemplateJimuSchemaJson:\s*opened\?\.formTemplateJimuSchemaJson/,
  '批次详情动态表单抽屉必须使用 task/open 返回的模板 Jimu 快照，不能依赖模板管理查询权限。'
)
assert.match(
  detailSource,
  /formTemplateRecognizedFields:\s*opened\?\.formTemplateRecognizedFields/,
  '批次详情动态表单抽屉必须使用 task/open 返回的识别字段快照，避免另调 FormCenter 管理接口。'
)

const loadTemplateStart = actionPanelSource.indexOf('const loadTemplateVersionForActionForm = async')
const loadTemplateEnd = actionPanelSource.indexOf('const blockerTitle = computed', loadTemplateStart)
assert.ok(loadTemplateStart >= 0 && loadTemplateEnd > loadTemplateStart, 'ActionFormPanel 模板加载函数必须存在。')
const loadTemplateBlock = actionPanelSource.slice(loadTemplateStart, loadTemplateEnd)
assert.ok(
  loadTemplateBlock.includes('resolveEmbeddedTemplateVersionForActionForm()'),
  'ActionFormPanel 必须优先从业务 openTask 内嵌模板快照构造渲染模板。'
)
assert.doesNotMatch(
  loadTemplateBlock,
  /getTemplateVersion\(templateId,\s*versionNo\)/,
  '运行态 FormCenter 抽屉不得调用模板管理查询接口，避免普通填写人命中 403 或请求地址不存在。'
)
assert.match(
  loadTemplateBlock,
  /动态表单运行态缺少 openTask 模板快照，无法渲染/,
  '运行态缺少 openTask 内嵌模板快照时必须可见失败，不能降级请求模板管理接口。'
)

console.log('edhr-switch-filler-formcenter-slot-static PASS')
