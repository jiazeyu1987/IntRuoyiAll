const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = process.cwd()
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8').replace(/\r\n/g, '\n')
const exists = (file) => fs.existsSync(path.join(root, file))

const itemForm = read('src/views/mes/md/item/MdItemForm.vue')
const routeProductApi = read('src/api/mes/pro/route/product/index.ts')
const routeApi = read('src/api/mes/pro/route/index.ts')
const mdItemApi = read('src/api/mes/md/item/index.ts')

assert(
  exists('src/views/mes/md/item/MdItemRouteForm.vue'),
  'MES 物料产品表单必须提供独立 MdItemRouteForm 维护产品侧工艺路线绑定。'
)

const itemRouteForm = read('src/views/mes/md/item/MdItemRouteForm.vue')

assert.match(
  itemForm,
  /import MdItemRouteForm from '\.\/MdItemRouteForm\.vue'/,
  'MdItemForm 必须引入产品侧工艺路线绑定组件。'
)
assert.match(
  itemForm,
  /<el-tab-pane\s+label="工艺路线"\s+name="route"\s+lazy\s+v-if="isProductItem">[\s\S]*?<MdItemRouteForm\s+:itemId="formData\.id!"\s+:formType="formType"\s*\/>/,
  'MES 物料产品编辑/详情必须在产品项下展示工艺路线页签并传入 itemId/formType。'
)
assert.match(
  itemForm,
  /const isProductItem = computed\(\(\) => currentItemOrProduct\.value === MesItemOrProductEnum\.PRODUCT\.value\)/,
  '工艺路线页签只能按 MES 物料产品的 PRODUCT 标识显示，不能混入 MDM 产品主数据。'
)

for (const token of [
  'ProRouteProductApi.getRouteProductByItem',
  'ProRouteProductApi.saveRouteProductByItem',
  'ProRouteApi.getRouteItemBindingList',
  'CommonStatusEnum.ENABLE',
  'persistedRouteId',
  '当前工艺路线已启用，不能在产品侧变更或解除',
  'v-model="routeId"',
  'label="工艺路线"',
  'clearable',
  '保存工艺路线',
  '保存工艺路线'
]) {
  assert(itemRouteForm.includes(token), `产品侧工艺路线表单缺少正式契约片段：${token}`)
}

assert(
  !itemRouteForm.includes(':disabled="isRouteOptionDisabled(route)"'),
  '未绑定产品必须可以选择已启用路线新增绑定。'
)
assert.match(
  itemRouteForm,
  /isCurrentRouteLocked[\s\S]*persistedRouteId[\s\S]*CommonStatusEnum\.ENABLE/,
  '只有已持久化绑定到启用路线的产品才保持变更/解除锁定。'
)

assert.match(
  routeProductApi,
  /getRouteProductByItem:\s*async \(itemId: number\)[\s\S]*\/mes\/pro\/route-product\/get-by-item/,
  'route-product API wrapper 必须提供按 itemId 查询当前绑定。'
)
assert.match(
  routeProductApi,
  /saveRouteProductByItem:\s*async \(data: ProRouteProductByItemSaveReqVO\)[\s\S]*\/mes\/pro\/route-product\/save-by-item/,
  'route-product API wrapper 必须提供按 itemId 保存/解除当前绑定。'
)
assert.match(
  routeApi,
  /getRouteItemBindingList:\s*async \(\)[\s\S]*\/mes\/pro\/route\/item-binding-list/,
  '产品侧工艺路线下拉必须使用产品维护权限下的专用路线选择接口。'
)
assert(
  !itemRouteForm.includes('getRouteSimpleList'),
  '产品侧工艺路线下拉必须继续使用包含状态的 item-binding-list。'
)

assert(
  !/routeId|工艺路线/.test(mdItemApi),
  'MES 物料产品主 API 不得新增第二套路由字段或关系源，必须复用 route-product API。'
)

for (const forbidden of ['formBindings', '表单槽位', '批记录表单', '工序开始']) {
  assert(
    !itemRouteForm.includes(forbidden),
    `产品选择工艺路线不得混入工艺路线三类配置的其它链路：${forbidden}`
  )
}

console.log('mes-md-item-route-selection-static PASS')
