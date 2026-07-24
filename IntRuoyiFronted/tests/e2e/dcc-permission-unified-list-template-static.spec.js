const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractTemplate = (source, tableKey, label) => {
  const pattern = new RegExp(
    `<UnifiedListTemplate[\\s\\S]*?table-key="${tableKey.replaceAll('.', '\\.')}"[\\s\\S]*?<\\/UnifiedListTemplate>`
  )
  const match = source.match(pattern)
  assert.ok(match, `${label} 必须使用稳定 tableKey 接入标准列表模板。`)
  return match[0]
}

const assertStandardList = ({
  source,
  label,
  tableKey,
  quickFilterName,
  columnsName,
  savingName,
  changeHandler,
  headerDragHandler,
  tableDataName,
  tableRowKey
}) => {
  assert.match(
    source,
    /import UnifiedListTemplate from '@\/components\/UnifiedListTemplate\/index\.vue'/,
    `${label} 必须导入标准列表模板。`
  )
  assert.match(
    source,
    /import \{ useUserTableColumns, type UserTableColumnDefinition \} from '@\/hooks\/web\/useUserTableColumns'/,
    `${label} 必须接入显示字段和列宽持久化 hook。`
  )
  assert.match(
    source,
    /useTableQuickFilter,[\s\S]*type TableQuickFilterDefinition[\s\S]*from '@\/hooks\/web\/useTableQuickFilter'/,
    `${label} 必须接入标准快速过滤 hook。`
  )

  const template = extractTemplate(source, tableKey, label)
  assert.match(template, new RegExp(`:filter-definitions="${quickFilterName}"`), `${label} 必须提供快速过滤字段定义。`)
  assert.match(template, new RegExp(`:columns="${columnsName}"`), `${label} 必须提供显示字段配置。`)
  assert.match(template, new RegExp(`:column-saving="${savingName}"`), `${label} 必须绑定列配置保存状态。`)
  assert.match(template, /@quick-filter-query="[^"]+\.applyQuickFilter"/, `${label} 快速过滤必须由标准 hook 执行。`)
  assert.match(template, new RegExp(`@column-change="${changeHandler}"`), `${label} 显示字段变更必须保存。`)
  assert.match(template, /@pagination="[^"]+"/, `${label} 分页必须由标准列表模板触发。`)
  assert.match(
    template,
    new RegExp(
      `<template\\s+#table(?:="[^"]*")?>[\\s\\S]*?<el-table[\\s\\S]*?data-user-table-column-explicit[\\s\\S]*?data-user-table-key="${tableKey.replaceAll('.', '\\.')}"[\\s\\S]*?:data="${tableDataName}"[\\s\\S]*?row-key="${tableRowKey}"[\\s\\S]*?@header-dragend="${headerDragHandler}"`
    ),
    `${label} 表格必须接入列宽拖拽持久化并保留行标识。`
  )
  assert.doesNotMatch(
    template,
    /<el-form[\s\S]*?class="[^"]*(toolbar|__form)[^"]*"/,
    `${label} 不得继续在标准模板内渲染旧查询表单。`
  )
}

const packageJson = JSON.parse(readSource('package.json'))
const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const reviewMatrixPage = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue'
)
const viewMatrixPage = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'
)
const directoryAuthorizationPage = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)

assert.equal(
  packageJson.scripts?.['e2e:dcc:permission-unified-list:static'],
  'node tests/e2e/dcc-permission-unified-list-template-static.spec.js',
  'package.json 必须暴露文控权限标准列表模板静态合同'
)

assertStandardList({
  source: categoriesPage,
  label: '类别列表页签',
  tableKey: 'dcc.controlledFile.permission.categories',
  quickFilterName: 'categoryQuickFilterDefinitions',
  columnsName: 'categoryColumns',
  savingName: 'categoryColumnSaving',
  changeHandler: 'saveCategoryColumnConfig',
  headerDragHandler: 'handleCategoryHeaderDragend',
  tableDataName: 'paginatedCategories',
  tableRowKey: 'id'
})

assertStandardList({
  source: reviewMatrixPage,
  label: '审阅矩阵页签',
  tableKey: 'dcc.controlledFile.permission.reviewMatrix',
  quickFilterName: 'reviewMatrixQuickFilterDefinitions',
  columnsName: 'reviewMatrixColumns',
  savingName: 'reviewMatrixColumnSaving',
  changeHandler: 'saveReviewMatrixColumnConfig',
  headerDragHandler: 'handleReviewMatrixHeaderDragend',
  tableDataName: 'paginatedReviewMatrixRows',
  tableRowKey: 'categoryId'
})

assertStandardList({
  source: viewMatrixPage,
  label: '查看矩阵页签',
  tableKey: 'dcc.controlledFile.permission.viewMatrix',
  quickFilterName: 'viewMatrixQuickFilterDefinitions',
  columnsName: 'viewMatrixColumns',
  savingName: 'viewMatrixColumnSaving',
  changeHandler: 'saveViewMatrixColumnConfig',
  headerDragHandler: 'handleViewMatrixHeaderDragend',
  tableDataName: 'paginatedViewMatrixRows',
  tableRowKey: 'categoryId'
})

assertStandardList({
  source: directoryAuthorizationPage,
  label: '目录授权页签',
  tableKey: 'dcc.controlledFile.permission.directoryAuthorization',
  quickFilterName: 'directoryAuthorizationQuickFilterDefinitions',
  columnsName: 'directoryAuthorizationColumns',
  savingName: 'directoryAuthorizationColumnSaving',
  changeHandler: 'saveDirectoryAuthorizationColumnConfig',
  headerDragHandler: 'handleDirectoryAuthorizationHeaderDragend',
  tableDataName: 'paginatedDirectoryAuthorizationRules',
  tableRowKey: 'subjectId'
})

assert.match(categoriesPage, /<template #actions>[\s\S]*新增类别/, '类别列表必须保留新增类别入口。')
assert.match(categoriesPage, /<template #default="\{ row \}">[\s\S]*上传策略[\s\S]*编辑[\s\S]*删除/, '类别列表必须保留行级业务按钮。')
assert.match(reviewMatrixPage, /<template #actions>[\s\S]*按人反查/, '审阅矩阵必须保留反查入口。')
assert.match(viewMatrixPage, /<template #actions>[\s\S]*按人反查/, '查看矩阵必须保留反查入口。')
assert.match(directoryAuthorizationPage, /<template #actions>[\s\S]*新增目录[\s\S]*新增规则[\s\S]*保存规则/, '目录授权必须保留规则维护入口。')

assert.doesNotMatch(
  `${categoriesPage}\n${reviewMatrixPage}\n${viewMatrixPage}\n${directoryAuthorizationPage}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '文控权限四页签标准列表替换不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: DCC permission tabs use unified list template')
