import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = dirname(dirname(fileURLToPath(import.meta.url)))
const apiPath = join(root, 'src/api/dcc/controlledFile/signatures.ts')
const pagePath = join(root, 'src/views/dcc/controlled-file/signatures/index.vue')
const mySignaturePath = join(root, 'src/views/signature-governance/components/SignatureGovernanceMySignaturePane.vue')

assert.ok(existsSync(apiPath), 'DCC signature API wrapper must exist')
assert.ok(existsSync(pagePath), 'DCC signature governance page must exist')
assert.ok(existsSync(mySignaturePath), 'my signature governance pane must exist')

const apiSource = readFileSync(apiPath, 'utf8')
const pageSource = readFileSync(pagePath, 'utf8')
const mySignatureSource = readFileSync(mySignaturePath, 'utf8')

assert.match(apiSource, /DccElectronicSignatureImageVO/, 'API must expose signature image VO')
assert.match(apiSource, /uploadDccElectronicSignatureImage/, 'API must expose signature image upload')
assert.match(
  apiSource,
  /request\.upload<\{\s*data:\s*DccElectronicSignatureImageVO\s*\}>/,
  'signature image upload must type the wrapped upload response'
)
assert.match(
  apiSource,
  /return\s+result\.data/,
  'signature image upload must unwrap the upload response payload before page usage'
)
assert.match(apiSource, /enableDccElectronicSignatureImage/, 'API must expose signature image enable')
assert.match(apiSource, /disableDccElectronicSignatureImage/, 'API must expose signature image disable')
assert.match(apiSource, /getMyDccElectronicSignatureImage/, 'API must expose my signature image query')
assert.match(apiSource, /signatureImageSha256/, 'signature record API type must expose image hash snapshot')
assert.match(apiSource, /signatureImageVerifiedStatus/, 'signature evidence API type must expose image verification status')

assert.match(mySignatureSource, /签名图片/, 'my signature pane must expose signature image setting copy')
assert.match(mySignatureSource, />\s*上传图片\s*</, 'my signature pane must provide short upload entry')
assert.match(mySignatureSource, />\s*启用图片\s*</, 'my signature pane must provide short enable action')
assert.match(mySignatureSource, />\s*停用图片\s*</, 'my signature pane must provide short disable action')
assert.doesNotMatch(`${pageSource}\n${mySignatureSource}`, /<el-button[\s\S]*?>\s*上传签名图片\s*<\/el-button>/, 'upload button label must be at most 4 chars')
assert.doesNotMatch(`${pageSource}\n${mySignatureSource}`, /<el-button[\s\S]*?>\s*启用签名图片\s*<\/el-button>/, 'enable button label must be at most 4 chars')
assert.doesNotMatch(`${pageSource}\n${mySignatureSource}`, /<el-button[\s\S]*?>\s*停用签名图片\s*<\/el-button>/, 'disable button label must be at most 4 chars')
assert.doesNotMatch(pageSource, /data-testid="dcc-signature-image-toolbar-actions"/, 'authorization page must not own personal signature image actions')
assert.match(pageSource, /signatureImageSha256/, 'record list must render signature image hash short code')
assert.match(pageSource, /signatureImageVerifiedStatus/, 'record/evidence view must show image verification state')
assert.match(mySignatureSource, /accept="image\/png,image\/jpeg"/, 'upload control must restrict PNG/JPEG at UI level')
assert.match(
  mySignatureSource,
  /signatureImagePreviewUrl/,
  'my signature pane must use a normalized preview URL instead of rendering the raw backend URL directly'
)
assert.match(
  mySignatureSource,
  /signatureImagePreviewList/,
  'my signature pane must expose the current image as an el-image preview list'
)
assert.match(
  mySignatureSource,
  /:preview-src-list="signatureImagePreviewList"/,
  'my signature pane must allow users to click and preview their own signature image'
)
assert.doesNotMatch(
  mySignatureSource,
  /:src="mySignatureImage\.fileUrl"/,
  'my signature pane must not bind el-image src directly to raw fileUrl'
)
assert.match(
  mySignatureSource,
  /formatSignatureImageDateTime\(mySignatureImage\.uploadedAt\)/,
  'uploaded time must be formatted as a human-readable date time'
)
assert.match(
  mySignatureSource,
  /formatSignatureImageDateTime\(mySignatureImage\.enabledAt\)/,
  'enabled time must be formatted as a human-readable date time'
)
assert.doesNotMatch(
  mySignatureSource,
  /\{\{\s*mySignatureImage\.(uploadedAt|enabledAt)\s*\|\|\s*'-'\s*\}\}/,
  'my signature pane must not render raw timestamp values'
)
