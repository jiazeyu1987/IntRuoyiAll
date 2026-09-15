import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const page = readFileSync(new URL('../src/views/dcc/controlled-file/upload/index.vue', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/api/dcc/controlledFile/workflow.ts', import.meta.url), 'utf8')
const submitter = readFileSync(new URL('../src/views/dcc/controlled-file/upload/submitter.ts', import.meta.url), 'utf8')
const controller = readFileSync(new URL('../../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java', import.meta.url), 'utf8')
const impactController = readFileSync(new URL('../../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccPublicationImpactAssessmentController.java', import.meta.url), 'utf8')
const workflow = readFileSync(new URL('../../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowService.java', import.meta.url), 'utf8')

test('ordinary upload exposes and validates an explicit iteration-one initial version', () => {
  assert.match(page, /:label="isExternalReview \? '版本号' : '初始版本号'"/)
  assert.match(page, /DEFAULT_CONTROLLED_INITIAL_VERSION_NO = 'A\/1'/)
  assert.match(page, /versionNo\.endsWith\('\/1'\)/)
  assert.match(page, /后续大小版本统一通过文件检出、检入生成/)
  assert.match(submitter, /processType === 'CONTROLLED_FILE'[\s\S]*CONTROLLED_INITIAL_VERSION_INVALID_MESSAGE/)
  assert.match(page, /buildSubmitFailureFeedback\([\s\S]*formData\.processType[\s\S]*\)/)
})

test('ordinary upload no longer calls revision candidate or standalone major APIs', () => {
  assert.doesNotMatch(page, /getControlledFileUploadRevisionCandidates/)
  assert.doesNotMatch(api, /upload-revision-candidates/)
  assert.doesNotMatch(api, /controlled-files\/major-revision/)
  assert.doesNotMatch(controller, /upload-revision-candidates|\/major-revision/)
  assert.doesNotMatch(impactController, /create-revision|createRevision/)
  assert.doesNotMatch(workflow, /createMajorRevision|getUploadRevisionCandidates/)
})
