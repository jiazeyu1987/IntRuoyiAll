import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()

const readText = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8')

test('approval tasks page exposes explicit controlled-file load error state', () => {
  const source = readText('src/views/dcc/controlled-file/approval-tasks/index.vue')
  assert.match(source, /dcc-controlled-file-approval-load-error/)
  assert.match(source, /categoryLoadErrorMessage/)
  assert.match(source, /listLoadErrorMessage/)
  assert.match(source, /resolveControlledFileReadErrorMessage/)
})

test('directories page exposes explicit controlled-file load error state', () => {
  const source = readText('src/views/dcc/controlled-file/directories/index.vue')
  assert.match(source, /dcc-controlled-file-directory-load-error/)
  assert.match(source, /loadErrorMessage/)
  assert.match(source, /resolveControlledFileReadErrorMessage/)
})

test('shared controlled-file error resolver treats generic backend messages as fallback candidates', () => {
  const source = readText('src/views/dcc/controlled-file/shared/utils.ts')
  assert.match(source, /GENERIC_CONTROLLED_FILE_ERROR_MESSAGES/)
  assert.match(source, /系统未知错误，请反馈给管理员/)
  assert.match(source, /服务器错误,请联系管理员!/)
  assert.match(source, /resolveControlledFileReadErrorMessage/)
})
