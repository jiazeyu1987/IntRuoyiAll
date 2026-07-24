import { expect, test } from 'playwright/test'
import { spawnSync } from 'node:child_process'
import { resolve } from 'node:path'

test.describe.configure({ mode: 'serial' })
test.setTimeout(600000)

const RELEASE_MATRIX_ARTIFACTS = [
  'controlled-content-dcc-sop-release-real.json',
  'controlled-content-dcc-work-instruction-review-readonly-real.json',
  'controlled-content-dcc-inspection-withdraw-draft-real.json',
  'controlled-content-dcc-drawing-obsolete-real.json',
  'controlled-content-mes-route-version-full-flow-real.json',
  'controlled-content-live-migration-real.json',
  'controlled-content-health-check-readonly-real.json'
]

function runNodeScript(script: string, args: string[] = []) {
  const result = spawnSync(process.execPath, [resolve(process.cwd(), script), ...args], {
    cwd: process.cwd(),
    encoding: 'utf8',
    env: process.env,
    timeout: 540000
  })

  if (result.error) {
    throw result.error
  }

  if (result.stdout) {
    console.log(result.stdout)
  }
  if (result.stderr) {
    console.error(result.stderr)
  }

  expect(result.status, `${script} failed\nstdout:\n${result.stdout}\nstderr:\n${result.stderr}`).toBe(0)
}

test('controlled content lifecycle static contract remains wired to MES and DCC pages', () => {
  runNodeScript('tests/e2e/controlled-content-state-view-static.spec.js')
})

test('controlled content DCC state view real readonly path uses test tenant and no write requests', () => {
  runNodeScript('tests/e2e/controlled-content-state-view-real-readonly.e2e.js')
})

test('controlled content release claim gate remains blocked until real write matrix evidence exists', () => {
  expect(RELEASE_MATRIX_ARTIFACTS.length, 'release matrix artifact contract must stay explicit').toBe(7)
  runNodeScript('scripts/controlled-content-release-claim-gate.mjs', ['--expect-blocked'])
})
