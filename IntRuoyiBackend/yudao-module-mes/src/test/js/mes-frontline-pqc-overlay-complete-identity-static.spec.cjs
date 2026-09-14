const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { execFileSync } = require('node:child_process')

const backendRoot = path.resolve(__dirname, '../../../..')
const workspaceRoot = path.resolve(backendRoot, '..')
const relativeSource =
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcTaskOverlay.java'
const source = process.argv.includes('--baseline')
  ? execFileSync('git', ['show', `HEAD:IntRuoyiBackend/${relativeSource}`], {
      cwd: workspaceRoot,
      encoding: 'utf8'
    })
  : fs.readFileSync(path.join(backendRoot, relativeSource), 'utf8')

for (const contract of [
  'Long routeProcessId',
  'Long processId',
  'expectedTask.routeProcessId(), task.getRouteProcessId()',
  'expectedTask.processId(), task.getProcessId()',
  'expectedTask.businessDate(), task.getBusinessDate()',
  'expectedTask.shiftCode(), task.getShiftCode()',
  'expectedTask.roundNo(), task.getRoundNo()'
]) {
  assert.ok(source.includes(contract), `RED: PQC overlay identity missing ${contract}`)
}

console.log('GREEN: PQC task overlay uses complete production-process and schedule identity')
