const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const projectRoot = path.resolve(repoRoot, '..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const mapperPath = path.join(
  projectRoot,
  'ruoyi-vue-pro/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java'
)

const page = fs.readFileSync(pagePath, 'utf8')
const mapper = fs.readFileSync(mapperPath, 'utf8')

const statusQuickFilterMatch = page.match(
  /key:\s*'status'[\s\S]*?options:\s*\[([\s\S]*?)\]\s*\}/
)
assert.ok(statusQuickFilterMatch, '批次执行列表必须声明状态快速过滤。')

assert.doesNotMatch(
  statusQuickFilterMatch[1],
  /EDHR_BATCH_STATUS_VOIDED|已作废/,
  '批次执行列表状态筛选不得提供“已作废”，作废记录应进入变更与异常列表。'
)

assert.match(
  page,
  /const normalizeBatchStatusValue = \([\s\S]*String\(status\)\.trim\(\)[\s\S]*Number\(trimmed\)[\s\S]*'VOIDED'[\s\S]*EDHR_BATCH_STATUS_VOIDED/,
  '批次执行列表必须先统一归一化数字、数字字符串和 VOIDED 状态，避免已作废行穿透展示边界。'
)

assert.match(
  page,
  /const isVoidedBatchExecutionStatus = \([\s\S]*normalizeBatchStatusValue\(status\) === EDHR_BATCH_STATUS_VOIDED/,
  '批次执行列表必须通过统一状态判断识别已作废批次。'
)

assert.match(
  page,
  /const isVisibleBatchExecutionRow = \(row: EdhrBatchExecutionRespVO\) =>[\s\S]*!isVoidedBatchExecutionStatus\(row\.status\)/,
  '批次执行列表必须在展示边界排除后端旧包或缓存返回的已作废行。'
)

assert.match(
  page,
  /const normalizeBatchExecutionQuery = \(\) =>[\s\S]*queryParams\.status === EDHR_BATCH_STATUS_VOIDED[\s\S]*queryParams\.status = undefined[\s\S]*queryParams\.quickFilter[\s\S]*EDHR_BATCH_STATUS_VOIDED[\s\S]*queryParams\.quickFilter = undefined/,
  '批次执行查询前必须清理已作废状态筛选和历史快速过滤缓存。'
)

assert.match(
  page,
  /list\.value = \(data\.list \|\| \[\]\)\.filter\(isVisibleBatchExecutionRow\)/,
  '批次执行列表赋值必须过滤已作废行。'
)

assert.match(
  mapper,
  /queryWrapper\.notIn\(MesProEdhrBatchExecutionDO::getStatus, BATCH_STATUS_VOIDED\)/,
  '后端批次执行分页必须排除已作废状态。'
)

console.log('edhr batch execution obsolete visibility static contract passed')
