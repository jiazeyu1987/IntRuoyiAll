const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = process.cwd()
const runner = fs.readFileSync(path.resolve(root, 'tests/e2e/edhr-ai-loop/runner.cjs'), 'utf8')

assert.match(
  runner,
  /async function executeProductionAndPqcInterleaved\(page, manifestOrder, activeOrder\)/,
  'runner must execute production and PQC in one interleaved business chain'
)
assert.match(
  runner,
  /baseline.productionProcesses/,
  'runner must cover the frozen production processes'
)
assert.match(
  runner,
  /await reviewProductionReport[\s\S]*await executePending\(step\)/,
  'interleaving plan must alternate production and PQC actions'
)
assert.match(
  runner,
  /const processExecution = await runWithStage\('S02',[\s\S]*executeProductionAndPqcInterleaved\(page, mainOrder, activeOrders\[0\]\)/,
  'main runner must use the interleaved process execution before S04'
)
assert.doesNotMatch(
  runner,
  /const production = await submitProductionReports\(page, ordersForMode\(manifest\)\[0\], activeOrders\[0\]\)[\s\S]*const pqc = await submitPqcInspections\(page, ordersForMode\(manifest\)\[0\], production\)/,
  'runner must not finish all production before starting all PQC'
)

console.log('PASS: eDHR AI loop production/PQC interleaving static contract')



assert.doesNotMatch(runner, /INTERLEAVED_MAIN_CHAIN_PLAN|PQC_ROUND_PLAN|buildProductionSubmissionPlan/)
assert.match(runner, /await executePending\(null\)/)
assert.match(runner, /async function completeActiveOrderAndApplyRelease\(page, manifestOrder\) \{\s*await verifyExecutionProgress/)
