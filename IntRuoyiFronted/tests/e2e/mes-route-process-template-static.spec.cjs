const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/route/index.ts')
const page = read('src/views/mes/pro/route/index.vue')
const form = read('src/views/mes/pro/route/RouteProcessTemplateImportForm.vue')

assert.match(api, /export-process-template-xlsx/)
assert.match(api, /import-process-template-xlsx/)
assert.match(api, /exportRouteProcessTemplate/)
assert.match(api, /importRouteProcessTemplate/)
assert.match(page, /RouteProcessTemplateImportForm/)
assert.match(page, /工序模板/)
assert.match(form, /REBUILD/)
assert.match(form, /UPGRADE/)
assert.match(form, /设备编号/)
assert.match(form, /批记录表单/)
assert.match(form, /表单槽位/)

console.log('MES route process template frontend contract passed')
