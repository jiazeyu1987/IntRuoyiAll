const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const projectRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(projectRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

const overviewStart = source.indexOf(
  '<ContentWrap v-show="selectedDccProjectCode && qaActiveTab === \'overview\'">'
)
const overviewEnd = source.indexOf('<ContentWrap v-show="qaActiveTab === \'items\'">', overviewStart)
assert.ok(overviewStart >= 0, 'QA overview section must exist.')
assert.ok(overviewEnd > overviewStart, 'QA overview section must have a stable end marker.')

const overviewSection = source.slice(overviewStart, overviewEnd)
const scopeCardEnd = overviewSection.indexOf('</el-card>')
const noteStart = overviewSection.indexOf('data-qa-regulation-overview-note')
assert.ok(scopeCardEnd >= 0, 'QA overview must retain the applicable-scope card.')
assert.ok(noteStart > scopeCardEnd, 'Overview note must render below the applicable-scope card.')

const noteSection = overviewSection.slice(noteStart)
assert.match(
  noteSection,
  /<template #header>\s*备注\s*<\/template>/,
  'Overview note must have the visible title 备注.'
)
assert.match(
  noteSection,
  /<ol[^>]*data-qa-regulation-overview-note-list/,
  'Overview note must use an ordered list for the four rules.'
)

const noteItems = [
  '设备初次开机、模具更换、参数调整、模具维修等需要按照抽样规则进行首件检验；',
  '首检如果发现不合格，及时向部门主管/领导汇报，待问题得到纠正后，生产稳定之后，重新进行首检，检验全部合格后，才可转入正常生产；',
  '如果样本量等于或超过批量，则进行100%检验；',
  '过程巡检应每班记录两次，上午和下午各一次，巡检过程中若发现产品不合格，应及时向部门主管反映不合格问题，并对之前生产的产品进行隔离，问题纠正之后，进行双倍检验，确认无异常之后，转入正常抽样。然后对之前生产的产品组织评审，根据评审结果对该批次产品进行处理。'
]

let previousIndex = -1
for (const [index, text] of noteItems.entries()) {
  const itemIndex = noteSection.indexOf(text)
  assert.ok(itemIndex > previousIndex, `Overview note item ${index + 1} must be complete and ordered.`)
  previousIndex = itemIndex
}

assert.match(
  noteSection,
  /class="qa-regulation-page__overview-note-list"[\s\S]*line-height|class="qa-regulation-page__overview-note-list"/,
  'Overview note list must expose a stable styling hook for readable wrapping.'
)

console.log('PASS QA regulation overview note static contract')
