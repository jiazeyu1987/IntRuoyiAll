const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(root, '..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readWorkspaceSource = (relativePath) =>
  fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const packageJson = JSON.parse(readSource('package.json'))
const apiSource = readSource('src/api/iot/ota/task/index.ts')
const taskListSource = readSource('src/views/iot/ota/task/OtaTaskList.vue')
const iotConstantsSource = readSource('src/views/iot/utils/constants.ts')
const backendControllerSource = readWorkspaceSource(
  'ruoyi-vue-pro/yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/controller/admin/ota/IotOtaTaskController.java'
)
const backendEnumSource = readWorkspaceSource(
  'ruoyi-vue-pro/yudao-module-iot/yudao-module-iot-biz/src/main/java/cn/iocoder/yudao/module/iot/enums/ota/IotOtaTaskStatusEnum.java'
)
const mysqlSeedSource = readWorkspaceSource('ruoyi-vue-pro/sql/mysql/ruoyi-vue-pro.sql')

assert.strictEqual(
  packageJson.scripts['e2e:iot:ota-task-pause-resume:static'],
  'node tests/e2e/iot-ota-task-pause-resume-static.spec.js',
  'package.json 必须提供 OTA 任务暂停继续静态契约脚本'
)

for (const apiToken of [
  'pauseOtaTask',
  '/iot/ota/task/pause?id=',
  'resumeOtaTask',
  '/iot/ota/task/resume?id='
]) {
  assert.ok(apiSource.includes(apiToken), `OTA 任务 API 必须声明 ${apiToken}`)
}

for (const frontendToken of [
  'PAUSED',
  "label: '已暂停'",
  'handlePauseTask',
  'handleResumeTask',
  "v-hasPermi=\"['iot:ota-task:pause']\"",
  "v-hasPermi=\"['iot:ota-task:resume']\"",
  '确认要暂停该升级任务吗？',
  '确认要继续该升级任务吗？',
  '暂停成功',
  '继续成功'
]) {
  assert.ok(taskListSource.includes(frontendToken) || iotConstantsSource.includes(frontendToken), `前端必须包含 ${frontendToken}`)
}

assert.ok(
  /scope\.row\.status === IoTOtaTaskStatusEnum\.IN_PROGRESS\.value[\s\S]*暂停/.test(taskListSource),
  '进行中的 OTA 任务必须显示暂停操作'
)
assert.ok(
  /scope\.row\.status === IoTOtaTaskStatusEnum\.PAUSED\.value[\s\S]*继续/.test(taskListSource),
  '已暂停的 OTA 任务必须显示继续操作'
)
assert.ok(
  /scope\.row\.status === IoTOtaTaskStatusEnum\.PAUSED\.value[\s\S]*取消/.test(taskListSource),
  '已暂停的 OTA 任务必须仍允许取消'
)

for (const backendToken of [
  '@PutMapping("/pause")',
  "hasPermission('iot:ota-task:pause')",
  '@PutMapping("/resume")',
  "hasPermission('iot:ota-task:resume')",
  'PAUSED(40)'
]) {
  assert.ok(
    backendControllerSource.includes(backendToken) || backendEnumSource.includes(backendToken),
    `后端契约必须包含 ${backendToken}`
  )
}

for (const seedToken of [
  "'已暂停', '40', 'iot_ota_task_status'",
  "'OTA 升级任务暂停', 'iot:ota-task:pause'",
  "'OTA 升级任务继续', 'iot:ota-task:resume'"
]) {
  assert.ok(mysqlSeedSource.includes(seedToken), `SQL 种子必须包含 ${seedToken}`)
}

assert.ok(
  !/mock|placeholder data|fallback|降级|吞异常/.test(taskListSource),
  'OTA 任务暂停继续不得引入 mock、placeholder、fallback、降级或吞异常'
)

console.log('PASS: IoT OTA task pause/resume static contract')
