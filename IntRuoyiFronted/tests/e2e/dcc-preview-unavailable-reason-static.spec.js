const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const viewerPath = path.join(repoRoot, 'src/views/dcc/controlled-file/view/index.vue')
const viewerSource = fs.readFileSync(viewerPath, 'utf8')

const requiredPreviewKinds = ['PDF', 'IMAGE', 'VIDEO', 'AUDIO', 'TEXT', 'OFFICE', 'DOWNLOAD_ONLY']
for (const kind of requiredPreviewKinds) {
  assert.match(
    viewerSource,
    new RegExp(`resolvedPreviewKind(?:\\.value)? === '${kind}'|'${kind}'`),
    `Protected viewer must keep an explicit preview path for ${kind}`
  )
}

const metadataReasonIndex = viewerSource.indexOf('resolvedPreviewUnavailableReason.value = metadata.previewUnavailableReason ||')
const unavailableGuardIndex = viewerSource.indexOf('if (resolvedPreviewUnavailableReason.value)')
const nonOfficeErrorIndex = viewerSource.indexOf("if (resolvedPreviewKind.value !== 'OFFICE')")
const genericReasonErrorIndex = viewerSource.indexOf('errorMessage.value = resolvedPreviewUnavailableReason.value')
const binaryLoadIndex = viewerSource.indexOf('const previewPayload = await resolvePreviewBlob()')

assert.notEqual(metadataReasonIndex, -1, 'metadata.previewUnavailableReason must be copied into viewer state')
assert.notEqual(unavailableGuardIndex, -1, 'viewer must guard previewUnavailableReason before binary loading')
assert.notEqual(nonOfficeErrorIndex, -1, 'non-Office preview types must render the unavailable reason in the generic alert')
assert.notEqual(genericReasonErrorIndex, -1, 'generic alert must use the precise previewUnavailableReason text')
assert.notEqual(binaryLoadIndex, -1, 'viewer must still support binary loading when preview is available')
assert.ok(metadataReasonIndex < unavailableGuardIndex, 'unavailable guard must run after metadata is loaded')
assert.ok(unavailableGuardIndex < binaryLoadIndex, 'unavailable guard must run before resolvePreviewBlob')
assert.ok(nonOfficeErrorIndex > unavailableGuardIndex && nonOfficeErrorIndex < binaryLoadIndex, 'non-Office reason display must happen before binary loading')
assert.ok(genericReasonErrorIndex > nonOfficeErrorIndex && genericReasonErrorIndex < binaryLoadIndex, 'generic error must be set inside the unavailable guard')

console.log('PASS: DCC preview unavailable reason short-circuits all preview kinds')
