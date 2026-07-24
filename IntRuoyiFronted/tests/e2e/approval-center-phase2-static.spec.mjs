import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const apiFile = path.join(root, 'src/api/approval-center/index.ts')
const viewFile = path.join(root, 'src/views/approval-center/index.vue')

const api = fs.readFileSync(apiFile, 'utf8')
const view = fs.readFileSync(viewFile, 'utf8')

assert.match(api, /getApprovalTaskTimeline/, 'missing approval center timeline API export')
assert.match(api, /ApprovalTaskTimelineEntryVO/, 'missing approval center timeline entry type')
assert.match(api, /ApprovalTaskTimelineReqVO/, 'missing approval center timeline request type')
assert.match(view, /轨迹/, 'missing timeline action label in approval center view')
assert.match(view, /el-drawer/, 'missing timeline drawer in approval center view')
assert.match(view, /el-timeline/, 'missing timeline component in approval center view')
assert.match(view, /timeline/i, 'missing timeline state or handler in approval center view')

process.stdout.write('approval-center phase2 static contract passed\n')
