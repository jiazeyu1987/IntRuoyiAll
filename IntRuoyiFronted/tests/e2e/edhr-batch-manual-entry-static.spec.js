const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..', '..')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/edhr/batchExecution.ts'), 'utf8')
const listPage = fs.readFileSync(path.join(root, 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue'), 'utf8')
const feedbackPage = fs.readFileSync(path.join(root, 'src/views/mes/pro/feedback/FeedbackForm.vue'), 'utf8')

if (!api.includes('openOrCreateManualEdhrBatchExecution')) throw new Error('manual eDHR API is missing')
if (!api.includes('open-or-create-manual')) throw new Error('manual endpoint is missing')
if (!listPage.includes('openOrCreateManualEdhrBatchExecution')) throw new Error('batch list does not use manual API')
if (!feedbackPage.includes('openOrCreateManualEdhrBatchExecution')) throw new Error('feedback entry does not use manual API')
if (/entryType\s*:/.test(listPage) || /entryType\s*:/.test(feedbackPage)) {
  throw new Error('frontend must not manufacture entryType')
}

console.log('edhr-batch-manual-entry-static: PASS')
