import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve(process.cwd())
const api = fs.readFileSync(path.join(root, 'src/api/approval-center/index.ts'), 'utf8')
const view = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')
const routerDir = path.join(root, 'src/router/modules')
const routerFiles = fs
  .readdirSync(routerDir)
  .filter((file) => file.endsWith('.ts'))
  .map((file) => fs.readFileSync(path.join(routerDir, file), 'utf8'))
  .join('\n')

for (const moduleCode of ['BPM', 'DCC', 'EDHR', 'SHOWROOM']) {
  assert.match(api, new RegExp(`'${moduleCode}'`), `approval center API must expose ${moduleCode}`)
}

for (const viewType of ['TODO', 'DONE', 'MY_INITIATED', 'CC']) {
  assert.match(api, new RegExp(`'${viewType}'`), `approval center API must expose ${viewType}`)
}
assert.doesNotMatch(api, /'SIGNATURE_PENDING'/, 'approval center API must not expose signature pending as an independent view')

for (const viewType of ['TODO', 'DONE', 'MY_INITIATED', 'CC']) {
  assert.match(view, new RegExp(viewType), `approval center view must keep ${viewType} tab wiring`)
}
assert.doesNotMatch(view, /SIGNATURE_PENDING:\s*use|SIGNATURE_PENDING:\s*'approval|approval\.center\.signaturePending/, 'approval center view must not keep an independent signature pending view type')
assert.doesNotMatch(
  view,
  /<el-tab-pane label="签名待处理"|name="signature-pending"|approval\.center\.signaturePending/,
  'approval center view must not expose signature pending as an independent visible list'
)

assert.match(view, /resolveModuleName/, 'approval center must display module source names')
assert.match(view, /openModuleDetail/, 'approval center must navigate to module formal pages')
assert.match(view, /getApprovalTaskTimeline/, 'approval center must keep unified timeline entry')
assert.match(view, /detailRoute/, 'approval center must use provider detailRoute')
assert.match(view, /detailQuery/, 'approval center must preserve provider detailQuery')
assert.match(view, /approvalTabNames/, 'approval center must define route-driven approval sub-tabs')
assert.match(view, /resolveRouteTab/, 'approval center must resolve active sub-tab from child route')
assert.match(view, /router\.push\(/, 'approval center must push child route when switching sub-tabs')
assert.match(view, /router\.replace\(/, 'approval center must canonicalize legacy query links to child routes')

assert.doesNotMatch(
  view,
  /supervisorApprove|gaoxinApprove|approveTask|rejectTask|bpmApprove|completeApproval|publishRevision/i,
  'approval center must not execute module-specific approval actions'
)

const globalApprovalCenterRoutes = routerFiles.match(/path:\s*'\/approval-center'/g) || []
assert.equal(globalApprovalCenterRoutes.length, 1, 'there must be exactly one global approval center route')
for (const childPath of ['todo', 'done', 'my-initiated', 'cc']) {
  assert.match(routerFiles, new RegExp(`path:\\s*'${childPath}'`), `approval center must expose child route ${childPath}`)
}
assert.doesNotMatch(
  routerFiles,
  /path:\s*'signature-pending'|ApprovalCenterSignaturePending|title:\s*'签名待处理'/,
  'approval center must not expose signature pending as an independent child route'
)

assert.match(routerFiles, /path:\s*'approval'/, 'Showroom formal approval route must remain available')

process.stdout.write('approval-center phase4 static contract passed\n')
