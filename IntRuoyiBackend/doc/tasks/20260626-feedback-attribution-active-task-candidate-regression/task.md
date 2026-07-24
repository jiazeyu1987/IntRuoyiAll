# 任务：报工归属候选误暴露无活动任务工序修复

## 任务目标

修复 MES 报工待归属在“选择归属”后允许选择没有活动任务的排产工单工序，导致用户点击保存时后端报错 `排产工单工序尚未生成活动任务，不能归属正式报工` 的问题；要求从根因收敛候选列表与正式归属校验的一致性，不引入 fallback、静默跳过或自动改归属目标。

## 里程碑

- [x] M1：创建任务文档，确认前一后端任务状态并记录经验门禁、设计约束检查与 BDD 场景。
- [ ] M2：补 RED 回归，锁定“无活动任务工序仍出现在候选列表中”的缺陷。
- [ ] M3：最小修复候选/归属的活动任务判定一致性，必要时补充前端约束。
- [ ] M4：运行定向验证、更新缺陷证据并执行收尾预览。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-feedback-attribution-active-task-candidate-regression\bug-regression-evidence.md`

## 当前状态

已完成。

## 最终验证结果

- `mvn -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-feedback-attribution-active-task-candidate-regression\bug-regression-evidence.md`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-feedback-attribution-active-task-candidate-regression --mode preview`：PASS，`delete/blocked/warnings` 均为 `<none>`

## 前一任务检查

- 后端前一任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260625-dcc-basic-data-main-code-doc-control-order\task.md` 已标记 `completed`，允许继续本任务。
- 当前后端仓库工作区干净；本任务只修改 MES 报工归属候选逻辑、定向单测与本任务文档，不扩散到其他模块。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本任务仅做本机源码与定向单测，不执行真实 E2E、数据库 schema 变更、服务器写入、发布、备份恢复或其他高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。候选列表与正式归属必须共享同一活动任务约束，不增加自动兜底到“其他订单”或静默过滤提交参数。
- `是否从根因和长期维护角度解决`：是。根因是候选列表与正式报工校验对“活动任务”判定不一致，正式方案是统一候选暴露口径，避免用户进入必败路径。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 无活动任务的排产工序不得出现在待归属候选中 -> Given 导入报工命中某个排产工单工序但该工序没有活动任务 / When 用户打开选择归属弹窗 / Then 该工序不应作为可选候选暴露给用户。`
- `BDD: 存在活动任务的排产工序仍可正常归属 -> Given 导入报工命中存在活动任务的排产工单工序 / When 用户选择该工序并确认归属 / Then 系统创建正式报工并更新排产工单工序进度。`
- `BDD: 缺少活动任务时正式归属仍需失败且暴露真实错误 -> Given 用户提交了一个没有活动任务的排产工序编号 / When 后端执行正式归属 / Then 系统必须显式返回“排产工单工序尚未生成活动任务，不能归属正式报工”，不得自动改投其他目标。`

## Cleanup Keep

- `doc/tasks/20260626-feedback-attribution-active-task-candidate-regression/task.md`
- `doc/tasks/20260626-feedback-attribution-active-task-candidate-regression/execution-log.md`
- `doc/tasks/20260626-feedback-attribution-active-task-candidate-regression/bug-regression-evidence.md`
