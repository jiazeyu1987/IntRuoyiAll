const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteEditPage.vue'),
  'utf8'
)

assert.ok(
  !source.includes('route-edit-page__header'),
  '编辑页顶部标题说明容器必须删除。'
)

assert.ok(
  !/<h3>\s*编辑工艺路线\s*<\/h3>/.test(source),
  '编辑页不得继续渲染“编辑工艺路线”标题。'
)

assert.ok(
  !source.includes('在页面页签中维护'),
  '编辑页不得继续渲染顶部说明文案。'
)

assert.match(
  source,
  /<RouteFormContent ref="contentRef" mode="page" @success="handleSaved" \/>/,
  '编辑页仍必须渲染工艺路线表单内容。'
)

assert.match(
  source,
  /<div class="route-edit-page__actions">[\s\S]*?@click="contentRef\?\.submitForm\(\)"[\s\S]*?保 存[\s\S]*?<\/div>/,
  '删除标题说明后仍必须保留保存操作。'
)

assert.ok(!source.includes('返回列表'), '编辑页底部不得继续渲染“返回列表”。')
assert.ok(!source.includes('handleBack'), '编辑页删除返回按钮后不得保留返回处理函数。')

console.log('mes-route-edit-header-removal-static PASS')
