import { spawnSync } from 'node:child_process'

const target = process.argv[2]
const knownTargets = new Map([
  ['ControlledContentLifecycle', ['node', 'tests/e2e/controlled-content-state-view-static.spec.js']],
  ['ControlledContentReleaseGate', ['node', 'tests/e2e/controlled-content-release-claim-gate-static.spec.js']]
])

if (!knownTargets.has(target)) {
  console.error(`Unknown frontend test target: ${target || '<empty>'}`)
  process.exit(1)
}

const [command, ...args] = knownTargets.get(target)
const result = spawnSync(command, args, { stdio: 'inherit', shell: process.platform === 'win32' })
process.exit(result.status ?? 1)
