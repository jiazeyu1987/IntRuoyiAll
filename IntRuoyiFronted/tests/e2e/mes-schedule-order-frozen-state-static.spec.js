const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), `排产工单页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')

const replanableMatch = source.match(
  /const isScheduleOrderReplanable = \(row: MesProScheduleOrderVO\) => \{([\s\S]*?)\n\}/
)
assert(replanableMatch, '排产工单页面必须集中定义手动重排资格。')
const replanableSource = replanableMatch[1]

assert(
  source.includes(':selectable="isScheduleOrderSelectable"') &&
    /const\s+isScheduleOrderSelectable\s*=\s*\([^)]*row[^)]*\)\s*=>\s*\{\s*return isScheduleOrderReplanable\(row\)\s*\}/.test(
      source
    ) &&
    replanableSource.includes('!row.frozen'),
  '排产工单主表 selection 必须绑定 isScheduleOrderSelectable，并禁止冻结行被勾选参与排产。'
)

assert(
  /selectedScheduleOrders\.value\s*=\s*rows\.filter\(\s*\(?\s*item\s*\)?\s*=>\s*isScheduleOrderReplanable\(item\)\s*\)/.test(
    source
  ),
  '排产工单选中集合必须复用统一重排资格再次过滤，避免冻结等不可重排工单进入集合。'
)

assert(
  /scheduleOrderList\.value\s*=\s*sortScheduleOrderListForDisplay\([^)]*data\.list/.test(source) &&
    /const\s+sortScheduleOrderListForDisplay\s*=/.test(source) &&
    /getScheduleOrderDisplaySortWeight/.test(source),
  '排产工单列表必须通过统一排序函数按未排产、已排产、冻结、已完成展示。'
)

assert(
  /row\.frozen[\s\S]*openUnfreezeDialog\(row\)[\s\S]*解冻/.test(source),
  '冻结排产工单行级操作必须显示解冻按钮。'
)

const actionColumnMatch = source.match(
  /<el-table-column label="操作"[\s\S]*?<template #default="\{ row \}">([\s\S]*?)<\/template>\s*<\/el-table-column>/
)
assert(actionColumnMatch, '排产工单主表必须存在操作列。')
const actionColumn = actionColumnMatch[1]

assert(
  actionColumn.includes('v-if="row.frozen"') && actionColumn.includes('v-else'),
  '排产工单操作列必须对冻结行和非冻结行使用互斥分支，确保冻结行只显示解冻。'
)

const frozenBranchMatch = actionColumn.match(
  /v-if="row\.frozen"[\s\S]*?(?=<template v-else>|<div v-else|v-else)/
)
assert(frozenBranchMatch, '排产工单操作列必须存在冻结行专用分支。')
const frozenBranch = frozenBranchMatch[0]
const actionButtonLabels = ['查看', '调整', '交期', '冻结', '完成', '撤销', '解冻']

assert(
  frozenBranch.includes('openUnfreezeDialog(row)') &&
    frozenBranch.includes('解冻') &&
    !frozenBranch.includes('openPriorityDialog(row)') &&
    !frozenBranch.includes('openPromiseDateDialog(row)') &&
    !frozenBranch.includes('openFreezeDialog(row)') &&
    !frozenBranch.includes('openManualFinishDialog(row)') &&
    !frozenBranch.includes('openRevokeManualFinishDialog(row)'),
  '冻结排产工单冻结分支只允许解冻操作，不得显示调整、设置交期、冻结、设为已完成或撤销已完成。'
)

assert(
  frozenBranch.includes(`v-hasPermi="['mes:pro-schedule-order:update']"`) &&
    !frozenBranch.includes('mes:pro-schedule-order:unfreeze'),
  '冻结排产工单行级解冻按钮必须复用后端解冻接口和批量解冻一致的 update 权限，避免按钮被不存在的 unfreeze 权限隐藏。'
)

for (const label of actionButtonLabels) {
  assert(actionColumn.includes(label), `排产工单操作列必须显示两字按钮：${label}`)
}

for (const longLabel of ['设置交期', '设为已完成', '撤销已完成']) {
  assert(
    !actionColumn.includes(longLabel),
    `排产工单操作列按钮文案必须压缩为两个字，不应出现：${longLabel}`
  )
}

assert(
  /\.schedule-order-pool__row-actions\s*\{[\s\S]*?flex-wrap:\s*wrap;[\s\S]*?max-height:\s*52px;[\s\S]*?\}/.test(
    source
  ),
  '排产工单操作按钮容器必须允许换行并限制为两行高度。'
)

assert(
  /\.schedule-order-pool__row-actions\s*\{[\s\S]*?width:\s*124px;[\s\S]*?\}/.test(source) &&
    /<el-table-column label="操作" width="140"[\s\S]*fixed="right">/.test(source),
  '排产工单操作列宽度必须只比三枚按钮总宽度略大，释放更多业务列展示空间。'
)

assert(
  /\.schedule-order-pool__row-actions\s*:deep\(\.el-button\)[\s\S]*?width:\s*34px;[\s\S]*?justify-content:\s*center;[\s\S]*?\}/.test(
    source
  ),
  '排产工单操作按钮必须使用固定窄宽度居中展示两个字。'
)

assert(
  !source.includes('schedule-order-pool__quantity-grid') &&
    !source.includes('row.completedQuantity') &&
    !source.includes('row.uncompletedQuantity') &&
    !source.includes('pendingApprovalQuantity') &&
    !source.includes('pendingInspectionQuantity') &&
    !source.includes('overReportedQuantity'),
  '排产工单数量/进度列必须隐藏完成、未完、待审批、待检、真实完工、超报明细，只保留总量、百分比和进度条。'
)

const mainTableMatch = source.match(
  /<el-table[\s\S]*?:data="scheduleOrderList"[\s\S]*?<\/el-table>/
)
assert(mainTableMatch, '排产工单主列表必须存在绑定 scheduleOrderList 的主表。')
const mainTable = mainTableMatch[0]

for (const hiddenColumn of ['状态', '差异', '风险', '冻结状态', '工艺路线', '路线版本']) {
  assert(
    !mainTable.includes(`<el-table-column label="${hiddenColumn}"`),
    `排产工单主列表必须隐藏黄框列：${hiddenColumn}`
  )
}

console.log('PASS: MES schedule order frozen state static contract')
