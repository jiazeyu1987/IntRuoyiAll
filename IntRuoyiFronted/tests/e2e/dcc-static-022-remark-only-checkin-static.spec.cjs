const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')

const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const api = readUtf8('IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts')
const page = readUtf8('IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue')
const backend = readUtf8(
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java'
)

const extractFunction = (source, marker) => {
  const markerIndex = source.indexOf(marker)
  assert.notEqual(markerIndex, -1, `${marker} should exist`)
  const braceStart = source.indexOf('{', markerIndex)
  assert.notEqual(braceStart, -1, `${marker} should have a function body`)
  let depth = 0
  for (let index = braceStart; index < source.length; index += 1) {
    const char = source[index]
    if (char === '{') depth += 1
    if (char === '}') depth -= 1
    if (depth === 0) {
      return source.slice(markerIndex, index + 1)
    }
  }
  assert.fail(`${marker} function body should close`)
}

const checkinTitleIndex = page.indexOf('title="检入新小版本"')
assert.notEqual(checkinTitleIndex, -1, 'checkin dialog should exist')
const checkinDialogStart = page.lastIndexOf('<el-dialog', checkinTitleIndex)
const checkinDialogEnd = page.indexOf('</el-dialog>', checkinTitleIndex)
assert.notEqual(checkinDialogStart, -1, 'checkin dialog should have an opening tag')
assert.notEqual(checkinDialogEnd, -1, 'checkin dialog should have a closing tag')
const checkinDialog = page.slice(checkinDialogStart, checkinDialogEnd + '</el-dialog>'.length)
const handleCheckin = extractFunction(page, 'const handleCheckin =')
const resetCheckinDialog = extractFunction(page, 'const resetCheckinDialog =')
const submitCheckin = extractFunction(page, 'const submitCheckin =')

assert.match(api, /export interface ControlledFileCheckinReqVO\s*\{[\s\S]*uploadTicket\?: string[\s\S]*changeDescription: string[\s\S]*remark\?: string[\s\S]*\}/)
assert.match(api, /export const checkinControlledFile[\s\S]*request\.post\(\{ url: `\/dcc\/controlled-files\/\$\{id\}\/checkin`, data \}\)/)

assert.match(backend, /boolean hasUpload = StrUtil\.isNotBlank\(reqVO\.getUploadTicket\(\)\);/)
assert.match(backend, /boolean hasMetadata = hasRemarkChange\(file, reqVO\.getRemark\(\)\);/)
assert.match(backend, /if \(!hasUpload && !hasMetadata\) \{[\s\S]*CONTROLLED_FILE_CHECKIN_REQUEST_INVALID/)
assert.match(backend, /preparedSource = sourceOwnershipService\.prepareSubmissionSource\(file\.getSourceFileId\(\), false\);/)
assert.match(backend, /if \(Objects\.equals\(baseHash, preparedSource\.sourceSha256\(\)\) && !hasMetadata\) \{[\s\S]*CONTROLLED_FILE_CHECKIN_NO_CHANGE/)

assert.match(checkinDialog, /data-testid="dcc-controlled-browser-checkin-upload"/)
assert.doesNotMatch(
  checkinDialog,
  /<el-form-item\s+label="修改后的源文件"\s+required/,
  'source upload must be optional when metadata changes'
)
assert.match(checkinDialog, /data-testid="dcc-controlled-browser-checkin-description"/)
assert.match(checkinDialog, /data-testid="dcc-controlled-browser-checkin-remark"/)
assert.match(checkinDialog, /源文件或备注至少一项真实变化/)
assert.match(checkinDialog, /未上传新源文件时，备注需与当前版本不同/)

assert.match(page, /const checkinForm = reactive\(\{\s*changeDescription: '',\s*remark: ''\s*\}\)/)
assert.match(handleCheckin, /checkinForm\.remark = String\(file\.remark \|\| ''\)/)
assert.match(resetCheckinDialog, /checkinForm\.remark = ''/)

assert.match(submitCheckin, /const normalizedCheckinRemark = checkinForm\.remark\.trim\(\)/)
assert.match(submitCheckin, /const hasCheckinUpload = Boolean\(uploaded\?\.uploadTicket\)/)
assert.match(
  submitCheckin,
  /const hasCheckinRemarkChange =\s*Boolean\(normalizedCheckinRemark\) && normalizedCheckinRemark !== String\(target\.remark \|\| ''\)\.trim\(\)/
)
assert.doesNotMatch(
  submitCheckin,
  /if \(!uploaded\?\.uploadTicket\) \{[\s\S]*?return[\s\S]*?\}/,
  'submitCheckin must not return solely because uploadTicket is absent'
)
assert.match(submitCheckin, /请上传修改后的源文件，或修改检入备注。/)
assert.match(submitCheckin, /uploadTicket:\s*hasCheckinUpload \? uploaded\?\.uploadTicket : undefined/)
assert.match(submitCheckin, /sessionId:\s*hasCheckinUpload \? checkinUploadSessionId\.value : undefined/)
assert.match(submitCheckin, /remark:\s*normalizedCheckinRemark/)

console.log('PASS: DCC-STATIC-022 remark-only checkin static contract')
