const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(frontendRoot, '..')

const readSource = (absolutePath) => {
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${absolutePath}`)
  return fs.readFileSync(absolutePath, 'utf8').replace(/\r\n/g, '\n')
}

const productCatalogPanelSource = readSource(
  path.join(
    frontendRoot,
    'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'
  )
)
const productCatalogApiSource = readSource(
  path.join(frontendRoot, 'src/api/dcc/controlledFile/productCatalog.ts')
)
const pageReqSource = readSource(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/productcatalog/vo/DccProductCatalogPageReqVO.java'
  )
)
const mapperSource = readSource(
  path.join(
    workspaceRoot,
    'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/productcatalog/DccProductCatalogMapper.java'
  )
)

const quickFilterStart = productCatalogPanelSource.indexOf('const productCatalogQuickFilterDefinitions')
const quickFilterEnd = productCatalogPanelSource.indexOf('const productCatalogDefaultColumns', quickFilterStart)
assert.notEqual(quickFilterStart, -1, '产品目录必须声明快速过滤定义。')
assert.notEqual(quickFilterEnd, -1, '产品目录快速过滤定义边界必须可定位。')
const quickFilterDefinitions = productCatalogPanelSource.slice(quickFilterStart, quickFilterEnd)

assert.match(
  quickFilterDefinitions,
  /key:\s*'projectCodeNotBlank'[\s\S]*label:\s*'项目代码'[\s\S]*type:\s*'select'[\s\S]*queryParamKey:\s*'projectCodeNotBlank'[\s\S]*label:\s*'不为空'[\s\S]*value:\s*true/,
  '产品目录快速过滤必须提供“项目代码 / 不为空”正式筛选项。'
)
assert.match(
  productCatalogPanelSource,
  /const queryParams = reactive<DccProductCatalogPageQuery>\(\{[\s\S]*projectCodeNotBlank:\s*undefined[\s\S]*sortField:\s*undefined/,
  '产品目录查询参数必须声明 projectCodeNotBlank，并由标准快速过滤负责写入和重置。'
)
assert.ok(
  productCatalogApiSource.includes('projectCodeNotBlank?: boolean'),
  '前端产品目录分页请求类型必须声明 projectCodeNotBlank 布尔参数。'
)
assert.ok(
  pageReqSource.includes('private Boolean projectCodeNotBlank;'),
  '后端产品目录分页 Request VO 必须声明 projectCodeNotBlank 布尔参数。'
)
assert.match(
  mapperSource,
  /applyProjectCodeNotBlankFilter\(wrapper,\s*reqVO\);[\s\S]*private void applyProjectCodeNotBlankFilter\(QueryWrapperX<DccProductCatalogDO> wrapper,\s*DccProductCatalogPageReqVO reqVO\)[\s\S]*Boolean\.TRUE\.equals\(reqVO\.getProjectCodeNotBlank\(\)\)[\s\S]*wrapper\.isNotNull\(PROJECT_CODE_COLUMN\)[\s\S]*TRIM\([\s\S]*PROJECT_CODE_COLUMN[\s\S]*\)\s*<>\s*''/,
  '后端 Mapper 必须在 projectCodeNotBlank=true 时排除 null、空字符串和空白字符串项目代码。'
)

console.log('PASS: DCC product catalog project-code not-blank filter static contract')
