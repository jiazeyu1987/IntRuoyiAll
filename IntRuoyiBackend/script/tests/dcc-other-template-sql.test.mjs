import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const sqlPath = path.join(root, 'sql', 'mysql', '20260526_dcc_other_template_category.sql')

const readSql = () => fs.readFileSync(sqlPath, 'utf8')

test('dcc other template seed copies governance from product technical requirement', () => {
  const sql = readSql()

  assert.match(sql, /DCC_OTHER_TEMPLATE/)
  assert.match(sql, /SET NAMES utf8mb4/)
  assert.match(sql, /'其他'/)
  assert.match(sql, /'产品技术要求'/)
  assert.match(sql, /SIGNAL SQLSTATE '45000'/)
  assert.match(sql, /Missing active DCC source template category: 产品技术要求/)
  assert.match(sql, /JOIN\s+`system_tenant`\s+tenant/)
  assert.match(sql, /tenant\.`id`\s*=\s*source_category\.`tenant_id`/)
  assert.match(sql, /tenant\.`status`\s*=\s*0/)
  assert.match(sql, /tenant\.`deleted`\s*=\s*0/)
  assert.match(sql, /dcc_file_category_permission_rule/)
  assert.match(sql, /dcc_file_category_distribution_rule/)
  assert.match(sql, /dcc_file_category_training_rule/)
  assert.match(sql, /dcc_category_approval_route/)
  assert.match(sql, /dcc_category_approval_route_node/)
  assert.match(sql, /target\.`deleted` = 0/)
  assert.match(sql, /NOT EXISTS/)
})
