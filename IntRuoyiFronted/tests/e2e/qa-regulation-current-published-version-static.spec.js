const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/processpool/QaRegulationPage.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/qc/template/index.ts'),
  'utf8'
)

assert.match(
  apiSource,
  /getPublishedQaRegulationVersion:\s*async\s*\(\s*versionId\?: number\s*\):\s*Promise<QaInspectionRegulationPublishedVersionVO>/,
  'The published QA regulation reader must expose its formal response type.'
)

const headerStart = pageSource.indexOf('<div class="qa-regulation-page__header">')
const headerEnd = pageSource.indexOf('</ContentWrap>', headerStart)
assert.ok(headerStart >= 0 && headerEnd > headerStart, 'The QA regulation header must exist.')
const headerSource = pageSource.slice(headerStart, headerEnd)

assert.match(
  headerSource,
  /data-qa-regulation-current-published-version/,
  'The screenshot target must have a stable current-published-version anchor.'
)
const publishedVersionMarker = headerSource.indexOf(
  'data-qa-regulation-current-published-version'
)
const publishedVersionStart = headerSource.lastIndexOf('<div', publishedVersionMarker)
const publishedVersionEnd = headerSource.indexOf(
  'class="qa-regulation-page__version-publish"',
  publishedVersionMarker
)
assert.ok(
  publishedVersionStart >= 0 && publishedVersionEnd > publishedVersionStart,
  'The current published version field must have stable header boundaries.'
)
const publishedVersionSource = headerSource.slice(publishedVersionStart, publishedVersionEnd)
assert.match(
  publishedVersionSource,
  /当前已发布版本[\s\S]*qaCurrentPublishedVersion\.versionNo/,
  'The header must display the formal current published version number.'
)
assert.match(
  publishedVersionSource,
  /加载中/,
  'The header must expose the published-version loading state.'
)
assert.match(
  publishedVersionSource,
  /暂无已发布版本/,
  'The header must expose the published-version empty state.'
)
assert.match(
  publishedVersionSource,
  /qaCurrentPublishedVersionLoadError/,
  'The header must expose formal published-version query failures.'
)
assert.doesNotMatch(
  publishedVersionSource,
  /qaRegulationDraft\.versionNo/,
  'The current published version must not fall back to the editable draft version.'
)

assert.match(
  pageSource,
  /const loadCurrentPublishedQaRegulationVersion = async \(project\?: DccProjectCodeRespVO\)/,
  'Project selection must have a dedicated published-version loader.'
)
assert.match(
  pageSource,
  /publishedStatus\?\.currentVersionId[\s\S]*getPublishedQaRegulationVersion\(currentVersionId\)/,
  'The loader must resolve the product currentVersionId and query that exact immutable version.'
)
assert.match(
  pageSource,
  /publishedVersion\.publishedVersionId !== currentVersionId[\s\S]*publishedVersion\.productId !== productId/,
  'The loader must fail fast when the published-version response does not match the selected product state.'
)
assert.match(
  pageSource,
  /applyDccProjectToQaDraft[\s\S]*void loadCurrentPublishedQaRegulationVersion\(project\)/,
  'Changing the DCC project must refresh the current published version.'
)
assert.match(
  pageSource,
  /const publishedVersion = await QcTemplateApi\.publishQaRegulation\(payload\)[\s\S]*latestPublishedVersion = publishedVersion[\s\S]*qaCurrentPublishedVersion\.value = latestPublishedVersion/,
  'A successful publish must update the current published version without a stale header.'
)
assert.match(
  pageSource,
  /@media \(max-width: 1500px\) \{[\s\S]*\.qa-regulation-page__header \{[\s\S]*flex-wrap: wrap;[\s\S]*\.qa-regulation-page__project-form \{[\s\S]*order: 3;[\s\S]*flex: 1 0 100%;/,
  'The expanded header must wrap before the desktop content area can clip publish controls.'
)

console.log('QA regulation current published version static contract passed.')
