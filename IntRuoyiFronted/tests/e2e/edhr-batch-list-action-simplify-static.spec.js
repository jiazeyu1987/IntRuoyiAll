const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const actionsMatch = source.match(/<div v-else class="edhr-batch-page__actions">([\s\S]*?)<\/div>/)
assert(actionsMatch, '批次列表必须保留默认行操作容器。')

const actionsBlock = actionsMatch[1]
const visibleLabels = [...actionsBlock.matchAll(/<el-button[\s\S]*?>([\s\S]*?)<\/el-button>/g)]
  .map((match) => match[1].replace(/\s+/g, ' ').trim())
  .filter(Boolean)

assert.deepEqual(
  visibleLabels,
  ['编辑', '作废'],
  '批次列表默认行操作区只能直接显示“编辑 / 作废”两个主按钮。'
)

for (const legacyLabel of [
  '去填写',
  '查看详情',
  '模板',
  '追溯',
  '流程追踪',
  '操作轨迹',
  '体验检查',
  '预检',
  '查看归档',
  '下载打印版 PDF',
  '打印'
]) {
  assert(
    !actionsBlock.includes(`>${legacyLabel}<`) && !actionsBlock.includes(`\n                  ${legacyLabel}\n`),
    `行操作区不应继续并列显示旧入口：${legacyLabel}`
  )
}

assert(
  actionsBlock.includes('@click="openDetail(row)"') &&
    actionsBlock.includes('>编辑</el-button>'),
  '编辑主按钮必须进入批次详情页。'
)

assert(
  actionsBlock.includes('@click="openVoidDialog(row)"') &&
    source.includes('const openVoidDialog = async (row: EdhrBatchExecutionRespVO) =>') &&
    source.includes('voidDialogVisible.value = true'),
  '作废主按钮必须打开批次执行作废流程弹窗。'
)

assert(
  !actionsBlock.includes('@click="openCurrentUserFillTask(row)"') &&
    !actionsBlock.includes('@click="openTemplate(row)"') &&
  !actionsBlock.includes('@click="handleDownloadArchive(row)"') &&
    !source.includes('const handleDownloadArchive = async'),
  '批次列表行操作区不应保留行内去填写、模板、打印或下载归档处理。'
)

console.log('PASS: eDHR batch list action simplify static contract')
