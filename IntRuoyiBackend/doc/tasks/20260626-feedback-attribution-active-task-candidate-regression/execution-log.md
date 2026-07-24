# Execution Log：报工归属候选误暴露无活动任务工序修复

BDD: 无活动任务的排产工序不得出现在待归属候选中 -> Given 导入报工命中某个排产工单工序但该工序没有活动任务 / When 用户打开选择归属弹窗 / Then 该工序不应作为可选候选暴露给用户。
BDD: 存在活动任务的排产工序仍可正常归属 -> Given 导入报工命中存在活动任务的排产工单工序 / When 用户选择该工序并确认归属 / Then 系统创建正式报工并更新排产工单工序进度。
BDD: 缺少活动任务时正式归属仍需失败且暴露真实错误 -> Given 用户提交了一个没有活动任务的排产工序编号 / When 后端执行正式归属 / Then 系统必须显式返回“排产工单工序尚未生成活动任务，不能归属正式报工”，不得自动改投其他目标。

INFO: task-created -> 已创建后端任务文档，准备先补 `MesProFeedbackImportRecordServiceImplTest` 的 RED 回归，锁定“候选暴露无活动任务工序”的缺陷。
RED: `mvn -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增回归暴露两处不一致：无活动任务工序仍进入候选列表；已完成任务在归属链路里未被视为“非活动任务”。
CHANGE: `MesProFeedbackImportRecordServiceImpl` 统一活动任务解析逻辑：候选列表必须先命中非终态任务，正式归属同样复用该解析，避免用户进入后端必败路径。
GREEN: `mvn -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，20 个报工归属相关单测通过。
