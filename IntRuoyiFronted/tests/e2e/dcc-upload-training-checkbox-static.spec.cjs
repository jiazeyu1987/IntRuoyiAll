const fs = require('fs')
const path = require('path')
const assert = require('assert')

const uploadPage = fs.readFileSync(
  path.join(__dirname, '../../src/views/dcc/controlled-file/upload/index.vue'),
  'utf8'
)

assert.match(uploadPage, /<el-checkbox[\s\S]*data-testid="dcc-upload-need-training"/)
assert.match(uploadPage, /v-model="formData\.needTraining"/)
assert.match(uploadPage, /v-if="!isExternalReview"/)

const submitter = fs.readFileSync(
  path.join(__dirname, '../../src/views/dcc/controlled-file/upload/submitter.ts'),
  'utf8'
)

assert.doesNotMatch(
  submitter,
  /selectedSignoffUserIds/,
  'new controlled upload payload must not send deprecated manual signoff users'
)
assert.doesNotMatch(
  uploadPage,
  /checkControlledFileRouteReadiness\(\{[\s\S]*selectedSignoffUserIds/,
  'upload route readiness must not send deprecated manual signoff users'
)
