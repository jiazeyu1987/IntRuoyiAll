const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const migration = fs.readFileSync(path.join(root, 'sql/mysql/20260903_mes_process_pool_device_selection_mode.sql'), 'utf8')
const bindingDo = fs.readFileSync(path.join(root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolTeamProcessDeviceDO.java'), 'utf8')
const snapshotDo = fs.readFileSync(path.join(root,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolActiveOrderProcessSnapshotDO.java'), 'utf8')

for (const column of ['device_group_key', 'selection_mode', 'device_selection_snapshot_json', 'device_selection_snapshot_sha256']) {
  assert.match(migration, new RegExp(column))
}
assert.match(bindingDo, /private String deviceGroupKey;/)
assert.match(bindingDo, /private String selectionMode;/)
assert.match(snapshotDo, /private String deviceSelectionSnapshotJson;/)
assert.match(snapshotDo, /private String deviceSelectionSnapshotSha256;/)

console.log('PASS: device selection mode persistence schema contract')
