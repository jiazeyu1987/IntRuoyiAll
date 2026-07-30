const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const remainingRouter = read('src/router/modules/remaining.ts')
const routeIndex = read('src/views/mes/pro/route/index.vue')
const routeEditPage = read('src/views/mes/pro/route/RouteEditPage.vue')
const routeFormContent = read('src/views/mes/pro/route/RouteFormContent.vue')

const assertMatch = (content, pattern, label) => {
  if (!pattern.test(content)) {
    throw new Error(`${label} missing`)
  }
}

const assertNotMatch = (content, pattern, label) => {
  if (pattern.test(content)) {
    throw new Error(`${label} must be removed`)
  }
}

const editRouteMatch = /path: 'pro\/route\/edit\/:id'[\s\S]*?meta: \{[\s\S]*?\n\s*\}/.exec(
  remainingRouter
)
if (!editRouteMatch) {
  throw new Error('工艺流程深链路由必须保留，避免排产工单和 eDHR 入口 404')
}
const editRouteMeta = editRouteMatch[0]

assertMatch(editRouteMeta, /noTagsView:\s*true/, '编辑工艺路线前端页签必须从 tags view 移除')
assertNotMatch(editRouteMeta, /title:\s*'编辑工艺路线'/, '隐藏路由标题不得继续暴露“编辑工艺路线”')
assertMatch(editRouteMeta, /title:\s*'工艺流程'/, '隐藏路由标题必须归并到工艺流程语义')

assertMatch(
  routeIndex,
  /const openEditPage = \(id\?: number, tab\?: RouteEditTab\) => \{[\s\S]*const targetTab = tab \?\? 'flow'[\s\S]*query: \{ tab: targetTab \}/,
  '工艺流程列表编辑入口必须显式默认进入流转关系图'
)

assertMatch(
  routeEditPage,
  /<RouteFormContent[\s\S]*mode="page"[\s\S]*:basic-readonly="true"/,
  '工艺流程深链页面必须把基础信息设为只读'
)
assertMatch(
  routeEditPage,
  /!\['flow', 'basic', 'mesProcess', 'product'\]\.includes\(activeRouteTab\)/,
  '基础信息和 MES 工序页签不得继续显示页面级保存按钮'
)

assertMatch(
  routeFormContent,
  /basicReadonly\?: boolean[\s\S]*basicReadonly: false/,
  'RouteFormContent 必须提供基础信息只读入参，避免影响新增弹窗'
)
assertMatch(
  routeFormContent,
  /const basicReadonly = computed\(\(\) => props\.basicReadonly\)[\s\S]*const isHeaderReadonly = computed\(\(\) => basicReadonly\.value \|\| \['enable', 'detail'\]\.includes\(formType\.value\)\)/,
  '基础信息只读必须由页面入参驱动，并保留启用/详情只读规则'
)
assertMatch(
  routeFormContent,
  /<el-button :disabled="isHeaderReadonly" @click="generateCode">/,
  '编码生成按钮必须随基础信息只读一起禁用'
)

console.log('mes-route-flow-entry-readonly-static PASS')
