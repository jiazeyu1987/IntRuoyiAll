const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repositoryRoot = path.resolve(frontendRoot, '..')
const apiSource = fs.readFileSync(
  path.join(frontendRoot, 'src/api/showroom-admin/index.ts'),
  'utf8'
)
const workbenchSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/showroom-admin/company/CompanyWorkbench.vue'),
  'utf8'
)
const roundtripSource = fs.readFileSync(
  path.join(frontendRoot, 'tests/e2e/showroom-award-export-import-roundtrip-real.e2e.js'),
  'utf8'
)
const backendControllerSource = fs.readFileSync(
  path.join(
    repositoryRoot,
    'IntRuoyiBackend/yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/admin/ShowroomClientDownloadController.java'
  ),
  'utf8'
)
const backendDownloadFileSource = fs.readFileSync(
  path.join(
    repositoryRoot,
    'IntRuoyiBackend/yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/controller/admin/ShowroomClientDownloadFile.java'
  ),
  'utf8'
)
const backendAttributesPath = path.join(repositoryRoot, 'IntRuoyiBackend/.gitattributes')
const frontendAttributesPath = path.join(repositoryRoot, 'IntRuoyiFronted/.gitattributes')
const desktopPackagePath = path.join(
  repositoryRoot,
  'IntRuoyiBackend/yudao-module-showroom/src/main/resources/showroom/client-downloads/v1.0/YingtaiShowroomClient-Win7-v1.0.zip'
)
const trackedWorkbookPath = path.join(
  repositoryRoot,
  'IntRuoyiFronted/doc/tasks/20260615-showroom-award-export-import-real-e2e/产品资料修改版-补充产品资料.xlsx'
)

assert.doesNotMatch(apiSource, /SHOWROOM_DESKTOP_CLIENT/)
assert.doesNotMatch(apiSource, /downloadDesktopClient/)
assert.doesNotMatch(apiSource, /\/showroom\/client-downloads\/desktop-win7/)
assert.doesNotMatch(workbenchSource, /下载电脑桌面端/)
assert.doesNotMatch(workbenchSource, /handleDownloadDesktopClient|downloadingDesktopClient/)

assert.match(roundtripSource, /require\('node:os'\)/)
assert.match(roundtripSource, /os\.tmpdir\(\)/)
assert.match(roundtripSource, /fs\.rmSync\(downloadPath,\s*\{\s*force:\s*true\s*\}\)/)
assert.doesNotMatch(
  roundtripSource,
  /doc\/tasks\/20260615-showroom-award-export-import-real-e2e/
)
assert.doesNotMatch(backendControllerSource, /desktop-win7|downloadDesktopClient/)
assert.doesNotMatch(backendDownloadFileSource, /DESKTOP_WIN7|YingtaiShowroomClient-Win7/)
assert.equal(fs.existsSync(desktopPackagePath), false)
assert.equal(fs.existsSync(trackedWorkbookPath), false)
assert.equal(fs.existsSync(backendAttributesPath), false)
assert.equal(fs.existsSync(frontendAttributesPath), false)

console.log('PASS: Win7 download is retired and showroom roundtrip workbook is temporary')
