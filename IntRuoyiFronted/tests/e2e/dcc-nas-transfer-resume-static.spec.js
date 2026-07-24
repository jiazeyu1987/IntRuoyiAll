const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const nasPagePath = path.join(repoRoot, 'src/views/system/nas/index.vue')
const source = fs.readFileSync(nasPagePath, 'utf8')

assert.match(
  source,
  /NAS_TRANSFER_LAST_TASK_ID_KEY/,
  'NAS management page must define a stable localStorage key for the last transfer task'
)

assert.match(
  source,
  /localStorage\.setItem\(NAS_TRANSFER_LAST_TASK_ID_KEY/,
  'NAS management page must persist the transfer task id after task creation or polling'
)

assert.match(
  source,
  /restoreLastTransferTask/,
  'NAS management page must load the last transfer task when the page is entered again'
)

assert.match(
  source,
  /restoreLastTransferTask\(\)/,
  'NAS management page must call task restoration during mounted initialization'
)

assert.match(
  source,
  /clearLastTransferTaskId/,
  'NAS management page must expose a dedicated stale last-transfer-task cleanup helper'
)

assert.match(
  source,
  /localStorage\.removeItem\(NAS_TRANSFER_LAST_TASK_ID_KEY\)/,
  'NAS management page must remove a stale last transfer task id from localStorage'
)

assert.match(
  source,
  /isNasTransferTaskNotFoundError/,
  'NAS management page must detect task-not-found errors explicitly instead of treating them as active tasks'
)

assert.match(
  source,
  /最近 NAS 转移任务已不存在，请重新发起转移/,
  'NAS management page must show an actionable message when a persisted transfer task no longer exists'
)

assert.match(
  source,
  /clearLastTransferTaskId\(\)[\s\S]*return/,
  'NAS management page must stop restore flow after clearing a stale transfer task id'
)
