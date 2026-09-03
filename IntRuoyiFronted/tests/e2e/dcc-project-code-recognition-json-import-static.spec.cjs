const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'),
  'utf8'
)
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/batchrecordreport/index.ts'), 'utf8')

assert.match(page, /data-testid="dcc-project-code-import-recognition-json"/)
assert.match(page, /accept="application\/json,\.json"/)
assert.match(page, /@click="submitRecognitionJsonImport"/)
assert.match(page, /BatchRecordReportApi\.importTotalRecognitionJson\(project\.id, file\)/)
assert.match(page, /message\.success\('批记录识别 JSON 已导入，设备参数已同步'\)/)
assert.match(api, /url: '\/mes\/pro\/batch-record-report\/import-total-recognition-json'/)
assert.match(api, /data\.append\('dccProjectCodeId', String\(dccProjectCodeId\)\)/)
assert.match(api, /data\.append\('file', file\)/)

console.log('PASS: DCC project code recognition JSON import static contract')
