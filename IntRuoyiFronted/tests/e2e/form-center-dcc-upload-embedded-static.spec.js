const fs = require('fs')
const path = require('path')

const root = process.cwd()

const read = (relativePath) => {
  const filePath = path.join(root, relativePath)
  if (!fs.existsSync(filePath)) {
    throw new Error(`Missing file: ${relativePath}`)
  }
  return fs.readFileSync(filePath, 'utf8')
}

const assertIncludes = (text, expected, message = expected) => {
  if (!text.includes(expected)) {
    throw new Error(`Expected ${message}`)
  }
}

const assertNotIncludes = (text, unexpected, message = unexpected) => {
  if (text.includes(unexpected)) {
    throw new Error(`Unexpected ${message}`)
  }
}

const uploadPage = read('src/views/dcc/controlled-file/upload/index.vue')

assertNotIncludes(uploadPage, "import ActionFormPanel from '@/views/form-center/business-action/ActionFormPanel.vue'")
assertNotIncludes(uploadPage, '<ActionFormPanel')
assertNotIncludes(uploadPage, 'data-testid="dcc-upload-section-form-center"')
assertNotIncludes(uploadPage, '表单中心审批表单')
assertNotIncludes(uploadPage, 'dccFormCenterContext')
assertNotIncludes(uploadPage, 'dccFormCenterFormData')
assertNotIncludes(uploadPage, 'dccFormCenterIdempotencyKey')
assertNotIncludes(uploadPage, 'dccFormCenterStartUserSelectAssignees')
assertIncludes(uploadPage, 'data-testid="dcc-upload-section-submit"', 'DCC upload must keep the official submit section')
assertIncludes(uploadPage, "submitControlledFile", 'DCC upload must keep the official controlled-file submit API')
assertIncludes(uploadPage, 'const submitForm = async () =>', 'DCC upload must keep the official submit handler')
assertIncludes(uploadPage, "const submitButtonText = computed(() => (isExternalReview.value ? '提交评审' : '提交审批'))")
assertNotIncludes(uploadPage, "{ signoff: signoffUserIds }", 'DCC upload must map selected signoff users to BPMN node id')
assertNotIncludes(uploadPage, 'formDataText', 'DCC upload node must not depend on manual JSON workbench state')

console.log('form-center DCC upload removal static contract passed')
