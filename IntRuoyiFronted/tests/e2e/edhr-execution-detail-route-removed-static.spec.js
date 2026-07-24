const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const srcRoot = path.join(repoRoot, 'src')
const routePath = path.join(srcRoot, 'router', 'modules', 'remaining.ts')
const executionPagePath = path.join(srcRoot, 'views', 'mes', 'pro', 'edhr', 'ExecutionPage.vue')
const routes = fs.readFileSync(routePath, 'utf8')
const executionPage = fs.readFileSync(executionPagePath, 'utf8')

const collectSourceFiles = (directory) =>
  fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const entryPath = path.join(directory, entry.name)
    if (entry.isDirectory()) return collectSourceFiles(entryPath)
    return /\.(ts|vue)$/.test(entry.name) ? [entryPath] : []
  })

const detailRoute = '/mes/pro/feedback/edhr-execution/detail'
const detailRouteReferences = collectSourceFiles(srcRoot)
  .filter((filePath) => fs.readFileSync(filePath, 'utf8').includes(detailRoute))
  .map((filePath) => path.relative(repoRoot, filePath))

assert(!routes.includes("path: 'pro/feedback/edhr-execution/detail'"), '路由表不得继续注册 eDHR 执行详情页。')
assert(!routes.includes("name: 'MesProFeedbackEdhrExecutionDetail'"), '路由表不得继续保留执行详情路由名称。')
assert.deepStrictEqual(detailRouteReferences, [], `源码不得继续引用废弃详情路由：${detailRouteReferences.join(', ')}`)

for (const obsoleteToken of [
  'const isExecutionFormPage = computed(',
  "'eDHR 执行详情'",
  '>执行摘要<',
  '>技术证据<',
  '>最终归档<',
  '<ExecutionRenderer',
  'class="edhr-page-shell__audit-tabs"'
]) {
  assert(!executionPage.includes(obsoleteToken), `执行表单组件不得继续保留详情页内容：${obsoleteToken}`)
}

assert(executionPage.includes("return `${reportName}填写`"), '执行表单必须继续使用当前报表名称加“填写”的标题。')
assert(
  executionPage.includes("'填写当前工序表单，保存字段变更后提交执行'"),
  '执行表单必须继续保留填写与提交说明。'
)

console.log('edhr execution detail route removed static contract passed')
