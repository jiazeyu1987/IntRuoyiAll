const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../../..')
const controllerPath = path.join(
  repoRoot,
  'ruoyi-vue-pro/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java'
)

const source = fs.readFileSync(controllerPath, 'utf8')

if (source.includes('ContentDisposition.inline().filename(binary.fileName()).build().toString()')) {
  throw new Error('DCC inline preview responses must UTF-8 encode localized filenames in Content-Disposition')
}

if (!source.includes('ContentDisposition.inline()') || !source.includes('filename(fileName, StandardCharsets.UTF_8)')) {
  throw new Error('DCC inline preview responses must use filename(fileName, StandardCharsets.UTF_8)')
}

for (const methodName of [
  'getUploadPreviewOnlyOfficeFile',
  'previewControlledFile',
  'getOnlyOfficePreviewFile'
]) {
  const methodPattern = new RegExp(`${methodName}[\\s\\S]*?contentDispositionInline\\(binary\\.fileName\\(\\)\\)`)
  if (!methodPattern.test(source)) {
    throw new Error(`${methodName} must build inline Content-Disposition through UTF-8 helper`)
  }
}

console.log('PASS: DCC inline preview Content-Disposition uses UTF-8 filenames')
