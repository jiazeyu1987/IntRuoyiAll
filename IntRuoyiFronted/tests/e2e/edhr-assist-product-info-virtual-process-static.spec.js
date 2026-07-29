const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const executionPage = fs
  .readFileSync(path.join(repoRoot, 'src/views/mes/pro/edhr/ExecutionPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

assert.match(
  executionPage,
  /const ASSIST_PRODUCT_INFO_PROCESS_SORT = 80[\s\S]*const ASSIST_PRODUCT_INFO_PROCESS_NAME = '产品信息'/,
  '填写页必须定义产品信息虚拟 80 工序展示口径。'
)

assert.match(
  executionPage,
  /const isAssistProductInfoProcessTask = \(task: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*task\.nodeType === EDHR_BATCH_NODE_ROUTE_FORM[\s\S]*task\.formSlotType === 'MAIN'[\s\S]*task\.recordCategory === 'BATCH_RECORD'[\s\S]*task\.batchRecordSort === ASSIST_PRODUCT_INFO_PROCESS_SORT/,
  '填写页必须按正式 MAIN+BATCH_RECORD 产品信息任务识别虚拟工序，不能从 formBindings 推断。'
)

const groupKeyMatch = executionPage.match(
  /const buildAssistProcessSwitchItemKey = \(task: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*?(?=\n\nconst )/
)
assert.ok(groupKeyMatch, '切换工序必须保留统一分组键函数。')
assert.match(
  groupKeyMatch[0],
  /isAssistProductInfoProcessTask\(task\)[\s\S]*`product-info:\$\{task\.batchRecordReportId \|\| task\.id\}`/,
  '产品信息任务必须使用独立 product-info 分组键，不能与粗洗共用 routeProcessId。'
)

assert.match(
  executionPage,
  /const resolveAssistProcessSwitchItemName = \(task: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*ASSIST_PRODUCT_INFO_PROCESS_NAME[\s\S]*task\.processName/,
  '产品信息工序卡片名称必须显示为产品信息。'
)
assert.match(
  executionPage,
  /const resolveAssistProcessSwitchItemSort = \(task: EdhrBatchExecutionTaskRespVO\) =>[\s\S]*ASSIST_PRODUCT_INFO_PROCESS_SORT[\s\S]*task\.routeProcessSort/,
  '产品信息工序卡片排序必须显示为 80。'
)

const processBuilderMatch = executionPage.match(
  /const buildAssistProcessSwitchItems = \(tasks: EdhrBatchExecutionTaskRespVO\[\]\) => \{[\s\S]*?(?=\n\nconst loadAssistWorkTaskSwitchItems)/
)
assert.ok(processBuilderMatch, '必须保留切换工序分组构造函数。')
assert.match(
  processBuilderMatch[0],
  /processName:\s*resolveAssistProcessSwitchItemName\(primaryTask\)/,
  '切换工序卡片名称必须使用虚拟工序名称解析。'
)
assert.match(
  processBuilderMatch[0],
  /routeProcessSort:\s*resolveAssistProcessSwitchItemSort\(primaryTask\)/,
  '切换工序卡片排序必须使用虚拟工序排序解析。'
)

const fillerLoaderMatch = executionPage.match(
  /const loadAssistFillerSwitchItems = async \(\) => \{[\s\S]*?(?=\n\nconst openAssistSwitchDialog)/
)
assert.ok(fillerLoaderMatch, '必须保留切换填写人候选加载函数。')
assert.match(
  fillerLoaderMatch[0],
  /const currentProcessGroupKey = buildAssistProcessSwitchItemKey\(currentTask\)/,
  '切换填写人必须先解析当前页面任务所属显示工序分组。'
)
assert.match(
  fillerLoaderMatch[0],
  /buildAssistProcessSwitchItemKey\(task\) === currentProcessGroupKey/,
  '切换填写人必须按显示工序分组筛选，隔离产品信息与粗洗任务。'
)
assert.doesNotMatch(
  fillerLoaderMatch[0],
  /sameRouteQueryId\(task\.routeProcessId,\s*routeProcessId\)/,
  '切换填写人不得继续只按来源 routeProcessId 合并产品信息和粗洗任务。'
)

console.log('PASS: eDHR assist process and filler switches isolate virtual product info process 80.')
