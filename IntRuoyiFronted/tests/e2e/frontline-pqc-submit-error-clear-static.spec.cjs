const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs.readFileSync(path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8').replace(/\r\n/g, '\n')

const start = source.indexOf('const handleConfirmPqcSubmit = async () => {')
const end = source.indexOf('const assertFormalPayloadContext = () => {', start)
assert.ok(start >= 0, 'PQC submit handler is missing.')
assert.ok(end > start, 'PQC submit handler boundary is invalid.')
const handler = source.slice(start, end)

const resetIndex = handler.indexOf('resetPqcSubmissionDrafts(submitPayloads.map((payload) => payload.pqcTaskId))')
const successMessageIndex = handler.indexOf('message.success(', resetIndex)
const clearIndex = handler.indexOf('clearFrontlineError()', resetIndex)
assert.ok(resetIndex >= 0, 'Successful PQC submit must reset the submitted draft.')
assert.ok(successMessageIndex > resetIndex, 'Successful PQC submit must show its success message after reset.')
assert.ok(clearIndex > resetIndex && clearIndex < successMessageIndex, 'Successful PQC submit must clear the inline error before showing success.')

const failureStart = handler.indexOf('if (!pqcSubmitResultUncertain.value) {')
const failureEnd = handler.indexOf('} finally {', failureStart)
assert.ok(failureStart >= 0 && failureEnd > failureStart, 'PQC submit failure branch is missing.')
assert.match(handler.slice(failureStart, failureEnd), /showFrontlineError\(error\)/, 'PQC submit failures must continue to show the latest inline error.')

console.log('PASS: successful PQC submit clears the inline error while failures keep it visible')
