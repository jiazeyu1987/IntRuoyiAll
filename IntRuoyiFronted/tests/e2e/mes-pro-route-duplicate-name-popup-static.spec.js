const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = process.cwd()
const apiSource = fs.readFileSync(path.join(repoRoot, 'src/api/mes/pro/route/index.ts'), 'utf8')
const formSource = fs.readFileSync(path.join(repoRoot, 'src/views/mes/pro/route/RouteFormContent.vue'), 'utf8')
const listSource = fs.readFileSync(path.join(repoRoot, 'src/views/mes/pro/route/index.vue'), 'utf8')

assert.match(
  apiSource,
  /createRoute:\s*async\s*\(\s*data:\s*ProRouteVO,\s*options:\s*Record<string,\s*unknown>\s*=\s*\{\}\s*\)/,
  '工艺路线创建 API 必须允许调用方关闭全局错误提示，以便重复名称时改用弹框提示。'
)

assert.match(
  apiSource,
  /request\.post\(\{\s*url:\s*`\/mes\/pro\/route\/create`,\s*data,\s*\.\.\.options\s*\}\)/,
  '工艺路线创建 API 必须把 ignoreErrorMessage 等选项传给 axios 请求。'
)

assert.match(
  apiSource,
  /copyRoute:\s*async\s*\(\s*data:\s*ProRouteCopyReqVO,\s*options:\s*Record<string,\s*unknown>\s*=\s*\{\}\s*\)/,
  '工艺路线复制 API 必须允许调用方关闭全局错误提示，以便重复名称时改用弹框提示。'
)

assert.match(
  apiSource,
  /request\.post\(\{\s*url:\s*`\/mes\/pro\/route\/copy`,\s*data,\s*\.\.\.options\s*\}\)/,
  '工艺路线复制 API 必须把 ignoreErrorMessage 等选项传给 axios 请求。'
)

assert.match(
  formSource,
  /ProRouteApi\.createRoute\(data,\s*\{\s*ignoreErrorMessage:\s*true\s*\}\)/,
  '新增工艺路线提交时必须关闭全局错误提示，避免重复名称同时出现通知和弹框。'
)

assert.match(
  formSource,
  /isDuplicateRouteNameError\([^)]*\)/,
  '新增工艺路线提交必须显式识别后端重复名称错误。'
)

assert(
  !formSource.includes('工艺路线名称已存在，已跳过创建'),
  '新增工艺路线同名时不得继续提示“已跳过创建”，必须引导用户做升版本决策。'
)

assert.match(
  formSource,
  /confirmDuplicateRouteVersionUpgrade\(\s*formData\.value\.name\s*\)/,
  '新增工艺路线同名时必须提示“是否升版本”。'
)

assert.match(
  formSource,
  /emit\(\s*'request-upgrade'\s*,\s*\{\s*routeName:\s*formData\.value\.name\s*\}\s*\)/,
  '新增工艺路线同名且用户确认后，必须把同名路线交给外层打开已有路线编辑升版。'
)

assert.match(
  listSource,
  /ProRouteApi\.copyRoute\([\s\S]*\{\s*ignoreErrorMessage:\s*true\s*\}[\s\S]*\)/,
  '复制工艺路线同名时必须关闭全局错误提示并交给当前页面弹框处理。'
)

assert.match(
  listSource,
  /catch\s*\(error\)\s*\{[\s\S]*isDuplicateRouteNameError\(error\)[\s\S]*confirmDuplicateRouteVersionUpgrade\(\s*copyForm\.targetName\s*\)/,
  '复制工艺路线同名时必须提示“是否升版本”。'
)

assert.match(
  listSource,
  /openExistingRouteForVersionUpgrade\(\s*copyForm\.targetName\s*\)/,
  '复制工艺路线同名且用户确认后，必须打开已有同名路线进行升版本。'
)

console.log('PASS: mes pro route duplicate name popup static contract')
