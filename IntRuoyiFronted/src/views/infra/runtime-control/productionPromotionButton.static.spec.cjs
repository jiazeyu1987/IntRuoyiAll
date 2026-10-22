const fs = require('node:fs')
const path = require('node:path')
const page = fs.readFileSync(path.join(__dirname, 'index.vue'), 'utf8')
const api = fs.readFileSync(path.join(__dirname, '../../../api/infra/runtimeControl/index.ts'), 'utf8')

for (const token of [
  'previewRuntimeControlReleaseWorkflowProduction',
  'productionPreview',
  'previewId',
  'expectedStateVersion',
  'idempotencyKey',
  'infra:runtime-control:promote-prod',
  'crypto.randomUUID()'
]) {
  if (!page.includes(token) && !api.includes(token)) {
    throw new Error(`missing production promotion contract token: ${token}`)
  }
}

if (page.includes('releaseTag: preview') || page.includes('targetEnvironment: preview')) {
  throw new Error('production button must not submit client-owned release target fields')
}

if (page.includes("{ action: 'promote-prod'")) {
  throw new Error('legacy generic promote-prod action entry must not be visible or openable')
}

console.log('production promotion button static contract: PASS')
