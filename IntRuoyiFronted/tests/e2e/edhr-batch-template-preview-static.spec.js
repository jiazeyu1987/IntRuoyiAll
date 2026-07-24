const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const resolvePath = (relativePath) => path.join(repoRoot, relativePath)

const listPage = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const router = read('src/router/modules/remaining.ts')
const batchApi = read('src/api/mes/pro/edhr/batchExecution.ts')
const reportApi = read('src/api/mes/pro/batchrecordreport/index.ts')
const templateRuleHelper = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const templateComponentPath = resolvePath('src/views/mes/pro/edhr/components/EdhrExecutionTemplateGuide.vue')
const templatePagePath = resolvePath('src/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue')

assert.ok(fs.existsSync(templatePagePath), '必须新增批次模板预览页 BatchExecutionTemplatePage.vue')
assert.ok(fs.existsSync(templateComponentPath), '必须新增只读模板说明组件 EdhrExecutionTemplateGuide.vue')

const templatePage = fs.readFileSync(templatePagePath, 'utf8')
const templateGuide = fs.readFileSync(templateComponentPath, 'utf8')

const assertIncludes = (content, token, message) => {
  assert.ok(content.includes(token), message)
}

const defaultActions = listPage.match(/<div v-else class="edhr-batch-page__actions">([\s\S]*?)<\/div>/)?.[1] || ''
assert.ok(defaultActions, '批次执行列表必须保留默认行操作区')
assert.ok(!defaultActions.includes('>模板</el-button>'), '批次执行列表行操作区不再直接显示模板按钮')
assert.ok(!defaultActions.includes('@click="openTemplate(row)"'), '批次执行列表行操作区不再绑定模板跳转')

assertIncludes(router, 'pro/feedback/edhr-batch-execution/template', '路由必须新增模板页隐藏路由')
assertIncludes(router, 'BatchExecutionTemplatePage.vue', '模板页路由必须指向模板页组件')
assertIncludes(router, "title: 'eDHR批次模板'", '模板页路由标题必须正确')

assertIncludes(batchApi, 'getEdhrBatchExecution = async', '模板页必须复用现有批次详情接口')
assertIncludes(reportApi, 'getCellRules', '模板页必须复用批记录模板规则读取接口')
assertIncludes(reportApi, 'getSignatureCellMarkers', '模板页必须复用签名位读取接口')

assertIncludes(templatePage, 'getEdhrBatchExecution(', '模板页必须先读取批次详情')
assertIncludes(templatePage, 'detail.value?.tasks', '模板页必须从批次详情任务中构建全量模板列表')
assertIncludes(templatePage, 'batchRecordReportId', '模板页必须按带模板任务过滤')
assertIncludes(templatePage, 'routeProcessSort', '模板页必须按工序顺序排序')
assertIncludes(templatePage, 'batchRecordSort', '模板页必须按表格顺序排序')
assertIncludes(templatePage, 'BatchRecordReportApi.getCellRules', '模板页必须读取模板规则')
assertIncludes(templatePage, 'BatchRecordReportApi.getSignatureCellMarkers', '模板页必须读取签名位')
assertIncludes(templatePage, 'templateCache', '模板页必须按 reportId 缓存模板数据')
assertIncludes(templatePage, 'Promise.all', '模板页必须并行加载规则与签名位')
assertIncludes(templatePage, '缺少有效批次执行ID', '模板页必须对缺少 id fail-fast')
assertIncludes(templatePage, '当前批次暂无可查看的模板表格', '模板页必须对无模板任务显示空态')
assertIncludes(templatePage, '模板规则加载失败', '模板页必须对规则接口错误明确报错')
assertIncludes(templatePage, '缺少电子批记录模板布局', '模板页必须对布局缺失明确报错')

assertIncludes(templateGuide, 'templateGuideValueTypeLabels', '模板说明组件必须复用共享类型中文映射')
assertIncludes(templateRuleHelper, '文字', '共享模板规则工具必须提供文字类型提示')
assertIncludes(templateRuleHelper, '数字', '共享模板规则工具必须提供数字类型提示')
assertIncludes(templateRuleHelper, '日期', '共享模板规则工具必须提供日期类型提示')
assertIncludes(templateRuleHelper, '日期时间', '共享模板规则工具必须提供日期时间类型提示')
assertIncludes(templateRuleHelper, '勾选', '共享模板规则工具必须提供勾选类型提示')
assertIncludes(templateRuleHelper, '签名', '共享模板规则工具必须提供签名类型提示')
assertIncludes(templateGuide, '附件', '模板说明组件必须支持附件提示')
assertIncludes(templateGuide, '复核签名', '模板说明组件必须支持默认复核签名提示')
assertIncludes(templateGuide, '提交签名', '模板说明组件必须支持默认提交签名提示')
assertIncludes(templateGuide, '审批签名', '模板说明组件必须支持默认审批签名提示')
assertIncludes(templateGuide, '必填', '模板说明组件必须展示必填提示')
assertIncludes(templateGuide, '至少', '模板说明组件必须展示附件最少数量提示')
assertIncludes(templateGuide, '最多', '模板说明组件必须展示附件最多数量提示')
assertIncludes(templateGuide, '格式', '模板说明组件必须展示日期/时间格式提示')
assertIncludes(templateGuide, '单位', '模板说明组件必须展示单位提示')
assertIncludes(templateGuide, '填写单元格', '模板页必须展示模板摘要信息')
assertIncludes(templateGuide, '签名位', '模板页必须展示签名位摘要信息')

console.log('PASS: eDHR batch template preview static contract')
