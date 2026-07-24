const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const workOrderPageSource = fs.readFileSync(workOrderPagePath, 'utf8')

assert(
  /<el-form-item label="产品名称" prop="productNameKeyword"/.test(workOrderPageSource),
  'Production work order page must render a 产品名称 keyword filter.'
)

assert(
  /<el-form-item label="产品编码" prop="productCodeKeyword"/.test(workOrderPageSource),
  'Production work order page must render a 产品编码 keyword filter.'
)

assert(
  /<el-form-item label="产品名称"[\s\S]*?<el-autocomplete[\s\S]*?v-model="queryParams\.productNameKeyword"[\s\S]*?:fetch-suggestions="queryProductNameSuggestions"[\s\S]*?@keyup\.enter="handleQuery"/.test(
    workOrderPageSource
  ),
  'Product name filter must keep a suggestion dropdown while querying the raw keyword on Enter without selecting a candidate.'
)

assert(
  /<el-form-item label="产品编码"[\s\S]*?<el-autocomplete[\s\S]*?v-model="queryParams\.productCodeKeyword"[\s\S]*?:fetch-suggestions="queryProductCodeSuggestions"[\s\S]*?@keyup\.enter="handleQuery"/.test(
    workOrderPageSource
  ),
  'Product code filter must keep a suggestion dropdown while querying the raw keyword on Enter without selecting a candidate.'
)

assert(
    workOrderPageSource.includes('MdItemApi.getItemPage') &&
    workOrderPageSource.includes('queryProductNameSuggestions') &&
    workOrderPageSource.includes('queryProductCodeSuggestions') &&
    workOrderPageSource.includes('popper-class="work-order-product-suggestion-popper"') &&
    workOrderPageSource.includes('placement="top-start"'),
  'Product keyword filters must load local MES product suggestions for the dropdown.'
)

assert(
  !workOrderPageSource.includes('productNameFilterId') &&
    !workOrderPageSource.includes('productCodeFilterId') &&
    !workOrderPageSource.includes('remote-method="searchProductNameCandidates"') &&
    !workOrderPageSource.includes('remote-method="searchProductCodeCandidates"'),
  'Frontend work order search must not require selected product candidate IDs or blocking remote-select state.'
)

assert(
  /const handleQuery = \(\) => \{\s*closeProductSuggestionPopovers\(\)\s*queryParams\.pageNo = 1\s*getList\(\)\s*\}/.test(
    workOrderPageSource
  ),
  'Query handler must close suggestion popovers and submit keyword fields directly without candidate mismatch gating.'
)

assert(
  /<el-button @pointerdown\.prevent="handleQuery"/.test(
    workOrderPageSource
  ),
  'Query button must submit on pointerdown before autocomplete blur or popper handling can cancel the search.'
)

assert(
  /:global\(\.work-order-product-suggestion-popper\)[\s\S]*?width: 320px !important;/.test(
    workOrderPageSource
  ),
  'Suggestion dropdown popper must be width-limited so it does not cover the query actions.'
)

assert(
  /\.work-order-query-actions \{\s*flex-basis: 100%;[\s\S]*?justify-content: flex-end;/.test(
    workOrderPageSource
  ),
  'Query actions must occupy a separate row so suggestion dropdowns cannot cover the buttons.'
)

assert(
  /const handleProductNameSuggestionSelect = \(item: ProductSuggestion\) => \{\s*queryParams\.productNameKeyword = item\.name\s*handleQuery\(\)\s*\}/.test(
    workOrderPageSource
  ) &&
    /const handleProductCodeSuggestionSelect = \(item: ProductSuggestion\) => \{\s*queryParams\.productCodeKeyword = item\.code\s*handleQuery\(\)\s*\}/.test(
      workOrderPageSource
    ),
  'Selecting a suggestion must fill the text keyword and immediately search with the keyword-based query.'
)

assert(
  !/catch\s*\{\s*\}/.test(workOrderPageSource),
  'Production work order page must not silently swallow failures.'
)

console.log('PASS: work order product candidate filters static contract')
