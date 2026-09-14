const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panel = read('src/views/mes/pro/processpool/PqcItemEquipmentConfigPanel.vue')
const qaPage = read('src/views/mes/pro/processpool/QaRegulationPage.vue')
const api = read('src/api/mes/qc/template/index.ts')

assert.match(qaPage, /<PqcItemEquipmentConfigPanel[\s\S]*:project-name="selectedDccProjectCodeLabel"/)
assert.match(qaPage, /:dcc-project-code-id="Number\(selectedDccProjectCode\.id\)"/)
assert.doesNotMatch(panel, /v-model="selectedProjectName"|data-pqc-item-equipment-project-select/)
assert.match(
  panel,
  /v-for="item in filteredItems"[\s\S]*:label="item\.itemName"[\s\S]*:value="item\.itemCode"/
)
assert.match(
  panel,
  /const filteredItems = computed\(\(\) =>[\s\S]*new Map<string, PqcItemEquipmentItemVO>\(\)[\s\S]*groupedByItemName\.has\(item\.itemName\)/
)
assert.match(panel, /itemCode: selectedItemCode\.value/)
assert.match(api, /export interface PqcItemEquipmentItemVO[\s\S]*dccProjectCodeId: number[\s\S]*projectName: string[\s\S]*itemName: string/)

console.log('PASS: QA selection supplies the current project context without a second project selector')
