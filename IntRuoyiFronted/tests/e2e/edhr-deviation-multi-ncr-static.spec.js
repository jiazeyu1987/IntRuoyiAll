const fs = require('fs')
const path = require('path')
const assert = require('assert')

const source = fs.readFileSync(path.resolve(__dirname, '../../src/views/mes/pro/edhr-deviation/index.vue'), 'utf8')

assert.match(source, /type="selection"/)
assert.match(source, /selectedCritical\.length > 0/)
assert.match(source, /deviationIds: selectedCritical\.value\.map\(row => row\.id\)/)
assert.match(source, /batchExecutionId,/)
console.log('PASS: multi-critical deviation NCR UI contract')
