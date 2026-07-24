import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('user page exposes delete-current-organization action tied to tree selection', () => {
  const source = readText('src/views/system/user/index.vue')

  assert.match(source, /删除组织/)
  assert.doesNotMatch(source, /删除当前组织/)
  assert.match(source, /handleDeleteSelectedDept/)
  assert.match(source, /DeptApi\.deleteDept\(/)
  assert.match(source, /selectedDeptId/)
  assert.match(source, /deptTreeRenderKey/)
})
