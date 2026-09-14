const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const routeForm = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/route/RouteForm.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

assert.match(
  routeForm,
  /const waitForContentRef = async \(\)[\s\S]*contentRef\.value\?\.open[\s\S]*return contentRef\.value/,
  'RouteForm must wait until the async RouteFormContent ref exposes open() before opening detail/create forms.'
)
assert.match(
  routeForm,
  /throw new Error\('打开工艺路线表单失败：表单内容未加载'\)/,
  'RouteForm must fail fast if the async content component never becomes available.'
)
assert.doesNotMatch(
  routeForm,
  /contentRef\.value\?\.open\(type,\s*id\)/,
  'RouteForm must not optionally skip contentRef.open(type, id), because that opens a blank detail dialog without loading route data.'
)
assert.match(
  routeForm,
  /const content = await waitForContentRef\(\)[\s\S]*await content\.open\(type,\s*id\)/,
  'RouteForm.open must call the resolved content ref with the original type and id.'
)

console.log('PASS: MES route form async open static contract')
