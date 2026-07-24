import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const sliceBetween = (source, startMarker, endMarker) => {
  const startIndex = source.indexOf(startMarker)
  if (startIndex === -1) {
    return ''
  }
  const endIndex = source.indexOf(endMarker, startIndex)
  return endIndex === -1 ? source.slice(startIndex) : source.slice(startIndex, endIndex)
}

test('showroom admin AI generation requests override the default 30s timeout', () => {
  const source = readText('src/api/showroom-admin/index.ts')
  const companyScriptBlock = sliceBetween(
    source,
    'generateCompanyNarrationScript: async (',
    'generateCompanyNarrationAudio: async ('
  )
  const companyAudioBlock = sliceBetween(
    source,
    'generateCompanyNarrationAudio: async (',
    'publishCompanyNarration: async ('
  )
  const productScriptBlock = sliceBetween(
    source,
    'generateProductNarrationScript: async (',
    'generateProductNarrationAudio: async ('
  )
  const productAudioBlock = sliceBetween(
    source,
    'generateProductNarrationAudio: async (',
    'batchGenerateProductNarrationAudio: async ('
  )
  const batchProductAudioBlock = sliceBetween(
    source,
    'batchGenerateProductNarrationAudio: async (',
    'generateProductCoverImage: async ('
  )
  const productCoverBlock = sliceBetween(
    source,
    'generateProductCoverImage: async (',
    'batchGenerateProductCoverImage: async ('
  )
  const batchProductCoverBlock = sliceBetween(
    source,
    'batchGenerateProductCoverImage: async (',
    'getHallPage: async ('
  )

  assert.match(source, /const SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT = 5 \* 60 \* 1000/)
  assert.match(companyScriptBlock, /timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT/)
  assert.match(companyAudioBlock, /timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT/)
  assert.match(productScriptBlock, /timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT/)
  assert.match(productAudioBlock, /timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT/)
  assert.match(batchProductAudioBlock, /timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT/)
  assert.match(productCoverBlock, /timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT/)
  assert.match(batchProductCoverBlock, /timeout: SHOWROOM_ADMIN_AI_REQUEST_TIMEOUT/)
})
