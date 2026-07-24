const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const productListSource = readText('src/views/mes/pro/route/RouteProductList.vue')
const productApiSource = readText('src/api/mes/pro/route/product/index.ts')

assert.match(
  productApiSource,
  /export interface ProRouteProductBindFromWorkOrdersReqVO \{[\s\S]*routeId: number[\s\S]*\}/,
  'API 类型必须定义按生产订单补齐产品的请求体。'
)
assert.match(
  productApiSource,
  /export interface ProRouteProductBindFromWorkOrdersRespVO \{[\s\S]*routeId: number[\s\S]*routeName: string[\s\S]*matchedCount: number[\s\S]*existingCount: number[\s\S]*createdCount: number[\s\S]*conflictCount: number[\s\S]*itemCodes: string\[\][\s\S]*creatableItemCodes: string\[\][\s\S]*existingItemCodes: string\[\][\s\S]*conflictItemCodes: string\[\][\s\S]*\}/,
  'API 类型必须定义补齐预览和结果响应字段。'
)
assert.match(
  productApiSource,
  /previewBindFromWorkOrders: async \(data: ProRouteProductBindFromWorkOrdersReqVO\) => \{[\s\S]*request\.post\(\{ url: `\/mes\/pro\/route-product\/preview-bind-from-work-orders`, data \}\)/,
  'API 必须调用后端补齐预览接口，且使用 POST body。'
)
assert.match(
  productApiSource,
  /bindFromWorkOrders: async \(data: ProRouteProductBindFromWorkOrdersReqVO\) => \{[\s\S]*request\.post\(\{ url: `\/mes\/pro\/route-product\/bind-from-work-orders`, data \}\)/,
  'API 必须调用后端批量补齐接口，且使用 POST body。'
)

assert.match(
  productListSource,
  /const bindFromWorkOrdersLoading = ref\(false\)/,
  '页面必须维护按钮 loading 状态。'
)
assert.match(
  productListSource,
  /const canBindFromWorkOrders = computed\([\s\S]*\['detail', 'update'\]\.includes\(props\.formType\) && !!props\.routeId[\s\S]*\)/,
  '按钮必须只在详情/编辑态且有 routeId 时展示。'
)
assert.match(
  productListSource,
  /<el-row v-if="canBindFromWorkOrders" class="mt-12px">[\s\S]*从生产订单补齐产品[\s\S]*<\/el-row>/,
  '补齐按钮必须位于表格下方左侧，而不是挤在顶部关联产品按钮旁。'
)
assert.match(
  productListSource,
  /<Icon icon="ep:connection" class="mr-5px" \/> 从生产订单补齐产品/,
  '按钮必须使用清晰的连接语义图标与指定文案。'
)
assert.match(
  productListSource,
  /const preview = await ProRouteProductApi\.previewBindFromWorkOrders\(\{ routeId: props\.routeId \}\)/,
  '关联产品页签按钮必须先调用预览接口。'
)
assert.match(
  productListSource,
  /await confirmBindFromWorkOrdersPreview\(preview\)/,
  '关联产品页签按钮必须先展示预览确认。'
)
assert.match(
  productListSource,
  /await ProRouteProductApi\.bindFromWorkOrders\(\{ routeId: props\.routeId \}\)/,
  '按钮必须调用 bindFromWorkOrders 并传入当前 routeId。'
)
assert.match(
  productListSource,
  /message\.success\([\s\S]*`补齐完成：新增 \$\{result\.createdCount\} 个，跳过 \$\{result\.existingCount\} 个，冲突 \$\{result\.conflictCount\} 个`[\s\S]*\)/,
  '成功提示必须展示新增、跳过和冲突数量。'
)
assert.match(
  productListSource,
  /await getList\(\)/,
  '补齐成功后必须刷新关联产品列表。'
)
assert.doesNotMatch(
  productListSource.slice(productListSource.indexOf('const handleBindFromWorkOrders')),
  /catch\s*\([^)]*\)\s*\{\s*\}/,
  '补齐按钮不得空 catch 吞掉后端错误。'
)

const routeListSource = readText('src/views/mes/pro/route/index.vue')

assert.match(
  routeListSource,
  /import \{ ProRouteProductApi, type ProRouteProductBindFromWorkOrdersRespVO \} from '@\/api\/mes\/pro\/route\/product'/,
  '工艺路线列表必须引入关联产品补齐 API。'
)
assert.match(
  routeListSource,
  /<el-button[\s\S]*@click="handleBindRouteProducts\(scope\.row\)"[\s\S]*>[\s\S]*产品[\s\S]*<\/el-button>[\s\S]*编辑/,
  '工艺路线列表操作区必须在编辑前提供“产品”按钮。'
)
assert.match(
  routeListSource,
  /const routeProductBindLoadingId = ref<number \| undefined>\(\)/,
  '工艺路线列表必须维护行级产品补齐 loading。'
)
assert.match(
  routeListSource,
  /const handleBindRouteProducts = async \(row: ProRouteVO\) => \{[\s\S]*previewBindFromWorkOrders\(\{ routeId: row\.id \}\)[\s\S]*confirmRouteProductBindPreview\(preview\)[\s\S]*bindFromWorkOrders\(\{ routeId: row\.id \}\)[\s\S]*await getList\(\)[\s\S]*\}/,
  '工艺路线列表“产品”按钮必须先预览、再确认写入并刷新列表。'
)

console.log('PASS: mes pro route product bind from work orders static contract')
