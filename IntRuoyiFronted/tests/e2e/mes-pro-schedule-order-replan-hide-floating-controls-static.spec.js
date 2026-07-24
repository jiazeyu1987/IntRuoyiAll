const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

const drawerStart = pageSource.indexOf('<el-drawer v-model="replanDrawerVisible"')
assert.notEqual(drawerStart, -1, '手动重排抽屉必须存在。')
const drawerEnd = pageSource.indexOf('\n    </el-drawer>', drawerStart)
assert.ok(drawerEnd > drawerStart, '手动重排抽屉范围必须可解析。')
const drawerSource = pageSource.slice(drawerStart, drawerEnd)

assert.doesNotMatch(
  drawerSource,
  /schedule-order-pool__tab-column-settings|#actions|show-column-settings|show-column-reset/,
  '手动重排抽屉内不得再渲染悬浮/重复显示字段控件。'
)

const preflightPanelStart = drawerSource.indexOf('class="schedule-order-pool__preflight-panel"')
assert.ok(preflightPanelStart >= 0, '手动重排抽屉必须保留排产前检查面板。')
const replanActionsStart = drawerSource.indexOf('class="schedule-order-pool__replan-actions"', preflightPanelStart)
assert.ok(replanActionsStart > preflightPanelStart, '排产前检查面板必须位于重排操作区之前。')
const preflightSource = drawerSource.slice(preflightPanelStart, replanActionsStart)

assert.match(
  preflightSource,
  /<el-table[\s\S]*?data-user-table-column-explicit[\s\S]*?:data="preflightResult\.issues"/,
  '排产前检查问题表必须显式声明列控隔离，避免触发全局悬浮显示字段控件。'
)

const previewStart = drawerSource.indexOf('<div v-if="replanPreview" class="schedule-order-pool__replan-summary">')
assert.ok(previewStart > replanActionsStart, '重排预览摘要必须位于重排操作区之后。')
const previewSource = drawerSource.slice(previewStart)

assert.match(
  previewSource,
  /<el-table[\s\S]*?data-user-table-column-explicit[\s\S]*?:data="replanIssueRows"/,
  '重排预览问题表必须显式声明列控隔离，避免触发全局悬浮显示字段控件。'
)

assert.match(
  drawerSource,
  /class="schedule-order-pool__replan-actions"[\s\S]*@click="applyReplan"[\s\S]*开始重排[\s\S]*@click="openReplanSettingsDialog"[\s\S]*设置/,
  '手动重排抽屉只保留开始重排和设置两个主操作入口。'
)

console.log('PASS: MES schedule order replan hides floating display-field controls')
