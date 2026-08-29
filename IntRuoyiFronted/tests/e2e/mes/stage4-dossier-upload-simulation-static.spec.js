const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const page = readFileSync(
  resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

for (const token of [
  'data-batch-simulate-stage4-dossier',
  '批次执行四份材料上传模拟',
  '四份材料上传模拟完成',
  'data-stage4-dossier-summary',
  'dossierReadyForRelease=true',
  'cleanedSimulationRunId',
  'stage2_5SimulationRunId: upstreamSimulationRunId',
  'stage4SimulationRunId: result.simulationRunId',
  'const inputMode = resolveStage4SimulationInputMode(upstreamSimulationRunId)',
  "'STAGE4_INDEPENDENT_BATCH_EXECUTION'",
  'Stage4 独立批次执行输入',
  'INCOMING_INSPECTION_REPORT',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD',
  'stage4SimulationResult.value = result'
]) {
  assert.ok(page.includes(token), 'Stage4 page is missing visible result token: ' + token)
}

assert.ok(
  /route\.query\.stage2_5SimulationRunId[\s\S]*!route\.query\.stage4SimulationRunId[\s\S]*route\.query\.simulationRunId/.test(page),
  'Stage4 rerun must prefer preserved Stage2.5 source run id before the current Stage4 run id'
)
assert.equal(
  /三类文件上传模拟/.test(page),
  false,
  'Stage4 UI must use the corrected four-material wording instead of the old three-file wording'
)
assert.equal(
  /当前批次缺少流程2\.5正式来源，不能上传流程4资料/.test(page),
  false,
  'Stage4 page must not block independent input mode only because the Stage2.5 run id is absent'
)
