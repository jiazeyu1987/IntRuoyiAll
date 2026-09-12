const fs = require('fs')
const path = require('path')
const assert = require('assert')

const page = fs.readFileSync(
  path.join(process.cwd(), 'src/views/dcc/controlled-file/browser/index.vue'),
  'utf8'
)

assert.match(
  page,
  /createControlledFileMajorRevision\(\{[\s\S]*?sourceControlledFileId:[\s\S]*?reason:[\s\S]*?idempotencyKey:/,
  'RED: creating a major revision must send the required idempotency key'
)

console.log('GREEN: controlled-file major revision sends an idempotency key')
