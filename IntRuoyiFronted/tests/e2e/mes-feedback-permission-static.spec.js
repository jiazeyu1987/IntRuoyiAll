const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
assert(fs.existsSync(pagePath), `生产报工页面必须存在：${pagePath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  pageSource.includes("const canUpdateImportRecord = checkPermi(['mes:pro-feedback:update'])"),
  '待归属页必须显式读取 mes:pro-feedback:update，用来统一控制归属与确认报工写入口。'
)

assert(
  pageSource.includes('isImportRecordEditable(scope.row)'),
  '待归属页的补填字段必须统一收口到 isImportRecordEditable，而不是对无更新权限用户继续暴露可编辑控件。'
)

assert(
  /<el-button[\s\S]*v-hasPermi="\['mes:pro-feedback:update'\]"[\s\S]*确认报工/.test(pageSource),
  '确认报工按钮必须绑定 mes:pro-feedback:update 权限。'
)

assert(
  /<el-button[\s\S]*v-hasPermi="\['mes:pro-feedback:update'\]"[\s\S]*选择归属/.test(pageSource),
  '选择归属按钮必须绑定 mes:pro-feedback:update 权限。'
)

assert(
  /<el-button[\s\S]*v-hasPermi="\['mes:pro-feedback:update'\]"[\s\S]*修改归属/.test(pageSource),
  '修改归属按钮必须绑定 mes:pro-feedback:update 权限。'
)

assert(
  /if \(!canUpdateImportRecord\) \{[\s\S]*缺少生产报工更新权限/.test(pageSource),
  '前端在程序化触发归属或整批确认时必须显式 fail-fast，而不是继续请求后端后再 403。'
)

console.log('PASS: MES feedback permission static contract')
