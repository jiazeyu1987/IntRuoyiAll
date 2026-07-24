const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')

function assertContains(source, expected, label) {
  if (!source.includes(expected)) {
    throw new Error(`missing ${label}: ${expected}`)
  }
}

assertContains(api, '/infra/runtime-control/overview', 'overview API')
assertContains(api, '/infra/runtime-control/restart', 'restart API')
assertContains(api, '/infra/runtime-control/operations', 'operations API')

assertContains(page, "defineOptions({ name: 'InfraRuntimeControl' })", 'component name')
assertContains(page, 'Local', 'local environment label')
assertContains(page, 'Test', 'test environment label')
assertContains(page, 'Production', 'production environment label')
assertContains(page, 'Backup', 'backup environment label')
assertContains(page, 'intruoyi-frontend', 'IntRuoyi frontend component')
assertContains(page, 'intruoyi-backend', 'IntRuoyi backend component')
assertContains(page, 'intruoyi-full', 'IntRuoyi full component')
assertContains(page, 'website-frontend', 'Website frontend component')
assertContains(page, '访问路径', 'access path label')
assertContains(page, 'shouldShowAccessPath', 'access path visibility helper')
assertContains(page, 'prodConfirmText', 'production confirmation field')
assertContains(page, 'PROD', 'production literal confirmation')
assertContains(page, 'infra:runtime-control:restart', 'restart permission')
assertContains(page, "{ key: 'backup', label: 'Backup' }", 'backup environment row source')

console.log('PASS: runtime control frontend API and production guard contracts are wired')
