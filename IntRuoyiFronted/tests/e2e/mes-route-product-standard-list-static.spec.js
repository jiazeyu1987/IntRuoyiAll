const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeProductList = read('src/views/mes/pro/route/RouteProductList.vue')
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeForm = read('src/views/mes/pro/route/RouteForm.vue')

const extractSlot = (source, slotName) => {
  const pattern = new RegExp(`<template\\s+#${slotName}\\b[^>]*>([\\s\\S]*?)<\\/template>`)
  const match = source.match(pattern)
  assert.ok(match, `missing #${slotName} slot`)
  return match[1]
}

const assertIncludes = (source, expected, label) => {
  assert.ok(source.includes(expected), `${label}: expected ${JSON.stringify(expected)}`)
}

const assertNotIncludes = (source, unexpected, label) => {
  assert.ok(!source.includes(unexpected), `${label}: must not include ${JSON.stringify(unexpected)}`)
}

assertIncludes(
  routeProductList,
  "import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'",
  '关联产品列表必须导入标准列表模板'
)
assertIncludes(
  routeProductList,
  "from '@/hooks/web/useUserTableColumns'",
  '关联产品列表必须接入显示字段和列宽持久化'
)
assertIncludes(
  routeProductList,
  "from '@/hooks/web/useTableQuickFilter'",
  '关联产品列表必须接入标准快速过滤'
)
assertIncludes(routeProductList, '<UnifiedListTemplate', '关联产品列表必须使用标准列表模板')
assertIncludes(
  routeProductList,
  'table-key="mes.pro.route.product"',
  '关联产品列表必须使用稳定 tableKey'
)
assertIncludes(
  routeProductList,
  ':columns="routeProductColumns"',
  '关联产品列表必须把显示字段状态交给模板'
)
assertIncludes(
  routeProductList,
  '@column-change="saveRouteProductColumnConfig"',
  '显示字段变更必须保存'
)
assertIncludes(
  routeProductList,
  '@header-dragend="handleRouteProductHeaderDragend"',
  '关联产品列宽拖拽必须持久化'
)
assertIncludes(
  routeProductList,
  'data-user-table-key="mes.pro.route.product"',
  '关联产品表格必须声明列配置 tableKey'
)

const actionsSlot = extractSlot(routeProductList, 'actions')
for (const label of ['关联产品', '补齐产品', '保存']) {
  assertIncludes(actionsSlot, label, `工具栏必须显示 ${label} 按钮`)
  assert.ok(label.length <= 4, `${label} 文案不得超过 4 个字`)
}
assertIncludes(actionsSlot, "openForm('create')", '关联产品按钮必须保持原新增逻辑')
assertIncludes(actionsSlot, 'handleBindFromWorkOrders', '补齐产品按钮必须保持原补齐逻辑')
assertIncludes(actionsSlot, 'request-submit', '保存按钮必须请求父级执行现有保存逻辑')
assertNotIncludes(actionsSlot, '从生产订单补齐产品', '补齐按钮文案必须缩短到 4 字以内')
assertNotIncludes(routeProductList, 'class="mt-12px"', '补齐按钮不得继续放在列表下方')

const tableSlot = extractSlot(routeProductList, 'table')
assertIncludes(tableSlot, ':data="pagedRouteProductList"', '表格必须展示标准模板分页后的列表数据')
assertIncludes(tableSlot, '@sort-change="handleTemplateSortChange"', '表格排序必须交给标准模板')
for (const key of [
  'itemCode',
  'itemName',
  'specification',
  'unitName',
  'quantity',
  'productionTime',
  'remark'
]) {
  assertIncludes(
    routeProductList,
    `v-if="isRouteProductColumnVisible('${key}')"`,
    `关联产品列 ${key} 必须受显示字段控制`
  )
  assertIncludes(
    routeProductList,
    `v-bind="sortColumnAttrs('${key}')"`,
    `关联产品列 ${key} 必须接入标准排序属性`
  )
}
assertIncludes(
  routeProductList,
  "v-if=\"isEditable && isRouteProductColumnVisible('operation')\"",
  '操作列必须继续只在编辑态显示并受固定列配置控制'
)

assertIncludes(
  routeFormContent,
  '@request-submit="submitForm"',
  '关联产品工具栏保存必须复用父级 submitForm'
)
assertIncludes(
  routeFormContent,
  ':submitting="formLoading"',
  '关联产品保存按钮必须沿用父级加载态'
)
assertIncludes(
  routeFormContent,
  "const isProductTabActive = computed(() => activeTab.value === 'product')",
  '父级必须暴露当前是否为关联产品页签'
)
assert.match(
  routeFormContent,
  /const shouldSaveFlowGraphOnSubmit = \(\) =>[\s\S]*activeTab\.value === 'flow'/,
  '父级保存只有在流转关系图页签才要求流转图组件'
)
assertIncludes(
  routeFormContent,
  'isProductTabActive,',
  '父级必须向外暴露关联产品页签状态'
)
assertIncludes(
  routeEditPage,
  "activeRouteTab !== 'product'",
  '编辑页底部旧保存按钮必须在关联产品页签隐藏'
)
assertIncludes(
  routeForm,
  '!contentRef?.isProductTabActive',
  '弹窗页脚旧保存按钮必须在关联产品页签隐藏'
)

console.log('mes-route-product-standard-list-static PASS')
