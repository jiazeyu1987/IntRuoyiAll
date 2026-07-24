import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'
import vm from 'node:vm'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const listPage = read('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const progressHelperSource = read('src/views/mes/pro/edhr-batch/progress.ts')

const loadProgressHelpers = () => {
  const transpiled = ts.transpileModule(progressHelperSource, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020
    }
  }).outputText

  const module = { exports: {} }
  vm.runInNewContext(transpiled, { module, exports: module.exports }, { filename: 'progress.ts' })
  return module.exports
}

const {
  isRequiredBatchRecordTask,
  resolveRequiredBatchRecordTaskTotal,
  resolveBatchRequiredCompletedCount,
  resolveBatchRequiredProgress,
  resolveBatchRequiredProgressText
} = loadProgressHelpers()

test('BDD: 批次完成进度只按必填批记录任务计算', () => {
  assert.equal(isRequiredBatchRecordTask({ requiredFlag: true, batchRecordReportId: 'R1' }), true)
  assert.equal(isRequiredBatchRecordTask({ requiredFlag: undefined, batchRecordReportId: 'R1' }), true)
  assert.equal(isRequiredBatchRecordTask({ requiredFlag: false, batchRecordReportId: 'R1' }), false)
  assert.equal(isRequiredBatchRecordTask({ requiredFlag: true, batchRecordReportId: '' }), false)

  const batch = {
    tasks: [
      { requiredFlag: true, batchRecordReportId: 'R1', status: 40 },
      { requiredFlag: true, batchRecordReportId: 'R2', status: 45 },
      { requiredFlag: true, batchRecordReportId: 'R3', status: 20 },
      { requiredFlag: true, status: 40 }
    ]
  }

  assert.equal(resolveRequiredBatchRecordTaskTotal(batch), 3)
  assert.equal(resolveBatchRequiredCompletedCount(batch), 2)
})

test('BDD: 特殊节点已完成时模板进度不能超过 100%', () => {
  const fullyCompletedTemplateBatch = {
    taskApprovedCount: 4,
    tasks: [
      { requiredFlag: true, batchRecordReportId: 'R1', status: 40 },
      { requiredFlag: true, batchRecordReportId: 'R2', status: 45 },
      { requiredFlag: true, batchRecordReportId: 'R3', status: 40 },
      { requiredFlag: true, status: 40 }
    ]
  }

  assert.equal(resolveBatchRequiredCompletedCount(fullyCompletedTemplateBatch), 3)
  assert.equal(resolveBatchRequiredProgress(fullyCompletedTemplateBatch), 100)
  assert.equal(resolveBatchRequiredProgressText(fullyCompletedTemplateBatch), '3 / 3')

  const partiallyCompletedTemplateBatch = {
    taskApprovedCount: 4,
    tasks: [
      { requiredFlag: true, batchRecordReportId: 'R1', status: 40 },
      { requiredFlag: true, batchRecordReportId: 'R2', status: 45 },
      { requiredFlag: true, batchRecordReportId: 'R3', status: 20 },
      { requiredFlag: true, status: 40 }
    ]
  }

  assert.equal(resolveBatchRequiredCompletedCount(partiallyCompletedTemplateBatch), 2)
  assert.equal(resolveBatchRequiredProgress(partiallyCompletedTemplateBatch), 67)
  assert.equal(resolveBatchRequiredProgressText(partiallyCompletedTemplateBatch), '2 / 3')
})

test('BDD: 列表和详情页不得用路线总工序 taskTotal 作为完成进度分母', () => {
  assert.match(
    listPage,
    /resolveBatchRequiredProgress\(row\)/,
    '列表页完成进度必须调用必填批记录进度 helper。'
  )
  assert.doesNotMatch(
    listPage,
    /taskApprovedCount[\s\S]{0,80}\/\s*row\.taskTotal/,
    '列表页不得继续使用 taskApprovedCount / taskTotal。'
  )
  assert.match(
    detailPage,
    /resolveBatchRequiredProgressText\(detail\)/,
    '详情页任务进度必须显示必填批记录通过数与必填批记录总数。'
  )
  assert.doesNotMatch(
    detailPage,
    /taskApprovedCount[\s\S]{0,80}\/[\s\S]{0,80}taskTotal/,
    '详情页不得继续显示 taskApprovedCount / taskTotal。'
  )
})
