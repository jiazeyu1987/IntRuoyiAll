# DCC/NAS 产品编号统一 DCC 项目代码口径

## Task Goal

将 DCC/NAS 链路中的产品编号来源统一为 DCC 项目代码 `DccProjectCode.projectCode`；DCC/NAS 新写入、提交、更新、导入链路必须写入 `productCode = projectCode`、`productName = projectName`、`productMasterId = null`。保留历史 `productMasterId/product_master_id` 字段用于旧数据展示和兼容读取，不改 MDM 产品主数据模块自身，不改展厅链路。

## Milestones

1. [in_progress] 建立 DCC/NAS 统一口径 BDD 与 RED 静态/单元契约。
2. [pending] 改造前端 DCC 外来评审、元数据弹窗、NAS 导入为 DCC 项目选择和只读自动生成产品编号。
3. [pending] 改造后端 DCC/NAS 写链路按 `dccProjectCodeId` 解析项目代码，清空 `productMasterId`。
4. [pending] 跑目标前后端验证、相邻回归和可行的真实页面 E2E。
5. [pending] 完成证据、经验沉淀、提交、推送和收尾。

## Expected Verification

- 静态契约先 RED 后 GREEN：DCC/NAS 前端不得出现用户可见产品主数据选择或 DCC product-options 调用，必须使用 DCC 项目代码自动生成产品编号。
- 后端目标单测先 RED 后 GREEN：DCC/NAS 新写入均使用 DCC 项目代码落库，`productMasterId` 为 `null`，且不调用 `MdmProductApi` 作为新写入来源。
- `mvn -pl yudao-module-dcc -am "-DskipTests" compile` 通过。
- 目标前端静态 E2E 与后端目标 JUnit 通过；全量 `pnpm ts:check` 若仍被无关 MES 历史问题阻塞，记录阻塞范围。
- 真实页面 E2E 尽量覆盖 DCC 上传、外来评审或 NAS 可达入口；如运行态或账号前置缺失，按 E2E 规则 fail-fast 记录。

## Current Status

in_progress

- 已完成实施前 Git 门禁：当前分支 `int_main`，`origin` 可用。
- 已按规则创建脏工作区基线提交 `a8ad9591`，包含任务开始前既有 MES 改动与 `20260728-rename-product-master-tab` 文档；本任务实现文件未进入基线。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务把 DCC/NAS 写入来源统一到 DCC 项目代码正式数据链路。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 适用门禁：严格无 fallback；缺少 DCC 项目代码时必须 fail-fast，不使用产品主数据、空值、默认值或临时编号补齐。
- 适用门禁：前端静态契约隔离门禁；若全量 `pnpm ts:check` 被无关历史问题阻塞，使用聚焦静态契约证明本任务行为。
- 适用门禁：PowerShell/Git 脏工作区基线门禁；任务实现提交必须与基线提交分离，并记录 commit hash 与文件清单。
- 适用门禁：E2E 真实路径门禁；真实页面验证必须使用 Playwright 用户路径，不用 API-only 替代。

## Cleanup Keep

doc/tasks/20260728-dcc-nas-product-code-unified/frontend-feature-evidence.md
doc/tasks/20260728-dcc-nas-product-code-unified/backend-api-evidence.md
doc/tasks/20260728-dcc-nas-product-code-unified/verification-report.md
