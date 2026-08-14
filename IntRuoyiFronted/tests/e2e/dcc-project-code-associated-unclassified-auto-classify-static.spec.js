const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const pageSource = readSource('src/views/dcc/controlled-file/basic-data/components/ProjectCodeTabPanel.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:project-code-associated-unclassified-auto-classify:static'],
  'node tests/e2e/dcc-project-code-associated-unclassified-auto-classify-static.spec.js',
  'package.json must expose the DCC associated unclassified auto-classify static contract'
)

for (const token of [
  'data-testid="dcc-project-code-auto-classify-unclassified"',
  '按文件名归类未分类',
  'unclassifiedAutoClassifyRunning',
  'handleAutoClassifyUnclassifiedAssociatedFiles',
  'associatedUnclassifiedFiles',
  'associatedUnclassifiedFileCount'
]) {
  assert.ok(pageSource.includes(token), `associated documents must expose unclassified auto-classify token: ${token}`)
}

assert.ok(
  pageSource.includes('updateControlledFileMetadata') &&
    pageSource.includes('ControlledFileMetadataUpdateReqVO'),
  'unclassified auto-classify must reuse the formal controlled-file metadata update API'
)

assert.ok(
  pageSource.includes('buildDccAssociatedFileAutoClassifyPayload') &&
    pageSource.includes('changeReason:') &&
    pageSource.includes('fileTypeTaxonomyId: target.taxonomyId') &&
    pageSource.includes('fileTypeLevel1: DCC_TECHNICAL_DOCUMENT_ROOT_NAME') &&
    pageSource.includes('fileTypeLevel2: target.stageName') &&
    pageSource.includes('fileTypeLevel3: target.label'),
  'auto-classify payload must preserve metadata and assign the chosen taxonomy path'
)

assert.ok(
  pageSource.includes('associatedTaxonomyStageTypeOptionsMap') &&
    pageSource.includes('associatedAutoClassifyTargetOptions') &&
    pageSource.includes('Array.from(associatedTaxonomyStageTypeOptionsMap.value.values()).flat()'),
  'auto-classify candidates must come from DCC taxonomy stage direct child file types'
)

assert.ok(
  pageSource.includes('DCC_UNCLASSIFIED_TAXONOMY_STAGE') &&
    pageSource.includes('DCC_PROJECT_CODE_UNCLASSIFIED_TYPE') &&
    pageSource.includes('isAssociatedFileUnclassified') &&
    pageSource.includes('resolveAssociatedStageKey(file) === DCC_UNCLASSIFIED_TAXONOMY_STAGE') &&
    pageSource.includes('resolveAssociatedTypeName(file) === DCC_PROJECT_CODE_UNCLASSIFIED_TYPE'),
  'auto-classify must only process unclassified stage or unclassified file-type files'
)

for (const token of [
  'normalizeAutoClassifyText',
  'splitAutoClassifyTokens',
  'calculateAutoClassifySimilarity',
  'resolveBestAssociatedAutoClassifyTarget',
  'autoClassifyTextSimilarityScore',
  'autoClassifyTokenOverlapScore',
  'autoClassifySubstringScore'
]) {
  assert.ok(pageSource.includes(token), `auto-classify must use deterministic name similarity helper: ${token}`)
}

assert.ok(
  pageSource.includes('targetOptions.length === 0') &&
    pageSource.includes('没有可用于归类的正式文件类型') &&
    !pageSource.includes('targetOptions[0] ||'),
  'auto-classify must fail fast when the formal taxonomy target list is missing'
)

assert.ok(
  pageSource.includes('await message.confirm') &&
    pageSource.includes('将按文件名相似度归类') &&
    pageSource.includes('不会保留在未分类或未分类文件类型中'),
  'auto-classify must ask for explicit confirmation before batch metadata writes'
)

assert.ok(
  /for \(const file of filesToClassify\)[\s\S]*await updateControlledFileMetadata\(file\.id, payload\)/.test(pageSource),
  'auto-classify must update each unclassified file through the formal metadata API'
)

assert.ok(
  pageSource.includes('自动归类失败：') &&
    pageSource.includes('resolveAiCategoryErrorMessage(error)') &&
    pageSource.includes('throw error'),
  'auto-classify failures must surface the backend error instead of swallowing it'
)

assert.ok(
  pageSource.includes('await getAssociatedFiles()') &&
    pageSource.includes('await getList()'),
  'auto-classify must refresh associated navigation and project-code counts after writes'
)

assert.ok(
  !pageSource.includes('Math.random') &&
    !pageSource.includes('未分类文件类型: target') &&
    !pageSource.includes('DCC_PROJECT_CODE_UNCLASSIFIED_TYPE as'),
  'auto-classify must not use random matching or classify into the unclassified bucket'
)

console.log('PASS: DCC project-code associated unclassified auto-classify static contract')
