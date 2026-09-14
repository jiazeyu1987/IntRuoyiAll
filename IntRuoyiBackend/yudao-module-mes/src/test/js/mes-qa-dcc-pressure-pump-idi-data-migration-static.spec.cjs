const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const migrationPath = path.join(
  backendRoot,
  'sql/mysql/20260812_mes_qa_pressure_pump_idi_seed.sql'
)
const sql = fs.readFileSync(migrationPath, 'utf8').replace(/\r\n/g, '\n')
const normalizedSql = sql.split(String.fromCharCode(96)).join('')

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

assert.match(normalizedSql, /project_code[^=]*= 'IDI'/)
assert.match(normalizedSql, /project_name[^=]*= '按压式球囊扩充压力泵'/)
assert.match(normalizedSql, /doc_control_no[^=]*= '1'/)
assert.match(normalizedSql, /status[^=]*= 'ENABLE'/)
assert.doesNotMatch(normalizedSql, /dcc_project_code_id\s*=\s*129|VALUES\s*\(129/i)
assert.doesNotMatch(normalizedSql, /WHERE\s+id\s+IN\s*\(\s*54\s*,\s*55\s*,\s*56\s*\)/i)
assert.doesNotMatch(normalizedSql, /route_process_id\s+IN\s*\(\s*980633\s*,\s*980634\s*,\s*980644\s*\)/i)

assert.match(sql, /20260812-IDI-PQC-IDI-001-B0-v1/)
assert.match(sql, /已有直接归属该DCC项目的QA规程与本次迁移不一致/)
assert.match(sql, /DCC项目业务键必须唯一命中1条/)
assert.match(sql, /IDI旧QA规程源数据必须唯一命中3条/)
assert.match(sql, /IDI旧QA规程源项目必须为22个逻辑项目和64条检验类型行/)
assert.match(sql, /tmp_mes_qa_idi_source_process/)

const expectedProcesses = [
  ["IDI-QA-001", "清洗工序", 1],
  ["IDI-QA-002", "清洁工序", 2],
  ["IDI-QA-003", "大包装工序", 3]
]
for (const [code, name, sort] of expectedProcesses) {
  assert.match(sql, new RegExp("'" + code + "'\\s*,\\s*'" + name + "'\\s*,\\s*" + sort))
}

assert.match(sql, /'PQC-IDI-001'/)
assert.match(sql, /'按压式球囊扩充压力泵组装过程检验规程'/)
assert.match(sql, /'B\/0'/)
assert.match(normalizedSql, /COUNT\(DISTINCT item_code\) <> 22/)
assert.match(normalizedSql, /COUNT\(DISTINCT qa_process_id\) <> 3/)
assert.match(sql, /COUNT\(1\) <> 64/)
assert.match(normalizedSql, /JSON_EXTRACT\([^,]*snapshot_json[^,]*, '\$\.migrationKey'\)/)
assert.match(sql, /ROW_NUMBER\(\) OVER/)
assert.match(normalizedSql, /mes_qa_inspection_regulation_item\s+source_item/)

console.log('PASS: pressure-pump IDI QA backend seed migration contract')
