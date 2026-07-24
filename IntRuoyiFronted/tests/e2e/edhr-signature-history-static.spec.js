const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const signaturePage = readSource('src/views/mes/pro/edhr/SignaturePage.vue')

assert.equal(
  packageJson.scripts['e2e:edhr:signature-history:static'],
  'node tests/e2e/edhr-signature-history-static.spec.js',
  'package.json 必须提供 eDHR 签名历史 FDA 快照静态合同脚本'
)

for (const label of ['业务记录', '动作结果', '签名人', '账号', '部门/岗位', '角色', '签名目的', '签名时间', '意见/原因']) {
  assert.match(signaturePage, new RegExp(`label="${label}"`), `eDHR 签名列表必须展示：${label}`)
}

for (const label of [
  '账号快照',
  '部门快照',
  '岗位快照',
  '角色快照',
  '签名目的',
  '认证方式',
  '记录版本',
  '审计状态',
  '权限依据'
]) {
  assert.match(signaturePage, new RegExp(label), `eDHR 签名展开审计区必须展示：${label}`)
}

for (const snapshotField of [
  'actorUsernameSnapshot',
  'actorDeptNameSnapshot',
  'actorPostNamesSnapshot',
  'actorRoleNamesSnapshot',
  'signaturePurpose',
  'authenticationMethod',
  'authorizationBasis',
  'recordVersionSnapshot',
  'recordHashSnapshot',
  'clientIpSnapshot',
  'userAgentSnapshot'
]) {
  assert.match(signaturePage, new RegExp(snapshotField), `eDHR 签名历史必须展示 FDA 快照字段：${snapshotField}`)
}

assert.match(
  signaturePage,
  /row\.actorUsernameSnapshot\s*\|\|\s*'旧版证据未记录'/,
  'eDHR 签名列表账号行只能展示签名当时账号快照或旧版证据提示'
)
assert.match(
  signaturePage,
  /return text \|\| '旧版证据未记录'/,
  'eDHR 空快照字段必须显示旧版证据未记录'
)
assert.doesNotMatch(
  signaturePage,
  /actorUsernameSnapshot\s*\|\|\s*row\.actorNickname/,
  'eDHR 历史账号快照不得用当前或旧兼容昵称字段回填'
)

console.log('PASS: eDHR signature history FDA snapshot static contract')
