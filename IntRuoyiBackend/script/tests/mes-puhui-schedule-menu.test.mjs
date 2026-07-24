import assert from 'node:assert/strict'
import { readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'

const repoRoot = resolve(import.meta.dirname, '..', '..')
const sqlPath = resolve(repoRoot, 'sql/mysql/20260606_mes_puhui_schedule_menu.sql')

assert.ok(existsSync(sqlPath), 'missing 20260606_mes_puhui_schedule_menu.sql')

const sql = readFileSync(sqlPath, 'utf8')

assert.match(sql, /900104/, 'menu id 900104 must be present')
assert.match(sql, /'璞慧排产'/, 'menu name must be 璞慧排产')
assert.match(sql, /5700/, 'parent menu 5700 must be validated or used')
assert.match(sql, /5540/, 'production schedule menu 5540 must be validated or used')
assert.match(sql, /'puhui-schedule'/, 'menu path must be puhui-schedule')
assert.match(sql, /'mes\/pro\/puhui-schedule\/index'/, 'component path must match frontend page')
assert.match(sql, /'MesProPuhuiSchedule'/, 'component name must match Vue component')
assert.match(sql, /sort\s*>=\s*5/i, 'sibling sort shift for sort >= 5 is required')
assert.match(sql, /JSON_VALID\(`?package`?\.`?menu_ids`?\)|JSON_VALID\(`?menu_ids`?\)/i, 'tenant package menu_ids JSON must be validated')
assert.match(sql, /JSON_TABLE/i, 'tenant package menu_ids must be merged structurally')
assert.match(sql, /system_role_menu/i, 'tenant_admin role menu permissions must be merged')
assert.match(sql, /SIGNAL SQLSTATE '45000'/i, 'migration must fail fast on missing prerequisites')

console.log('mes-puhui-schedule-menu contract passed')

