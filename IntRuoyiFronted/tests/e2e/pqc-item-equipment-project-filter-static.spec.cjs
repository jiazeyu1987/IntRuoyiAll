const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(frontendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const panel = read('src/views/mes/pro/processpool/PqcItemEquipmentConfigPanel.vue')

assert.match(panel, /defineProps<\{[\s\S]*dccProjectCodeId: number/)
assert.doesNotMatch(panel, /v-model="selectedProjectName"|data-pqc-item-equipment-project-select/)
assert.match(
  panel,
  /v-for="item in filteredItems"[\s\S]*:label="item\.itemName"[\s\S]*:value="item\.itemCode"/
)
assert.match(panel, /const filteredItems = computed\(\(\) =>/)
assert.match(panel, /item\.dccProjectCodeId === props\.dccProjectCodeId/)
assert.match(panel, /new Map<string, PqcItemEquipmentItemVO>\(\)/)
assert.match(panel, /const existing = groupedByItemName\.get\(item\.itemName\)/)
assert.match(panel, /const itemCodes = Array\.from\(new Set/)
assert.match(panel, /resetSelectedItemState = \(\) => \{/)
assert.match(panel, /selectedItemCode\.value = ''/)
assert.match(panel, /draftGroups\.value = \[\]/)
assert.match(panel, /let itemsLoadSerial = 0/)
assert.match(panel, /if \(loadSerial === itemsLoadSerial\) \{[\s\S]*loadError\.value = '检验项目列表加载失败，请稍后重试。'/)
assert.match(panel, /finally \{[\s\S]*if \(loadSerial === itemsLoadSerial\) \{[\s\S]*itemsLoading\.value = false/)
assert.match(panel, /getPqcItemEquipmentConfigBatch\(/)
assert.match(panel, /dccProjectCodeId: props\.dccProjectCodeId/)
assert.match(panel, /itemCodes.*selectedItem\.value\?\.itemCodes/)
assert.match(panel, /savePqcItemEquipmentConfigBatch\(/)

console.log('PASS: QA equipment configuration filters by exact project ID and deduplicates inspection names')
