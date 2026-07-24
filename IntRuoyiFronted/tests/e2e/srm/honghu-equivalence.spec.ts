import { expect, test } from 'playwright/test'
import { spawnSync } from 'node:child_process'
import { resolve } from 'node:path'

test.setTimeout(2400000)

const playwrightCli = resolve(process.cwd(), 'node_modules/playwright/cli.js')
const specs = [
  'tests/e2e/srm/supplier-access-risk.spec.ts',
  'tests/e2e/srm/procurement-plan.spec.ts',
  'tests/e2e/srm/non-bidding.spec.ts',
  'tests/e2e/srm/tender.spec.ts',
  'tests/e2e/srm/contract.spec.ts'
]

test('D7-D10 SRM final Honghu-equivalence regression', () => {
  for (const spec of specs) {
    const result = spawnSync(
      process.execPath,
      [playwrightCli, 'test', spec, '--project=chromium', '--reporter=line'],
      {
        cwd: process.cwd(),
        encoding: 'utf8',
        env: process.env,
        timeout: 900000
      }
    )

    if (result.error) {
      throw result.error
    }

    if (result.stdout) {
      console.log(result.stdout)
    }
    if (result.stderr) {
      console.error(result.stderr)
    }

    expect(
      result.status,
      `${spec} failed\nstdout:\n${result.stdout}\nstderr:\n${result.stderr}`
    ).toBe(0)
  }
})
