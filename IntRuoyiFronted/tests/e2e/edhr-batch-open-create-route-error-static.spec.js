import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')
const submitBlock = source.match(/const\s+submitOpenOrCreate\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*?\n\}\n\nconst\s+/)?.[0] || ''

assert(submitBlock, '必须能定位打开或创建提交函数。')
assert(
  !/createLoading\.value\s*=\s*true\s*\n\s*createError\.value\s*=\s*''/.test(submitBlock),
  '提交开始时不得清空路线加载阶段的正式错误。'
)
assert.match(
  submitBlock,
  /if\s*\(createForm\.routeId\s*==\s*null\)\s*\{[\s\S]*if\s*\(!createError\.value\)[\s\S]*请选择工艺路线。'[\s\S]*return/,
  '缺少路线时应保留路线请求错误；只有没有正式错误时才提示请选择工艺路线。'
)
assert.match(
  submitBlock,
  /if\s*\(createRouteOptionsLoading\.value\)\s*throw new Error\('工艺路线正在加载，请稍候再确认。'\)/,
  '路线选项仍在加载时必须阻止提交并提示等待。'
)

console.log('PASS: eDHR route loading errors remain visible during open/create submit')
