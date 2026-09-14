const fs = require('fs')
const path = require('path')
const assert = require('assert')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')

function read(relPath) {
  return fs.readFileSync(path.join(moduleRoot, relPath), 'utf8')
}

function methodBlock(source, methodName) {
  const declaration = new RegExp(`^\\s*(?:public|private|protected)\\s+[^\\n{;=]*\\b${methodName}\\s*\\(`, 'm')
  const match = declaration.exec(source)
  assert(match, `missing method ${methodName}`)
  const start = match.index
  const firstBrace = source.indexOf('{', start)
  assert.notStrictEqual(firstBrace, -1, `missing body for ${methodName}`)
  let depth = 0
  for (let i = firstBrace; i < source.length; i += 1) {
    if (source[i] === '{') {
      depth += 1
    } else if (source[i] === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, i + 1)
      }
    }
  }
  throw new Error(`unterminated method ${methodName}`)
}

function assertOrdered(source, first, second, message) {
  const firstIndex = source.indexOf(first)
  const secondIndex = source.indexOf(second)
  assert(firstIndex >= 0 && secondIndex >= 0 && firstIndex < secondIndex, message)
}

const batchExecution = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java')
const reportService = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/report/MesProductionReleaseReportServiceImpl.java')

const freezeGate = methodBlock(batchExecution, 'validateBatchNotFrozenForSpecialAttachmentSave')
assert(
  freezeGate.includes('nonconformanceReviewService.ensureBatchNotFrozen(batch.getId(), "eDHR批次操作")'),
  'special attachment freeze gate must call the nonconformance freeze authority'
)
assert(
  !freezeGate.includes('hasGoldenFingerActionBypass'),
  'special attachment freeze gate must not allow golden-finger bypass'
)

const saveGate = methodBlock(batchExecution, 'validateBatchForSpecialAttachmentSave')
assertOrdered(
  saveGate,
  'validateBatchNotFrozenForSpecialAttachmentSave(batch)',
  'releaseTransactionMapper.selectByBatchExecutionId(batch.getId())',
  'attachment save validation must check frozen state before release-state reads'
)
assert(
  !saveGate.includes('hasGoldenFingerActionBypass'),
  'attachment save validation must not allow golden-finger bypass'
)

const taskUploadGate = methodBlock(batchExecution, 'validateTaskForSpecialAttachmentUpload')
assert(
  taskUploadGate.includes('validateBatchForSpecialAttachmentSave(task.getBatchExecutionId())'),
  'report upload prepare must enter the shared attachment save gate before storage'
)

const privatePrepareStart = batchExecution.indexOf(
  'private MesProEdhrSpecialNodeAttachmentPrepareUploadResult prepareSpecialNodeAttachmentUpload('
)
const privatePrepareEnd = batchExecution.indexOf(
  'private MesProEdhrSpecialNodeAttachmentPrepareUploadResult toPrepareUploadResult(',
  privatePrepareStart
)
assert(privatePrepareStart >= 0 && privatePrepareEnd > privatePrepareStart, 'missing private prepare upload body')
const prepareUpload = batchExecution.slice(privatePrepareStart, privatePrepareEnd)
assertOrdered(
  prepareUpload,
  'validateTaskForSpecialAttachmentUpload(command.getTaskId())',
  'fileService.createFileAndReturnId',
  'report upload prepare must validate frozen state before creating a file'
)

const completeReport = methodBlock(batchExecution, 'completeProductionReleaseReportNode')
assertOrdered(
  completeReport,
  'validateBatchForSpecialAttachmentSave(task.getBatchExecutionId())',
  'persistSpecialNodeAttachments(task, attachments, operatedAt)',
  'report completion must validate frozen state before reading or booking attachments'
)

const savePending = methodBlock(batchExecution, 'savePendingSpecialNodeAttachments')
assertOrdered(
  savePending,
  'validateBatchForSpecialAttachmentSave(batchExecutionId)',
  'resolvePendingSpecialNodeAttachmentRecords',
  'pending attachment save must validate frozen state before booking pending files'
)

const reportProcessableGate = methodBlock(reportService, 'requireProcessable')
assert(
  reportProcessableGate.includes(
    'nonconformanceReviewService.ensureBatchNotFrozen(application.getBatchExecutionId(), "生产放行报告上传")'
  ),
  'new production release report service must call the nonconformance batch freeze authority'
)
assert(
  reportProcessableGate.includes(
    'nonconformanceReviewService.ensureWorkOrderNotFrozen(application.getWorkOrderId(), "生产放行报告上传")'
  ),
  'new production release report service must call the nonconformance work-order freeze authority'
)
assertOrdered(
  reportProcessableGate,
  'nonconformanceReviewService.ensureBatchNotFrozen(application.getBatchExecutionId(), "生产放行报告上传")',
  'isProcessableWorkTask(workTask)',
  'new report service must reject frozen batches before task processability writes'
)
assertOrdered(
  reportProcessableGate,
  'nonconformanceReviewService.ensureWorkOrderNotFrozen(application.getWorkOrderId(), "生产放行报告上传")',
  'isProcessableWorkTask(workTask)',
  'new report service must reject frozen work orders before task processability writes'
)

const reportServiceTest = read('src/test/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/report/MesProductionReleaseReportServiceTest.java')
assert(
  reportServiceTest.includes('frozenBatchRejectsPrepareAttachmentBeforeStoragePort') &&
    reportServiceTest.includes('frozenWorkOrderRejectsCompleteBeforeReportNodePort') &&
    reportServiceTest.includes('verify(reportNodePort, never()).prepareAttachment(any())') &&
    reportServiceTest.includes('verify(reportNodePort, never()).complete(any())'),
  'report service tests must prove frozen prepare and complete stop before report node port writes'
)

console.log('PASS: EDHR-STATIC-010 frozen report upload gate static contract')
