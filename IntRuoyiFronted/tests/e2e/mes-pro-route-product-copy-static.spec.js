const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/route/RouteProductList.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/route/product/index.ts')

assert(fs.existsSync(pagePath), `required file missing: ${pagePath}`)
assert(fs.existsSync(apiPath), `required file missing: ${apiPath}`)

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const submitCopyStart = pageSource.indexOf('const submitCopyForm = async () => {')
const submitCopyEnd = pageSource.indexOf('/** 删除按钮操作 */', submitCopyStart)
assert.notEqual(submitCopyStart, -1, '关联产品页面必须声明 submitCopyForm。')
assert.notEqual(submitCopyEnd, -1, 'submitCopyForm 后必须保留删除操作锚点，避免静态检查误扫其它函数。')
const submitCopySource = pageSource.slice(submitCopyStart, submitCopyEnd)

assert.match(
  apiSource,
  /export interface ProRouteProductCopyReqVO \{[\s\S]*sourceRouteProductId: number[\s\S]*targetItemId: number[\s\S]*\}/,
  '工艺路线产品 API 必须声明复制请求类型，包含源关联产品编号和目标产品编号。'
)
assert.match(
  apiSource,
  /copyRouteProduct:\s*async\s*\(data:\s*ProRouteProductCopyReqVO\)[\s\S]*\/mes\/pro\/route-product\/copy/,
  '工艺路线产品 API 必须调用后端正式复制接口。'
)
assert.match(
  pageSource,
  /<el-button link type="primary" @click="openCopyForm\(scope\.row\)">复制<\/el-button>/,
  '关联产品表格操作列必须提供单行复制按钮。'
)
assert.match(
  pageSource,
  /<el-button type="primary" plain @click="openForm\('create'\)">[\s\S]*关联产品/,
  '关联产品表格必须保留新增入口。'
)
assert.match(
  pageSource,
  /<el-button link type="primary" @click="openForm\('update', scope\.row\)">编辑<\/el-button>/,
  '关联产品表格必须保留编辑入口。'
)
assert.match(
  pageSource,
  /await ProRouteProductApi\.createRouteProduct\(formData\.value\)[\s\S]*message\.success\(t\('common\.createSuccess'\)\)/,
  '新增关联产品必须继续调用 createRouteProduct 并提示新增成功。'
)
assert.match(
  pageSource,
  /await ProRouteProductApi\.updateRouteProduct\(formData\.value\)[\s\S]*message\.success\(t\('common\.updateSuccess'\)\)/,
  '编辑关联产品必须继续调用 updateRouteProduct 并提示修改成功。'
)
assert.match(
  pageSource,
  /<Dialog\s+:title="copyFormTitle"\s+v-model="copyFormVisible"[\s\S]*<MdItemSelect v-model="copyFormData\.targetItemId" \/>/,
  '复制弹窗必须使用现有 MdItemSelect 选择目标产品。'
)
assert.match(
  pageSource,
  /const openCopyForm = \(row: ProRouteProductVO\) => \{[\s\S]*sourceRouteProductId:\s*row\.id[\s\S]*targetItemId:\s*undefined[\s\S]*quantity:\s*row\.quantity[\s\S]*productionTime:\s*row\.productionTime[\s\S]*timeUnitType:\s*row\.timeUnitType[\s\S]*remark:\s*row\.remark/,
  '复制弹窗必须继承源关联产品的生产参数，并要求用户重新选择目标产品。'
)
assert.match(
  pageSource,
  /await ProRouteProductApi\.copyRouteProduct\(copyFormData\.value\)[\s\S]*message\.success\('复制成功'\)[\s\S]*copyFormVisible\.value = false[\s\S]*await getList\(\)/,
  '复制提交必须调用后端接口，成功后关闭弹窗并刷新列表。'
)
assert.match(
  submitCopySource,
  /const submitCopyForm = async \(\) => \{[\s\S]*try \{[\s\S]*await ProRouteProductApi\.copyRouteProduct\(copyFormData\.value\)[\s\S]*\} finally \{[\s\S]*copyFormLoading\.value = false[\s\S]*\}/,
  '复制提交必须使用 finally 释放 loading，不能通过 catch 伪造成功。'
)
assert.doesNotMatch(
  submitCopySource,
  /catch\s*\{\s*\}/,
  '关联产品复制链路不允许空 catch 吞掉后端错误。'
)

console.log('PASS: MES route product copy static contract')
