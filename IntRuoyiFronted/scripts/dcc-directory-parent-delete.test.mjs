import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const apiFile = path.join(root, 'src/api/dcc/controlledFile/directories.ts')
const pageFile = path.join(root, 'src/views/dcc/controlled-file/directories/index.vue')

const apiSource = fs.readFileSync(apiFile, 'utf8')
const pageSource = fs.readFileSync(pageFile, 'utf8')

test('DCC 目录 API 暴露删除父文件夹接口并提交 PROD 确认文本', () => {
  assert.match(apiSource, /interface\s+ControlledFileDirectoryDeleteSubtreeRespVO/)
  assert.match(apiSource, /deleteDirectorySubtree/)
  assert.match(apiSource, /\/dcc\/directories\/\$\{id\}\/delete-subtree/)
  assert.match(apiSource, /confirmText:\s*string/)
})

test('DCC 目录管理页提供删除父文件夹按钮和 PROD 二次确认', () => {
  assert.match(pageSource, /删除父文件夹/)
  assert.match(pageSource, /handleDeleteParentFolder/)
  assert.match(pageSource, /deleteDirectorySubtree/)
  assert.match(pageSource, /deleteConfirmText/)
  assert.match(pageSource, /PROD/)
  assert.match(pageSource, /:disabled="deleteConfirmText\.trim\(\) !== 'PROD'"/)
  assert.match(pageSource, /v-hasPermi="\['dcc:controlled-file:directory:manage'\]"/)
})
