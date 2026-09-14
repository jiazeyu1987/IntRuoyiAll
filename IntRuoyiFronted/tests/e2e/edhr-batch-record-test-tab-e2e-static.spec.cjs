const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue'),
  'utf8'
)

const listKeys = [
  'productionLeader',
  'frontlinePqc',
  'frontlineProduction',
  'orderAllocation',
  'batchRecordMapping'
]

const toolbarActionBlocks = Array.from(
  page.matchAll(/<template\s+#actions\s*>([\s\S]*?)<\/template>/g)
).map((match) => match[1])

assert.equal(toolbarActionBlocks.length, 5, '五个批记录测试 Tab 都必须有顶部操作面板。')
assert.equal(
  (page.match(/data-edhr-batch-record-test-e2e-button/g) || []).length,
  5,
  '五个 Tab 顶部都必须在现有测试全部按钮左侧提供 E2E 按钮。'
)

for (const [index, listKey] of listKeys.entries()) {
  const toolbar = toolbarActionBlocks[index]
  assert.match(
    toolbar,
    new RegExp(
      "data-edhr-batch-record-test-e2e-button[\\s\\S]*?@click=\"handleE2eTab\\('" +
        listKey +
        "'\\)\""
    ),
    listKey + ' 顶部 E2E 按钮必须只启动当前 Tab 的 E2E。'
  )
  assert.ok(
    toolbar.indexOf('data-edhr-batch-record-test-e2e-button') <
      toolbar.indexOf('data-edhr-batch-record-test-run-all-button'),
    listKey + ' 顶部 E2E 按钮必须位于测试全部按钮左侧，对应截图红框位置。'
  )
  assert.match(
    toolbar,
    new RegExp(":loading=\"e2eTestingTabListKey === '" + listKey + "'\""),
    listKey + ' 顶部 E2E 按钮必须绑定当前 Tab 的 E2E loading。'
  )
  assert.match(
    toolbar,
    new RegExp("getTabE2eButtonText\\('" + listKey + "'\\)"),
    listKey + ' 顶部 E2E 按钮必须复用 E2E 进度文案函数。'
  )
}

assert.match(
  page,
  /const\s+e2eTestingTabListKey\s*=\s*ref<BatchRecordTestListKey>\(\)/,
  'E2E 批量执行必须保存当前运行的 Tab，供互斥和进度状态使用。'
)
assert.match(
  page,
  /const\s+e2eTabProgress\s*=\s*reactive\(\{\s*completed:\s*0,\s*total:\s*0\s*\}\)/,
  'E2E 批量执行必须维护独立完成进度。'
)
assert.match(
  page,
  /function\s+getTabE2eButtonText\(listKey:\s*BatchRecordTestListKey\)[\s\S]*return\s+'E2E'[\s\S]*E2E中[\s\S]*completed[\s\S]*total/,
  'E2E 按钮未运行时显示 E2E，运行中显示已完成数和总数。'
)
assert.match(
  page,
  /function\s+buildPlaywrightE2eCaseName\(row:\s*BatchRecordTestRow\)[\s\S]*replace\('批记录测试-',\s*'批记录E2E-'\)/,
  'E2E 测试项名称必须与 CODE_READONLY 测试项分离，不能覆盖现有只读分析项。'
)
assert.match(
  page,
  /function\s+buildPlaywrightE2eCasePayload\(\s*row:\s*BatchRecordTestRow,\s*previousExecution:\s*CodexTestApi\.CodexTestExecutionVO\s*\|\s*undefined\s*\)[\s\S]*analysisMode:\s*'PLAYWRIGHT_E2E'[\s\S]*testDataText:\s*buildPreviousE2eResultContext\(previousExecution\)/,
  'E2E 测试项必须使用 PLAYWRIGHT_E2E 模式，并把前一个 E2E 结果写入测试数据上下文。'
)
assert.match(
  page,
  /function\s+buildPreviousE2eResultContext\(\s*previousExecution:\s*CodexTestApi\.CodexTestExecutionVO\s*\|\s*undefined\s*\)[\s\S]*executionId[\s\S]*status[\s\S]*summary/,
  'E2E 上下文必须包含前一个 executionId、状态和摘要。'
)
assert.match(
  page,
  /async function\s+upsertPlaywrightE2eCase\(\s*row:\s*BatchRecordTestRow,\s*previousExecution:[\s\S]*getCodexTestCasePage\(\{[\s\S]*project:\s*'批记录'[\s\S]*name:\s*casePayload\.name[\s\S]*createCodexTestCase\(casePayload\)/,
  '启动 E2E 前必须按项目和精确名称持久化 PLAYWRIGHT_E2E 测试项。'
)
assert.match(
  page,
  /async function\s+handleE2eRow\(\s*row:\s*BatchRecordTestRow,\s*previousExecution:\s*CodexTestApi\.CodexTestExecutionVO\s*\|\s*undefined[\s\S]*startCodexTestExecution\(\{[\s\S]*executionMode:\s*'SEQUENTIAL'[\s\S]*caseIds:\s*\[caseId\]/,
  '单行 E2E 必须通过正式测试执行接口以 SEQUENTIAL 模式启动一个 Playwright E2E 测试项。'
)
assert.match(
  page,
  /async function\s+handleE2eTab\(listKey:\s*BatchRecordTestListKey\)[\s\S]*const\s+rows\s*=\s*\[\.\.\.getBatchRecordTestRowsRef\(listKey\)\.value\]/,
  'E2E 批量入口必须读取当前 Tab 的完整正式行集合，不能只读取分页或筛选结果。'
)
assert.match(
  page,
  /async function\s+handleE2eTab\(listKey:[\s\S]*let\s+previousExecution:\s*CodexTestApi\.CodexTestExecutionVO\s*\|\s*undefined[\s\S]*for\s*\(const\s+row\s+of\s+rows\)[\s\S]*await\s+handleE2eRow\(row,\s*previousExecution,\s*'tab'\)[\s\S]*previousExecution\s*=\s*execution/,
  '当前 Tab 的 E2E 必须逐行等待终态，并把上一行结果传给下一行。'
)
assert.doesNotMatch(
  page,
  /handleE2eTab[\s\S]{0,2200}Promise\.all/,
  'E2E 批量入口不得通过 Promise.all 并发启动。'
)
assert.match(
  page,
  /async function\s+handleE2eTab\(listKey:[\s\S]*catch\s*\(error\)[\s\S]*E2E验证在[\s\S]*已停止/,
  '启动或读取传输异常必须明确指出失败行并停止后续 E2E。'
)
assert.match(
  page,
  /testingRowCaseName\s*!==\s*undefined\s*\|\|\s*testingTabListKey\s*!==\s*undefined\s*\|\|\s*e2eTestingTabListKey\s*!==\s*undefined/,
  'E2E、测试全部和单行测试必须互斥。'
)

console.log('edhr-batch-record-test-tab-e2e-static PASS')
