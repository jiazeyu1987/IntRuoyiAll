const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const feedbackPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
const productionFillPagePath = path.resolve(
  process.cwd(),
  'src/views/mes/pro/edhr-batch/BatchProductionFillPage.vue'
)
const pqcFillPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue')

for (const requiredPath of [feedbackPagePath, productionFillPagePath, pqcFillPagePath]) {
  assert(fs.existsSync(requiredPath), `必要页面必须存在：${requiredPath}`)
}

const feedbackPageSource = fs.readFileSync(feedbackPagePath, 'utf8')
const productionFillPageSource = fs.readFileSync(productionFillPagePath, 'utf8')
const pqcFillPageSource = fs.readFileSync(pqcFillPagePath, 'utf8')

const feedbackListStart = feedbackPageSource.indexOf('<ContentWrap v-if="activeTab === \'feedback\'">')
assert(feedbackListStart >= 0, '正式报工页签内容区域必须存在。')

const feedbackListEnd = feedbackPageSource.indexOf('</UnifiedListTemplate>', feedbackListStart)
assert(feedbackListEnd > feedbackListStart, '正式报工 UnifiedListTemplate 必须存在并闭合。')
const feedbackListSource = feedbackPageSource.slice(feedbackListStart, feedbackListEnd)

assert(
  feedbackListSource.includes('<UnifiedListTemplate'),
  '隐藏红框面板后仍必须保留正式报工列表。'
)
assert(
  feedbackListSource.includes('class="feedback-filter-action-relocation"'),
  '隐藏红框面板后仍必须保留正式报工筛选行操作区。'
)

assert(
  !feedbackListSource.includes('FrontlineFixedTemplatePanel'),
  '报工页不得继续渲染截图红框中的一线固定填报面板。'
)
assert(
  !feedbackPageSource.includes("import FrontlineFixedTemplatePanel from './FrontlineFixedTemplatePanel.vue'"),
  '报工页不得继续导入一线固定填报面板。'
)

assert(
  productionFillPageSource.includes('<FrontlineFixedTemplatePanel mode="production"'),
  '生产独立填报页必须继续保留生产模式一线固定填报面板。'
)
assert(
  pqcFillPageSource.includes('<FrontlineFixedTemplatePanel mode="pqc"'),
  'PQC 独立填报页必须继续保留 PQC 模式一线固定填报面板。'
)

assert(!feedbackPageSource.includes('catch {}'), '报工页不得吞掉异常。')
assert(!feedbackPageSource.includes('catch{}'), '报工页不得吞掉异常。')

console.log('PASS: MES feedback page hides frontline fixed template panel')
