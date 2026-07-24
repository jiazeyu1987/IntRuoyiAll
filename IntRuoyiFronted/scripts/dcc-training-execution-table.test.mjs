import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('training execution table shows fileName column and formats acknowledged date as YYYY-MM-DD', () => {
  const source = readText('src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue')

  assert.match(source, /label="文件标题"/)
  assert.match(source, /label="文件名称"/)
  assert.match(source, /prop="fileName"/)
  assert.match(source, /label="确认完成时间"[\s\S]*?:formatter="dateFormatter2"/)
  assert.match(source, /from '\@\/utils\/formatTime'/)
})
