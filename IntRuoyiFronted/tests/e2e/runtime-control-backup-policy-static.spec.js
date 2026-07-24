const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')

assertContains(api, 'retentionMaxNasUsedPercent?: number', 'NAS retention threshold API field')
assertContains(api, 'objectAddedCount?: number', 'object added count API field')
assertContains(api, 'objectModifiedCount?: number', 'object modified count API field')
assertContains(api, 'objectDeletedCount?: number', 'object deleted count API field')
assertContains(api, 'objectReusedCount?: number', 'object reused count API field')
assertContains(page, '当前保留策略', 'retention policy summary')
assertContains(page, 'latest.retentionMaxNasUsedPercent', 'NAS retention threshold display')
assertContains(page, 'objectAddedCount', 'object added count display')
assertContains(page, 'objectModifiedCount', 'object modified count display')
assertContains(page, 'objectDeletedCount', 'object deleted count display')
assertContains(page, 'objectReusedCount', 'object reused count display')

console.log('PASS: runtime-control backup policy and object delta summary contract is wired')
