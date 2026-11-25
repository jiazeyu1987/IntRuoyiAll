const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const ts = require('typescript')

const source = fs.readFileSync('src/views/mes/pro/scheduleorder/index.vue', 'utf8')
const submitSource = source.slice(
  source.indexOf('const submitPriorityAdjust ='),
  source.indexOf('const openPromiseDateDialog =')
)
assert(submitSource.includes('const submitPriorityAdjust = async () =>'))
const submitJs = ts.transpile(submitSource, { target: ts.ScriptTarget.ES2022 })

async function submit(overrides = {}, failure) {
  const calls = []
  const state = {
    priorityForm: { id: 900, priorityNo: 99, promiseDate: '', plannedStartTime: '', remark: '', reason: '调整优先级', ...overrides },
    prioritySaving: { value: false },
    priorityDialogVisible: { value: true },
    message: {
      warning: (text) => calls.push(['warning', text]),
      success: (text) => calls.push(['success', text])
    },
    MesProScheduleOrderApi: {
      updateScheduleOrder: async (data) => {
        calls.push(['update', JSON.parse(JSON.stringify(data))])
        if (failure) throw failure
      }
    },
    getScheduleOrderList: async () => calls.push(['refresh'])
  }
  vm.createContext(state)
  vm.runInContext(`${submitJs}\nglobalThis.runSubmit = submitPriorityAdjust`, state)
  return { state, calls, run: () => state.runSubmit() }
}

async function main() {
  const emptyDates = await submit()
  await emptyDates.run()
  assert.deepEqual(emptyDates.calls, [
    ['update', { id: 900, priorityNo: 99, remark: '', reason: '调整优先级' }],
    ['success', '排产工单已调整'], ['refresh']
  ], '空日期工单应能保存优先级，不提交空字符串日期，也不补造日期')
  assert.equal(emptyDates.state.prioritySaving.value, false)
  assert.equal(emptyDates.state.priorityDialogVisible.value, false)

  const withDates = await submit({ promiseDate: '2026-11-26', plannedStartTime: '2026-11-25 08:00:00' })
  await withDates.run()
  assert.equal(withDates.calls[0][1].promiseDate, '2026-11-26')
  assert.equal(withDates.calls[0][1].plannedStartTime, '2026-11-25 08:00:00')

  for (const overrides of [{ priorityNo: 0 }, { priorityNo: 1.5 }, { reason: ' ' }, { id: undefined }]) {
    const invalid = await submit(overrides)
    await invalid.run()
    assert.equal(invalid.calls.length, 1)
    assert.equal(invalid.calls[0][0], 'warning')
    assert.equal(invalid.state.priorityDialogVisible.value, true)
  }

  const failure = new Error('保存失败')
  const failed = await submit({}, failure)
  await assert.rejects(failed.run, (error) => error === failure)
  assert.equal(failed.calls.length, 1, '失败后不得提示成功或刷新列表')
  assert.equal(failed.state.prioritySaving.value, false)
  assert.equal(failed.state.priorityDialogVisible.value, true)

  const calendar = fs.readFileSync('src/views/mes/pro/task/calendar/index.vue', 'utf8')
  const lockButton = calendar.match(/<el-button\b[^>]*@click="toggleTaskLock\(row\)"[^>]*>/)?.[0]
  assert(lockButton?.includes(`v-hasPermi="['mes:pro-schedule-order:update']"`), '日历锁定和解锁按钮必须使用排产更新权限')
  const controller = fs.readFileSync(
    '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/MesProTaskController.java',
    'utf8'
  )
  for (const route of ['/lock', '/unlock']) {
    const routeSource = controller.slice(controller.indexOf(`@PutMapping("${route}")`))
    assert(routeSource.startsWith(`@PutMapping("${route}")\n    @Operation`))
    assert.match(routeSource.slice(0, 220), /@PreAuthorize\("@ss\.hasPermission\('mes:pro-schedule-order:update'\)"\)/)
  }
  console.log('PASS: priority-only submission, validation, failed-save handling and calendar lock permission')
}

main().catch((error) => { console.error(error); process.exitCode = 1 })
