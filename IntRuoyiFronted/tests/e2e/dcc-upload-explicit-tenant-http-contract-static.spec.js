const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const workflow = fs.readFileSync(
  path.join(root, 'src/api/dcc/controlledFile/workflow.ts'),
  'utf8'
)

const sliceBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing start anchor: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing end anchor: ${end}`)
  return source.slice(startIndex, endIndex)
}

assert.match(
  workflow,
  /const buildDccExplicitTenantHeaders = \(\) => \{[\s\S]*Number\.isSafeInteger\(tenantId\)[\s\S]*tenantId <= 0[\s\S]*throw new DccControlledFileContractError[\s\S]*'tenant-id': String\(tenantId\)/,
  'DCC upload API must fail fast unless the cached system tenant is a positive safe integer'
)

const uploadBlock = sliceBetween(
  workflow,
  'export const uploadControlledFilePreview',
  'export const getControlledFileUploadTemporaryStatus'
)
const statusBlock = sliceBetween(
  workflow,
  'export const getControlledFileUploadTemporaryStatus',
  'export const cleanupControlledFileUploadSession'
)
const cleanupBlock = sliceBetween(
  workflow,
  'export const cleanupControlledFileUploadSession',
  'export const checkControlledFileRouteReadiness'
)

for (const [name, block] of [
  ['upload-preview', uploadBlock],
  ['upload-temporary/status', statusBlock],
  ['upload-temporary/session-cleanup', cleanupBlock]
]) {
  assert.match(
    block,
    /headers:\s*\{[\s\S]*\.\.\.buildDccExplicitTenantHeaders\(\)/,
    `${name} must explicitly attach the validated tenant-id header`
  )
  assert.doesNotMatch(block, /validateStatus|catch\s*\([^)]*\)\s*\{\s*return\s*\{/,
    `${name} must not convert non-2xx failures into success data`)
}

console.log('PASS: DCC upload endpoints require explicit tenant headers and preserve non-2xx failures')
