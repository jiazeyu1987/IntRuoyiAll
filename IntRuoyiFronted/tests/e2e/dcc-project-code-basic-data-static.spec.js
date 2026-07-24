const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readWorkspaceSource = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const apiSource = readSource('src/api/dcc/controlledFile/projectCodes.ts')
const pageSource = readSource('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')
const pageEntrySource = readSource('src/views/dcc/controlled-file/basic-data/project-code/index.vue')
const dccSchema = readWorkspaceSource('ruoyi-vue-pro/sql/mysql/20260513_dcc_base_schema.sql')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.strictEqual(
  packageJson.scripts['e2e:dcc:project-code-basic-data:static'],
  'node tests/e2e/dcc-project-code-basic-data-static.spec.js',
  'package.json 必须提供 DCC 项目代码基础数据静态契约脚本'
)

for (const requiredToken of [
  "defineOptions({ name: 'DccProjectCodeBasicDataPage' })",
  '<ProjectCodeTabPanel />'
]) {
  assert.ok(pageEntrySource.includes(requiredToken), `DCC 项目代码独立页面必须包含 ${requiredToken}`)
}

for (const apiToken of [
  '/dcc/project-codes/page',
  '/dcc/project-codes/create',
  '/dcc/project-codes/update',
  '/dcc/project-codes/delete',
  '/dcc/project-codes/export-excel',
  '/dcc/project-codes/import-preview',
  '/dcc/project-codes/import-confirm',
  '/dcc/project-codes/import-template'
]) {
  assert.ok(apiSource.includes(apiToken), `DCC 项目代码 API 必须声明 ${apiToken}`)
}

const previewApi = extractBetween(
  apiSource,
  'export const importProjectCodePreview',
  'export const importProjectCodeConfirm'
)
assert.ok(
  previewApi.includes('request.upload<UploadCommonResult<DccProjectCodeImportPreviewRespVO>>'),
  '上传预览 API 必须声明 CommonResult 解包类型'
)
assert.ok(
  previewApi.includes('return result.data'),
  '上传预览 API 必须返回业务 data，不能把 CommonResult 直接赋给 previewResult'
)

for (const permissionToken of [
  `v-hasPermi="['dcc:project-code:create']"`,
  `v-hasPermi="['dcc:project-code:update']"`,
  `v-hasPermi="['dcc:project-code:delete']"`,
  `v-hasPermi="['dcc:project-code:import']"`,
  `v-hasPermi="['dcc:project-code:export']"`
]) {
  assert.ok(pageSource.includes(permissionToken), `DCC 基础数据页必须绑定按钮权限：${permissionToken}`)
}

assert.ok(pageSource.includes("path: '/mdm/project-code'"), '项目代码详情跳转必须指向新的全局基础数据子页面')

const standardList = extractBetween(pageSource, '<UnifiedListTemplate', '</UnifiedListTemplate>')
assert.ok(
  standardList.includes('table-key="dcc.projectCode.main"'),
  '项目代码主列表必须使用标准列表模板'
)
for (const [key, label, queryParamKey] of [
  ['docControlNo', '文控', 'keyword'],
  ['primaryCode', '主编码', 'keyword'],
  ['projectName', '项目名称', 'projectName'],
  ['projectCode', '项目代码', 'projectCode'],
  ['category', '类别', 'category']
]) {
  assert.ok(
    pageSource.includes(`key: '${key}'`) &&
      pageSource.includes(`label: '${label}'`) &&
      pageSource.includes(`queryParamKey: '${queryParamKey}'`),
    `快速过滤字段下拉必须展示项目代码列名：${label}`
  )
}
assert.ok(!pageSource.includes("label: '关键词'"), '项目代码快速过滤字段下拉不得显示泛化关键词')
assert.ok(
  !standardList.includes('<template #extra-filters>'),
  '项目代码列表不得继续渲染类别、优先级和状态附加筛选'
)
assert.ok(!standardList.includes('handleQuery'), '项目代码列表不得继续渲染重复查询按钮')
assert.ok(!standardList.includes('resetQuery'), '项目代码列表不得继续渲染重复重置按钮')
assert.ok(
  standardList.includes(':show-column-reset="false"'),
  '项目代码列表必须隐藏独立的重置列按钮'
)
assert.ok(standardList.includes('新增项目代码'), '筛选栏必须提供新增项目代码入口')

const dataTable = extractBetween(
  standardList,
  '<el-table',
  '</el-table>'
)
for (const column of ['文控', '主编码', '项目名称', '项目代码', '类别', '关联文件数', '更新时间', '关联文档']) {
  assert.ok(dataTable.includes(`label="${column}"`), `表格必须展示 ${column} 列`)
}
assert.ok(dataTable.includes('编辑'), '主列表必须提供编辑操作')
assert.ok(dataTable.includes('删除'), '主列表必须提供删除操作')
for (const removedColumn of ['委托生产', '项目负责人', '项目工程师', '状态', '存放位置', '优先级']) {
  assert.ok(!dataTable.includes(`label="${removedColumn}"`), `主列表不得继续展示 ${removedColumn} 列`)
}
assert.ok(
  dataTable.includes("isProjectCodeColumnVisible('primaryCode')") &&
    dataTable.includes('label="主编码"') &&
    dataTable.includes('prop="primaryCode"'),
  '主编码列必须存在并接入显示字段配置'
)
assert.ok(dataTable.includes('>无<'), '主编码列必须固定渲染 无')
assert.ok(
  dataTable.includes('prop="associatedFileCount"') &&
    dataTable.includes(`v-bind="sortColumnAttrs('associatedFileCount')"`) &&
    pageSource.includes("{ key: 'associatedFileCount', label: '关联文件数', width: 120 }"),
  '关联文件数列必须绑定 associatedFileCount 并通过标准列表模板默认排序'
)
assert.ok(pageSource.includes('@sort-change="handleSortChange"'), '项目代码表格必须监听排序变化')
assert.ok(apiSource.includes('fileCountSort?:'), '项目代码分页参数必须声明 fileCountSort')
assert.ok(
  pageSource.includes("queryParams.fileCountSort = sortOrder === 'ascending' ? 'asc' : 'desc'"),
  '前端必须把关联文件数升序/降序转换为后端 fileCountSort 参数'
)

const importDialog = extractBetween(pageSource, '<el-dialog', '</el-dialog>')
for (const importColumn of ['存放位置', '优先级']) {
  assert.ok(importDialog.includes(`label="${importColumn}"`), `导入预览必须保留 ${importColumn} 列`)
}

const detailDrawer = extractBetween(pageSource, '<el-drawer', '</el-drawer>')
for (const detailLabel of ['委托生产', '状态', '存放位置', '优先级']) {
  assert.ok(detailDrawer.includes(`label="${detailLabel}"`), `详情抽屉必须保留 ${detailLabel} 字段`)
}
for (const hiddenDetailLabel of ['项目组负责人', '项目工程师']) {
  assert.ok(
    !detailDrawer.includes(`label="${hiddenDetailLabel}"`),
    `详情抽屉不得继续展示 ${hiddenDetailLabel} 字段`
  )
}

const syncDetailBlock = extractBetween(
  pageSource,
  'const syncDetailFromRoute = async () => {',
  'const openProjectCodeDetail'
)
assert.ok(
  syncDetailBlock.includes('const requestToken = ++detailRequestSequence'),
  '详情加载必须使用请求序号隔离快速切换产生的旧请求'
)
assert.ok(
  syncDetailBlock.includes('void loadAssociatedFilesForDetail(id, requestToken)'),
  '详情基础信息就绪后必须异步触发关联文档加载，避免阻塞抽屉首屏'
)
assert.ok(
  !/await\s+getAssociatedFiles\s*\(/.test(syncDetailBlock),
  '详情打开流程不得 await 关联文档全量加载'
)
assert.ok(
  syncDetailBlock.indexOf('detailLoading.value = false') <
    syncDetailBlock.indexOf('void loadAssociatedFilesForDetail(id, requestToken)'),
  '详情基础信息 loading 必须在关联文档加载前结束'
)
assert.ok(
  dataTable.includes('@click="openProjectCodeDetail(row)"'),
  '列表点击详情必须把当前行数据传入详情打开函数，首屏无需再等详情接口返回'
)
const openDetailBlock = extractBetween(
  pageSource,
  'const openProjectCodeDetail = async',
  'const openControlledFileDetail'
)
assert.ok(
  openDetailBlock.includes('selectedProjectCode.value = projectCode'),
  '打开详情时必须先用当前行数据填充基础信息'
)

for (const importToken of [
  'data-testid="dcc-project-code-import-dialog"',
  'data-testid="dcc-project-code-import-summary"',
  'failureCount',
  ':disabled="!previewResult || previewResult.failureCount > 0"',
  'importRows',
  '失败原因'
]) {
  assert.ok(pageSource.includes(importToken), `导入预览必须包含 ${importToken}`)
}

assert.ok(!pageSource.includes("UNCHANGED: ''"), '不变动作的标签类型不得传空字符串给 el-tag')

for (const requiredManualToken of [
  "openForm('create'",
  "openForm('update'",
  'handleDelete',
  '新增项目代码',
  '编辑项目代码',
  '删除项目代码'
]) {
  assert.ok(pageSource.includes(requiredManualToken), `DCC 基础数据页必须提供手工维护入口：${requiredManualToken}`)
}

for (const dialogToken of [
  '项目代码维护',
  '文控',
  '项目名称',
  '项目代码',
  '类别',
  '委托生产',
  '项目组负责人',
  '项目工程师',
  '存放位置',
  '优先级',
  '启用状态',
  'submitForm',
  'formRules'
]) {
  assert.ok(pageSource.includes(dialogToken), `DCC 基础数据维护弹窗必须包含 ${dialogToken}`)
}

for (const schemaToken of [
  'DCC项目代码',
  '/mdm',
  'project-code',
  'dcc/controlled-file/basic-data/project-code/index',
  'dcc:project-code:query',
  'dcc:project-code:create',
  'dcc:project-code:update',
  'dcc:project-code:delete',
  'dcc:project-code:import',
  'dcc:project-code:export',
  "source_menu.`path` = 'controlled-file/categories'"
]) {
  assert.ok(dccSchema.includes(schemaToken), `DCC SQL 必须包含菜单/权限种子：${schemaToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(pageSource),
  'DCC 基础数据页不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC project-code basic-data static contract')
