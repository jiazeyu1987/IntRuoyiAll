const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue')

const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const tabsMarkerIndex = qaSource.indexOf('data-qa-regulation-tabs')
assert.ok(tabsMarkerIndex >= 0, 'QA page must render its owned tabs.')

const tabsStartIndex = qaSource.lastIndexOf('<el-tabs', tabsMarkerIndex)
const tabsEndIndex = qaSource.indexOf('</el-tabs>', tabsMarkerIndex)
assert.ok(tabsStartIndex >= 0 && tabsEndIndex > tabsStartIndex, 'QA tabs block must be complete.')

const tabsSource = qaSource.slice(tabsStartIndex, tabsEndIndex + '</el-tabs>'.length)
const paneTags = [...tabsSource.matchAll(/<el-tab-pane\b[^>]*\/>/g)].map((match) => match[0])
const panes = paneTags.map((tag) => ({
  label: tag.match(/\blabel="([^"]+)"/)?.[1],
  name: tag.match(/\bname="([^"]+)"/)?.[1]
}))

assert.deepEqual(
  panes,
  [
    { label: '总览', name: 'overview' },
    { label: '检验项目', name: 'items' },
    { label: '检验设备', name: 'equipment' },
    { label: '任务预览', name: 'verification' }
  ],
  'QA navigation must expose overview, item configuration, equipment configuration and formal task preview tabs.'
)

assert.doesNotMatch(
  qaSource,
  /qaActiveTab === 'rules'|data-qa-regulation-inspection-rules|table-key="mes\.qa\.regulation\.rules"/,
  'QA page must not keep the old standalone inspection-rules table.'
)
assert.doesNotMatch(
  qaSource,
  /<el-switch\s+v-model="row\.required"/,
  'The removed global inspection-rule rows must not return.'
)
assert.match(
  qaSource,
  /data-qa-regulation-first-inspection[\s\S]*v-model="row\.firstInspectionEnabled"/,
  'First-inspection applicability must be editable on each inspection item.'
)
assert.match(
  qaSource,
  /data-qa-regulation-patrol-inspection[\s\S]*v-model="row\.patrolInspectionEnabled"/,
  'Patrol applicability must be editable on each inspection item.'
)

const itemsTitleIndex = qaSource.indexOf('<span>工序检验方法与抽样方案</span>')
assert.ok(itemsTitleIndex >= 0, 'Inspection item section title must exist.')
const itemsHeaderStartIndex = qaSource.lastIndexOf('<template #header>', itemsTitleIndex)
const itemsHeaderEndIndex = qaSource.indexOf('</template>', itemsTitleIndex)
assert.ok(
  itemsHeaderStartIndex >= 0 && itemsHeaderEndIndex > itemsHeaderStartIndex,
  'Inspection item header must be complete.'
)
const itemsHeaderSource = qaSource.slice(
  itemsHeaderStartIndex,
  itemsHeaderEndIndex + '</template>'.length
)

assert.match(
  itemsHeaderSource,
  /qa-regulation-page__final-inspection-switch[\s\S]*data-qa-regulation-final-inspection-switch[\s\S]*是否需要末检[\s\S]*<el-switch[\s\S]*v-model="finalInspectionRequired"/,
  'Inspection item toolbar must expose the final-inspection switch in the red-box action area.'
)
assert.match(
  itemsHeaderSource,
  /data-qa-regulation-final-not-applicable-reason[\s\S]*v-if="!finalInspectionRequired"[\s\S]*v-model="finalInspectionNotApplicableReason"/,
  'Final-inspection switch must keep the formal not-applicable evidence input when disabled.'
)

assert.match(
  qaSource,
  /type QaRegulationTabName = 'overview' \| 'items' \| 'equipment' \| 'verification'/,
  'QA tab state type must include the equipment configuration tab and exclude the removed rules tab.'
)
assert.match(
  qaSource,
  /const finalInspectionRule = computed\(\(\) =>\s*qaInspectionTypeRules\.find\(\(rule\) => rule\.key === 'FINAL'\)\s*\)/,
  'Final switch must read the formal FINAL inspection rule.'
)
assert.match(
  qaSource,
  /const finalInspectionRequired = computed<boolean>\(\{[\s\S]*get:\s*\(\) => Boolean\(finalInspectionRule\.value\?\.required\)[\s\S]*set:\s*\(required: boolean\) => \{[\s\S]*finalInspectionRule\.value\.required = required/,
  'Final switch must write back to the formal FINAL inspection rule.'
)
assert.match(
  qaSource,
  /const finalInspectionNotApplicableReason = computed<string>\(\{[\s\S]*get:\s*\(\) => finalInspectionRule\.value\?\.notApplicableReason \?\? ''[\s\S]*set:\s*\(reason: string\) => \{[\s\S]*finalInspectionRule\.value\.notApplicableReason = reason/,
  'Final not-applicable reason must remain attached to the formal FINAL inspection rule.'
)
assert.match(
  qaSource,
  /const replaceQaInspectionTypeRules = \([\s\S]*rule\.key === 'FINAL' \? Boolean\(rule\.required\) : true/,
  'Global rule metadata must keep final project-owned while item applicability owns first and patrol.'
)

console.log('PASS qa-regulation-final-inspection-switch-static')
