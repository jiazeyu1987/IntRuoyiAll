import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const readSource = (relativePath) => readFileSync(join(root, relativePath), 'utf8')

const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const submitter = readSource('src/views/dcc/controlled-file/upload/submitter.ts')

const extractBetween = (source, startToken, endToken) => {
  const startIndex = source.indexOf(startToken)
  const endIndex = source.indexOf(endToken, startIndex + startToken.length)
  assert.ok(startIndex >= 0 && endIndex > startIndex, `无法提取 ${startToken} 到 ${endToken} 内容`)
  return source.slice(startIndex, endIndex)
}

const retryGuardBlock = extractBetween(
  uploadPage,
  'const canRetryWorkingDraftCreationAfterCurrentVersionConflictMessage',
  'const loadCurrentVersionByFileNumber'
)
const loadCurrentVersionBlock = extractBetween(
  uploadPage,
  'const loadCurrentVersionByFileNumber',
  'const queryUploadNameSuggestions'
)
const submitFormBlock = extractBetween(
  uploadPage,
  'const submitForm = async () =>',
  'watch('
)

assert.match(
  submitter,
  /export const isFileNumberChainConflictMessage = \(/,
  'submitter must expose a reusable exact logical-chain conflict classifier'
)
assert.match(
  uploadPage,
  /isFileNumberChainConflictMessage/,
  'upload page must use the logical-chain conflict classifier instead of stringly bypasses'
)

assert.match(
  retryGuardBlock,
  /!isExternalReview\.value/,
  'same-session retry recovery must not apply to external review submissions'
)
assert.match(
  retryGuardBlock,
  /formData\.changeType === 'NEW'/,
  'same-session retry recovery must be limited to NEW working draft creation'
)
assert.match(
  retryGuardBlock,
  /previewUpload\.value\?\.sessionId === uploadSessionId/,
  'same-session retry recovery must require the preview ticket to belong to the active upload session'
)
assert.match(
  retryGuardBlock,
  /Boolean\(previewUpload\.value\?\.uploadTicket\)/,
  'same-session retry recovery must require an existing preview upload ticket'
)
assert.match(
  retryGuardBlock,
  /isFileNumberChainConflictMessage\(errorMessage\)/,
  'same-session retry recovery must only open for the known logical-chain conflict'
)

assert.match(
  loadCurrentVersionBlock,
  /suppressWorkingDraftRetryConflictMessage\?: boolean/,
  'current-version lookup must support suppressing only the recoverable retry conflict during submit'
)
assert.match(
  loadCurrentVersionBlock,
  /canRetryWorkingDraftCreationAfterCurrentVersionConflictMessage\(errorMessage\)/,
  'current-version lookup must classify the same recoverable conflict before showing the formal-version error'
)
assert.match(
  loadCurrentVersionBlock,
  /message\.error\(errorMessage\)/,
  'current-version lookup must continue surfacing non-recoverable preflight errors'
)

assert.match(
  submitFormBlock,
  /await loadCurrentVersionByFileNumber\(\{\s*suppressWorkingDraftRetryConflictMessage: true\s*\}\)/,
  'submit must suppress only the recoverable working-draft retry conflict message'
)
assert.match(
  submitFormBlock,
  /if \(currentVersionLookupError\.value\) \{[\s\S]*if \(!canRetryWorkingDraftCreationAfterCurrentVersionConflict\(\)\) \{[\s\S]*return[\s\S]*\}[\s\S]*\}/,
  'submit must continue failing fast for non-recoverable current-version errors'
)
assert.match(
  submitFormBlock,
  /await uploadSubmitterService\.submit\(/,
  'submit must still reach the formal create-working API so backend idempotency returns the existing draft'
)
assert.doesNotMatch(
  submitFormBlock,
  /if \(currentVersionLookupError\.value\) \{\s*submitFieldErrors\.versionNo = currentVersionLookupError\.value[\s\S]*?return\s*\}/,
  'submit must not unconditionally return on current-version logical-chain conflict before create-working'
)

console.log('PASS: DCC-STATIC-004 same-session working draft retry static contract')
