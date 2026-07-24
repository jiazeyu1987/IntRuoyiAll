# 任务：eDHR Jimu 在线填写、多人电子签名与最终打印

## 任务目标

在 `edhr_jimu` 独立 worktree 中实现 eDHR 表单链路：使用 Jimu 报表作为模板来源，在线填写一张执行表单，允许同一张表单产生多名人员的电子签名，并生成可下载打印的最终表单归档。

## 范围边界

- Jimu 负责模板设计与报表来源，不在 Jimu 设计器内直接填写生产执行数据。
- 在线填写发生在 eDHR 执行页，保存字段变更时记录字段审计和 `FIELD_CHANGE` 电子签名。
- 一张执行表单允许多条电子签名记录，签名动作至少覆盖 `FIELD_CHANGE`、`FORM_REVIEW`、`SUBMIT`、`APPROVE`、`ARCHIVE_SEAL`。
- 最终打印以审批关闭后的受控 PDF 归档为准，下载 PDF 后可打印。
- 不实现实时 Excel 式多人光标协同；并发保存通过 hash/revision 基准冲突检测失败关闭。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。复用 eDHR 执行、字段审计、签名、审批、归档边界，避免把业务填写塞进 Jimu 设计器。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 在线填写字段并签名保存 -> Given 操作员打开一张草稿 eDHR 表单 / When 修改字段并输入当前账号密码保存 / Then 系统保存字段值，记录字段审计链，并生成 `FIELD_CHANGE` 电子签名。
- BDD: 同一张表单多人签名 -> Given 一张 eDHR 表单已有填写人字段签名 / When 另一名具备权限的用户执行表单复核、提交或审批该表单 / Then 同一执行记录下保留多条不同签名人、签名动作和签名含义的电子签名。
- BDD: 基准冲突失败关闭 -> Given 用户 A 和用户 B 同时打开同一草稿表单 / When 用户 A 先保存字段变更 / Then 用户 B 使用旧 hash/revision 保存时被拒绝，不能覆盖用户 A 的签名数据。
- BDD: 最终表单可打印 -> Given eDHR 表单审批通过并关闭 / When 授权用户输入电子签名密码生成 PDF 归档 / Then 系统封存签名、生成 `SEALED` PDF 归档，并提供下载用于打印。

## 里程碑

- [x] M1：创建 `edhr_jimu` 前后端独立 worktree 和同名分支。
- [x] M2：梳理现有 eDHR 在线填写、签名、归档能力，补齐缺口测试。
- [x] M3：实现后端多人签名汇总与最终打印归档契约。
- [x] M4：实现前端在线填写、多人签名展示、生成/下载最终表单入口。
- [x] M5：运行后端目标测试、前端静态/单元测试和必要的页面验证。
- [x] M6：运行 task-closeout-cleanup 预览并提交本任务改动。
- [x] M7：修复最终表单归档存储保全策略，重新验证下载/打印真实 E2E。

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionFieldAuditServiceTest,MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionControllerTest" test`
- 前端 eDHR 相关静态/脚本测试。
- Playwright 真实页面路径：打开 eDHR 执行详情，填写字段，保存签名，查看签名记录，生成并下载最终 PDF。

## 当前状态

completed；后端 `/cosign` 表单复核签名、`FORM_REVIEW` 签名动作、字段审计证据绑定、PDF 归档签名含义输出和目标回归验证已完成。M7 已修复最终表单归档生成时未显式传入 eDHR 受保护存储保全策略的问题，真实 E2E 已在测试租户 `测试租户/aoteman` 通过：重新生成 `SEALED` PDF 归档并下载，浏览器下载 Blob 的 SHA-256 与最新归档摘要一致，可作为最终表单打印件。
