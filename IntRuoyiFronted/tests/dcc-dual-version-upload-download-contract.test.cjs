const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const workspace = path.resolve(__dirname, '..')
const backendRoot = path.join(workspace, '..', 'IntRuoyiBackend')
const vo = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-dcc',
    'src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java'
  ),
  'utf8'
)
const policy = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-dcc',
    'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileUploadTypePolicy.java'
  ),
  'utf8'
)
const controller = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-dcc',
    'src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java'
  ),
  'utf8'
)
const workflow = fs.readFileSync(
  path.join(
    backendRoot,
    'yudao-module-dcc',
    'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java'
  ),
  'utf8'
)
const frontendApi = fs.readFileSync(
  path.join(workspace, 'src/api/dcc/controlledFile/workflow.ts'),
  'utf8'
)
const submitter = fs.readFileSync(
  path.join(workspace, 'src/views/dcc/controlled-file/upload/submitter.ts'),
  'utf8'
)
const schemaSql = fs.readFileSync(
  path.join(backendRoot, 'sql/mysql/20260919_dcc_controlled_file_dual_version.sql'),
  'utf8'
)
const permissionSql = fs.readFileSync(
  path.join(backendRoot, 'sql/mysql/20260919_dcc_dual_version_download_permissions.sql'),
  'utf8'
)

test('submit contract distinguishes required read-only and optional editable tickets', () => {
  assert.match(vo, /readOnlyUploadTicket/)
  assert.match(vo, /editableUploadTicket/)
  assert.match(vo, /readOnlyFileId/)
  assert.match(vo, /editableFileId/)
  assert.match(workflow, /getReadOnlyUploadTicket\(\)/)
  assert.match(workflow, /getEditableUploadTicket\(\)/)
  assert.doesNotMatch(submitter, /originalUploadTicket:\s*previewFile\.uploadTicket/)
  assert.match(submitter, /readOnlyUploadTicket/)
})

test('upload policy has explicit read-only and editable purposes', () => {
  assert.match(policy, /PURPOSE_READ_ONLY_VIEW/)
  assert.match(policy, /PURPOSE_EDITABLE_SOURCE/)
  assert.match(policy, /isRealPdfFile/)
})

test('download endpoints and permissions are independent', () => {
  assert.match(controller, /\/download\/read-only/)
  assert.match(controller, /\/download\/editable/)
  assert.match(controller, /dcc:controlled-file:download-read-only/)
  assert.match(controller, /dcc:controlled-file:download-editable/)
  assert.match(
    controller,
    /@GetMapping\("\/\{id:\\\\d\+\}"\)\s+@Operation\(summary = "Get controlled file detail"\)\s+@PreAuthorize\("@ss\.hasPermission\('dcc:controlled-file:query'\)"\)/
  )
  assert.match(
    controller,
    /@GetMapping\("\/\{id:\\\\d\+\}\/download"\)\s+@Operation\(summary = "Download controlled file"\)\s+@PreAuthorize\("@ss\.hasPermission\('dcc:controlled-file:download-read-only'\)"\)/
  )
  assert.match(frontendApi, /download-read-only/)
  assert.match(frontendApi, /download-editable/)
})

test('database migration persists dual artifact columns and split permissions', () => {
  assert.match(schemaSql, /information_schema\.COLUMNS/)
  assert.match(schemaSql, /read_only_file_id/)
  assert.match(schemaSql, /editable_file_id/)
  assert.match(permissionSql, /dcc:controlled-file:download-read-only/)
  assert.match(permissionSql, /dcc:controlled-file:download-editable/)
  assert.doesNotMatch(permissionSql, /dcc:controlled-file:download'\s*,/)
})
