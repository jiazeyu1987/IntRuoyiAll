const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  return fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')
}

const api = readUtf8('src/api/infra/runtimeControl/index.ts')
const page = readUtf8('src/views/infra/runtime-control/index.vue')

for (const fragment of [
  'RuntimeControlRemoteRootDiskStatusVO',
  'RuntimeControlRemoteRootCleanupReqVO',
  '/infra/runtime-control/remote-root-disk/status',
  '/infra/runtime-control/remote-root-disk/cleanup',
  "RuntimeControlRootDiskTargetEnvironment = 'test' | 'prod' | 'backup'",
  'prodConfirmText?: string'
]) {
  assert(api.includes(fragment) || page.includes(fragment), `runtime-control remote root cleanup contract missing ${fragment}`)
}

for (const fragment of [
  '远程根分区',
  '刷新根分区',
  '清理临时目录',
  'remoteRootDiskStatus',
  'loadRemoteRootDiskStatus',
  'openRemoteRootCleanupDialog',
  'submitRemoteRootCleanup',
  'remoteRootCleanupRequiresProdConfirm',
  'remoteRootTargetEnvironment',
  'rootDiskTargetOptions',
  '/opt/intruoyi/ops/backup/tmp',
  '/tmp',
  '172.30.30.58',
  '172.30.30.57',
  '172.30.30.59',
  '输入 PROD',
  'prodConfirmText'
]) {
  assert(page.includes(fragment), `runtime-control page missing remote root cleanup UI fragment ${fragment}`)
}

assert(page.includes("targetEnvironment: remoteRootTargetEnvironment.value"), 'remote root cleanup UI must submit the selected fixed target')
assert(page.includes("remoteRootCleanupDialog.prodConfirmText !== 'PROD'"), 'remote root cleanup UI must require PROD before protected cleanup')
assert(page.includes("['prod', 'backup'].includes(environment)"), 'remote root cleanup UI must protect prod and backup cleanup')
assert(!page.includes('/infra/runtime-control/test-root-disk/cleanup'), 'remote root cleanup UI must not keep old test-root cleanup endpoint')

console.log('PASS: runtime-control remote root cleanup frontend wiring is present')
