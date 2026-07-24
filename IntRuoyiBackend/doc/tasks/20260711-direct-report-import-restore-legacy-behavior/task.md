# 20260711 直接报工导入恢复重构前业务口径

## Task Goal

分析直接报工导入在重构前后的业务逻辑与导入后弹框差异，在不回滚代码的前提下，将导入归属失败处理、统计口径和报工后弹框恢复为重构前可理解、可使用的业务行为。

## Milestones

1. 对比重构前后后端导入业务链路：明确成功创建、跳过、待归属和审批提交口径差异。
2. 对比重构前后前端导入后弹框：明确统计项、报工单号、明细面板差异。
3. 先补回归测试：证明不可归属行应按旧口径跳过，直接报工结果弹框应回到旧口径。
4. 修改后端与前端：保留当前正式匹配链路，不做回滚；恢复旧业务口径与弹框展示。
5. 执行目标回归验证并记录证据。

## Expected Verification

- `mvn -pl yudao-module-mes "-Dtest=ThirdPartyFeedbackImportServiceImplTest,MesProFeedbackControllerImportDirectWorkReportXlsxTest" test`
- `node tests/e2e/mes-direct-work-report-import-result-static.spec.js`
- `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`

## 经验门禁

- PowerShell / Windows shell / 中文编码：执行命令已先读取 `docs/powershell-memory.md`；中文文件读写显式 UTF-8，禁止 Bash heredoc 和 `&&`。
- MES 旧工序 ID / 报工导入旧工序：保留稳定路线工序身份链路，不回退为旧 `process_id` 唯一口径，不用产品号或默认值兜底。
- 前端页面 / 表格 / 样式：本次目标是恢复重构前弹框行为，不引入额外视觉重设计。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，恢复直接报工导入的业务统计契约，同时保留当前正式归属校验链路。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## 差异分析摘要

- 重构前：直接报工导入只对成功归属并创建正式报工的行写入导入记录；无法归属、报工人缺失、排产链路缺失、剩余数量不足或路线工序不可用的行统一计入跳过，不进入待归属记录。
- 重构后：无法归属的直接报工行会写入 `PENDING` 导入记录，并返回待归属明细；这让 `importedCount` 从“创建报工数”变成“导入记录数”，导致用户看到导入后弹框和旧业务口径不一致。
- 本次修复：不回滚路线工序身份、排产链路、剩余数量等正式校验逻辑，只恢复直接报工导入的结果口径：失败归属行跳过；`importedCount` 等于成功创建报工数；`pendingCount=0`；前端恢复旧版汇总 alert。

## Closeout Evidence

- 后端实现提交：`decd4e8ed4`，提交信息 `任务: 恢复直接报工导入口径`。
- 前端实现提交：`a8e80b798`，提交信息 `任务: 恢复直接报工导入弹框`。
- `task-closeout-cleanup` preview/apply：PASS；无可删除的本任务临时产物。
