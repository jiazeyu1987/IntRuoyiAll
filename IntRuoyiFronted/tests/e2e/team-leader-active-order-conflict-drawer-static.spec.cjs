const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const normalize = (source) => source.replace(/\r\n/g, '\n')
const source = normalize(page)

const sliceByMarker = (marker, startToken, endToken) => {
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `Expected marker: ${marker}`)
  const start = source.lastIndexOf(startToken, markerIndex)
  const end = source.indexOf(endToken, markerIndex)
  assert.notEqual(start, -1, `Expected start token ${startToken} for ${marker}`)
  assert.notEqual(end, -1, `Expected end token ${endToken} for ${marker}`)
  return source.slice(start, end + endToken.length)
}

const sliceFunction = (functionName, nextFunctionName) => {
  const start = source.indexOf(`const ${functionName}`)
  assert.notEqual(start, -1, `Expected function ${functionName}`)
  const end = source.indexOf(`const ${nextFunctionName}`, start)
  assert.notEqual(end, -1, `Expected next function ${nextFunctionName}`)
  return source.slice(start, end)
}

const conflictTag = sliceByMarker(
  'data-team-leader-active-order-quantity-conflict',
  '<el-tag',
  '</el-tag>'
)
assert.match(
  conflictTag,
  /@click\.stop="openActiveOrderConflictDrawer\(row\)"/,
  '数量冲突标签必须可点击并打开当前订单的冲突处理抽屉。'
)
assert.match(conflictTag, /role="button"/, '数量冲突标签必须具备按钮语义。')
assert.match(
  conflictTag,
  /@keydown\.enter\.prevent\.stop="openActiveOrderConflictDrawer\(row\)"[\s\S]*@keydown\.space\.prevent\.stop="openActiveOrderConflictDrawer\(row\)"/,
  '数量冲突标签必须支持键盘 Enter/Space 打开。'
)
assert.match(conflictTag, /title="点击处理数量冲突"/, '数量冲突标签必须提示可处理。')

const drawer = sliceByMarker(
  'data-team-leader-active-order-conflict-drawer',
  '<el-drawer',
  '</el-drawer>'
)
assert.match(
  drawer,
  /v-model="activeOrderConflictDrawerVisible"[\s\S]*data-team-leader-active-order-conflict-drawer/,
  '冲突处理必须使用独立抽屉状态。'
)
assert.match(
  drawer,
  /v-loading="activeOrderConflictLoading"[\s\S]*activeOrderConflictError[\s\S]*retryActiveOrderConflictDetail/,
  '冲突抽屉必须覆盖加载、错误和重试。'
)
assert.match(
  drawer,
  /data-team-leader-active-order-conflict-summary[\s\S]*冲突工序[\s\S]*正常工序/,
  '冲突抽屉顶部必须汇总冲突工序和正常工序数量。'
)
assert.match(
  drawer,
  /data-team-leader-active-order-conflict-recommended-action[\s\S]*按推荐方案修复/,
  '冲突抽屉必须提供一个推荐修复主按钮，简化重建和模拟完成操作。'
)
assert.match(
  drawer,
  /v-for="process in resolveActiveOrderConflictProcesses\(activeOrderConflictDetail\)"[\s\S]*data-team-leader-active-order-conflict-process[\s\S]*冲突工序[\s\S]*无冲突/,
  '冲突抽屉必须按工序逐项展示，并区分冲突工序与无冲突工序。'
)
for (const label of ['应提数量', '已提交', '超出数量', '提交记录']) {
  assert.match(drawer, new RegExp(label), `冲突抽屉每个工序必须显示${label}。`)
}

const openHandler = sliceFunction('openActiveOrderConflictDrawer', 'retryActiveOrderConflictDetail')
assert.match(
  openHandler,
  /activeOrderConflictDrawerVisible\.value\s*=\s*true[\s\S]*await\s+loadActiveOrderConflictDetail\(activeOrderId\)/,
  '点击冲突标签必须先打开抽屉，再加载当前活跃订单的正式详情。'
)

const recommendedHandler = sliceFunction(
  'handleRecommendedActiveOrderConflictResolution',
  'handleRemoveActiveOrder'
)
assert.match(
  recommendedHandler,
  /previewTeamLeaderActiveOrderRebuild[\s\S]*rebuildTeamLeaderActiveOrder[\s\S]*simulateTeamLeaderActiveOrderCompletion[\s\S]*await loadActiveOrders\(\)/,
  '自动分配必须点击后直接预检、重建、模拟完成并刷新列表。'
)
assert.doesNotMatch(
  recommendedHandler,
  /ElMessageBox\.confirm/,
  '自动分配点击后不得再次弹出确认框。'
)
assert.match(
  recommendedHandler,
  /writeCompletedPhase[\s\S]*重建已完成，但模拟完成失败[\s\S]*写入已完成，但列表刷新失败/,
  '推荐修复必须区分重建成功后模拟失败、以及写入成功后刷新失败。'
)

console.log('PASS: team leader active-order conflict drawer static contract')
