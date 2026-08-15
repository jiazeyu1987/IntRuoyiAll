import { spawnSync } from 'node:child_process'

const target = process.argv[2]
const knownTargets = new Map([
  ['ControlledContentLifecycle', ['node', 'tests/e2e/controlled-content-state-view-static.spec.js']],
  ['ControlledContentReleaseGate', ['node', 'tests/e2e/controlled-content-release-claim-gate-static.spec.js']],
  ['e2e:frontline-formal-submit:static', ['node', 'tests/e2e/frontline-formal-submit-static.spec.cjs']],
  ['e2e:frontline-team-config:static', ['node', 'tests/e2e/frontline-team-config-static.spec.cjs']],
  ['e2e:team-leader-report-allocation:static', ['node', 'tests/e2e/team-leader-report-allocation-static.spec.cjs']],
  ['e2e:team-leader-workbench:static', ['node', 'tests/e2e/team-leader-workbench-static.spec.cjs']],
  ['sp1-production-release-contract', ['node', 'tests/e2e/sp1-production-release-contract.spec.cjs']],
  ['e2e:team-leader-workbench:real:check', ['node', '--check', 'tests/e2e/team-leader-workbench-real-flow.e2e.js']],
  ['e2e:team-leader-workbench:real', ['node', 'tests/e2e/team-leader-workbench-real-flow.e2e.js']]
])

if (!knownTargets.has(target)) {
  console.error(`Unknown frontend test target: ${target || '<empty>'}`)
  process.exit(1)
}

const [command, ...args] = knownTargets.get(target)
const result = spawnSync(command, args, { stdio: 'inherit', shell: process.platform === 'win32' })
process.exit(result.status ?? 1)
