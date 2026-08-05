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
const panes = paneTags.map((tag) => {
  const label = tag.match(/\blabel="([^"]+)"/)?.[1]
  const name = tag.match(/\bname="([^"]+)"/)?.[1]
  return { label, name }
})

assert.deepEqual(
  panes,
  [
    { label: '总览', name: 'overview' },
    { label: '检验规则', name: 'rules' },
    { label: '检验项目', name: 'items' }
  ],
  'QA navigation must expose exactly the three approved tabs.'
)
assert.doesNotMatch(
  tabsSource,
  /发布检查|name="verification"/,
  'QA navigation must not display the publish verification tab.'
)

for (const retainedPublishContract of [
  /const previewQaRegulationDraft = async \(\) =>/,
  /QcTemplateApi\.saveQaRegulationDraft\(payload\)/,
  /const runQaPublishPrecheck = async \(\) =>/,
  /QcTemplateApi\.publishQaRegulation\(payload\)/
]) {
  assert.match(
    qaSource,
    retainedPublishContract,
    'Hiding the tab must not remove or replace the existing draft/publish implementation.'
  )
}

console.log('PASS qa-regulation-publish-tab-hidden-static')
