import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const targetFile = path.join(
  root,
  'src/views/dcc/controlled-file/directories/index.vue'
)

const source = fs.readFileSync(targetFile, 'utf8')

test('DCC 目录管理导入按钮显示 NAS同步 文案并保留原导入交互入口', () => {
  assert.match(
    source,
    /@click="handleImportFromIntAuth"/,
    'directory import button should keep the existing import handler'
  )
  assert.match(source, /NAS同步/, 'directory import button should show the NAS同步 label')
  assert.doesNotMatch(
    source,
    /从 IntAuth 导入目录树/,
    'legacy IntAuth wording should no longer be rendered in the button label'
  )
})
