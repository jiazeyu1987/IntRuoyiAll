# 任务：MES 导入报工批量确认与待归属草稿回显后端改造

## 任务目标

- 保持现有 `attribute / reattribute` 只负责归属与草稿生成，不在归属时自动提交正式报工。
- 扩展待归属分页返回值，带出关联草稿的 `报工人/报工时间/当前审批人/备注/状态摘要` 等行内编辑字段。
- 新增按 `importRecordIds` 处理的批量确认接口，整批校验并事务提交当前导入批次内所有真实工序草稿。
- 对“其他订单”归属记录只保留缓存池链路，不生成正式报工草稿，也不阻断整批确认。
- 阻止来源于导入记录的 `PREPARE` 草稿继续通过旧的单条提交路径绕过批量确认流程。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-edhr-signature-cell-electronic-signature\task.md`
- 状态：`BLOCKED`
- 处理说明：该任务已因用户切换主题显式阻塞；本次只修改 MES 报工归属 / 正式报工批量确认链路、定向单测与任务文档，不回退工作区其他无关改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：无后端本机单测专属经验文档。
- 适用强制门禁：
  - 本轮先做本机后端代码与定向单元测试，不执行服务器动作、不改远端环境、不做真实登录写入。
  - 如后续进入真实 E2E、本机数据库写入或长链路验证，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS` 或明确阻塞原因。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。批量确认遇到未归属、漏填、非 PREPARE 或链路不完整时必须明确整批失败。
- `是否从根因和长期维护角度解决`：是。直接把导入批次确认建模为正式接口，而不是继续依赖前端逐条调用旧提交接口。
- `是否存在临时补丁或绕过`：否。不保留对导入草稿的单条提交兼容路径。

## BDD 场景

- `BDD: 待归属分页带出关联草稿字段 -> Given 导入记录已归属真实工序并生成 PREPARE 草稿 / When 前端分页查询当前批次待归属记录 / Then 响应带出 feedbackUserId、feedbackUserNickname、approveUserId、approveUserNickname、feedbackTime、remark、linkedFeedbackStatus 等行内编辑所需字段。`
- `BDD: 批量确认遇到未归属整批失败 -> Given 当前 importRecordIds 批次内仍存在真实工序 PENDING 行 / When 调用确认报工接口 / Then 返回明确错误且零提交。`
- `BDD: 批量确认遇到漏填字段整批失败 -> Given 当前批次已归属真实工序草稿缺少报工人或报工时间或当前审批人 / When 调用确认报工接口 / Then 返回明确错误且零提交。`
- `BDD: 批量确认跳过其他订单 -> Given 当前批次同时包含真实工序草稿和其他订单归属行 / When 调用确认报工接口 / Then 只提交真实工序草稿，其他订单行被排除且不阻断事务。`
- `BDD: 批量确认成功后整批进入审批中 -> Given 当前批次所有真实工序草稿都存在且状态为 PREPARE / When 调用确认报工接口 / Then 系统先回写行内编辑字段，再在同一事务内逐条 submitFeedback，使全部真实工序草稿进入 APPROVING。`
- `BDD: 导入草稿不再允许单条提交绕过批量确认 -> Given 正式报工列表中存在 sourceImportRecordId 非空的 PREPARE 草稿 / When 用户尝试走旧的单条提交路径 / Then 系统必须阻止该路径或前端不再暴露该入口，避免绕过当前批量确认规则。`

## 里程碑

1. M1：创建任务包、补 RED 单测与接口合同。
2. M2：扩展待归属分页 VO 与批量确认请求模型。`COMPLETED`
3. M3：实现整批校验、事务回写与提交规则。`COMPLETED`
4. M4：运行后端定向验证并回写证据。`COMPLETED`

## 预期验证

- `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest,MesProFeedbackServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 最终验证结果

- `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS

## Cleanup Keep

- `doc/tasks/20260626-mes-feedback-pending-batch-confirm/backend-api-evidence.md`
