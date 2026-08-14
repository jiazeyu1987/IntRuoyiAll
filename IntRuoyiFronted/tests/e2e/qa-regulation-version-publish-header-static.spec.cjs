const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const headerStart = qaSource.indexOf('<div class="qa-regulation-page__header">')
const loadErrorStart = qaSource.indexOf('v-if="dccProjectCodeLoadError"', headerStart)
const headerEnd = qaSource.lastIndexOf('</div>', loadErrorStart)
const overviewStart = qaSource.indexOf(
  '<ContentWrap v-show="selectedDccProjectCode && qaActiveTab === \'overview\'">'
)
const overviewEnd = qaSource.indexOf(
  '<ContentWrap v-show="qaActiveTab === \'rules\'">',
  overviewStart
)
const verificationStart = qaSource.indexOf(
  '<ContentWrap v-show="qaActiveTab === \'verification\'">'
)
const verificationEnd = qaSource.indexOf('<script setup', verificationStart)

assert.ok(headerStart >= 0 && headerEnd > headerStart, 'QA title bar must exist.')
assert.ok(overviewStart >= 0 && overviewEnd > overviewStart, 'QA overview section must exist.')
assert.ok(
  verificationStart >= 0 && verificationEnd > verificationStart,
  'QA hidden verification section must remain structurally complete.'
)

const header = qaSource.slice(headerStart, headerEnd)
const overview = qaSource.slice(overviewStart, overviewEnd)
const verification = qaSource.slice(verificationStart, verificationEnd)

assert.match(
  header,
  /data-qa-regulation-version-publish/,
  'QA title bar must provide one owned version and publish action group.'
)
assert.match(
  header,
  /aria-label="规程版本"/,
  'QA title bar must label the regulation version input.'
)
assert.match(
  header,
  /v-model="qaRegulationDraft\.versionNo"/,
  'QA title bar must keep the regulation version binding.'
)
assert.match(
  header,
  /aria-label="生效日期"/,
  'QA title bar must label the effective date input.'
)
assert.match(
  header,
  /v-model="qaRegulationDraft\.effectiveDate"/,
  'QA title bar must keep the effective date binding.'
)
assert.match(
  header,
  /\{\{\s*qaRegulationDraft\.lifecycleStatus\s*\}\}[\s\S]*@click="runQaPublishPrecheck"[\s\S]*发布规程/,
  'DRAFT status and the publish button must be adjacent in the title-bar action group.'
)
assert.match(
  header,
  /:loading="qaRegulationPublishing"/,
  'The moved publish button must keep the formal publishing loading state.'
)
assert.doesNotMatch(
  overview,
  /v-model="qaRegulationDraft\.(?:versionNo|effectiveDate)"/,
  'The overview must not duplicate version fields after they move to the title bar.'
)
assert.doesNotMatch(
  verification,
  /@click="runQaPublishPrecheck"[\s\S]*发布规程/,
  'The hidden verification section must not retain a duplicate publish button.'
)
assert.match(
  qaSource,
  /\.qa-regulation-page__version-publish\s*\{[\s\S]*display:\s*flex[\s\S]*align-items:\s*center[\s\S]*gap:/,
  'The version and publish group must use a compact aligned layout.'
)

console.log('PASS qa-regulation-version-publish-header-static')
