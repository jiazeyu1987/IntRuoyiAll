const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const reportApi = read('src/api/mes/pro/batchrecordreport/index.ts')
const feedbackApi = read('src/api/mes/pro/feedback/index.ts')
const ruleHelper = read('src/views/mes/pro/batchrecord-shared/batchRecordTemplateRules.ts')
const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const realFlowE2e = read('tests/e2e/edhr-assist-fill-mode-real-flow.e2e.js')
const executionPageNormalized = executionPage.replace(/\r\n/g, '\n')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)

assertIncludes(reportApi, 'helpText?: string', '批记录规则 VO 必须声明 helpText 字段。')
assertIncludes(feedbackApi, 'helpText?: string', 'eDHR 执行快照字段 VO 必须声明 helpText 字段。')
assertIncludes(ruleHelper, 'helpText?: string', '模板规则上下文必须携带 helpText 字段。')
assertIncludes(ruleHelper, 'helpText: normalizedRule.helpText', '模板规则 normalize/build 过程必须透传 helpText。')

assertIncludes(dialog, 'label="字段说明"', '单元格规则弹框必须提供字段说明编辑项。')
assertIncludes(dialog, 'v-model="selectedRule.helpText"', '字段说明编辑项必须绑定 selectedRule.helpText。')
assertIncludes(dialog, 'type="textarea"', '字段说明应使用 textarea，不能复用短 placeholder 输入。')
assertIncludes(dialog, 'placeholder="说明这个单元格要填写什么内容"', '字段说明输入框必须明确用于说明填写内容。')

assertIncludes(executionPage, "const fillViewMode = ref<'assist' | 'original'>('assist')", '填写页默认必须进入辅助模式。')
assertIncludes(executionPage, '填写辅助模式', '页面必须提供填写辅助模式按钮或标题。')
assertIncludes(executionPage, '原表模式', '页面必须提供原表模式切换按钮。')
assertIncludes(executionPage, 'edhr-fill-workspace__assist-panel', '辅助模式必须有独立字段清单容器。')
assertIncludes(executionPage, 'edhr-fill-workspace__assist-topbar', '辅助模式顶部必须有常驻工作台栏。')
assertIncludes(executionPage, 'edhr-fill-workspace__assist-switch-grid', '辅助模式顶部必须提供任务、工序、填写人快速切换区。')
assertIncludes(executionPage, 'edhr-fill-workspace__assist-switch', '辅助模式快速切换按钮必须复用同一类名，便于真实 E2E 定位。')
assertIncludes(executionPage, '任务 / 批次', '辅助模式必须显示任务/批次快速切换。')
assertIncludes(executionPage, '工序', '辅助模式必须显示工序快速切换。')
assertIncludes(executionPage, '填写人', '辅助模式必须显示填写人快速切换。')
assertIncludes(executionPage, '<el-dialog', '辅助模式三个快速切换必须使用当前页面弹框选择。')
assertIncludes(executionPage, ':append-to-body="false"', '辅助模式切换弹框必须保留在原生全屏工作区内，不能传送到 body。')
assertIncludes(executionPage, 'edhr-fill-workspace__assist-switch-dialog', '辅助模式切换弹框必须有稳定类名。')
assertIncludes(executionPage, 'assistSwitchDialogVisible', '辅助模式切换必须由共享弹框显示状态控制。')
assertIncludes(executionPage, 'assistSwitchDialogType', '辅助模式切换必须复用共享弹框并按类型展示列表。')
const assistSwitchTemplate = executionPage.slice(
  executionPage.indexOf('edhr-fill-workspace__assist-switch-grid'),
  executionPage.indexOf('edhr-fill-workspace__assist-summary')
)
assertNotIncludes(assistSwitchTemplate, '<el-popover', '辅助模式快速切换不能继续使用最大化模式下可能被裁剪的 popover。')
assertIncludes(executionPage, 'loadAssistTaskSwitchItems', '任务/批次切换必须加载当前账号可处理任务列表。')
assertIncludes(executionPage, 'loadAssistProcessSwitchItems', '工序切换必须加载当前批次工序任务列表。')
assertIncludes(executionPage, 'loadAssistFillerSwitchItems', '填写人切换必须加载当前批次当前工序表单填写人。')
assertIncludes(executionPage, 'AssistFillerSwitchItem', '填写人列表必须使用表单任务与填写人的关系模型。')
assertIncludes(executionPage, 'resolveCurrentAssistBatchTask', '填写人解析必须先识别当前批次任务。')
assertIncludes(executionPage, 'resolveAssistFillerFormSourceLabel', '填写人列表必须区分 MAIN 批处理表单和工艺路线表单槽位。')
assertIncludes(executionPage, 'data-assist-filler-task-id', '填写人关系项必须暴露关联批次任务编号供真实 E2E 核验。')
assertIncludes(executionPage, 'data-assist-filler-user-id', '填写人关系项必须暴露关联填写人编号供真实 E2E 核验。')
const assistFillerLoaderMatch = executionPage.match(
  /const loadAssistFillerSwitchItems = async \(\) => \{[\s\S]*?(?=(?:\r?\n){2}const openAssistSwitchDialog)/
)
assert.ok(assistFillerLoaderMatch, '必须保留填写人列表加载函数。')
assert.match(
  assistFillerLoaderMatch[0],
  /getEdhrBatchExecution\(batchExecutionId\)[\s\S]*routeProcessId[\s\S]*fillableUsers/,
  '填写人必须从当前批次详情按当前 routeProcessId 汇总各表单任务 fillableUsers。'
)
assert.match(
  assistFillerLoaderMatch[0],
  /formTemplateId[\s\S]*formSlotType[\s\S]*MAIN/,
  '填写人解析必须结合 formTemplateId 区分 MAIN 批处理表单和动态表单槽位。'
)
assert.doesNotMatch(
  assistFillerLoaderMatch[0],
  /loadAssistWorkTaskSwitchItems/,
  '填写人不得继续读取跨工单、跨工序的全局我的待办。'
)
assertIncludes(executionPage, 'resolveAssistWorkTaskStatusLabel', '任务/批次切换列表必须展示工作任务状态。')
assertIncludes(executionPage, '缺少 workTaskId，不能切换。', '工作任务缺少 workTaskId 时必须在当前弹出列表内 fail-fast。')
assertIncludes(executionPage, '缺少批次任务编号，不能切换。', '工作任务缺少 batchTaskId 时必须在当前弹出列表内 fail-fast。')
const assistSwitchHandlers = executionPage.slice(
  executionPage.indexOf('const handleAssistTaskSwitch'),
  executionPage.indexOf('const resolveAssistFieldElement')
)
assertNotIncludes(assistSwitchHandlers, "path: '/mes/pro/feedback/edhr-batch-execution'", '任务/批次切换不能跳回批次列表。')
assertNotIncludes(assistSwitchHandlers, "path: '/mes/pro/feedback/edhr-batch-execution/detail'", '工序/填写人切换不能跳回批次详情。')
assertNotIncludes(assistSwitchHandlers, "focus: 'process'", '工序切换不能通过批次详情 focus 参数离开辅助模式。')
assertNotIncludes(assistSwitchHandlers, "focus: 'fillers'", '填写人切换不能通过批次详情 focus 参数离开辅助模式。')
assertIncludes(executionPage, '我的填写项', '辅助模式必须把当前用户需要填写的字段作为主内容。')
assertIncludes(executionPage, 'assistMissingFieldCount', '辅助模式必须实时计算未完成项数量。')
assertIncludes(executionPage, '还差 {{ assistMissingFieldCount }} 项', '顶部必须始终显示还差 N 项。')
assertIncludes(executionPage, '@click="scrollToFirstIncompleteAssistField"', '点击还差 N 项必须定位第一个未完成项。')
assertIncludes(executionPage, 'highlightedAssistFieldIdentity', '定位第一个未完成项后必须高亮该行。')
assertIncludes(executionPage, 'data-assist-field-id', '每个辅助填写行必须有稳定字段定位属性。')
assertIncludes(executionPage, 'edhr-fill-workspace__assist-row', '辅助模式字段必须使用紧凑行式布局。')
assertNotIncludes(executionPage, 'edhr-fill-workspace__assist-card"', '辅助模式不能继续使用大卡片字段布局。')
assertIncludes(executionPage, '字段说明未配置', '缺少 helpText 时必须显式显示字段说明未配置。')
assertIncludes(executionPage, 'field.helpText', '辅助模式必须显示字段级 helpText。')
assertIncludes(executionPage, 'helpText: resolveSnapshotFieldHelpText(field)', '执行页必须从快照字段规范化 helpText。')
assertIncludes(executionPage, 'draftFieldValues[field.fieldIdentity]', '辅助模式必须复用现有 draftFieldValues。')
assertNotIncludes(executionPage, 'assistDraftFieldValues', '辅助模式不得新增独立草稿对象。')
assertIncludes(executionPage, "type: 'choice-group'", '辅助模式必须把同一业务内容下的多个 checkbox 提升为互斥选项组。')
assertIncludes(executionPage, 'buildAssistChoiceGroupItems', '辅助模式必须通过通用归并函数生成 checkbox 选项组。')
assertIncludes(executionPage, 'edhr-fill-workspace__choice-group', '辅助模式必须为互斥选项组提供独立渲染容器。')
assertIncludes(executionPage, '<el-radio-group', '互斥 checkbox 选项组必须渲染为单选组，不能继续显示多个独立勾选项。')
assertIncludes(executionPage, 'resolveAssistChoiceGroupValue(field)', '选项组选中值必须从底层 checkbox 布尔字段推导。')
assertIncludes(executionPage, 'updateAssistChoiceGroupValue(field, value)', '选项组变更必须回写到底层 checkbox 布尔字段。')
assertIncludes(executionPage, 'resolveAssistChoiceGroupLabel', '选项组标题必须从表头或字段上下文通用解析，不能硬编码检测结果。')
assertIncludes(executionPage, 'isSingleChoiceCheckboxField', '单字段多选项 checkbox 必须被识别为字符串单选字段。')
assertIncludes(executionPage, 'hydrateStoredDraftValue(storedValue.value, field)', '已保存的单选 checkbox 值回填时必须保留选项字符串，不能走布尔 checkbox 回填。')
assertNotIncludes(executionPage, "符合要求') return '检测结果'", '选项组标题不能针对符合要求/不符合要求写硬编码补丁。')
assertNotIncludes(executionPage, ".filter((field) => field.componentKind !== 'signature')\n)", '辅助模式字段清单必须包含签名单元格，不能过滤签名。')
assertIncludes(executionPage, 'resolveSignatureCellDisplay(field)', '辅助模式签名项必须显示现有签名记录。')
assertIncludes(executionPage, 'handleSignatureCellAction(field)', '辅助模式签名项必须复用现有电子签名动作。')
assertIncludes(executionPage, `@click="fillViewMode = 'assist'"`, '辅助模式切换必须只改变 fillViewMode。')
assertIncludes(executionPage, `@click="fillViewMode = 'original'"`, '原表模式切换必须只改变 fillViewMode。')
assertIncludes(executionPage, '@click="openFieldAuditSignatureDialog"', '辅助模式保存必须继续走现有字段审计签名入口。')
assertIncludes(executionPage, '@click="openSubmitDialog"', '辅助模式提交必须继续走现有提交执行弹框入口。')
assertIncludes(executionPage, 'const hasFillTaskContext = computed(() => workTaskId.value !== undefined)', '无 workTaskId 的列表查看路径必须被识别为只读上下文。')
assertIncludes(executionPage, '!hasFillTaskContext.value', '只读判断必须包含无工作任务上下文，不能仅按执行状态判断。')
assertIncludes(
  executionPageNormalized,
  `v-if="!isReadonly"\n                    class="edhr-fill-workspace__primary-action"`,
  '辅助模式只读时不能显示保存入口。'
)
assertIncludes(
  executionPageNormalized,
  `v-if="!isReadonly"\n                    v-hasPermi="['mes:pro-batch-record-execution:update', 'mes:pro-batch-record-execution:golden-finger']"\n                    class="edhr-fill-workspace__submit-action"`,
  '辅助模式只读时不能显示提交执行入口，提交入口必须保留普通 update 权限并额外允许金手指测试权限。'
)
assertIncludes(executionPage, 'await saveEdhrFieldChanges({', '保存变更必须继续调用现有字段审计保存接口。')
assertIncludes(executionPage, 'await ProFeedbackApi.submitEdhrExecution({', '提交执行必须继续调用现有 eDHR 提交接口。')
assertIncludes(realFlowE2e, '.edhr-fill-workspace__assist-row', '真实 E2E 必须定位新的紧凑行式辅助填写项。')
assertIncludes(realFlowE2e, 'verifyMissingJump', '真实 E2E 必须验证“还差 N 项”点击定位和高亮。')
assertIncludes(realFlowE2e, "const workTaskPath = '/mes/pro/feedback/edhr-work-task'", '真实 E2E 必须从当前账号工作任务看板打开填写任务。')
assertIncludes(realFlowE2e, 'openFillableWorkTask', '真实 E2E 必须使用当前账号 FILL/REWORK 工作任务作为入口。')
assertIncludes(realFlowE2e, '/mes/pro/edhr-work-task/my-page', '真实 E2E 必须加载当前账号可处理工作任务。')
assertNotIncludes(realFlowE2e, "name: '去填写'", '真实 E2E 不得继续依赖批次列表已下线的“去填写”入口。')
assertIncludes(realFlowE2e, 'enterAssistWorkspaceFullscreen', '真实 E2E 必须先进入辅助填写工作区最大化模式。')
assertIncludes(realFlowE2e, 'document.fullscreenElement', '真实 E2E 必须断言切换弹框在原生全屏状态下可见。')
assertIncludes(realFlowE2e, 'verifyAssistSwitchDialogs', '真实 E2E 必须点击验证辅助模式三个就地切换弹框。')
assertIncludes(realFlowE2e, "menu: 'task'", '真实 E2E 必须验证任务/批次切换弹出列表。')
assertIncludes(realFlowE2e, "menu: 'process'", '真实 E2E 必须验证工序切换弹出列表。')
assertIncludes(realFlowE2e, "menu: 'filler'", '真实 E2E 必须验证填写人切换弹出列表。')
assertIncludes(realFlowE2e, 'data-assist-filler-task-id', '真实 E2E 必须采集填写人关联批次任务编号。')
assertIncludes(realFlowE2e, 'data-assist-filler-user-id', '真实 E2E 必须采集填写人编号。')
assertIncludes(
  realFlowE2e,
  'assist_switch_filler_should_preserve_form_task_user_relationships',
  '真实 E2E 必须验证当前工序表单、任务和填写人的对应关系。'
)
assertIncludes(realFlowE2e, 'assist_switch_${item.menu}_should_not_show_error', '真实 E2E 必须把三个切换弹框的接口错误作为失败处理。')
assertIncludes(realFlowE2e, 'apiErrorsDuringAssistSwitch', '真实 E2E 必须记录辅助切换期间的接口错误证据。')
assertIncludes(realFlowE2e, 'assist_switch_dialogs_should_not_navigate', '真实 E2E 必须断言切换弹框不离开当前填写页。')
assertNotIncludes(realFlowE2e, "const cards = page.locator('.edhr-fill-workspace__assist-card')", '真实 E2E 不能继续遍历旧大卡片字段。')
assertNotIncludes(realFlowE2e, 'assistCardCount', '真实 E2E 不能继续用旧大卡片数量作为通过条件。')

console.log('PASS: edhr assist fill mode static contract')
