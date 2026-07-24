const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const extractConstFunction = (source, name) => {
  const start = source.indexOf(`const ${name} =`)
  assert.notEqual(start, -1, `missing ${name}`)
  const firstBrace = source.indexOf('{', start)
  assert.notEqual(firstBrace, -1, `missing ${name} body`)
  let depth = 0
  for (let index = firstBrace; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') {
      depth -= 1
      if (depth === 0) return source.slice(start, index + 1)
    }
  }
  throw new Error(`cannot find end of ${name}`)
}

const signatureCandidatePanes = [
  'src/views/signature-governance/components/RetentionGovernanceListPane.vue',
  'src/views/signature-governance/components/PeriodicReviewGovernanceListPane.vue',
  'src/views/signature-governance/components/CsvPackageGovernanceListPane.vue'
]

for (const relativePath of signatureCandidatePanes) {
  const source = readSource(relativePath)
  assert.match(
    source,
    /useUserStore/,
    `${relativePath} must read the current user's permissions before loading DCC signature candidates.`
  )
  assert.match(
    source,
    /dcc:controlled-file:signature:manage/,
    `${relativePath} must explicitly gate DCC signature candidate loading by the backend permission.`
  )
  assert.match(
    source,
    /hasDccSignatureManagePermission/,
    `${relativePath} must expose a DCC signature management permission guard.`
  )

  const loader = extractConstFunction(source, 'loadDccSignatureCandidates')
  const permissionCheckIndex = loader.indexOf('hasDccSignatureManagePermission.value')
  const apiCallIndex = loader.indexOf('getDccElectronicSignaturePage')
  assert.notEqual(permissionCheckIndex, -1, `${relativePath} loader must check DCC signature permission.`)
  assert.notEqual(apiCallIndex, -1, `${relativePath} loader must still use real DCC signature candidates when allowed.`)
  assert.ok(
    permissionCheckIndex < apiCallIndex,
    `${relativePath} loader must check permission before calling DCC signature management page API.`
  )
  assert.match(
    loader,
    /当前账号没有DCC电子签名管理权限/,
    `${relativePath} loader must fail fast with a visible permission message instead of letting the backend 403 surface from an automatic read path.`
  )
}

const routeQueryScanRoots = [
  'src/views/mes/pro/edhr',
  'src/views/signature-governance/components'
]

const collectVueFiles = (relativeDir) => {
  const absoluteDir = path.join(root, relativeDir)
  const results = []
  for (const entry of fs.readdirSync(absoluteDir, { withFileTypes: true })) {
    const entryRelativePath = path.join(relativeDir, entry.name).replace(/\\/g, '/')
    if (entry.isDirectory()) {
      results.push(...collectVueFiles(entryRelativePath))
    } else if (entry.isFile() && entry.name.endsWith('.vue')) {
      results.push(entryRelativePath)
    }
  }
  return results
}

const unsafeRouteQueryIdPatterns = [
  /Number\s*\(\s*route\.query\.[A-Za-z0-9_]+\s*\)/,
  /parsePositive(?:Number|QueryNumber)\s*\(\s*route\.query\.[A-Za-z0-9_]+\s*\)/,
  /parsePositive(?:Number|QueryNumber)\s*\(\s*route[A-Z][A-Za-z0-9_]*\s*\)/
]

for (const relativePath of routeQueryScanRoots.flatMap(collectVueFiles)) {
  const source = readSource(relativePath)
  for (const pattern of unsafeRouteQueryIdPatterns) {
    assert.doesNotMatch(
      source,
      pattern,
      `${relativePath} must preserve route query Long ids as strings; JavaScript Number loses BIGINT precision.`
    )
  }
}

console.log('PASS: signature governance permission gate and eDHR route query BIGINT static contract')
