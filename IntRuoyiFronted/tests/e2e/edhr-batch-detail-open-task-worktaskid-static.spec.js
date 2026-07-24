const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const detailPage = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

const extractArrowBody = (source, signature) => {
  const start = source.indexOf(signature)
  assert.ok(start >= 0, `missing signature: ${signature}`)
  const nextConst = source.indexOf('\nconst ', start + signature.length)
  return source.slice(start, nextConst > start ? nextConst : source.length)
}

const canOpenTaskBody = extractArrowBody(
  detailPage,
  'const canOpenTask = (row: EdhrBatchExecutionTaskRespVO) =>'
)
const handleOpenTaskBody = extractArrowBody(
  detailPage,
  'const handleOpenTask = async ('
)

assert.match(
  canOpenTaskBody,
  /hasAllowedTaskAction\(row,\s*['"]OPEN_FORM['"]\)/,
  '批次详情显示填写入口必须继续依赖后端返回的 OPEN_FORM 动作。'
)

assert.match(
  canOpenTaskBody,
  /hasActiveWorkTask\(row\)|row\.activeWorkTaskId/,
  '批次详情显示填写入口必须同时要求 activeWorkTaskId，不能只有 OPEN_FORM 就允许点击。'
)

assert.match(
  handleOpenTaskBody,
  /workTaskId\s*:\s*row\.activeWorkTaskId/,
  '批次详情打开批次填写任务时必须把 row.activeWorkTaskId 作为 workTaskId 传给 openEdhrBatchTask。'
)

assert.match(
  handleOpenTaskBody,
  /if\s*\(\s*!row\.activeWorkTaskId\s*\)|if\s*\(\s*!hasActiveWorkTask\(row\)\s*\)/,
  '批次详情缺 activeWorkTaskId 时必须先阻断，不得继续调用后端打开任务。'
)

console.log('PASS: eDHR batch detail open task requires active workTaskId')
