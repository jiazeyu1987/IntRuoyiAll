const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const stage25 = fs.readFileSync(path.join(moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage2_5/MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java'), 'utf8')
const batchExecution = fs.readFileSync(path.join(moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'), 'utf8')

assert(!stage25.includes('batchExecutionService.openOrCreate(buildFormalBatchRequest('),
  'P2 must not open the formal batch with the minimal receipt-only request.')
assert(stage25.includes('MesCompletionBackfillReceipt backfillReceipt = buildBackfillReceipt('),
  'P2 must build the formal completion backfill receipt before opening the batch.')
assert(stage25.includes('batchExecutionService.openOrCreate(buildBatchRequest('),
  'P2 must open the formal batch with the rich backfill receipt request.')
assert(stage25.includes('.setPickListSources(receipt.getPickListSources())'),
  'P2 batch request must carry pick-list sources for active-order trace capture.')
assert(stage25.includes('.setCompletionBackfillReceipt(receipt)'),
  'P2 batch request must carry the full completion receipt for loss/source metadata.')
assert(!batchExecution.includes('.setCompletionBackfillReceipt(reqVO.getCompletionBackfillReceipt())'),
  'Batch execution service must not trust a request-supplied completion receipt before authoritative resolution.')
assert(batchExecution.includes('.setCompletionBackfillReceipt(command.getCompletionBackfillReceipt())'),
  'Batch execution service must preserve the authoritative completion receipt after resolver validation.')
assert(batchExecution.includes('MesProEdhrBatchTraceTxCApplicationService'),
  'P2 batch completion must invoke the formal Flow 7 trace capture boundary.')
assert(batchExecution.includes('batchTraceTxCApplicationService.handle(event)'),
  'P2 must synchronously capture the formal active-order trace before returning success.')
assert(batchExecution.includes('MesProEdhrBatchTraceTxCProducer.MAPPING_STATUS_CAPTURED'),
  'P2 must require the formal trace capture success status.')
assert(batchExecution.includes('FLOW7_TRACE_CAPTURE_FAILED'),
  'P2 must fail explicitly when formal trace capture is incomplete.')
assert(batchExecution.includes('operationAuditService.recordInCallerTransaction(command)'),
  'The provision OPEN audit must be visible to the synchronous formal trace capture in the same transaction.')
assert(batchExecution.includes('entryAuditMetadata(provisionCommand, provisioningRecord.getId()), true);'),
  'Formal batch creation must mark its provision OPEN audit as caller-transactional.')

console.log('mes-stage2-5-p2-dossier-source-static: PASS')
