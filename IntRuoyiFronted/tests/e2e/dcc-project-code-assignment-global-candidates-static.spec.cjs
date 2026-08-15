const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue'),
  'utf8'
)
const api = fs.readFileSync(
  path.join(root, 'src/api/dcc/controlledFile/projectCodeAssignments.ts'),
  'utf8'
)

assert.match(api, /getProjectCodeAssignmentCandidatePage/)
assert.match(api, /project-codes\/\$\{projectCodeId\}\/assignment-candidates\/page/)
assert.match(page, /data-testid="dcc-project-code-assignment-global-search"/)
assert.match(page, /getProjectCodeAssignmentCandidatePage/)
assert.match(page, /currentProjectName/)
assert.match(page, /row\.selectable === false/)
assert.match(page, /审批中的文件不可创建修正任务，请先撤回或完成审批后处理/)
assert.doesNotMatch(page, /将按当前勾选的 \{\{ selectedAssociatedFileIds\.length \}\} 份文件生成快照/)

console.log('DCC project-code assignment global candidate static contract passed')
