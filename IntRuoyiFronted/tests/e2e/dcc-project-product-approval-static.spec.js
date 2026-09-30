const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const productCatalog = fs.readFileSync(
  path.join(
    root,
    'src',
    'views',
    'dcc',
    'controlled-file',
    'basic-data',
    'components',
    'ProductCatalogTabPanel.vue'
  ),
  'utf8'
)
const projectCode = fs.readFileSync(
  path.join(
    root,
    'src',
    'views',
    'dcc',
    'controlled-file',
    'basic-data',
    'components',
    'ProjectCodeTabPanel.vue'
  ),
  'utf8'
)
const api = fs.readFileSync(
  path.join(root, 'src', 'api', 'dcc', 'controlledFile', 'projectProductRequests.ts'),
  'utf8'
)

const requiredFields = ['项目名称', '项目代码', '项目负责人', '产品编码', '产品名称', '分类', '备注']
for (const field of requiredFields) {
  if (!productCatalog.includes(field)) {
    throw new Error(`联合新建界面缺少字段：${field}`)
  }
}

for (const marker of [
  'dcc-project-product-create-open',
  'createDccProjectProductRequest',
  'reviewDccProjectProductRequest',
  'approveDccProjectProductRequest',
  'retryDccProjectProductRequestWrite'
]) {
  if (!productCatalog.includes(marker) && !api.includes(marker)) {
    throw new Error(`联合新建审批契约缺少：${marker}`)
  }
}

if (productCatalog.includes('@click="openForm(\'create\')"') || productCatalog.includes('>新增产品目录<')) {
  throw new Error('产品目录页面仍保留直接新增产品目录入口')
}
if (projectCode.includes('@click="openForm(\'create\')"') || projectCode.includes('>新增项目代码<')) {
  throw new Error('项目代码页面仍保留直接新增项目代码入口')
}
for (const endpoint of [
  "const requestUrl = '/dcc/project-product-requests'",
  "`${requestUrl}/create`",
  "`${requestUrl}/pending`",
  "`${requestUrl}/${id}/review/${approve ? 'approve' : 'reject'}`",
  "`${requestUrl}/${id}/approve/${approve ? 'approve' : 'reject'}`",
  "`${requestUrl}/${id}/retry-write`"
]) {
  if (!api.includes(endpoint)) {
    throw new Error(`联合新建 API 缺少端点：${endpoint}`)
  }
}

console.log('DCC project-product approval static contract passed')
