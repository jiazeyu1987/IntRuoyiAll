import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import assert from 'node:assert/strict'

// The upload dialog now uses shared facts validation; run its behavior and rendered-field tests.
const root = fileURLToPath(new URL('../', import.meta.url))
const result = spawnSync(process.execPath, ['--test', 'tests/unit/registration-certificate-ce-creation.test.cjs', 'tests/unit/registration-certificate-creation-layout.test.cjs'], { cwd: root, encoding: 'utf8' })
process.stdout.write(result.stdout ?? '')
process.stderr.write(result.stderr ?? '')
assert.ifError(result.error)
assert.equal(result.status, 0, 'creation fields and date validation behavior must pass')
