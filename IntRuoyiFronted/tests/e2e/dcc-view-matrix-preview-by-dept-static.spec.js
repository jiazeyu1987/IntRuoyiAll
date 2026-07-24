const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const viewMatrixDialog = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixDialog.vue'
)

assert.ok(
  viewMatrixDialog.includes('data-testid="dcc-view-matrix-dialog-effective-user-groups"'),
  '有效权限预览必须提供按部门/主体分组的容器'
)

assert.ok(
  viewMatrixDialog.includes('previewUserGroups') &&
    viewMatrixDialog.includes('resolvePreviewSubjectGroupTitle') &&
    viewMatrixDialog.includes('subjectDepartmentPath'),
  '有效权限预览必须按对应部门或主体来源聚合用户'
)

assert.ok(
  viewMatrixDialog.includes('view-matrix-user-group__title') &&
    viewMatrixDialog.includes('view-matrix-user-grid') &&
    viewMatrixDialog.includes('view-matrix-user-chip'),
  '有效权限预览每个部门分组内必须以 grid/chip 展示人名'
)

for (const removedPreviewCopy of [
  'label="来源规则"',
  'label="查阅标记"',
  'data-testid="dcc-view-matrix-dialog-risks"',
  'label="风险编码"',
  'label="主体 ID"'
]) {
  assert.ok(
    !viewMatrixDialog.includes(removedPreviewCopy),
    `有效权限预览不得继续展示旧技术列：${removedPreviewCopy}`
  )
}

console.log('dcc view matrix preview by department static contract PASS')
