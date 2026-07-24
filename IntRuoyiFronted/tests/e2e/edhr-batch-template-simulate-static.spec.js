const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
const resolvePath = (relativePath) => path.join(repoRoot, relativePath)

const router = read('src/router/modules/remaining.ts')
const templatePagePath = resolvePath('src/views/mes/pro/edhr-batch/BatchExecutionTemplatePage.vue')
const simulatePagePath = resolvePath('src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue')
const editableFormPath = resolvePath('src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue')
const fitViewportPath = resolvePath('src/views/mes/pro/edhr/components/EdhrTemplateFitViewport.vue')
const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')
const templateRuleHelper = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')

assert.ok(fs.existsSync(templatePagePath), '必须保留批次模板说明页 BatchExecutionTemplatePage.vue')
assert.ok(fs.existsSync(simulatePagePath), '必须新增模拟填写页 BatchExecutionTemplateSimulatePage.vue')
assert.ok(fs.existsSync(editableFormPath), '必须新增模板内可编辑组件 EdhrExecutionTemplateEditableForm.vue')
assert.ok(fs.existsSync(fitViewportPath), '必须新增模板等比缩放视口组件 EdhrTemplateFitViewport.vue')

const templatePage = fs.readFileSync(templatePagePath, 'utf8')
const simulatePage = fs.readFileSync(simulatePagePath, 'utf8')
const editableForm = fs.readFileSync(editableFormPath, 'utf8')
const fitViewport = fs.readFileSync(fitViewportPath, 'utf8')

const assertIncludes = (content, token, message) => {
  assert.ok(content.includes(token), message)
}
const assertNotIncludes = (content, token, message) => {
  assert.ok(!content.includes(token), message)
}

assertIncludes(templatePage, '模拟填写', '模板说明页卡片右上角必须显示模拟填写按钮')
assertIncludes(templatePage, '@click.stop="openSimulate(task)"', '模拟填写按钮必须阻止卡片选中点击冒泡')
assertIncludes(
  templatePage,
  "/mes/pro/feedback/edhr-batch-execution/template-simulate",
  '模板说明页必须跳转到模拟填写路由'
)
assertIncludes(templatePage, 'returnTo: route.fullPath', '模板说明页进入模拟填写时必须透传来源页 fullPath')
assertIncludes(templatePage, "returnLabel: '返回模板说明'", '模板说明页进入模拟填写时必须透传返回文案')

assertIncludes(
  router,
  'pro/feedback/edhr-batch-execution/template-simulate',
  '路由必须新增模拟填写隐藏路由'
)
assertIncludes(router, 'BatchExecutionTemplateSimulatePage.vue', '模拟填写路由必须指向新页面')
assertIncludes(router, "title: 'eDHR模板模拟填写'", '模拟填写路由标题必须正确')

assertIncludes(simulatePage, 'const batchExecutionId = computed(() => Number(route.query.id))', '模拟页必须校验批次执行 ID')
assertIncludes(simulatePage, 'const taskId = computed(() => Number(route.query.taskId))', '模拟页必须校验 taskId')
assertIncludes(simulatePage, "const directReportId = computed(() => String(route.query.reportId || '').trim())", '模拟页必须支持 reportId 直达模式')
assertIncludes(simulatePage, "const returnTo = computed(() => String(route.query.returnTo || '').trim())", '模拟页必须支持来源路由返回参数')
assertIncludes(simulatePage, "const returnLabel = computed(() => String(route.query.returnLabel || '').trim())", '模拟页必须支持来源返回文案参数')
assertIncludes(simulatePage, '<el-button link type="primary" @click="handleBack">', '模拟页头部必须渲染返回按钮')
assertIncludes(simulatePage, "const backButtonLabel = computed(() => returnLabel.value || '返回')", '模拟页必须计算返回按钮文案')
assertIncludes(simulatePage, 'await router.push(returnTo.value)', '模拟页返回时必须优先回到来源页')
assertIncludes(simulatePage, 'if (directReportId.value)', '模拟页必须在 reportId 模式下绕过批次详情加载')
assertIncludes(simulatePage, 'getEdhrBatchExecution(', '模拟页必须先读取批次详情')
assertIncludes(simulatePage, 'detail.value?.tasks', '模拟页必须从批次详情内定位当前任务')
assertIncludes(simulatePage, 'Promise.all([', '模拟页必须并行加载模板规则和签名位')
assertIncludes(simulatePage, 'BatchRecordReportApi.getCellRules', '模拟页必须复用模板规则接口')
assertIncludes(simulatePage, 'BatchRecordReportApi.getSignatureCellMarkers', '模拟页必须复用签名位接口')
assertIncludes(simulatePage, 'normalizeCellRule', '模拟页必须复用规则归一化逻辑')
assertIncludes(simulatePage, 'EdhrExecutionTemplateEditableForm', '模拟页左侧必须使用模板内可编辑组件')
assertIncludes(simulatePage, 'EdhrExecutionReadonlyForm', '模拟页右侧必须复用只读模板组件')
assertIncludes(simulatePage, 'executionSnapshotJson', '模拟页必须构造 synthetic formViewModel')
assertIncludes(simulatePage, 'cellValuesJson', '模拟页必须把左侧模板输入映射为单元格值')
assertIncludes(simulatePage, 'signatureRecords', '模拟页必须向右侧表单传入签名记录集合')
assertIncludes(simulatePage, 'const signatureRecords = computed', '模拟页必须显式声明签名记录来源')
assertIncludes(
  simulatePage,
  'return []',
  '模拟页未走真实电子签名时签名记录必须保持为空，不得伪造本地签名记录'
)
assertIncludes(simulatePage, '模板内填写', '模拟页左侧标题必须明确是模板内填写')
assertIncludes(simulatePage, '表单显示', '模拟页右侧标题必须明确是表单显示')
assertIncludes(simulatePage, '模拟填写加载失败', '模拟页必须对接口或配置错误明确报错')
assertIncludes(simulatePage, 'fit-to-viewport', '模拟页左右模板必须启用等比缩放视口')
assertIncludes(simulatePage, 'edhr-batch-template-simulate__surface-body', '模拟页必须提供左右一致的模板显示体')
assertIncludes(simulatePage, 'width-only', '模拟页必须声明模板只按宽度适配容器')

assertIncludes(editableForm, 'sheetLayoutJson', '模板内可编辑组件必须接收模板布局')
assertIncludes(editableForm, 'cellRules', '模板内可编辑组件必须接收模板规则')
assertIncludes(editableForm, 'signatureMarkers', '模板内可编辑组件必须接收签名位')
assertIncludes(editableForm, 'update:modelValue', '模板内可编辑组件必须把模拟状态回传父页')
assertIncludes(editableForm, 'el-input', '模板内可编辑组件必须支持文字输入')
assertIncludes(editableForm, 'el-input-number', '模板内可编辑组件必须支持数字输入')
assertIncludes(editableForm, 'el-date-picker', '模板内可编辑组件必须支持日期和日期时间输入')
assertIncludes(editableForm, 'el-checkbox', '模板内可编辑组件必须支持勾选输入')
assertIncludes(editableForm, '电子签名', '模板内签名格必须显示电子签名入口')
assertIncludes(editableForm, 'signatureAction', '模板内签名格必须通过事件通知父级触发电子签名')
assertNotIncludes(editableForm, '签名人姓名', '模板内签名格不得支持手动输入签名人姓名')
assertNotIncludes(editableForm, 'placeholder="签名时间"', '模板内签名格不得支持手动选择签名时间')
assertIncludes(editableForm, '<slot name="field"', '模板内可编辑组件必须允许正式填写页注入受控字段控件')
assertNotIncludes(editableForm, '模拟页不支持上传附件', '共享模板组件不得固化模拟页附件限制')
assertIncludes(editableForm, '<table', '模板内可编辑组件必须按原模板表格渲染')
assertIncludes(editableForm, ':rowspan="cell.rowSpan"', '模板内可编辑组件必须保留合并单元格 rowspan')
assertIncludes(editableForm, ':colspan="cell.colSpan"', '模板内可编辑组件必须保留合并单元格 colspan')
assertIncludes(editableForm, 'EdhrTemplateFitViewport', '模板内可编辑组件必须接入等比缩放视口')
assertIncludes(editableForm, 'fitToViewport', '模板内可编辑组件必须支持模板等比缩放模式')

assertIncludes(templateRuleHelper, 'TemplateEditableCellContext', '共享模板规则工具必须导出单元格编辑上下文类型')
assertIncludes(templateRuleHelper, 'buildTemplateEditableCellContext', '共享模板规则工具必须支持构造可编辑单元格上下文')
assertIncludes(templateRuleHelper, 'formatTemplateAttachmentRule', '共享模板规则工具必须导出附件规则摘要')

assertIncludes(readonlyForm, '未签名', '只读模板组件必须支持未签名态显示')
assertIncludes(readonlyForm, 'edhr-template-sheet', '右侧表单显示必须继续使用原模板表格')
assertIncludes(readonlyForm, 'EdhrTemplateFitViewport', '只读模板组件必须接入等比缩放视口')
assertIncludes(readonlyForm, 'fitToViewport', '只读模板组件必须支持模板等比缩放模式')

assertIncludes(fitViewport, 'ResizeObserver', '模板等比缩放视口必须监听尺寸变化')
assertIncludes(fitViewport, 'transform: `scale(${scale.value})`', '模板等比缩放视口必须按比例缩放内容')
assertIncludes(fitViewport, 'ref="viewportRef"', '模板等比缩放视口必须具备视口容器')
assertIncludes(fitViewport, 'ref="measureRef"', '模板等比缩放视口必须具备内容测量节点')
assertIncludes(fitViewport, 'widthOnly?: boolean', '模板缩放视口必须支持仅按宽度适配模式')
assertIncludes(fitViewport, 'const nextScale = props.widthOnly', '模板缩放视口必须在宽度模式下忽略高度限制')

console.log('PASS: eDHR batch template simulate static contract')
