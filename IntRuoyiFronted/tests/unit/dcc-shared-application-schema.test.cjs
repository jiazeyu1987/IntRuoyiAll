const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const repo = path.resolve(__dirname, '../../..')
const be = 'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/'
const read = file => fs.readFileSync(path.join(repo, file), 'utf8')
test('shared file model stores effective fact separately from daily activation and distribution choices', () => {
  const source = read(be + 'dal/dataobject/file/DccControlledFileDO.java')
  for (const field of ['effectiveMode', 'effectiveTime', 'effectiveSignatureId', 'effectiveProcessInstanceId', 'distributionDepartmentIdsJson', 'activatedTime']) assert.match(source, new RegExp('private [^;]+ ' + field + ';'))
})
test('new normal submissions require an explicit conditional effective selection and separate departments', () => {
  for (const name of ['DccControlledFileSubmitReqVO', 'DccControlledFileSubmitIterationReqVO']) {
    const source = read(be + 'controller/admin/file/vo/' + name + '.java')
    assert.match(source, /private String effectiveMode;/)
    assert.match(source, /private List<Long> selectedDistributionDepartmentIds;/)
    assert.match(source, /@AssertTrue/)
    assert.match(source, /DccApplicationEffectiveSelection\.isComplete/)
    assert.doesNotMatch(source, /@NotNull\(message = "effectiveDate is required"\)/)
  }
})
test('forward migration introduces nullable fields without rewriting historical data', () => {
  const sql = read('IntRuoyiBackend/sql/mysql/20261011_dcc_application_effective_distribution.sql')
  assert.match(sql, /release-migration:/)
  assert.match(sql, /information_schema\.COLUMNS/)
  for (const column of ['effective_mode','effective_time','effective_signature_id','effective_process_instance_id','distribution_department_ids_json']) assert.ok(sql.includes(column))
  assert.doesNotMatch(sql, /\b(?:UPDATE|DELETE\s+FROM|INSERT\s+INTO)\s+`?dcc_controlled_file`?/i)
  assert.doesNotMatch(sql, /ADD COLUMN IF NOT EXISTS/i)
})
test('working resource accepts a structured real application and separates submission-only validation', () => {
  const controller = read(be + 'controller/admin/file/DccControlledFileController.java')
  assert.match(controller, /@RequestBody DccWorkingApplicationSaveReqVO/)
  assert.match(controller, /workflowService\.saveWorkingApplication\(getLoginUserId\(\), id, request\)/)
  const submit = read(be + 'controller/admin/file/vo/DccControlledFileSubmitReqVO.java')
  assert.match(submit, /groups\s*=\s*DccSubmissionValidationGroup\.class/)
})
