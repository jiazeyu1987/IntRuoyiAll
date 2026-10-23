import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const source = readFileSync(fileURLToPath(new URL('../src/views/dcc/controlled-file/detail/index.vue', import.meta.url)), 'utf8')
test('ordinary final approval asks for stamped PDF while directory is configured automatically', () => {
  const start = source.indexOf('<template v-if="shouldCollectFourthNodeFiles">')
  const end = source.indexOf('<template v-if="shouldCollectExternalReviewConclusion">', start)
  assert.ok(start > 0 && end > start)
  const approval = source.slice(start, end)
  assert.match(approval, /stampedPdfUploadTicket/)
  assert.doesNotMatch(approval, /dcc-doc-control-confirmed-directory|dcc-doc-control-distribution-departments/)
  assert.match(approval, /默认目录/)
})
