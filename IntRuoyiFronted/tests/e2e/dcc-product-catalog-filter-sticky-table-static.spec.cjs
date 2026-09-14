const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../../..')
const frontendRoot = path.join(repoRoot, 'IntRuoyiFronted')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8')

const panelSource = readSource(
  'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
)
const apiSource = readSource('src/api/dcc/controlledFile/productCatalog.ts')
const backendReqSource = fs.readFileSync(
  path.join(
    repoRoot,
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/productcatalog/vo/DccProductCatalogPageReqVO.java'
  ),
  'utf8'
)
const mapperSource = fs.readFileSync(
  path.join(
    repoRoot,
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/productcatalog/DccProductCatalogMapper.java'
  ),
  'utf8'
)

const extractConstArray = (source, constName) => {
  const marker = `const ${constName}`
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `missing const ${constName}`)
  const equalsIndex = source.indexOf('=', markerIndex)
  assert.notEqual(equalsIndex, -1, `missing assignment for ${constName}`)
  const start = source.indexOf('[', equalsIndex)
  assert.notEqual(start, -1, `missing array start for ${constName}`)
  let depth = 0
  for (let index = start; index < source.length; index += 1) {
    const char = source[index]
    if (char === '[') depth += 1
    if (char === ']') {
      depth -= 1
      if (depth === 0) return source.slice(start, index + 1)
    }
  }
  throw new Error(`missing array end for ${constName}`)
}

const quickFilterSource = extractConstArray(panelSource, 'productCatalogQuickFilterDefinitions')

const requiredFilters = [
  ['categoryLevel1', '产品类别 I'],
  ['categoryLevel2', '产品类别 II'],
  ['productSequence', '产品序号'],
  ['product', '产品'],
  ['dataSource', '数据来源'],
  ['productCode', '产品编码'],
  ['projectName', '项目名称'],
  ['projectCode', '项目代码'],
  ['registrationCertificateName', '注册证名称'],
  ['registrationCertificateNumber', '注册证号'],
  ['certificateHolder', '持证人'],
  ['registrationPlace', '注册地'],
  ['effectiveDate', '生效日期'],
  ['expiryDate', '有效期至'],
  ['classification', '分类'],
  ['productStatus', '产品状态'],
  ['registrationInfoLink', '注册证信息链接'],
  ['remark', '备注']
]

for (const [key, label] of requiredFilters) {
  assert.match(
    quickFilterSource,
    new RegExp(`key:\\s*'${key}'[\\s\\S]*?label:\\s*'${label}'[\\s\\S]*?queryParamKey:\\s*'${key}'`),
    `产品目录快速筛选必须包含 ${label} / ${key}`
  )
  assert.ok(apiSource.includes(`${key}?: string`), `前端分页请求类型必须声明 ${key}`)
  const javaField = key.replace(/[A-Z]/g, (match) => match.toUpperCase())
  assert.match(
    backendReqSource,
    new RegExp(`private String ${key};`),
    `后端分页 Request VO 必须声明 ${key}`
  )
}

assert.doesNotMatch(
  quickFilterSource,
  /batchRecordTotalRecognitionJson|批记录识别JSON/,
  '批记录识别JSON 不得作为产品目录快速筛选 title'
)
assert.doesNotMatch(
  quickFilterSource,
  /projectCodeNotBlank/,
  '项目代码 title 筛选必须按项目代码文本过滤，不得继续只提供不为空筛选'
)

for (const token of [
  'COLUMN_PRODUCT_SEQUENCE',
  'PROJECT_NAME_COLUMN',
  'PROJECT_CODE_COLUMN',
  'COLUMN_REGISTRATION_PLACE',
  'COLUMN_EFFECTIVE_DATE',
  'COLUMN_EXPIRY_DATE',
  'COLUMN_CLASSIFICATION',
  'COLUMN_REGISTRATION_INFO_LINK',
  'COLUMN_REMARK'
]) {
  assert.ok(mapperSource.includes(token), `后端 Mapper 必须声明并使用字段过滤列：${token}`)
}

assert.match(
  mapperSource,
  /likeIfPresent\(COLUMN_PRODUCT_SEQUENCE,\s*reqVO\.getProductSequence\(\)\)/,
  '产品序号必须按正式字段支持包含过滤'
)
assert.match(
  mapperSource,
  /likeIfPresent\(PROJECT_NAME_COLUMN,\s*reqVO\.getProjectName\(\)\)/,
  '项目名称必须按正式字段支持包含过滤'
)
assert.match(
  mapperSource,
  /likeIfPresent\(PROJECT_CODE_COLUMN,\s*reqVO\.getProjectCode\(\)\)/,
  '项目代码必须按正式字段支持包含过滤'
)
assert.match(
  mapperSource,
  /likeIfPresent\(COLUMN_REGISTRATION_CERTIFICATE_NUMBER,\s*reqVO\.getRegistrationCertificateNumber\(\)\)/,
  '注册证号必须按正式字段支持包含过滤'
)

assert.match(
  panelSource,
  /<div class="dcc-product-catalog-table-shell">[\s\S]*<el-table[\s\S]*height="100%"/,
  '产品目录右侧表格必须放入固定高度 shell，并用 Element Plus height 固定表头与底部横向滚动条'
)
assert.match(
  panelSource,
  /\.dcc-product-catalog-split-layout\s*\{[\s\S]*height:\s*clamp\(420px,\s*calc\(100vh - 258px\),\s*680px\)/,
  '产品目录左树右表布局必须使用视口约束高度，让横向滚动条停留在当前页面可视区底部'
)
assert.match(
  panelSource,
  /\.dcc-product-catalog-detail-panel\s*\{[\s\S]*display:\s*flex[\s\S]*flex-direction:\s*column[\s\S]*min-height:\s*0/,
  '产品目录右侧明细区域必须是纵向 flex 布局，避免底部滚动条被页面纵向滚动挤走'
)
assert.match(
  panelSource,
  /\.dcc-product-catalog-table-shell\s*\{[\s\S]*flex:\s*1 1 auto[\s\S]*min-height:\s*0/,
  '产品目录表格 shell 必须填满剩余高度并允许内部滚动'
)

console.log('PASS: DCC product catalog filter sticky table static contract')
