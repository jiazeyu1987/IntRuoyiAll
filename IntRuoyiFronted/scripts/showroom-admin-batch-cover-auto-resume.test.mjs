import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('showroom-admin batch cover api contract exposes auto-resume metadata', () => {
  const source = readText('src/api/showroom-admin/index.ts')

  assert.match(source, /taskId\?: number \| null/)
  assert.match(source, /taskStatus\?: 'WAITING' \| 'RUNNING' \| 'COMPLETED' \| string/)
  assert.match(source, /remainingPendingCount\?: number/)
  assert.match(source, /nextCheckAt\?: string \| null/)
})

test('showroom-admin batch cover fixed task area exposes backend auto-resume status', () => {
  const source = readText('src/views/showroom-admin/index.vue')
  const listSource = readText('src/views/showroom-admin/components/ProductListTable.vue')

  assert.match(source, /已开启后台定时检查，每 10 分钟自动续跑，全部完成后自动停止/)
  assert.match(source, /latestProductCoverTaskSummary/)
  assert.match(source, /remainingPendingCount/)
  assert.match(source, /nextCheckAt/)
  assert.match(source, /taskStatus/)
  assert.match(listSource, /一键封面任务/)
  assert.match(listSource, /剩余未完成/)
  assert.match(listSource, /下一次检查/)
})
