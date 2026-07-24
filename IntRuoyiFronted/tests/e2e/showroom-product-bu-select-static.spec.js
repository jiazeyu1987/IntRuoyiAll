const assert = require('assert')
const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '..', '..')
const contractsPath = path.join(repoRoot, 'src', 'views', 'showroom-admin', 'product', 'contracts.ts')
const indexPath = path.join(repoRoot, 'src', 'views', 'showroom-admin', 'index.vue')

const contractsSource = fs.readFileSync(contractsPath, 'utf8')
const indexSource = fs.readFileSync(indexPath, 'utf8')

const expectedBuOptions = [
  '非血管BU',
  '外周血管BU',
  '结构心BU',
  '心血管BU',
  '神经血管BU',
  '心脏电生理BU'
]

assert.match(
  contractsSource,
  /export const SHOWROOM_PRODUCT_BU_OPTIONS = \[/,
  'product contracts must expose the fixed BU option list'
)

for (const option of expectedBuOptions) {
  assert.match(
    contractsSource,
    new RegExp(`label: '${option}'[\\s\\S]*value: '${option}'`),
    `product contracts must keep BU option ${option}`
  )
}

assert.match(
  contractsSource,
  /key: 'pipeline_layout'[\s\S]*type: 'select'/,
  'pipeline_layout must be modeled as a select field'
)

assert.match(
  indexSource,
  /<el-form-item label="BU">[\s\S]*<el-select[\s\S]*v-model="productForm\.pipelineLayout"/,
  'Chinese BU field must use an el-select'
)

assert.ok(
  !/<el-form-item label="BU">[\s\S]*<el-input[\s\S]*v-model="productForm\.pipelineLayout"/.test(indexSource),
  'Chinese BU field must no longer use a free-text el-input'
)

assert.match(
  indexSource,
  /v-for="item in SHOWROOM_PRODUCT_BU_OPTIONS"[\s\S]*:label="item\.label"[\s\S]*:value="item\.value"/,
  'Chinese BU select must render the shared BU options list'
)

console.log('PASS: showroom product Chinese BU is constrained to the fixed option select')
