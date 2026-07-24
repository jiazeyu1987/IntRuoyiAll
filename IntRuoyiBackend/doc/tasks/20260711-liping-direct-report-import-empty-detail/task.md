# 20260711 李萍报工单导入结果明细为空修复

## Task Goal

修复导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 后结果弹窗显示“创建报工数 50”但明细区为空的问题，确保直接报工导入结果的统计口径、明细展示和失败/待归属状态一致、可追踪。

## Milestones

1. 复现并定位真实文件导入结果：确认 Excel 解析行数、跳过杂务行数、创建报工数、待归属数和前端明细绑定关系。
2. 补充回归测试：覆盖“存在导入记录但未创建报工明细时，结果不能误报创建数且应展示可追踪导入行状态”。
3. 实现正式修复：统一后端结果口径与前端展示，不引入 fallback、静默吞异常或临时绕过。
4. 执行目标回归验证：后端导入服务/控制器测试、前端静态或组件契约测试、必要的真实路径验证。
5. 收尾记录与提交：更新执行日志、验证报告和请求命令日志，仅提交本任务相关改动。

## Expected Verification

- `mvn -pl yudao-module-mes "-Dtest=ThirdPartyFeedbackExcelParserTest,ThirdPartyFeedbackImportServiceImplTest,MesProFeedbackControllerImportDirectWorkReportXlsxTest" test`
- 前端导入结果展示相关静态/单元契约测试。
- 若启动本地前端/后端并具备测试租户登录前置，则使用真实 Excel 走 Playwright 导入路径复验。

## 经验门禁

- PowerShell / Windows shell / 中文编码：执行 PowerShell 前已读取 `docs/powershell-memory.md`；中文文件读写与命令输出必须显式 UTF-8，禁止 Bash heredoc，禁止 `&&`。
- MES 旧工序 ID / 报工导入旧工序：直接报工导入必须按任务单、生产订单、路线工序身份、工序编码和租户链路唯一归属；不能用产品号或默认值绕过真实链路。
- 前端页面 / 表格 / 样式：若修改导入结果弹窗，保持蓝/中性运营控制台风格，使用紧凑表格和可扫描状态，不新增解释性大段文案。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是统一导入结果业务口径与明细可追踪展示，而不是仅隐藏空态。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## Closeout Evidence

- 后端实现提交：`267c2ea076`，提交信息 `任务: 修复李萍报工导入结果明细`。
- 前端实现提交：`a52915be8`，提交信息 `任务: 修复李萍报工导入结果明细`。
- `task-closeout-cleanup` preview/apply：PASS；无可删除的本任务临时产物。
- 最终状态：实现、目标验证、收尾记录均已完成。
