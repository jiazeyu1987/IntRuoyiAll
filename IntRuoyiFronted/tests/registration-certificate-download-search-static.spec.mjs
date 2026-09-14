import fs from 'node:fs'
import assert from 'node:assert/strict'

const api = fs.readFileSync(new URL('../src/api/dcc/registrationCertificate/index.ts', import.meta.url), 'utf8')
const listPage = fs.readFileSync(new URL('../src/views/dcc/registration-certificate/index/index.vue', import.meta.url), 'utf8')
const detailPage = fs.readFileSync(new URL('../src/views/dcc/registration-certificate/detail/index.vue', import.meta.url), 'utf8')
const actionPanel = fs.readFileSync(new URL('../src/views/dcc/registration-certificate/workflow/ActionPanel.vue', import.meta.url), 'utf8')

assert.match(api, /businessFileId\?:\s*number \| string/, 'history API type must expose change approval business file id')
assert.match(api, /fileKind\?:\s*string/, 'history API type must expose the business file kind')

assert.match(detailPage, /const downloadableFiles = computed/, 'detail page must build formal downloadable file options')
assert.match(detailPage, /history\.value[\s\S]{0,500}businessFileId/, 'detail page must include change history business files')
assert.match(detailPage, /:downloadable-files="downloadableFiles"/, 'detail page must pass downloadable files into the action panel')

assert.match(actionPanel, /downloadableFiles\?:\s*DownloadableFileOption\[\]/, 'action panel must receive downloadable file options')
assert.match(actionPanel, /selectedDownloadBusinessFileId/, 'action panel must keep the user selected download file')
assert.match(actionPanel, /businessFileIds:\s*\[requireSelectedDownloadBusinessFileId\(\)\]/,
  'download request must submit the selected business file id')
assert.match(actionPanel, /projectCodeId:\s*props\.projectCodeId\s*\|\|\s*undefined/,
  'download request must omit projectCodeId when the certificate has no project code')
assert.doesNotMatch(actionPanel, /const requireProjectCodeId[\s\S]{0,240}缺少项目代码/,
  'download request must not require a project code before submitting')
assert.match(actionPanel, /const hasDownloadFacts = computed\(\(\) =>\s*Boolean\(selectedDownloadBusinessFileId\.value\)\s*\)/,
  'download request availability must depend on the selected business file, not project code')
assert.doesNotMatch(actionPanel, /当前档案缺少项目代码或可下载文件，下载已锁定/,
  'download request warning must not claim missing project code locks the action')
assert.doesNotMatch(actionPanel, /projectCodeId:\s*requireProjectCodeId\(\)/,
  'download request must not require project code before submission')
assert.doesNotMatch(actionPanel, /props\.businessFileId[\s\S]{0,180}REGISTRATION_CERTIFICATE/,
  'download file options must come from the formal downloadableFiles list, not a single-field fallback')
assert.doesNotMatch(actionPanel, /requestType,\s*\n\s*purpose:\s*'页面提交的注册证文件下载申请'\s*\n\s*}/,
  'download request must not omit the selected file identity')

const oldFilterBlock = listPage.slice(
  listPage.indexOf('const oldQuickFilterDefinitions'),
  listPage.indexOf('const resolveRouteQueryText')
)
assert.ok(oldFilterBlock.length > 0, 'old certificate quick filter block must exist')
for (const field of [
  'certificateNo',
  'ownerCompanyName',
  'productName',
  'classification',
  'registrantName',
  'modelSpecification',
  'productionAddress',
  'entrustedEnterpriseName',
  'projectCode',
  'firstObtainedStart',
  'firstObtainedEnd',
  'effectiveStart',
  'effectiveEnd',
  'expiryStart',
  'expiryEnd'
]) {
  assert.match(oldFilterBlock, new RegExp(`queryParamKey: '${field}'`),
    `old certificate index must bind ${field}`)
}

console.log('registration certificate download/search contract: PASS')
