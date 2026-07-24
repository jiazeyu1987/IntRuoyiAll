const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const firmwareApi = read('src/api/iot/ota/firmware/index.ts')
const firmwarePage = read('src/views/iot/ota/firmware/index.vue')
const productCommentApi = read('src/api/mall/product/comment.ts')
const mailLogApi = read('src/api/system/mail/log/index.ts')
const mailLogPage = read('src/views/system/mail/log/index.vue')

assert.match(firmwareApi, /\/iot\/ota\/firmware\/delete\?id=/)
assert.match(productCommentApi, /\/product\/comment\/get\?id=/)
assert.match(mailLogApi, /\/system\/mail-log\/export-excel/)
assert.ok(!firmwarePage.includes('catch {}'), 'OTA firmware deletion must expose request failures.')
assert.ok(!mailLogPage.includes('catch {\n  }'), 'Mail log export must expose request failures.')
