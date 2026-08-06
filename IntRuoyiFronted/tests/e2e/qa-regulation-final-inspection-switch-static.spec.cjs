const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)

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
    { label: '检验项目', name: 'items' }
  ],
  'QA navigation must remove the standalone inspection-rules tab.'
)

assert.doesNotMatch(
  qaSource,
  /qaActiveTab === 'rules'|data-qa-regulation-inspection-rules|table-key="mes\.qa\.regulation\.rules"/,
  'QA page must not keep the old standalone inspection-rules table.'
)
assert.doesNotMatch(
  qaSource,
  /<el-switch\s+v-model="row\.required"/,
  'First inspection and patrol inspection applicability must not remain row-editable.'
)

const itemsTitleIndex = qaSource.indexOf('<span>工序检验方法与抽样方案</span>')
assert.ok(itemsTitleIndex >= 0, 'Inspection item section title must exist.')
const itemsHeaderStartIndex = qaSource.lastIndexOf('<template #header>', itemsTitleIndex)
const itemsHeaderEndIndex = qaSource.indexOf('</template>', itemsTitleIndex)
assert.ok(
  itemsHeaderStartIndex >= 0 && itemsHeaderEndIndex > itemsHeaderStartIndex,
  'Inspection item header must be complete.'
)
const itemsHeaderSource = qaSource.slice(itemsHeaderStartIndex, itemsHeaderEndIndex + '</template>'.length)

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
  /type QaRegulationTabName = 'overview' \| 'items' \| 'verification'/,
  'QA tab state type must no longer include the removed rules tab.'
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
  /const normalizeQaInspectionTypeRules = \(rules: QaInspectionTypeRule\[\]\): QaInspectionTypeRule\[\] =>[\s\S]*rule\.key === 'FINAL'[\s\S]*\{\s*\.\.\.rule,\s*required: true\s*\}/,
  'First inspection and patrol inspection rules must be normalized to fixed required=true.'
)

console.log('PASS qa-regulation-final-inspection-switch-static')
