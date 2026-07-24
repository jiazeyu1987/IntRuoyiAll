const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const viewMatrixDialog = fs.readFileSync(
  path.join(root, 'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixDialog.vue'),
  'utf8'
)

assert.ok(
  viewMatrixDialog.includes('schedulePreviewRefresh') &&
    viewMatrixDialog.includes('previewRefreshTimer') &&
    viewMatrixDialog.includes('clearScheduledPreviewRefresh'),
  '查看矩阵弹窗必须提供防抖自动刷新预览调度'
)

assert.ok(
  viewMatrixDialog.includes('watch(') &&
    viewMatrixDialog.includes('formRules') &&
    viewMatrixDialog.includes('deep: true') &&
    viewMatrixDialog.includes('previewAutoRefreshReady'),
  '查看矩阵弹窗必须监听规则变更后自动刷新有效权限预览'
)

assert.ok(
  viewMatrixDialog.includes('await refreshPreview()') &&
    viewMatrixDialog.includes('previewAutoRefreshReady.value = true'),
  '查看矩阵弹窗打开并完成初步对应后必须自动刷新有效权限预览'
)

assert.ok(
  viewMatrixDialog.includes('onBeforeUnmount') &&
    viewMatrixDialog.includes('clearScheduledPreviewRefresh()'),
  '查看矩阵弹窗必须在卸载或关闭时清理自动刷新计时器'
)

console.log('dcc view matrix auto preview static contract PASS')
