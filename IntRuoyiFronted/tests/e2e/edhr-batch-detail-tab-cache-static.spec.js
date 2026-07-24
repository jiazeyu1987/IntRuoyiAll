import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const repoRoot = process.cwd()
const routerSource = fs.readFileSync(
  path.join(repoRoot, 'src', 'router', 'modules', 'remaining.ts'),
  'utf8'
)
const detailSource = fs.readFileSync(
  path.join(
    repoRoot,
    'src',
    'views',
    'mes',
    'pro',
    'edhr-batch',
    'BatchExecutionDetailPage.vue'
  ),
  'utf8'
)

const routeStart = routerSource.indexOf(
  "path: 'pro/feedback/edhr-batch-execution/detail'"
)
const routeEnd = routerSource.indexOf(
  "path: 'pro/feedback/edhr-batch-execution/review'",
  routeStart
)

assert(routeStart >= 0 && routeEnd > routeStart, '必须能定位 eDHR 批次详情路由。')

const detailRoute = routerSource.slice(routeStart, routeEnd)

assert.match(detailRoute, /name:\s*'MesProEdhrBatchExecutionDetail'/)
assert.match(
  detailRoute,
  /noCache:\s*false/,
  '批次详情必须加入 keep-alive 缓存，标签切走再返回时不得重新创建页面。'
)
assert.doesNotMatch(detailRoute, /noCache:\s*true/)

assert.match(
  detailSource,
  /defineOptions\(\{\s*name:\s*'MesProEdhrBatchExecutionDetail'\s*\}\)/,
  '页面组件名称必须与路由名称一致，确保 keep-alive include 能命中。'
)
const routeWatch = detailSource.match(
  /watch\(\s*\(\)\s*=>\s*\[route\.name,\s*route\.query\.id\]\s*as const,[\s\S]*?\n\)/
)?.[0]
assert(routeWatch, '必须同时监听当前路由名称与批次标识。')
assert(
  routeWatch.includes("routeName !== 'MesProEdhrBatchExecutionDetail'"),
  '离开批次详情路由后不得触发详情加载。'
)
assert(
  routeWatch.includes('nextBatchExecutionId === Number(detail.value?.id)'),
  '返回同一批次详情时必须复用已经加载的数据。'
)
assert(routeWatch.includes('loadDetail()'), '切换到不同批次标识时必须继续加载新批次数据。')

console.log('PASS: eDHR batch detail keeps its instance across tag switches.')
