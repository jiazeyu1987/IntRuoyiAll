const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const timelineSource = readSource(
  'src/views/bpm/processInstance/detail/ProcessInstanceTimeline.vue'
)

assert.equal(
  packageJson.scripts['e2e:bpm:timeline-current-node-green:static'],
  'node tests/e2e/bpm-process-timeline-current-node-green-static.spec.js',
  'package.json must expose the current-node green static contract.'
)

assert.match(
  timelineSource,
  /const APPROVAL_ACTIVE_COLOR\s*=\s*'#00b32a'/,
  'timeline must define a single green color for current approval nodes.'
)

assert.match(
  timelineSource,
  /const isCurrentApprovalNodeStatus = \(taskStatus: number\) =>[\s\S]*TaskStatusEnum\.WAIT[\s\S]*TaskStatusEnum\.RUNNING[\s\S]*TaskStatusEnum\.APPROVING/,
  'timeline must classify WAIT, RUNNING, and APPROVING as current approval node statuses.'
)

assert.match(
  timelineSource,
  /:style="\{ backgroundColor: getApprovalNodeDotColor\(activity\.status\) \}"/,
  'timeline main node dot must use status-aware current-node coloring.'
)

assert.match(
  timelineSource,
  /:style="\s*isCurrentApprovalNodeStatus\(activity\.status\)\s*\?\s*\{\s*color:\s*APPROVAL_ACTIVE_COLOR\s*\}\s*:\s*undefined\s*"/,
  'timeline node label must become green for the current active node.'
)

const statusIconMap2Block = extractBetween(
  timelineSource,
  'const statusIconMap2 = {',
  'const statusIconMap = {',
  'small status icon color map'
)
const statusIconMapBlock = extractBetween(
  timelineSource,
  'const statusIconMap = {',
  'const nodeTypeSvgMap = {',
  'timeline status color map'
)

assert.match(
  statusIconMap2Block,
  /'1':\s*\{\s*color:\s*APPROVAL_ACTIVE_COLOR/,
  'RUNNING avatar status badge must use the current-node green color.'
)
assert.match(
  statusIconMapBlock,
  /'1':\s*\{\s*color:\s*APPROVAL_ACTIVE_COLOR/,
  'RUNNING timeline status color must use the current-node green color.'
)
assert.doesNotMatch(
  statusIconMap2Block + statusIconMapBlock,
  /'1':\s*\{\s*color:\s*'#448ef7'/,
  'RUNNING current node must not keep the old blue status color.'
)

console.log('PASS: BPM process timeline current node uses green styling')
