const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const sourcePath = path.join(root, 'src/views/dcc/controlled-file/browser/index.vue')
const source = fs.readFileSync(sourcePath, 'utf8')

const templateMatch = source.match(
  /<UnifiedListTemplate[\s\S]*?table-key="dcc\.controlledFile\.browser\.main"[\s\S]*?<\/UnifiedListTemplate>/
)
assert.ok(templateMatch, '文件查阅列表必须继续使用标准列表模板。')
const template = templateMatch[0]

const actionsStart = template.indexOf('<template #actions>')
const tableStart = template.search(/<template\s+#table\b[^>]*>/)
assert.notEqual(actionsStart, -1, '文件查阅列表必须保留标准模板操作区。')
assert.ok(tableStart > actionsStart, '文件查阅列表操作区必须位于表格插槽之前。')
const actions = template.slice(actionsStart, tableStart)

assert.match(actions, /<el-popover[\s\S]*trigger="click"[\s\S]*v-model:visible="advancedActionsVisible"/, '截图按钮必须收纳到点击触发的高级弹框中。')
assert.match(actions, /<template #reference>[\s\S]*高级[\s\S]*<\/template>/, '操作区必须只暴露“高级”入口按钮。')
assert.match(actions, /class="browser-advanced-actions"/, '高级弹框内必须使用专用布局容器重新排布按钮。')

const advancedMatch = actions.match(/<div class="browser-advanced-actions">([\s\S]*?)<\/div>/)
assert.ok(advancedMatch, '高级弹框内必须包含按钮网格。')
const advancedActions = advancedMatch[1]
const outsideAdvancedActions = actions.replace(advancedMatch[0], '')

for (const forbidden of [
  '刷新列表',
  '导出文件名/编号',
  '导入文件名/编号',
  '导出识别记录',
  '导出识别迁移包',
  '导入识别迁移包',
  '识别当前文件夹及子文件夹'
]) {
  assert.doesNotMatch(actions, new RegExp(forbidden), `高级按钮区不得显示超过 4 个字的按钮文案：${forbidden}`)
}

for (const required of [
  '导出名编',
  '导入名编',
  '导出记录',
  '导出迁移',
  '导入迁移',
  '批量识别',
  '识别编号'
]) {
  assert.match(advancedActions, new RegExp(required), `高级弹框必须包含短文案按钮：${required}`)
}

const buttonMatches = [...advancedActions.matchAll(/<el-button[\s\S]*?>([\s\S]*?)<\/el-button>/g)]
assert.equal(buttonMatches.length, 8, '高级弹框必须只展示保留的 8 个业务按钮。')
for (const match of buttonMatches) {
  const text = match[1]
    .replace(/<Icon[\s\S]*?\/>/g, '')
    .replace(/<[^>]+>/g, '')
    .replace(/\s+/g, '')
    .trim()
  assert.ok(text, '高级弹框按钮必须有可见文字。')
  assert.ok(
    [...text].length <= 4 || text === '后缀黑名单',
    `高级弹框按钮文字不得超过 4 个字：${text}`
  )
}

for (const [handler, message] of [
  ['handleAdvancedAction(handleMetadataExport)', '导出名编按钮必须复用原文件名/编号导出逻辑。'],
  ['handleAdvancedAction(openMetadataImportDialog)', '导入名编按钮必须复用原文件名/编号导入逻辑。'],
  ['handleAdvancedAction(handleRecognitionRecordExport)', '导出记录按钮必须复用原识别记录导出逻辑。'],
  ['handleAdvancedAction(handleRecognitionMigrationExport)', '导出迁移按钮必须复用原识别迁移包导出逻辑。'],
  ['handleAdvancedAction(openRecognitionMigrationImportDialog)', '导入迁移按钮必须复用原识别迁移包导入逻辑。'],
  ['handleAdvancedAction(openBatchRecognitionDialog)', '批量识别按钮必须复用原批量识别逻辑。'],
  ['handleAdvancedAction(openFileNumberRecognitionDialog)', '识别编号按钮必须打开文件编号批量识别弹框。']
]) {
  assert.match(advancedActions, new RegExp(`@click="${handler.replace(/[()]/g, '\\$&')}"`), message)
}

assert.match(advancedActions, /:loading="metadataExporting"/, '导出名编按钮必须保留导出加载态。')
assert.match(advancedActions, /:loading="recognitionRecordExporting"/, '导出记录按钮必须保留导出加载态。')
assert.match(advancedActions, /:loading="recognitionMigrationExporting"/, '导出迁移按钮必须保留导出加载态。')
assert.match(advancedActions, /:loading="batchRecognitionCreating"/, '批量识别按钮必须保留创建加载态。')
assert.equal((advancedActions.match(/v-if="canEditMetadata"/g) || []).length, 7, '元数据和识别操作必须保留原权限控制。')
assert.doesNotMatch(
  advancedActions,
  /handleAdvancedAction\(handleQuery\)|handleAdvancedAction\(resetQuery\)|handleAdvancedAction\(refreshList\)/,
  '高级弹框不得继续显示查询、重置、刷新入口。'
)

for (const directHandler of [
  'handleQuery',
  'resetQuery',
  'refreshList',
  'handleMetadataExport',
  'openMetadataImportDialog',
  'handleRecognitionRecordExport',
  'handleRecognitionMigrationExport',
  'openRecognitionMigrationImportDialog',
  'openBatchRecognitionDialog',
  'openFileNumberRecognitionDialog'
]) {
  assert.doesNotMatch(outsideAdvancedActions, new RegExp(`@click="[^"]*${directHandler}`), `截图按钮不得继续直接铺在工具栏：${directHandler}`)
}

assert.match(source, /const advancedActionsVisible = ref\(false\)/, '高级弹框必须有显式可控显示状态。')
assert.match(source, /const handleAdvancedAction = async \(action: \(\) => void \| Promise<void>\)/, '高级弹框按钮必须通过统一入口关闭弹框并执行原动作。')

console.log('PASS: dcc browser advanced actions static contract')
