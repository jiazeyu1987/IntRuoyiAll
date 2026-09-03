const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

assert.match(source, /<el-select[\s\S]*v-else-if="isSelectParameter\(parameter\)"[\s\S]*filterable[\s\S]*allow-create/)
assert.match(source, /@update:model-value="updateProductionDeviceSelectParameter\(activeProductionDevice\.key, parameter\.parameterCode, \$event\)"/)
assert.match(source, /const updateProductionDeviceSelectParameter[\s\S]*value: string[\s\S]*normalized = value\.trim\(\)/)

console.log('PASS: frontline select parameters support manual entry')
