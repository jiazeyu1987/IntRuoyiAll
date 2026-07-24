const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const routeDir = path.join(repoRoot, 'src/views/mes/pro/route')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const routeVueFiles = fs
  .readdirSync(routeDir)
  .filter((file) => file.endsWith('.vue'))
  .map((file) => `src/views/mes/pro/route/${file}`)

const emptyCatchOffenders = routeVueFiles.filter((file) =>
  /catch\s*(?:\([^)]*\))?\s*\{\s*\}/.test(read(file))
)

assert.deepEqual(
  emptyCatchOffenders,
  [],
  `工艺路线页面不得用空 catch 掩盖后端错误：${emptyCatchOffenders.join(', ')}`
)

const routeError = read('src/views/mes/pro/route/routeError.ts')

assert.match(
  routeError,
  /export const isRouteConfirmCancel/,
  '工艺路线删除确认取消必须有明确识别函数，不能吞掉全部异常。'
)

assert.match(
  routeError,
  /export const resolveRouteOperationErrorMessage/,
  '工艺路线后端错误必须有统一错误解析函数，确保页面可见。'
)

for (const file of [
  'src/views/mes/pro/route/RouteProductList.vue',
  'src/views/mes/pro/route/RouteProductBomList.vue',
  'src/views/mes/pro/route/RouteProcessList.vue'
]) {
  const source = read(file)
  assert.match(source, /catch\s*\(\s*error\s*\)/, `${file} 删除失败必须接收错误对象。`)
  assert.match(
    source,
    /if\s*\(\s*isRouteConfirmCancel\(error\)\s*\)\s*\{[\s\S]*return[\s\S]*\}/,
    `${file} 仅允许用户取消删除时静默返回。`
  )
  assert.match(
    source,
    /message\.error\(resolveRouteOperationErrorMessage\(error,/,
    `${file} 删除接口失败必须展示后端错误或明确兜底文案。`
  )
}

console.log('mes-pro-route-error-visibility-static PASS')
