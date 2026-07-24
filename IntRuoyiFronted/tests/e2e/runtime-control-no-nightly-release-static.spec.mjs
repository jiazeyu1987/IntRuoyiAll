import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const repoRoot = process.cwd()

const readUtf8 = (relativePath) => {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')

const forbiddenFragments = [
  'RuntimeNightlyReleaseRunVO',
  'RuntimeNightlyReleaseStatusVO',
  'getRuntimeNightlyReleaseStatus',
  '/infra/runtime-control/nightly-release',
  'nightlyReleaseStatus',
  'loadNightlyReleaseStatus',
  'nightly-release-panel',
  '夜间定时发布',
  '下一次运行'
]

for (const fragment of forbiddenFragments) {
  assert(
    !api.includes(fragment) && !page.includes(fragment),
    `runtime-control frontend must remove nightly release wiring: ${fragment}`
  )
}

for (const manualAction of [
  "action: 'build-release'",
  "action: 'publish-test'",
  "action: 'promote-backup'"
]) {
  assert(page.includes(manualAction), `manual runtime-control action must remain: ${manualAction}`)
}

console.log('PASS: runtime-control no longer exposes nightly release wiring')
