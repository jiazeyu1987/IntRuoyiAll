const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workspaceRoot = path.resolve(process.cwd(), '..')
const goalPath = path.join(workspaceRoot, '实现目标', '批记录目标', '1.txt')
const scriptPath = path.resolve(process.cwd(), 'tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js')

const goals = fs
  .readFileSync(goalPath, 'utf8')
  .replace(/^\uFEFF/, '')
  .split(/\r?\n/)
  .map((line) => line.trim())
  .filter(Boolean)
const source = fs.readFileSync(scriptPath, 'utf8')

assert.equal(goals.length, 58, '当前批记录目标清单必须按 58 条校验覆盖口径。')
assert(
  source.includes('const EXPECTED_GOAL_COUNT = 58') &&
    source.includes('const CORE_REQUIREMENT_IDS = Array.from({ length: 54 }, (_, index) => index + 1)'),
  '主链路脚本必须明确区分当前 58 条目标与 1-54 核心批记录闭环。'
)
assert(
  source.includes('const TAIL_FOUR_COMPANION_REQUIREMENTS = [55, 56, 57, 58]') &&
    source.includes("edhr-tail-four-goals-real-flow.e2e.js"),
  '第 55-58 条目标必须由独立真实路径脚本作为伴随验证。'
)
assert(
  source.includes('assertCoreCoverageMatrix(goals)') && source.includes('assertTailFourCompanionCoverage(goals)'),
  '主链路启动前必须同时校验核心覆盖矩阵和尾部伴随脚本入口。'
)
assert(
  !source.includes('应拆分为 54 条目标') && source.includes('PASS: coveredCoreRequirements='),
  '主链路输出不得再把 58 条目标全部误报为单脚本覆盖。'
)

console.log('PASS: eDHR full-chain goal coverage static contract')
