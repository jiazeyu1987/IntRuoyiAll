const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const page = fs.readFileSync(pagePath, 'utf8')

const extractFunction = (name) => {
  const start = page.indexOf(`const ${name} = async`)
  assert.notEqual(start, -1, `必须存在异步函数：${name}`)
  const nextConst = page.indexOf('\nconst ', start + 1)
  assert.notEqual(nextConst, -1, `函数 ${name} 后必须存在后续声明，便于静态截取。`)
  return page.slice(start, nextConst)
}

const getList = extractFunction('getList')
const secondaryLoader = extractFunction('loadRecordFormSecondaryData')
const permissionLoader = extractFunction('loadRecordFormPermissionRules')

assert.match(
  getList,
  /catch\s*\(error\)[\s\S]*listErrorMessage\.value\s*=\s*resolveErrorMessage/,
  '批记录表单真实分页接口失败仍必须写入全局列表错误。'
)

assert.doesNotMatch(
  secondaryLoader,
  /listErrorMessage\.value\s*=/,
  '填写人规则、默认预览、路由动作等延迟辅助加载失败不得污染全局列表错误。'
)

assert.match(
  permissionLoader,
  /catch\s*\(error\)[\s\S]*permissionRuleErrorMessage\s*=/,
  '填写人规则加载失败必须落到行级错误状态，不能静默隐藏。'
)

assert.match(
  page,
  /row\.permissionRuleErrorMessage[\s\S]*加载失败/,
  '填写人列必须把行级加载失败显式展示给用户。'
)

assert.match(
  page,
  /title="\s*row\.permissionRuleErrorMessage/,
  '行级填写人规则错误必须保留真实错误文本，便于定位后端或权限问题。'
)

console.log('PASS: eDHR batch record form list secondary errors stay row scoped.')
