const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const detailPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
)

assert.match(
  detailPage,
  /const RELEASE_ACTION_ERROR_AUTO_HIDE_DELAY_MS = 5000/,
  '放行错误提示必须定义 5 秒自动隐藏时长。'
)
assert.match(
  detailPage,
  /let releaseActionErrorAutoHideTimer: number \| undefined/,
  '放行错误提示必须持有浏览器定时器句柄，便于重复错误和卸载时清理，且不得引入 Node 定时器类型。'
)
assert.match(
  detailPage,
  /const clearReleaseActionErrorAutoHideTimer = \(\) => \{[\s\S]*window\.clearTimeout\(releaseActionErrorAutoHideTimer\)/,
  '放行错误提示必须提供定时器清理函数。'
)
assert.match(
  detailPage,
  /const clearReleaseActionError = \(\) => \{[\s\S]*clearReleaseActionErrorAutoHideTimer\(\)[\s\S]*releaseActionError\.value = ''/,
  '放行错误提示必须通过集中函数清空状态并同步清理定时器。'
)
assert.match(
  detailPage,
  /const showReleaseActionError = \(errorText: string\) => \{[\s\S]*releaseActionError\.value = errorText[\s\S]*window\.setTimeout\(\(\) => \{[\s\S]*if \(releaseActionError\.value === errorText\) \{[\s\S]*clearReleaseActionError\(\)[\s\S]*RELEASE_ACTION_ERROR_AUTO_HIDE_DELAY_MS/,
  '放行错误提示必须通过集中函数展示，并只在当前错误仍相同时于 5 秒后自动清空。'
)
assert.match(
  detailPage,
  /onBeforeUnmount\(clearReleaseActionErrorAutoHideTimer\)/,
  '放行错误提示自动隐藏定时器必须在组件卸载时清理。'
)
assert.doesNotMatch(
  detailPage,
  /releaseActionError\.value = (batchActionLocked|resolveErrorMessage|'当前批次|`)/,
  '放行错误提示不得绕过集中自动隐藏函数直接写入非空错误。'
)

console.log('PASS edhr release action error autohide static contract')
