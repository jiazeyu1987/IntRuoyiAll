const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260811_mes_qa_pressure_pump_id_seed.sql'
)
const sql = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')

assert.match(
  sql,
  /^-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260811_mes_qa_dcc_project_scope; type=data; riskLevel=high\n/
)
assert.match(sql, /START TRANSACTION;/)
assert.match(sql, /DECLARE EXIT HANDLER FOR SQLEXCEPTION/)
assert.match(sql, /ROLLBACK;/)
assert.match(sql, /RESIGNAL;/)
assert.match(sql, /COMMIT;/)
assert.match(sql, /SIGNAL SQLSTATE '45000'/)
assert.match(sql, /`project_code` = 'ID'/)
assert.match(sql, /`project_name` = '球囊扩张压力泵'/)
assert.match(sql, /`doc_control_no` = '112'/)
assert.match(sql, /`status` = 'ENABLE'/)
assert.doesNotMatch(sql, /dcc_project_code_id`\s*=\s*147|VALUES\s*\(147/i)
assert.match(sql, /20260811-ID-PQC-ID-001-G0-v1/)
assert.match(sql, /已有直接归属该DCC项目的QA规程与本次迁移不一致/)
assert.match(sql, /DCC项目业务键必须唯一命中1条/)
assert.match(
  sql,
  /CREATE TEMPORARY TABLE `tmp_mes_qa_id_logical_item`[\s\S]*ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;/
)

const expectedProcesses = new Map([
  ['ID-QA-001', ['清洗', 1, 1]],
  ['ID-QA-002', ['精洗', 2, 1]],
  ['ID-QA-003', ['清洁', 3, 1]],
  ['ID-QA-004', ['组装Ⅰ', 4, 3]],
  ['ID-QA-005', ['光固Ⅰ', 5, 7]],
  ['ID-QA-006', ['组装Ⅱ / 硅化Ⅰ', 6, 1]],
  ['ID-QA-007', ['检测', 7, 2]],
  ['ID-QA-008', ['光固Ⅱ', 8, 2]]
])
for (const [code, [name, sort]] of expectedProcesses) {
  assert.match(sql, new RegExp(`'${code}'\\s*,\\s*'${name.replace('/', '\\/')}'\\s*,\\s*${sort}`))
}

const expectedItemCodes = [
  'ID-001-WASH-APP',
  'ID-001-FINE-WASH-APP',
  'ID-002-CLEAN-APP',
  'ID-003-ASSEMBLY-I-APP',
  'ID-004-ASSEMBLY-I-RELEASE',
  'ID-005-ASSEMBLY-I-NOJUMP',
  'ID-006-UV-I-SWIVEL-APP',
  'ID-007-UV-I-SWIVEL-STRENGTH',
  'ID-008-UV-I-GAUGE-APP',
  'ID-009-UV-I-GAUGE-STRENGTH',
  'ID-010-UV-I-GAUGE-TORQUE',
  'ID-011-UV-I-TUBE-APP',
  'ID-012-UV-I-TUBE-STRENGTH',
  'ID-013-ASSEMBLY-II-APP',
  'ID-014-TEST-HIGH-PRESSURE',
  'ID-015-TEST-LOW-PRESSURE',
  'ID-016-UV-II-APP',
  'ID-017-UV-II-STRENGTH'
]
for (const itemCode of expectedItemCodes) {
  assert.equal(
    sql.split(`'${itemCode}'`).length - 1,
    1,
    `${itemCode} must have one logical source row`
  )
}

assert.match(sql, /'PQC-ID-001'/)
assert.match(sql, /'（椎体）球囊扩张压力泵组装过程检验规程'/)
assert.match(sql, /'G\/0'/)
assert.match(sql, /'2025-09-30'/)
assert.match(sql, /'用户指定 PDF PQC-ID-001（G\/0）5\.1 检验内容。'/)
assert.match(sql, /COUNT\(DISTINCT `item_code`\) <> 18/)
assert.match(sql, /COUNT\(DISTINCT `qa_process_id`\) <> 8/)
assert.match(sql, /COUNT\(1\) <> 51/)
assert.match(sql, /JSON_EXTRACT\([^,]*`snapshot_json`, '\$\.migrationKey'\)/)

console.log('PASS: pressure-pump ID QA backend seed migration contract')
