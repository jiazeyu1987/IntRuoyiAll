const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const backendRoot = path.resolve(root, '../IntRuoyiBackend')
const readFrontend = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const api = readFrontend('src/api/system/codexTestManagement/index.ts')
const page = readFrontend('src/views/system/codex-test-management/index.vue')
const runner = readFrontend('scripts/codex-test-runner.mjs')
const controller = readBackend(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/controller/admin/codextest/CodexTestRunnerController.java'
)
const service = readBackend(
  'yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/codextest/CodexTestRunnerServiceImpl.java'
)

assert.match(api, /getCodexTestExecutionMonitor/, '前端 API 必须提供运行监控查询方法。')
assert.match(
  api,
  /\/system\/codex-test-execution\/monitor/,
  '运行监控必须使用后端 monitor 接口，不得由前端本地推断运行任务。'
)
for (const field of ['progressPhase', 'currentMethodSort', 'currentCheckpointSort', 'progressMessage']) {
  assert.match(api, new RegExp(`${field}\\??:`), `执行项类型必须暴露 ${field}。`)
}

assert.match(page, /<el-tabs[\s\S]*运行监控/, '测试管理页必须提供运行监控页签。')
assert.match(page, /monitorRunningCount/, '运行监控页必须展示当前运行任务数量。')
assert.match(page, /getCodexTestExecutionMonitor/, '运行监控页必须调用监控接口刷新真实状态。')
assert.match(page, /monitorRefreshTimer/, '运行监控页必须有轮询刷新控制。')
assert.match(page, /resolveMethodStepState/, '运行监控页必须按方法项当前进度计算颜色。')
assert.match(page, /resolveCheckpointStepState/, '运行监控页必须按目标项验证结果计算颜色。')
for (const cssClass of [
  'codex-run-monitor-step--success',
  'codex-run-monitor-step--running',
  'codex-run-monitor-step--failed'
]) {
  assert.match(page, new RegExp(cssClass), `运行监控缺少状态样式 ${cssClass}。`)
}
assert.match(page, /openFailedCheckpointReason/, '点击红色目标必须打开失败原因。')
assert.match(page, /目标失败原因/, '失败原因弹窗标题必须明确。')
assert.doesNotMatch(page, /mock|placeholder data|fallback data|降级|吞异常/i)

assert.match(controller, /@PostMapping\("\/progress"\)/, 'Runner 协议必须提供 progress 上报接口。')
assert.match(service, /reportProgress/, 'Runner 服务必须持久化 progress 上报。')
assert.match(runner, /function reportProgress/, '本地 Runner 必须实现 progress 上报函数。')
assert.match(runner, /\/system\/codex-test-runner\/progress/, 'Runner 必须调用后端 progress 接口。')
assert.match(runner, /phase:\s*'METHOD'/, 'Runner 启动 Codex 前必须上报方法项执行阶段。')
assert.match(runner, /phase:\s*'CHECKPOINT'/, 'Runner 回写检查点前必须上报目标验证阶段。')

console.log('PASS: Codex test run monitor static contract')
