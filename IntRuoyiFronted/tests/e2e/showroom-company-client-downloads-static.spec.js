const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const apiSource = readText('src/api/showroom-admin/index.ts')
const workbenchSource = readText('src/views/showroom-admin/company/CompanyWorkbench.vue')

assert.match(apiSource, /SHOWROOM_ANDROID_CLIENT_DOWNLOAD_URL\s*=\s*'\/showroom\/client-downloads\/android'/)
assert.match(apiSource, /SHOWROOM_DESKTOP_CLIENT_DOWNLOAD_URL\s*=\s*'\/showroom\/client-downloads\/desktop-win7'/)
assert.match(apiSource, /SHOWROOM_ANDROID_CLIENT_FILE_NAME\s*=\s*'YingtaiShowroomClient-Android-v1\.0\.apk'/)
assert.match(apiSource, /SHOWROOM_DESKTOP_CLIENT_FILE_NAME\s*=\s*'YingtaiShowroomClient-Win7-v1\.0\.zip'/)
assert.match(apiSource, /downloadAndroidClient:\s*async\s*\(\)\s*=>\s*\{/)
assert.match(apiSource, /request\.download\(\{\s*url:\s*SHOWROOM_ANDROID_CLIENT_DOWNLOAD_URL\s*\}\)/)
assert.match(apiSource, /downloadDesktopClient:\s*async\s*\(\)\s*=>\s*\{/)
assert.match(apiSource, /request\.download\(\{\s*url:\s*SHOWROOM_DESKTOP_CLIENT_DOWNLOAD_URL\s*\}\)/)

assert.match(workbenchSource, /下载安卓客户端/)
assert.match(workbenchSource, /下载电脑桌面端/)
assert.match(workbenchSource, /@click="handleDownloadAndroidClient"/)
assert.match(workbenchSource, /@click="handleDownloadDesktopClient"/)
assert.match(workbenchSource, /const downloadingAndroidClient = ref\(false\)/)
assert.match(workbenchSource, /const downloadingDesktopClient = ref\(false\)/)
assert.match(workbenchSource, /await ShowroomAdminApi\.downloadAndroidClient\(\)/)
assert.match(workbenchSource, /downloadByData\(\s*data,\s*SHOWROOM_ANDROID_CLIENT_FILE_NAME,\s*'application\/vnd\.android\.package-archive'/)
assert.match(workbenchSource, /SHOWROOM_ANDROID_CLIENT_FILE_NAME/)
assert.match(workbenchSource, /await ShowroomAdminApi\.downloadDesktopClient\(\)/)
assert.match(workbenchSource, /downloadByData\(\s*data,\s*SHOWROOM_DESKTOP_CLIENT_FILE_NAME,\s*'application\/zip'/)
assert.match(workbenchSource, /SHOWROOM_DESKTOP_CLIENT_FILE_NAME/)
assert.ok(
  workbenchSource.indexOf('下载安卓客户端') < workbenchSource.indexOf('下载电脑桌面端'),
  '安卓客户端下载按钮应位于电脑桌面端按钮之前'
)
assert.ok(
  workbenchSource.indexOf('下载电脑桌面端') < workbenchSource.indexOf('@click="openEditDialog"'),
  '客户端下载按钮应位于编辑公司按钮之前，便于用户进入页面后直接下载'
)
