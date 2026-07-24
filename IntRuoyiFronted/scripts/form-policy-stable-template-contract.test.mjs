import { strict as assert } from 'node:assert'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const repoRoot = resolve(__dirname, '..')

const apiSource = readFileSync(resolve(repoRoot, 'src/api/form-center/policy.ts'), 'utf8')
const pageSource = readFileSync(resolve(repoRoot, 'src/views/form-center/policy/index.vue'), 'utf8')

assert.match(apiSource, /templateId:\s*number/, 'policy save slot payload must use stable templateId')
assert.doesNotMatch(apiSource, /templateVersionId:\s*number/, 'policy save payload must not expose templateVersionId')
assert.match(pageSource, /policyForm\.templateId/, 'policy form state must bind templateId')
assert.match(pageSource, /templateId:\s*policyForm\.templateId/, 'save payload must submit templateId')
assert.doesNotMatch(pageSource, /templateVersionId:\s*policyForm\./, 'save payload must not submit templateVersionId')
