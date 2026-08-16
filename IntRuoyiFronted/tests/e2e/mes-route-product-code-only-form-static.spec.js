const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const routeProductList = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteProductList.vue'),
  'utf8'
)

const assertIncludes = (source, expected, label) => {
  assert.ok(source.includes(expected), label)
}

const assertNotIncludes = (source, unexpected, label) => {
  assert.ok(!source.includes(unexpected), label)
}

const extractDialog = (source, titleBinding) => {
  const start = source.indexOf(titleBinding)
  assert.ok(start >= 0, 'missing dialog ' + titleBinding)
  const end = source.indexOf('</Dialog>', start)
  assert.ok(end > start, 'missing dialog close ' + titleBinding)
  return source.slice(start, end)
}

const formDialog = extractDialog(routeProductList, ':title="formTitle"')
const copyDialog = extractDialog(routeProductList, ':title="copyFormTitle"')
const savePayloadBlock = routeProductList.slice(
  routeProductList.indexOf('const buildRouteProductSavePayload'),
  routeProductList.indexOf('const buildRouteProductCopyPayload')
)
const copyPayloadBlock = routeProductList.slice(
  routeProductList.indexOf('const buildRouteProductCopyPayload'),
  routeProductList.indexOf('/** 添加/修改操作 */')
)

assertIncludes(formDialog, 'label="产品编号"', '关联产品弹窗必须只暴露产品编号输入')
assertIncludes(formDialog, 'prop="productCode"', '关联产品弹窗必须用产品编号作为可见校验字段')
assertIncludes(formDialog, 'v-model="formData.productCode"', '关联产品弹窗必须直接输入产品编号')
assertIncludes(
  routeProductList,
  'resolveRouteProductCodeForSubmit()',
  '保存前必须把产品编号解析为正式物料编号'
)

for (const label of ['生产数量', '生产用时', '时间单位', '备注']) {
  assertNotIncludes(formDialog, label, '关联产品弹窗不得继续显示' + label)
  assertNotIncludes(copyDialog, label, '复制产品弹窗不得继续显示' + label)
}

for (const field of ['quantity', 'productionTime', 'timeUnitType', 'remark']) {
  assertNotIncludes(savePayloadBlock, field, '关联产品保存载荷不得继续提交' + field)
  assertNotIncludes(copyPayloadBlock, field, '复制产品保存载荷不得继续提交' + field)
}

assertIncludes(copyDialog, 'label="目标产品编号"', '复制产品弹窗必须只录入目标产品编号')
assertIncludes(copyDialog, 'prop="targetProductCode"', '复制产品弹窗必须校验目标产品编号')
assertIncludes(
  routeProductList,
  'resolveCopyTargetProductCodeForSubmit()',
  '复制保存前必须把目标产品编号解析为正式物料编号'
)
assertNotIncludes(
  routeProductList,
  'quantity: 1,',
  '新增关联产品不得再写入生产数量默认值'
)
assertNotIncludes(
  routeProductList,
  'productionTime: 1,',
  '新增关联产品不得再写入生产用时默认值'
)
assertNotIncludes(
  routeProductList,
  "timeUnitType: 'MINUTE'",
  '新增关联产品不得再写入时间单位默认值'
)

console.log('mes-route-product-code-only-form-static PASS')
