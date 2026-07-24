const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const apiSource = readSource('src/api/mdm/product/index.ts')
const pageSource = readSource('src/views/mdm/product/index.vue')

assert.match(
  pageSource,
  /const handleReferences = async \(row: MdmProductRespVO\) => \{[\s\S]*ProductApi\.getProductReferences\(row\.id\)/,
  '产品主数据列表点击引用按钮时必须把当前行 id 传给 API 封装'
)

assert.match(
  apiSource,
  /request\.get\(\{ url: '\/mdm\/product\/references', params: \{ id: productId \} \}\)/,
  '产品主数据引用接口必须按后端契约提交 id 查询参数'
)

assert.doesNotMatch(
  apiSource,
  /request\.get\(\{ url: '\/mdm\/product\/references', params: \{ productId \} \}\)/,
  '产品主数据引用接口不得提交 productId 查询参数，否则后端无法绑定 @RequestParam(\"id\")'
)

console.log('PASS: mdm product reference API sends id query param')
