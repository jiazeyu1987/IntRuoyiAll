const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routerHelper = readSource('src/utils/routerHelper.ts')
const tagsViewStore = readSource('src/store/modules/tagsView.ts')
const workOrderPage = readSource('src/views/mes/pro/workorder/index.vue')

assert.match(
  workOrderPage,
  /defineOptions\(\{\s*name:\s*'MesProWorkOrder'\s*\}\)/,
  '生产工单页面必须保留稳定组件名称，避免缓存身份漂移。'
)

assert.match(
  tagsViewStore,
  /view\.meta\?\.tagsViewKeyMode\s*===\s*'path'/,
  'TagsView 必须支持按 path 作为标签身份，避免同一路由不同 query 打开多个顶栏页签。'
)

assert.match(
  routerHelper,
  /MES_PRO_WORK_ORDER_ROUTE_COMPONENTS\s*=\s*new Set\(\[[\s\S]*'mes\/pro\/workorder\/index'[\s\S]*'mes\/pro\/workorder'[\s\S]*\]\)/,
  '动态路由覆盖必须声明生产工单组件路径。'
)

assert.match(
  routerHelper,
  /MES_PRO_WORK_ORDER_ROUTE_PATHS\s*=\s*new Set\(\[[\s\S]*'mes\/pro\/workorder'[\s\S]*'pro\/workorder'[\s\S]*'mes\/pro\/work-order'[\s\S]*'pro\/work-order'[\s\S]*\]\)/,
  '动态路由覆盖必须声明生产工单菜单路径，兼容 workorder 与 work-order 两种历史路径。'
)

assert.match(
  routerHelper,
  /MES_PRO_WORK_ORDER_ROUTE_PATHS\.has\(routePath\)[\s\S]*MES_PRO_WORK_ORDER_ROUTE_COMPONENTS\.has\(componentPath\)[\s\S]*meta\.tagsViewKeyMode\s*=\s*'path'/,
  '生产工单动态菜单路由必须设置 tagsViewKeyMode=path，使 query-only 变化不再新增 生产工单(2)。'
)

const workOrderOverrideStart = routerHelper.indexOf('MES_PRO_WORK_ORDER_ROUTE_COMPONENTS')
const workOrderOverrideSnippet = routerHelper.slice(
  Math.max(0, workOrderOverrideStart),
  workOrderOverrideStart + 1200
)

assert.doesNotMatch(
  workOrderOverrideSnippet,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '生产工单顶栏页签去重不得引入 mock、placeholder、fallback、降级或吞异常。'
)

process.stdout.write('PASS: production work order single tags view static contract\n')
