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
const overview = [...qaSource.matchAll(/<ContentWrap\b[\s\S]*?<\/ContentWrap>/g)]
  .map((match) => match[0]).find((wrap) => wrap.includes('data-qa-regulation-scope'))
assert.ok(headerStart >= 0 && headerEnd > headerStart, 'QA title bar must exist.')
assert.ok(overview && overview.includes("qaActiveTab === 'overview'"), 'QA overview section must exist.')
const header = qaSource.slice(headerStart, headerEnd)

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
  /v-model="selectedQaRegulationVersionId"/,
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
  /qaSelectedVersionStatusText[\s\S]*@click="runQaPublishPrecheck"[\s\S]*发布规程/,
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
assert.equal((qaSource.match(/@click="runQaPublishPrecheck"/g) || []).length, 1,
  'The page must retain exactly one publish action in its header.')
assert.match(
  qaSource,
  /\.qa-regulation-page__version-publish\s*\{[\s\S]*display:\s*flex[\s\S]*align-items:\s*center[\s\S]*gap:/,
  'The version and publish group must use a compact aligned layout.'
)

console.log('PASS qa-regulation-version-publish-header-static')
