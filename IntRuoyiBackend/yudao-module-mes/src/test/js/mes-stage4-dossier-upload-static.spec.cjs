const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const stage4Service = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage4/MesStage4DossierUploadSimulationServiceImpl.java'
)
const batchExecutionService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionService.java'
)
const batchExecutionServiceImpl = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
)
const stage4Command = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage4/MesStage4DossierUploadSimulationCommand.java'
)
const stage4Req = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrStage4DossierUploadSimulationReqVO.java'
)

const methodStart = batchExecutionServiceImpl.indexOf('completePreReleaseDossierNode(')
assert.notEqual(methodStart, -1, 'batch execution service must expose a Stage4 pre-release dossier completion method')
const nextOverride = batchExecutionServiceImpl.indexOf('@Override', methodStart + 1)
const methodBody = batchExecutionServiceImpl.slice(methodStart, nextOverride)

assert.match(batchExecutionService, /completePreReleaseDossierNode\(/,
  'service interface must expose the Stage4 dossier completion boundary')
assert.match(stage4Service, /completePreReleaseDossierNode\(/,
  'Stage4 must complete four dossier nodes through the no-advance dossier boundary')
assert.doesNotMatch(stage4Service, /completeSpecialNode\(/,
  'Stage4 must not call the legacy special-node completion because it advances ordinary route fill tasks')
assert.doesNotMatch(methodBody, /createNextFillAfterSpecialNodeResolved/,
  'Stage4 dossier completion must not create the next ordinary route fill task')
assert.match(methodBody, /MesProductionReleaseReportNodeEvidence/,
  'Stage4 dossier completion must persist formal release report evidence payload')
assert.match(methodBody, /syncBatchStatus\(batch\)/,
  'Stage4 dossier completion must resync the batch aggregate after each material node')

assert.match(stage4Command, /private String inputMode;/,
  'Stage4 command must expose an explicit input mode instead of inferring fallback from missing Stage2.5 data')
assert.match(stage4Req, /private String inputMode;/,
  'Stage4 request must expose an explicit input mode for independent fixture input')
assert.doesNotMatch(stage4Req, /@jakarta\.validation\.constraints\.NotNull\(message = "batchExecutionId 不能为空"\)/,
  'Stage4 independent input mode cannot require a pre-existing Stage2.5 batch execution id at request validation time')
assert.doesNotMatch(stage4Req, /@NotBlank\(message = "stage2_5SimulationRunId 不能为空"\)/,
  'Stage4 independent input mode cannot require a Stage2.5 run id at request validation time')

assert.match(stage4Service, /STAGE4_INDEPENDENT_BATCH_EXECUTION/,
  'Stage4 must name the explicit independent batch execution input mode')
assert.match(stage4Service, /resolveStage4Input\(/,
  'Stage4 must resolve Stage2.5 input and independent input through a single explicit boundary')
assert.match(stage4Service, /createIndependentBatchExecutionInputFixture\(/,
  'Stage4 independent mode must create a complete batch execution input fixture before extracting Stage4 fields')
assert.match(stage4Service, /stage4IndependentBatchExecutionSnapshot\.v1/,
  'Stage4 independent fixture must expose a stable complete-batch input contract')
assert.match(stage4Service, /buildStage4InputFromCompleteBatchExecution\(/,
  'Stage4 must extract its input from the complete batch execution fixture rather than fabricating Stage4-only fields')
assert.match(stage4Service, /MesProEdhrBatchTraceCaptureCommand/,
  'Stage4 independent fixture must create a formal batch trace capture command')
assert.match(stage4Service, /MesProEdhrBatchTraceabilityValidator/,
  'Stage4 independent fixture must validate its formal trace source bundle')
assert.match(stage4Service, /traceabilityService\.capture\(traceCommand\)/,
  'Stage4 independent fixture must persist trace links and a manifest for downstream stages')
assert.doesNotMatch(stage4Service, /catch[\s\S]{0,120}requireStage2_5Batch[\s\S]{0,120}createIndependentBatchExecutionInputFixture/,
  'Stage4 independent input must not be a catch-and-fallback path after Stage2.5 validation fails')

const shortRunIdStart = stage4Service.indexOf('private String shortRunId(String simulationRunId)')
assert.notEqual(shortRunIdStart, -1, 'Stage4 must keep a compact run-id helper for bounded source identifiers')
const shortRunIdEnd = stage4Service.indexOf('\n    private String hash(', shortRunIdStart)
const shortRunIdBody = stage4Service.slice(shortRunIdStart, shortRunIdEnd)
assert.match(shortRunIdBody, /DigestUtil\.sha256Hex\(simulationRunId\)/,
  'Stage4 compact source identifiers must include a hash suffix so repeated runs in the same minute do not collide')
assert.doesNotMatch(shortRunIdBody, /value\.substring\(0,\s*24\)/,
  'Stage4 compact source identifiers must not rely on the timestamp prefix only')

console.log('mes-stage4-dossier-upload-static: PASS')
