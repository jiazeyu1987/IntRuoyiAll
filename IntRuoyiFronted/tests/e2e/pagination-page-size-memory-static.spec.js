const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const paginationPath = path.join(root, 'src/components/Pagination/index.vue')
const unifiedListTemplatePath = path.join(root, 'src/components/UnifiedListTemplate/index.vue')
const useTablePath = path.join(root, 'src/hooks/web/useTable.ts')

const paginationSource = fs.readFileSync(paginationPath, 'utf8')
const unifiedListTemplateSource = fs.readFileSync(unifiedListTemplatePath, 'utf8')
const useTableSource = fs.readFileSync(useTablePath, 'utf8')

assert.match(
  paginationSource,
  /const\s+DEFAULT_PAGE_SIZE\s*=\s*20/,
  'Pagination must define 20 as the global default page size.'
)

assert.match(
  paginationSource,
  /limit:\s*\{[\s\S]*type:\s*Number[\s\S]*default:\s*20/,
  'Pagination defineProps default must use literal 20 to satisfy vue/valid-define-props.'
)

assert.match(
  paginationSource,
  /const\s+PAGE_SIZE_OPTIONS\s*=\s*\[10,\s*20,\s*30,\s*50,\s*100\]/,
  'Pagination must expose the standard page size options including 20.'
)

assert.match(
  paginationSource,
  /:page-sizes="PAGE_SIZE_OPTIONS"/,
  'Pagination must render page size options from the shared constant.'
)

assert.match(
  paginationSource,
  /storageKey:\s*\{[\s\S]*type:\s*String[\s\S]*default:\s*''/,
  'Pagination must accept an optional stable storageKey prop.'
)

assert.match(
  paginationSource,
  /PAGE_SIZE_STORAGE_PREFIX/,
  'Pagination must use a namespaced storage key prefix for remembered page sizes.'
)

assert.match(
  paginationSource,
  /import\s+\{\s*useRoute\s*\}\s+from 'vue-router'/,
  'Pagination must derive stable route keys for legacy direct Pagination usages.'
)

assert.match(
  paginationSource,
  /let\s+ANONYMOUS_PAGE_SIZE_STORAGE_SEED\s*=\s*0/,
  'Pagination must isolate multiple direct Pagination components on the same route.'
)

assert.match(
  paginationSource,
  /if\s*\(\s*props\.storageKey\s*\)\s*return\s+props\.storageKey/,
  'Pagination must prefer explicit storageKey and derive a route key only when absent.'
)

assert.match(
  paginationSource,
  /route:\$\{routePath\}:\$\{anonymousPageSizeStorageIndex\}/,
  'Pagination must include route path and component index in derived storage keys.'
)

assert.match(
  paginationSource,
  /window\.localStorage\.getItem\(pageSizeStorageKey\.value\)/,
  'Pagination must read the remembered page size from localStorage by resolved key.'
)

assert.match(
  paginationSource,
  /window\.localStorage\.setItem\(pageSizeStorageKey\.value,\s*String\(nextPageSize\)\)/,
  'Pagination must persist user-selected page size by resolved key.'
)

assert.match(
  paginationSource,
  /onMounted\(applyRememberedPageSize\)/,
  'Pagination must restore remembered page size when mounted.'
)

assert.match(
  paginationSource,
  /const\s+nextPageSize\s*=\s*readRememberedPageSize\(\)\s*\?\?\s*DEFAULT_PAGE_SIZE/,
  'Pagination must resolve page size from remembered value first, then default to 20.'
)

assert.match(
  paginationSource,
  /emit\('update:limit',\s*nextPageSize\)/,
  'Pagination must update the parent limit when restoring remembered or default page size.'
)

const applyRememberedPageSizeMatch = paginationSource.match(
  /const\s+applyRememberedPageSize\s*=\s*\(\)\s*=>\s*\{[\s\S]*?\n\}/
)
assert.ok(applyRememberedPageSizeMatch, 'Pagination must keep page size restore logic explicit.')

assert.doesNotMatch(
  applyRememberedPageSizeMatch[0],
  /emit\('pagination'/,
  'Pagination must not trigger an extra list refresh while restoring remembered or default page size on mount.'
)

assert.match(
  paginationSource,
  /const\s+handleSizeChange\s*=\s*\(val\)\s*=>\s*\{[\s\S]*?emit\('pagination',\s*\{\s*page:\s*currentPage\.value,\s*limit:\s*val\s*\}\)/,
  'Pagination must still refresh the list when the user changes page size.'
)

assert.match(
  paginationSource,
  /const\s+handleCurrentChange\s*=\s*\(val\)\s*=>\s*\{[\s\S]*?emit\('pagination',\s*\{\s*page:\s*val,\s*limit:\s*pageSize\.value\s*\}\)/,
  'Pagination must still refresh the list when the user changes page number.'
)

assert.match(
  unifiedListTemplateSource,
  /<Pagination[\s\S]*:storage-key="tableKey"/,
  'UnifiedListTemplate must pass its stable tableKey to Pagination for per-list page size memory.'
)

assert.match(
  useTableSource,
  /pageSize:\s*20/,
  'Legacy useTable lists must also default to 20 rows per page.'
)

console.log('PASS: pagination page size memory static contract')
