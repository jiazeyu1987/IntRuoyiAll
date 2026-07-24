const fs = require('fs')
const path = require('path')

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const marker of [
  'ShowroomHallPreviewAssetPublishReqVO',
  'ShowroomHallPreviewAssetPublishRespVO',
  'publishHallPreviewAsset',
  "url: '/showroom/hall/publish-preview-asset'"
]) {
  if (!apiSource.includes(marker)) {
    throw new Error(`missing hall preview asset API marker "${marker}" in ${apiPath}`)
  }
}

const tablePath = path.resolve(__dirname, '../../src/views/showroom-admin/components/HallListTable.vue')
const tableSource = fs.readFileSync(tablePath, 'utf8')

for (const marker of [
  'publishingPreviewHallId',
  "emit('publishPreviewAsset', row)",
  '发布预览图'
]) {
  if (!tableSource.includes(marker)) {
    throw new Error(`missing hall preview asset table marker "${marker}" in ${tablePath}`)
  }
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

for (const marker of [
  ':publishing-preview-hall-id="publishingHallPreviewAssetId"',
  '@publish-preview-asset="handlePublishHallPreviewAsset"',
  'const publishingHallPreviewAssetId = ref<number | null>(null)',
  'const handlePublishHallPreviewAsset = async',
  'await ElMessageBox.prompt',
  'await ShowroomAdminApi.publishHallPreviewAsset',
  "message.success('展柜预览图已发布')"
]) {
  if (!indexSource.includes(marker)) {
    throw new Error(`missing hall preview asset page marker "${marker}" in ${indexPath}`)
  }
}

const handler = indexSource.match(/const handlePublishHallPreviewAsset = async [\s\S]*?\n\}/)?.[0]

if (!handler) {
  throw new Error(`missing handlePublishHallPreviewAsset implementation in ${indexPath}`)
}

if (!/catch \(error\) \{[\s\S]*message\.error\(resolveError\(error\)\.message\)[\s\S]*throw error/.test(handler)) {
  throw new Error(`hall preview asset publish must surface and rethrow real API failures in ${indexPath}`)
}

console.log('PASS: showroom hall preview asset frontend wiring is present')
