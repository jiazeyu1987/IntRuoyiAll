const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')
const page = read('src/views/mes/pro/batchrecordformlist/index.vue')

const assertIncludes = (content, token, message) => assert.ok(content.includes(token), message)
const assertNotIncludes = (content, token, message) => assert.ok(!content.includes(token), message)
const assertMatches = (content, pattern, message) => assert.ok(pattern.test(content), message)
const assertNotMatches = (content, pattern, message) => assert.ok(!pattern.test(content), message)

assertIncludes(
  dialog,
  'data-fill-config-toolbar="primary"',
  '填写配置弹窗顶部必须有三段式主工具栏。'
)
assertIncludes(
  dialog,
  'data-fill-config-navigation="same-product-version"',
  '黄框位置必须渲染同产品同版本上一张/下一张导航区。'
)
assertMatches(dialog, />\s*上一张\s*</, '同版本导航必须提供“上一张”按钮。')
assertMatches(dialog, />\s*下一张\s*</, '同版本导航必须提供“下一张”按钮。')
assertIncludes(
  dialog,
  'data-fill-config-actions="primary"',
  '蓝框位置必须集中放置关闭、重新读取和保存填写配置操作。'
)
assertIncludes(
  dialog,
  "navigate: [offset: -1 | 1]",
  '填写配置弹窗必须通过 navigate 事件请求父组件切换表单。'
)
assertIncludes(
  dialog,
  'hasUnsavedChanges',
  '点击上一张/下一张前必须识别未保存填写配置变更。'
)
assertIncludes(
  dialog,
  '放弃未保存修改并切换表单',
  '存在未保存修改时必须先确认，不得直接切走。'
)
assertNotIncludes(
  dialog,
  '<template #footer>',
  '关闭、重新读取和保存填写配置必须移入顶部蓝框，弹窗不得继续使用全宽 footer。'
)

assertIncludes(
  page,
  'CELL_RULES_NAVIGATION_PAGE_SIZE = 200',
  '同版本候选加载必须使用后端允许的最大分页 200，不得请求超限页大小。'
)
assertIncludes(
  page,
  'loadCellRulesNavigationReports',
  '打开填写配置时必须加载同产品同版本候选集合。'
)
assertIncludes(
  page,
  'productName: sourceReport.productName',
  '同版本候选必须按当前表单产品名称查询。'
)
assertIncludes(
  page,
  'versionNo: sourceReport.versionNo',
  '同版本候选必须按当前表单版本号查询。'
)
assertIncludes(
  page,
  'batchRecordVersionId',
  '当前行有批记录版本 ID 时必须用它精确过滤候选。'
)
assertIncludes(
  page,
  'navigateCellRulesDialog',
  '父组件必须处理填写配置弹窗上一张/下一张导航事件。'
)
assertIncludes(
  page,
  'cellRulesNavigation.reports.find((item) => item.reportId === selectedReportId.value)',
  '切换目标不在当前列表页时，页面预览上下文必须能从同版本候选集合解析当前表单。'
)
assertIncludes(
  page,
  '@navigate="navigateCellRulesDialog"',
  '填写配置弹窗必须把导航事件接回父组件。'
)
assertNotMatches(
  page,
  /pageSize:\s*(20[1-9]|[3-9]\d{2,}|\d{4,})/,
  '同版本候选加载不得使用超过 200 的 pageSize。'
)
assertNotIncludes(
  page,
  'formBindings',
  '批记录表单同版本导航不得使用表单槽位 formBindings 作为候选来源。'
)

console.log('PASS: batch record cell rule navigation static contract')
