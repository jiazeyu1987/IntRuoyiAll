const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const ts = require('typescript')

const frontendRoot = path.resolve(__dirname, '../..')
const diagnosticPath = path.join(
  frontendRoot,
  'src/views/mes/pro/feedback/frontlinePqcTaskAvailability.ts'
)

const loadTypeScriptModule = (filename) => {
  const source = fs.readFileSync(filename, 'utf8')
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    },
    fileName: filename
  }).outputText
  const loadedModule = { exports: {} }
  const compile = new Function('exports', 'require', 'module', '__filename', '__dirname', output)
  compile(loadedModule.exports, require, loadedModule, filename, path.dirname(filename))
  return loadedModule.exports
}

const {
  isExecutableFrontlinePqcTaskOption,
  resolveFrontlinePqcTaskAvailabilityIssue
} = loadTypeScriptModule(diagnosticPath)

const task = (overrides = {}) => ({
  pqcTaskId: 101,
  regulationVersionId: 201,
  qaProcessId: 301,
  inspectionRuleKey: 'FIRST',
  taskStatus: 'PENDING',
  inspectionType: 'FIRST',
  businessDate: '2026-08-17',
  shiftCode: 'DAY',
  roundNo: 1,
  plannedInspectionQuantity: 5,
  ruleSort: 1,
  inspectionTypeRule: {},
  inspectionItems: [{ itemCode: 'PQC-001' }],
  ...overrides
})

const process = (pqcTaskOptions, taskSummary = {}) => ({
  pqcTaskOptions,
  taskSummary: {
    state: pqcTaskOptions.length ? 'PENDING' : 'NOT_CREATED',
    totalCount: pqcTaskOptions.length,
    pendingCount: pqcTaskOptions.filter((item) => item.taskStatus === 'PENDING').length,
    submittedCount: pqcTaskOptions.filter((item) => item.taskStatus === 'SUBMITTED').length,
    confirmedCount: pqcTaskOptions.filter((item) => item.taskStatus === 'CONFIRMED').length,
    cancelledCount: pqcTaskOptions.filter((item) => item.taskStatus === 'CANCELLED').length,
    ...taskSummary
  }
})

assert.deepEqual(
  resolveFrontlinePqcTaskAvailabilityIssue(process([])),
  {
    code: 'TASK_NOT_CREATED',
    message: 'PQC任务未生成：当前工序尚未生成任务，请检查任务生成。'
  }
)

assert.deepEqual(
  resolveFrontlinePqcTaskAvailabilityIssue(process([], { totalCount: 2 })),
  {
    code: 'TASK_DETAILS_MISSING',
    message: 'PQC任务明细缺失：汇总显示 2 条任务，但接口未返回任务明细。'
  }
)

assert.deepEqual(
  resolveFrontlinePqcTaskAvailabilityIssue(process([task({ taskStatus: 'SUBMITTED' })])),
  {
    code: 'NO_PENDING_TASK',
    message: 'PQC任务已提交：当前工序任务正在等待PQC组长复核，不能重复提交。'
  }
)

assert.deepEqual(
  resolveFrontlinePqcTaskAvailabilityIssue(process([task({ taskStatus: 'CONFIRMED' })])),
  {
    code: 'NO_PENDING_TASK',
    message: 'PQC任务已确认：当前工序任务已完成复核，没有待执行任务。'
  }
)

assert.deepEqual(
  resolveFrontlinePqcTaskAvailabilityIssue(process([task({ taskStatus: 'CANCELLED' })])),
  {
    code: 'NO_PENDING_TASK',
    message: 'PQC任务已取消：当前工序没有可执行任务，请联系PQC组长重新生成。'
  }
)

assert.deepEqual(
  resolveFrontlinePqcTaskAvailabilityIssue(process([
    task({ pqcTaskId: 101, taskStatus: 'SUBMITTED' }),
    task({ pqcTaskId: 102, taskStatus: 'CONFIRMED' }),
    task({ pqcTaskId: 103, taskStatus: 'CANCELLED' })
  ])),
  {
    code: 'NO_PENDING_TASK',
    message: 'PQC任务状态不可执行：当前工序没有待执行任务（已提交 1 条、已确认 1 条、已取消 1 条）。'
  }
)

const invalidPendingTask = task({
  regulationVersionId: 0,
  qaProcessId: 0,
  inspectionType: 'UNKNOWN',
  businessDate: '',
  shiftCode: '',
  roundNo: 0,
  plannedInspectionQuantity: 0,
  inspectionItems: []
})
assert.equal(isExecutableFrontlinePqcTaskOption(invalidPendingTask), false)
assert.deepEqual(
  resolveFrontlinePqcTaskAvailabilityIssue(process([invalidPendingTask])),
  {
    code: 'PENDING_TASK_INVALID',
    message:
      'PQC任务数据不完整：任务 101 缺少或无效字段：QA规程版本、QA工序、检验类型、业务日期、班次、轮次、计划检验数量、检验项目。'
  }
)

const validTask = task()
assert.equal(isExecutableFrontlinePqcTaskOption(validTask), true)
assert.equal(resolveFrontlinePqcTaskAvailabilityIssue(process([validTask])), undefined)

const panelSource = fs
  .readFileSync(
    path.join(frontendRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
    'utf8'
  )
  .replace(/\r\n/g, '\n')

assert.match(
  panelSource,
  /isExecutableFrontlinePqcTaskOption/,
  'The selectable-task predicate must reuse the detailed task diagnostic.'
)
assert.match(
  panelSource,
  /pqcTaskAvailabilityIssue\.value\?\.message/,
  'The PQC page must display the detailed reason returned for the selected process.'
)
assert.match(
  panelSource,
  /throw new Error\(pqcTaskAvailabilityIssue\.value\.message\)/,
  'Submit preflight must expose the same detailed task reason.'
)
assert.ok(
  !panelSource.includes("return '当前工序暂无待执行PQC任务，不能提交'"),
  'The old merged task error must be removed.'
)

console.log('frontline-pqc-task-unavailable-reason-static: PASS')
