const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const executionPage = read('IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue')
const feedbackApi = read('IntRuoyiFronted/src/api/mes/pro/feedback/index.ts')
const executionRespVO = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionRespVO.java')
const executionService = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java')

const loaderStart = executionPage.indexOf('const loadAssistFillerSwitchItems = async () => {')
const loaderEnd = executionPage.indexOf('const openAssistSwitchDialog', loaderStart)
assert.ok(loaderStart >= 0 && loaderEnd > loaderStart, '前端必须保留切换填写人加载函数。')
const fillerLoader = executionPage.slice(loaderStart, loaderEnd)

assert.match(feedbackApi, /EdhrBatchExecutionTaskRespVO/, '执行详情前端类型必须复用批次任务快照类型。')
assert.match(feedbackApi, /assistSwitchTasks\?:\s*EdhrBatchExecutionTaskRespVO\[\]/, '执行详情前端类型必须暴露 assistSwitchTasks 快照。')
assert.match(executionRespVO, /private\s+List<EdhrBatchExecutionTaskRespVO>\s+assistSwitchTasks;/, '执行详情后端 VO 必须返回 assistSwitchTasks 快照。')
assert.match(executionService, /setAssistSwitchTasks\(buildAssistSwitchTasksSnapshot\(execution\)\)/, '执行详情构建必须填充 assistSwitchTasks。')
assert.match(executionService, /getCandidateUserSnapshot\(\)/, '后端快照必须来自工作任务 candidateUserSnapshot。')
assert.doesNotMatch(fillerLoader, /getEdhrBatchExecution\(/, '切换填写人不得再调用全量批次详情接口。')
assert.match(fillerLoader, /execution\.value\?\.assistSwitchTasks/, '切换填写人必须从执行详情 assistSwitchTasks 快照读取候选人。')

console.log('mes-edhr-assist-filler-switch-snapshot-static PASS')
