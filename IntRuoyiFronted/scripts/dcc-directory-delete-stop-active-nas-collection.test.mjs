import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..')

const directoryApi = readFileSync(
  resolve(repoRoot, 'src/api/dcc/controlledFile/directories.ts'),
  'utf8'
)
const directoryPage = readFileSync(
  resolve(repoRoot, 'src/views/dcc/controlled-file/directories/index.vue'),
  'utf8'
)

assert.match(
  directoryApi,
  /getDirectoryActiveNasTransfer/,
  '目录 API 必须提供删除前查询 active 后台收集任务的方法'
)
assert.match(
  directoryApi,
  /stopDirectoryActiveNasTransfer/,
  '目录 API 必须提供停止 active 后台收集任务的方法'
)
assert.match(
  directoryPage,
  /getDirectoryActiveNasTransfer/,
  '删除父文件夹前必须查询 active 后台收集任务'
)
assert.match(
  directoryPage,
  /stopDirectoryActiveNasTransfer/,
  '发现 active 后台收集任务后必须支持停止收集'
)
assert.match(
  directoryPage,
  /后台收集/,
  '删除父文件夹交互必须提示后台收集仍在运行'
)
assert.match(
  directoryPage,
  /确认停止/,
  '删除父文件夹交互必须要求用户确认停止后台收集'
)
