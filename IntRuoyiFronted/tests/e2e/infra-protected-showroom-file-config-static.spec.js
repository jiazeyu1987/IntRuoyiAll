const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const fileConfigPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/infra/fileConfig/index.vue'),
  'utf8'
)
const filePage = fs.readFileSync(path.join(repoRoot, 'src/views/infra/file/index.vue'), 'utf8')

function assertIncludes(source, fragment, label) {
  assert(source.includes(fragment), `missing ${label}: ${fragment}`)
}

for (const fragment of [
  'PROTECTED_SHOWROOM_FILE_CONFIG_ID = 28',
  'isProtectedShowroomFileConfig',
  ':selectable="isFileConfigRowSelectable"',
  ':disabled="checkedIds.length === 0 || hasProtectedCheckedFileConfig"',
  ':disabled="isProtectedShowroomFileConfig(scope.row)"',
  'scope.row.master || isProtectedShowroomFileConfig(scope.row)',
  '展厅固定文件配置 28 受保护，禁止修改',
  '受保护'
]) {
  assertIncludes(fileConfigPage, fragment, `file config protected contract ${fragment}`)
}

for (const fragment of [
  'PROTECTED_SHOWROOM_FILE_CONFIG_ID = 28',
  "PROTECTED_SHOWROOM_PATH_PREFIX = 'showroom/'",
  'isProtectedShowroomFile',
  ':selectable="isFileRowSelectable"',
  ':disabled="checkedIds.length === 0 || hasProtectedCheckedFile"',
  ':disabled="isProtectedShowroomFile(scope.row)"',
  'handleDelete(scope.row)',
  '展厅文件配置 28 的 showroom/ 媒体受保护，禁止在文件管理页删除',
  '受保护'
]) {
  assertIncludes(filePage, fragment, `file protected contract ${fragment}`)
}

console.log('PASS: infra protected showroom file config static contract is wired')
