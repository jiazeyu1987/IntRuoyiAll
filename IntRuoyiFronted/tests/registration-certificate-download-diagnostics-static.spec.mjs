import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')

const detailPath = 'IntRuoyiFronted/src/views/dcc/registration-certificate/detail/index.vue'
const controllerPath = 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/file/DccRegistrationCertificateFilePreviewController.java'

assert.equal(existsSync(join(root, detailPath)), true, `${detailPath} must exist`)
assert.equal(existsSync(join(root, controllerPath)), true, `${controllerPath} must exist`)

const detail = read(detailPath)
const controller = read(controllerPath)

assert.match(
  detail,
  /console\.info\(\s*'\[registration-certificate-download\]'[\s\S]*businessFileId[\s\S]*viewMode[\s\S]*detailStatus[\s\S]*routeVersionId[\s\S]*registrationFileId[\s\S]*sourceFileName[\s\S]*savedFileName[\s\S]*expired[\s\S]*\)/,
  'frontend detail download must log route mode, detail status, ids, backend filename, final saved filename, and expired flag'
)
assert.match(
  controller,
  /LoggerFactory\.getLogger\(DccRegistrationCertificateFilePreviewController\.class\)/,
  'registration certificate file controller must own a logger'
)
assert.match(
  controller,
  /registration-certificate-preview-metadata[\s\S]*businessFileId=\{\}[\s\S]*fileName=\{\}/,
  'preview metadata endpoint must log the resolved file name'
)
assert.match(
  controller,
  /registration-certificate-preview-file[\s\S]*businessFileId=\{\}[\s\S]*fileName=\{\}/,
  'preview file endpoint must log the resolved inline file name'
)
assert.match(
  controller,
  /registration-certificate-download-file[\s\S]*businessFileId=\{\}[\s\S]*fileName=\{\}/,
  'download endpoint must log the resolved attachment file name'
)

console.log('registration certificate download diagnostics static contract passed')
