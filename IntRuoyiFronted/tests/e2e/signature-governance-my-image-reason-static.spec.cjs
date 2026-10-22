const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const source = fs.readFileSync(
  path.join(repoRoot, 'src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue'),
  'utf8'
)

assert.match(
  source,
  /data-testid="dcc-my-signature-image-reason"[\s\S]*?v-model="signatureImageReason"[\s\S]*?请输入本次签名图片变更原因/,
  'personal signature image page must expose a visible reason field'
)

const extractHandler = (name, nextName) => {
  const start = source.indexOf(`const ${name} = async`)
  const end = source.indexOf(`const ${nextName} = async`, start + 1)
  assert.notEqual(start, -1, `${name} handler must exist`)
  assert.notEqual(end, -1, `${nextName} boundary must exist`)
  return source.slice(start, end)
}

const uploadHandler = extractHandler(
  'handleSignatureImageFileChange',
  'handleEnableSignatureImage'
)
assert.match(uploadHandler, /getRequiredSignatureImageReason\(\)/)
assert.match(uploadHandler, /uploadDccElectronicSignatureImage\(rawFile, reason\)/)
assert.match(uploadHandler, /enableDccElectronicSignatureImage\(uploaded\.id, reason\)/)

const enableHandler = extractHandler('handleEnableSignatureImage', 'handleDisableSignatureImage')
assert.match(enableHandler, /getRequiredSignatureImageReason\(\)/)
assert.match(enableHandler, /mySignatureImage\.value\.id,[\s\S]*?reason/)

assert.match(source, /const getRequiredSignatureImageReason = \(\) =>/)
assert.match(source, /请填写签名图片变更原因/)
assert.doesNotMatch(source, /用户上传签名图片|用户启用签名图片/)

console.log('PASS: signature image changes require and reuse the visible reason')
