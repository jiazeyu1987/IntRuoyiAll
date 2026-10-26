const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const read = p => fs.readFileSync(path.resolve(__dirname, '../..', p), 'utf8')
for (const entry of ['src/views/Profile/components/ProductionTaskRecords.vue', 'src/workbench-sources/mes/registration.ts', 'src/utils/notifyMessageNavigation.ts']) {
  assert.match(read(entry), /navigateToEdhrWorkTask/)
}
assert.doesNotMatch(read('src/router/modules/remaining.ts'), /WorkTaskBoardPage/)
console.log('PASS: production task entries use unified navigation')
