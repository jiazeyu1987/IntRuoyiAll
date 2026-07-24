import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')

const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const detailPageSource = () => readText('src/views/mes/pro/edhr/ExecutionPage.vue')
const archiveApiSource = () => readText('src/api/mes/pro/edhr/archive.ts')

const archiveGateSource = (source) => {
  const lines = source.split(/\r?\n/)
  const selected = new Set()
  lines.forEach((line, index) => {
    if (/archive|归档|canGenerateArchive|showArchiveGenerateAction|openArchiveGenerateDialog|handleGenerateArchive|canDownload/i.test(line)) {
      for (let current = Math.max(0, index - 10); current <= Math.min(lines.length - 1, index + 14); current += 1) {
        selected.add(current)
      }
    }
  })
  return [...selected].sort((left, right) => left - right).map((index) => lines[index]).join('\n')
}

test('BDD: 未审批禁止归档 -> SUBMITTED 执行记录不能打开生成归档弹窗或调用归档 API', () => {
  const source = `${readText('src/api/mes/pro/edhr/approval.ts')}\n${detailPageSource()}`
  const gateSource = archiveGateSource(source)

  assert.match(source, /EDHR_EXECUTION_STATUS[\s\S]*APPROVED:\s*3/, '前端必须声明 APPROVED=3 关闭终态')
  assert.match(
    gateSource,
    /status\s*===\s*EDHR_EXECUTION_STATUS\.APPROVED|isApproved/,
    '生成归档按钮必须以 APPROVED 关闭终态为必要条件'
  )
  assert.match(
    gateSource,
    /closedAt/,
    '生成归档按钮必须校验后端关闭证据 closedAt'
  )
  assert.match(
    gateSource,
    /canGenerateArchive\s*===\s*true|\.canGenerateArchive/,
    '生成归档按钮必须校验后端 canGenerateArchive 字段'
  )
  assert.match(
    gateSource,
    /审批关闭后才可归档|只有审批关闭后的 eDHR 执行记录才允许归档/,
    'SUBMITTED 状态必须提示审批关闭后才可归档'
  )
  assert.doesNotMatch(
    gateSource,
    /status\s*!==\s*EXECUTION_STATUS_SUBMITTED|status\s*===\s*EXECUTION_STATUS_SUBMITTED|isSubmitted/,
    '归档生成门槛不得继续使用 SUBMITTED'
  )
})

test('BDD: 归档门槛类型 -> 执行表单数据包含后端归档门槛字段', () => {
  const apiSource = `${readText('src/api/mes/pro/feedback/index.ts')}\n${archiveApiSource()}`

  assert.match(apiSource, /canGenerateArchive\??:\s*boolean/, '执行表单 VO 必须包含 canGenerateArchive')
  assert.match(apiSource, /closedAt\??:\s*string/, '执行表单 VO 必须包含 closedAt')
  assert.match(apiSource, /canDownloadArchive\??:\s*boolean/, '归档下载必须由后端 canDownloadArchive 显式放行')
})
