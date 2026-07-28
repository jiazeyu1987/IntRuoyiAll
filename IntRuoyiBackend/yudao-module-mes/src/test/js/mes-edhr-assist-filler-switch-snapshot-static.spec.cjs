const fs = require('fs')
const path = require('path')
const assert = require('assert')

const workspaceRoot = path.resolve(__dirname, '../../../../..')
const read = (relativePath) => fs.readFileSync(path.join(workspaceRoot, relativePath), 'utf8')

const executionPage = read('IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue')
const feedbackApi = read('IntRuoyiFronted/src/api/mes/pro/feedback/index.ts')
const batchExecutionApi = read('IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts')
const executionRespVO = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/MesProBatchRecordExecutionRespVO.java')
const taskOpenReqVO = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionTaskOpenReqVO.java')
const taskOpenRespVO = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/vo/EdhrBatchExecutionTaskOpenRespVO.java')
const executionService = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionServiceImpl.java')
const batchExecutionService = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java')
const executionMapper = read('IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProBatchRecordExecutionMapper.java')

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
assert.match(batchExecutionApi, /assistUserId\?:\s*EdhrRouteId/, '前端 openTask 请求必须声明 assistUserId。')
assert.match(taskOpenReqVO, /private\s+Long\s+assistUserId;/, '后端 openTask 请求必须接收 assistUserId。')
assert.match(taskOpenRespVO, /private\s+Long\s+assistUserId;/, '后端 openTask 响应必须返回已确认的 assistUserId。')
assert.match(batchExecutionService, /resolveAssistUserIdForOpenTask/, '后端必须解析并校验所选 assistUserId。')
assert.match(batchExecutionService, /resolveVisibleAssistRows\(task,\s*openWorkTask,\s*assistUserId\)/, '后端辅助行必须按所选 assistUserId 解析。')
assert.match(batchExecutionService, /resolveVisibleAssistScopes\(openWorkTask,\s*assistUserId\)/, '后端辅助行必须读取所选填写人的正式责任范围，不能只用 rowKey 字符串集合。')
assert.match(batchExecutionService, /assistRowVisibleInScope\(/, '后端辅助行必须支持 ALL\/ranges 职责范围投影，不能只支持 scopeKey 等于 assist rowKey。')
assert.match(batchExecutionService, /\.setTaskId\(task\.getId\(\)\)/, '批次任务打开传统批记录时必须把批次任务 ID 写入执行记录请求。')
assert.match(executionService, /\.taskId\(reqVO\.getTaskId\(\)\)/, '执行记录创建必须保存请求中的批次任务 ID，避免新批次复用旧执行详情。')
assert.match(
  executionService,
  /selectActiveByContext\([\s\S]*reqVO\.getBatchExecutionId\(\)[\s\S]*reqVO\.getTaskId\(\)/,
  '执行记录 active 查询必须按 batchExecutionId + taskId 隔离新批次，不能复用其它批次旧执行详情。'
)
assert.match(executionMapper, /getBatchExecutionId[\s\S]*getTaskId/, '执行记录 active 查询必须支持 batchExecutionId 和 taskId 条件。')

console.log('mes-edhr-assist-filler-switch-snapshot-static PASS')
