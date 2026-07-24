const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
assert(fs.existsSync(pagePath), '生产报工页面必须存在。')

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(!/catch\s*\{\s*\}/.test(pageSource), '生产报工页不得存在空 catch。')
assert(
  pageSource.includes('isCancelError') || pageSource.includes('取消删除'),
  '删除报工必须区分用户取消和真实删除失败。'
)
assert(
  /message\.error\([\s\S]*删除报工失败/.test(pageSource),
  '真实删除失败必须向用户暴露“删除报工失败”反馈。'
)

console.log('PASS: MES feedback error exposure static contract')
