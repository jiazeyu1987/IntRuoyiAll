const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const nasPagePath = path.join(repoRoot, 'src/views/system/nas/index.vue')
const source = fs.readFileSync(nasPagePath, 'utf8')

assert.match(
  source,
  /ElMessageBox\.alert\(\s*['"]全部转移结束['"]/,
  'NAS transfer completion must show an Element Plus alert with text 全部转移结束'
)

assert.match(
  source,
  /result\.status\s*===\s*['"]COMPLETED['"][\s\S]*ElMessageBox\.alert\(\s*['"]全部转移结束['"]/,
  'The 全部转移结束 alert must be triggered from the COMPLETED transfer task branch'
)

assert.match(
  source,
  /clearTransferTaskPolling\(\)[\s\S]*result\.status\s*===\s*['"]COMPLETED['"]/,
  'NAS transfer completion must still stop polling before showing the final completion alert'
)
