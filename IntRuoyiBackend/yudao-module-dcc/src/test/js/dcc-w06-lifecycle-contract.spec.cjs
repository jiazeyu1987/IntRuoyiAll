const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const service = path.resolve(__dirname, '../../main/java/cn/iocoder/yudao/module/dcc/service/file')
const lifecycle = fs.readFileSync(path.join(service, 'DccControlledFileLifecycleService.java'), 'utf8')
const panel = fs.readFileSync(path.resolve(__dirname, '../../../../../IntRuoyiFronted/src/views/dcc/controlled-file/workflow/ControlledLifecyclePanel.vue'), 'utf8')
function method(source, start, next) {return source.slice(source.indexOf(start),source.indexOf(next,source.indexOf(start)))}
test('pending distribution requires both controlled and active facts', () => {
  const body=method(lifecycle,'public List<DccControlledFileDO> pendingDistribution(boolean remindersOnly,DccWorkflowDatePolicy.ReminderDates dates)','public List<DccControlledFileDO> dueVersions()')
  assert.match(body,/\.eq\(DccControlledFileDO::getStatus,\s*"ACTIVE"\)/)
  assert.match(body,/\.isNotNull\(DccControlledFileDO::getControlledTime\)/)
  assert.match(body,/\.isNotNull\(DccControlledFileDO::getActivatedTime\)/)
  assert.doesNotMatch(body,/CONTROLLED_PENDING_EFFECTIVE/)
})
test('lifecycle panel cannot expose distribution slot before activation', () => {
  assert.match(panel,/v-if="view\.canDistribute && facts\.status === 'ACTIVE' && facts\.controlledTime && facts\.activatedTime"/)
})
