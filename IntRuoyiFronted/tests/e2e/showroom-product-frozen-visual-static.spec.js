const fs = require('fs')
const path = require('path')

const listPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/ProductListTable.vue'
)
const listSource = fs.readFileSync(listPath, 'utf8')

const requiredSnippets = [
  ':row-class-name="resolveProductRowClassName"',
  'showroom-product-list__row--frozen',
  'showroom-product-list__frozen-tag',
  '已冻结',
  'showroom-product-list__freeze-action--frozen',
  ':class="{ \'showroom-product-list__freeze-action--frozen\': row.frozen }"',
  'const resolveProductRowClassName =',
  "row.frozen ? 'showroom-product-list__row--frozen' : ''"
]

for (const snippet of requiredSnippets) {
  if (!listSource.includes(snippet)) {
    throw new Error(`missing frozen visual snippet ${snippet} in ${listPath}`)
  }
}

const frozenRowStyleIndex = listSource.indexOf('.showroom-product-list__table :deep(.showroom-product-list__row--frozen')
const hoverStyleIndex = listSource.indexOf('.showroom-product-list__table :deep(.showroom-product-list__row--frozen:hover')
const frozenTagStyleIndex = listSource.indexOf('.showroom-product-list__frozen-tag')

if (frozenRowStyleIndex === -1 || hoverStyleIndex === -1 || frozenTagStyleIndex === -1) {
  throw new Error(`missing frozen row, hover, or tag CSS in ${listPath}`)
}

for (const color of ['#e6f7ff', '#8cccf0', '#0b6f9c']) {
  if (!listSource.includes(color)) {
    throw new Error(`missing ice-blue frozen color ${color} in ${listPath}`)
  }
}

console.log('PASS: showroom frozen products have visible ice-blue row, tag, and action styling')
