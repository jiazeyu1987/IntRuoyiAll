const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const apiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/workorder/index.ts'), 'utf8')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/workorder/index.vue'), 'utf8')
const formSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/workorder/WorkOrderForm.vue'), 'utf8')

assert.match(apiSource, /drawingNumber:\s*string/)
assert.match(apiSource, /refNo:\s*string/)

assert.match(pageSource, /prop="drawingNumber"[^>]*label="图号"|label="图号"[^>]*prop="drawingNumber"/)
assert.match(pageSource, /prop="refNo"[^>]*label="REF\.NO\."|label="REF\.NO\."[^>]*prop="refNo"/)
assert.match(pageSource, /key:\s*'drawingNumber',\s*label:\s*'图号'/)
assert.match(pageSource, /key:\s*'refNo',\s*label:\s*'REF\.NO\.'/)

assert.match(formSource, /label="图号"[\s\S]{0,160}v-model="formData\.drawingNumber"/)
assert.match(formSource, /label="REF\.NO\."[\s\S]{0,160}v-model="formData\.refNo"/)
assert.match(formSource, /drawingNumber:\s*undefined/)
assert.match(formSource, /refNo:\s*undefined/)

console.log('PASS: MES work order drawing number and REF.NO. sync UI contract')
