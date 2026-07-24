const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const domainTracePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/DomainTracePage.vue'
)
const domainTraceDetailPath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr/DomainTraceDetailPage.vue'
)

const domainTraceSource = fs.readFileSync(domainTracePath, 'utf8')
const domainTraceDetailSource = fs.readFileSync(domainTraceDetailPath, 'utf8')

assert(
  !domainTraceSource.includes('label="domainTraceHash"') &&
    !domainTraceSource.includes('label="domainTraceSnapshotId"') &&
    !domainTraceSource.includes('items={{') &&
    !domainTraceSource.includes('blockerCount={{'),
  'Domain trace list must not expose hash, snapshot ID, item counts, or blocker counts as raw primary columns.'
)

assert(
  domainTraceSource.includes('type="expand"') &&
    domainTraceSource.includes('label="追溯概况"') &&
    domainTraceSource.includes('label="阻塞摘要"') &&
    domainTraceSource.includes('label="最近校验"'),
  'Domain trace list must show business summary columns and move trace evidence into an expandable panel.'
)

assert(
  !domainTraceDetailSource.includes('label="executionId"') &&
    !domainTraceDetailSource.includes('label="status"') &&
    !domainTraceDetailSource.includes('label="domainTraceHash"') &&
    !domainTraceDetailSource.includes('label="domainTraceSnapshotId"') &&
    !domainTraceDetailSource.includes('label="blockers"') &&
    !domainTraceDetailSource.includes('label="items"'),
  'Domain trace detail summary must stop exposing raw canonical field names.'
)

assert(
  !domainTraceDetailSource.includes('label="itemType"') &&
    !domainTraceDetailSource.includes('label="itemKey"') &&
    !domainTraceDetailSource.includes('label="sourceId"') &&
    !domainTraceDetailSource.includes('label="sourceVersion"') &&
    !domainTraceDetailSource.includes('label="snapshotHash"') &&
    !domainTraceDetailSource.includes('label="snapshotJson"') &&
    !domainTraceDetailSource.includes('label="blockerReason"'),
  'Domain trace detail tables must use Chinese business labels and move snapshot JSON/hash into expandable evidence.'
)

assert(
  domainTraceDetailSource.includes('追溯摘要') &&
    domainTraceDetailSource.includes('追溯证据') &&
    domainTraceDetailSource.includes('type="expand"') &&
    domainTraceDetailSource.includes('empty-text="暂无追溯项"'),
  'Domain trace detail must provide a readable summary, separated evidence, expandable rows, and Chinese empty state.'
)

console.log('PASS: EDHR domain trace UI static contract')
