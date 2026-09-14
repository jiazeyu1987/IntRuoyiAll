const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const vue = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'),
  'utf8'
)
const api = fs.readFileSync(path.join(root, 'src/api/dcc/controlledFile/productCatalog.ts'), 'utf8')

assert.match(api, /batchRecordTotalRecognitionJson\?: string \| null/)
assert.match(vue, /key: 'batchRecordTotalRecognitionJson', label: '批记录识别JSON'/)
assert.match(vue, /data-testid="dcc-product-catalog-copy-recognition-json"/)
assert.match(vue, /copyProductCatalogBatchRecordTotalRecognitionJson\(row\)/)
assert.match(vue, /source: row\.batchRecordTotalRecognitionJson \|\| ''/)

