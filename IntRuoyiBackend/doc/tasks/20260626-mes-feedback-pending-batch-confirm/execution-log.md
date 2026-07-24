# 执行日志：MES 导入报工批量确认与待归属草稿回显后端改造

BDD: 待归属分页带出关联草稿字段 -> Given 导入记录已归属真实工序并生成 PREPARE 草稿 / When 前端分页查询当前批次待归属记录 / Then 响应带出 feedbackUserId、feedbackUserNickname、approveUserId、approveUserNickname、feedbackTime、remark、linkedFeedbackStatus 等行内编辑所需字段。
BDD: 批量确认遇到未归属整批失败 -> Given 当前 importRecordIds 批次内仍存在真实工序 PENDING 行 / When 调用确认报工接口 / Then 返回明确错误且零提交。
BDD: 批量确认遇到漏填字段整批失败 -> Given 当前批次已归属真实工序草稿缺少报工人或报工时间或当前审批人 / When 调用确认报工接口 / Then 返回明确错误且零提交。
BDD: 批量确认跳过其他订单 -> Given 当前批次同时包含真实工序草稿和其他订单归属行 / When 调用确认报工接口 / Then 只提交真实工序草稿，其他订单行被排除且不阻断事务。
BDD: 批量确认成功后整批进入审批中 -> Given 当前批次所有真实工序草稿都存在且状态为 PREPARE / When 调用确认报工接口 / Then 系统先回写行内编辑字段，再在同一事务内逐条 submitFeedback，使全部真实工序草稿进入 APPROVING。
BDD: 导入草稿不再允许单条提交绕过批量确认 -> Given 正式报工列表中存在 sourceImportRecordId 非空的 PREPARE 草稿 / When 用户尝试走旧的单条提交路径 / Then 系统必须阻止该路径或前端不再暴露该入口，避免绕过当前批量确认规则。

READONLY: 已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`；本轮尚未进入真实库写入、真实登录或远端环境动作。

RED: `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，预期失败；当前缺少 `MesProFeedbackImportConfirmBatchReqVO`、批量确认服务接口及待归属分页扩展字段。
GREEN: `mvn --% -pl yudao-module-mes -Dtest=MesProFeedbackImportRecordServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，24 tests run, 0 failures, 0 errors；已覆盖批次摘要、未归属阻断、非 PREPARE 阻断、其他订单跳过提交与导入草稿单条提交封口链路。
