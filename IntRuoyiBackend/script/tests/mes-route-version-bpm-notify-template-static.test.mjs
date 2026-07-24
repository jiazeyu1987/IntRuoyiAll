import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const sqlPath = path.join(root, 'sql', 'mysql', '20260718_mes_route_version_bpm_notify_to_inbox.sql')

const readSql = () => fs.readFileSync(sqlPath, 'utf8')

test('route version bpm notify templates are seeded as enabled inbox templates', () => {
  assert.ok(fs.existsSync(sqlPath), '必须新增工艺路线版本审批站内信模板 SQL。')
  const sql = readSql()

  assert.match(sql, /release-migration:/)
  assert.match(sql, /system_notify_template/)
  assert.match(sql, /mes-route-version-approval-v1/)
  assert.match(sql, /Fail fast|fail fast/i)
  assert.doesNotMatch(sql, /system_sms|sms_template/i)

  const templates = [
    ['MES_ROUTE_VERSION_BPM_TASK_ASSIGNED', '["processInstanceName","taskName","startUserNickname","detailUrl"]'],
    ['MES_ROUTE_VERSION_BPM_APPROVED', '["processInstanceName","detailUrl"]'],
    ['MES_ROUTE_VERSION_BPM_REJECTED', '["processInstanceName","reason","detailUrl"]'],
    ['MES_ROUTE_VERSION_BPM_TASK_TIMEOUT', '["processInstanceName","taskName","detailUrl"]']
  ]

  for (const [code, params] of templates) {
    assert.match(sql, new RegExp(`'${code}'`), `缺少模板 ${code}`)
    assert.match(sql, new RegExp(params.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `模板 ${code} 参数不完整`)
    assert.match(
      sql,
      new RegExp("WHERE\\s+`code`\\s*=\\s*'" + code + "'[\\s\\S]*`deleted`\\s*=\\s*b'0'"),
      `模板 ${code} 必须按 code + deleted 做幂等判断`
    )
  }

  assert.match(sql, /,\s*0,\s*'工艺路线版本审批通知站内信化'/, '新增模板必须保持启用状态。')
})
