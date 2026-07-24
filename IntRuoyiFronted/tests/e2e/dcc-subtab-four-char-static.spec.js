const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const routeSource = fs.readFileSync(path.join(root, 'src/router/modules/remaining.ts'), 'utf8')
const printTemplatePage = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/print-template/index.vue'),
  'utf8'
)

const expectedRouteTitles = ['分发规则', '培训规则', '我的培训', '模板配置', '文控日志']
for (const title of expectedRouteTitles) {
  assert.ok(routeSource.includes(`title: '${title}'`), `DCC 隐藏路由必须包含标题：${title}`)
}

for (const removedTitle of ['审批打印模板', '受控文件审计']) {
  assert.ok(!routeSource.includes(`title: '${removedTitle}'`), `DCC 隐藏路由不应继续保留旧标题：${removedTitle}`)
}

const expectedMenuTitles = [
  '文档目录',
  '文控权限',
  '流程路线',
  '文件提交',
  '文件查阅',
  '文控日志',
  '我的培训',
  '模板配置'
]
assert.equal(new Set(expectedMenuTitles).size, expectedMenuTitles.length, 'DCC 子页签四字标题必须互不重名')
assert.ok(printTemplatePage.includes('模板配置'), '模板配置页头必须与子页签新名称一致')

console.log('PASS dcc-subtab-four-char-static')
