const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = path.resolve(__dirname, '../..')
const viewSource = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/batchrecordformlist/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(root, 'src/api/mes/pro/batchrecordreport/index.ts'),
  'utf8'
)

test('Word 导入产品下拉框以 DCC 项目记录 ID 作为选中值', () => {
  assert.match(viewSource, /v-model="wordImportDialog\.selectedDccProjectCodeId"/)
  assert.match(viewSource, /:value="item\.id"/)
  assert.doesNotMatch(viewSource, /:value="item\.projectName"/)
})

test('Word 导入预检和提交都传递 DCC 项目记录 ID', () => {
  assert.match(apiSource, /data\.append\('dccProjectCodeId', String\(dccProjectCodeId\)\)/)
  assert.match(apiSource, /params\.append\('dccProjectCodeId', String\(dccProjectCodeId\)\)/)
  assert.match(viewSource, /preflightUploadedRoute\([\s\S]*selectedDccProjectCodeId/)
  assert.match(viewSource, /recognizeUploadedRoute\([\s\S]*selectedDccProjectCodeId/)
})

test('Word 导入按正式 DCC 路线绑定提示新建或升版', () => {
  assert.match(viewSource, /尚未绑定工艺路线，确认后将新建路线并写入正式绑定/)
  assert.match(viewSource, /所选 DCC 项目代码已正式绑定工艺路线/)
  assert.match(viewSource, /所选 DCC 项目代码存在多条正式路线绑定/)
  assert.doesNotMatch(viewSource, /存在多条同名工艺路线/)
})
