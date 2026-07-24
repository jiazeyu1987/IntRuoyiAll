const fs = require('fs')
const path = require('path')

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

for (const marker of [
  '<el-tab-pane label="奖项" name="award"',
  '生成中英文语音',
  '中文语音',
  '英文语音',
  "targetType: 'AWARD'",
  'awardNarrationDraftStale',
  'handleGenerateAwardNarrationAudio',
  'buildAwardPublishPayload'
]) {
  if (!indexSource.includes(marker)) {
    throw new Error(`missing award audio marker "${marker}" in ${indexPath}`)
  }
}

if (!indexSource.includes('奖项讲解内容已变更，请重新生成中英文语音。')) {
  throw new Error(`missing award narration stale warning in ${indexPath}`)
}

if (!indexSource.includes('中文语音未生成，无法发布奖项')) {
  throw new Error(`missing Chinese audio publish gate in ${indexPath}`)
}

if (!indexSource.includes('英文语音未生成，无法发布奖项')) {
  throw new Error(`missing English audio publish gate in ${indexPath}`)
}

const generateAudioFunction = indexSource.match(
  /const handleGenerateAwardNarrationAudio = async \(\) => \{[\s\S]*?\n\}/
)?.[0]

if (!generateAudioFunction) {
  throw new Error(`missing handleGenerateAwardNarrationAudio implementation in ${indexPath}`)
}

if (!generateAudioFunction.includes('await ShowroomAdminApi.generateNarrationAudio')) {
  throw new Error(`award audio generation must call the real narration audio API in ${indexPath}`)
}

if (!generateAudioFunction.includes("message.success('奖项中英文语音已生成')")) {
  throw new Error(`award audio generation must show success only after API calls in ${indexPath}`)
}

if (!/catch \(error\) \{[\s\S]*message\.error\(resolveError\(error\)\.message\)[\s\S]*throw error/.test(generateAudioFunction)) {
  throw new Error(`award audio generation must surface and rethrow real API failures in ${indexPath}`)
}

const publishFunction = indexSource.match(
  /const handlePublishAward = async \(\) => \{[\s\S]*?\n\}/
)?.[0]

if (!publishFunction) {
  throw new Error(`missing handlePublishAward implementation in ${indexPath}`)
}

const publishIndex = publishFunction.indexOf('await ShowroomAdminApi.publishAward')

if (publishIndex === -1) {
  throw new Error(`award publish must call publishAward API in ${indexPath}`)
}

const zhAudioGateIndex = publishFunction.indexOf('中文语音未生成，无法发布奖项')
const enAudioGateIndex = publishFunction.indexOf('英文语音未生成，无法发布奖项')

if (zhAudioGateIndex === -1 || enAudioGateIndex === -1) {
  throw new Error(`award publish must gate both narration audio URLs before publish API in ${indexPath}`)
}

if (!(zhAudioGateIndex < publishIndex && enAudioGateIndex < publishIndex)) {
  throw new Error(`award publish API must run only after both narration audio URL gates in ${indexPath}`)
}

const publishPayloadFunction = indexSource.match(
  /const buildAwardPublishPayload = \(\) => \{[\s\S]*?\n\}/
)?.[0]

if (!publishPayloadFunction || !publishPayloadFunction.includes('revisionId: awardNarrationDraft.sourceRevisionId')) {
  throw new Error(`award publish payload must include the narration source revisionId in ${indexPath}`)
}

if (publishFunction.includes('saveAwardDraftAndRefreshNarrationSource')) {
  throw new Error(`award publish must not create a new draft revision after audio generation in ${indexPath}`)
}

if (publishFunction.includes('publishNarration')) {
  throw new Error(`award publish must not use generic narration publish workflow in ${indexPath}`)
}

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

if (!apiSource.includes("targetType: 'COMPANY' | 'HALL' | 'PRODUCT' | 'AWARD'")) {
  throw new Error(`showroom narration API must accept AWARD target type in ${apiPath}`)
}

const contractsPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/narration/contracts.ts'
)
const contractsSource = fs.readFileSync(contractsPath, 'utf8')

if (!contractsSource.includes("targetType: 'COMPANY' | 'PRODUCT' | 'HALL' | 'AWARD'")) {
  throw new Error(`showroom narration contract must accept AWARD target type in ${contractsPath}`)
}

const hallMappingPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/HallProductMappingDialog.vue'
)
const hallMappingSource = fs.readFileSync(hallMappingPath, 'utf8')

if (!hallMappingSource.includes("option.itemType === 'AWARD' ? '奖项' : '产品'")) {
  throw new Error(`hall selector must distinguish award options in ${hallMappingPath}`)
}

console.log('PASS: showroom award audio and hall selector wiring is present')
