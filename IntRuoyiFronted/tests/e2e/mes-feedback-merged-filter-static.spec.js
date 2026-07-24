const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
assert(fs.existsSync(pagePath), `报工页面必须存在：${pagePath}`)

const source = fs.readFileSync(pagePath, 'utf8')
const feedbackFilterStart = source.indexOf('<ContentWrap v-if="activeTab === \'feedback\'">')
const feedbackFilterEnd = source.indexOf('<ContentWrap v-else>', feedbackFilterStart)
assert(feedbackFilterStart >= 0, '正式报工筛选块必须存在。')
assert(feedbackFilterEnd > feedbackFilterStart, '正式报工筛选块必须位于待归属筛选块之前。')
const feedbackFilterSource = source.slice(feedbackFilterStart, feedbackFilterEnd)

assert(source.includes('feedbackFilterFields'), '正式报工筛选必须通过 feedbackFilterFields 集中声明可选筛选类型。')
assert(source.includes('activeFeedbackFilter'), '正式报工筛选必须维护当前筛选类型 activeFeedbackFilter。')
assert(source.includes('clearInactiveFeedbackFilters'), '搜索前必须清理未选中的旧查询参数，避免多个旧筛选同时生效。')
assert(source.includes('handleFeedbackFilterChange'), '切换筛选类型时必须重置动态筛选值。')
assert(source.includes('selectedFeedbackFilterField'), '动态输入区域必须根据当前筛选类型计算控件。')

for (const field of ['id', 'code', 'type', 'workOrderId', 'itemId', 'feedbackUserId', 'creator', 'status', 'feedbackTime']) {
  assert(
    source.includes(`key: '${field}'`) || source.includes(`key: "${field}"`),
    `合并筛选必须保留旧查询字段：${field}`
  )
}

for (const label of ['报工编号', '报工单号', '报工类型', '生产工单', '产品物料', '报工人', '记录人', '状态', '报工时间']) {
  assert(source.includes(label), `筛选类型下拉必须保留字段文案：${label}`)
}

assert(feedbackFilterSource.includes('label="筛选类型"'), '筛选栏必须显示“筛选类型”下拉。')
assert(feedbackFilterSource.includes('label="筛选值"'), '筛选栏必须显示统一“筛选值”动态输入。')
assert(feedbackFilterSource.includes('v-if="selectedFeedbackFilterField?.component === \'text\'"'), '文本字段必须渲染文本输入。')
assert(feedbackFilterSource.includes('v-else-if="selectedFeedbackFilterField?.component === \'feedbackType\'"'), '报工类型必须渲染字典下拉。')
assert(feedbackFilterSource.includes('v-else-if="selectedFeedbackFilterField?.component === \'status\'"'), '状态必须渲染字典下拉。')
assert(feedbackFilterSource.includes('v-else-if="selectedFeedbackFilterField?.component === \'workOrder\'"'), '生产工单必须复用 ProWorkOrderSelect。')
assert(feedbackFilterSource.includes('v-else-if="selectedFeedbackFilterField?.component === \'item\'"'), '产品物料必须复用 MdItemSelect。')
assert(feedbackFilterSource.includes('v-else-if="activeFeedbackFilter === \'feedbackUserId\'"'), '报工人必须复用 UserSelectV2。')
assert(feedbackFilterSource.includes('v-else-if="activeFeedbackFilter === \'creator\'"'), '记录人必须复用 UserSelectV2。')
assert(feedbackFilterSource.includes('v-else-if="selectedFeedbackFilterField?.component === \'dateRange\'"'), '报工时间必须渲染日期范围。')

for (const legacyProp of ['prop="id"', 'prop="code"', 'prop="type"', 'prop="workOrderId"', 'prop="itemId"', 'prop="feedbackUserId"', 'prop="creator"', 'prop="status"', 'prop="feedbackTime"']) {
  assert(!feedbackFilterSource.includes(legacyProp), `正式报工筛选不得继续平铺旧字段：${legacyProp}`)
}

assert(!source.includes('catch {}'), '报工页不得吞掉异常。')
assert(!source.includes('catch{}'), '报工页不得吞掉异常。')

console.log('PASS: MES feedback merged filter static contract')
