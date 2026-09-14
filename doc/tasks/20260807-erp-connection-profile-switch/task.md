# ERP 账套连接切换

## 任务目标

在个人工作台“配置 > ERP表格自动同步”页签指定区域增加测试账套/正式账套切换控件、独立保存按钮和当前连接状态。只有点击保存并由后端持久化成功后，后续 ERP 同步才切换到所选账套。

## 范围

- 前端：`ProfileErpTableAutoSyncSetting.vue` 增加当前连接展示、二选一分段控件、待保存状态和保存连接按钮。
- 后端：增加当前连接查询/保存 API；测试账套继续使用既有连接配置，正式账套读取独立隐藏配置。
- 运行数据：在本机 `infra_config` 配置正式账套连接和初始当前连接；不修改数据库 schema。
- 验证：后端单元测试、前端静态合同、TypeScript 检查和 Playwright 真实保存/回切路径。

## 里程碑

- [x] M1：核对现有 Profile 页面、正式 Job 同步链路、金蝶配置服务和运行数据来源。
- [x] M2：用 BDD 和失败测试固定 API、持久化、失败处理和 UI 交互契约。
- [x] M3：实现后端连接选择和有效配置解析。
- [x] M4：实现前端账套切换、保存和当前状态展示。
- [x] M5：配置本机两套连接并完成真实页面验证，最终恢复测试账套。
- [x] M6：归档验证证据并完成任务清理。

## 预期验证

- 后端 GET 返回 `TEST/PRODUCTION` 两个固定选项和实际保存的当前连接，不返回账号、密码、Cookie 或签名。
- 后端 PUT 只接受 `TEST/PRODUCTION`；目标连接缺失或无效时 fail fast，且不更新当前连接。
- `getEffectiveProperties()` 在保存正式账套后覆盖连接地址、账套、用户名、密码和 `lcid`，同步查询参数继续复用既有配置。
- 前端切换分段控件后，当前连接标签保持不变并显示待保存；保存成功后当前连接标签更新。
- 保存失败显示正式错误，不能伪造当前连接或吞掉异常。
- Playwright 从真实页面保存正式账套、刷新确认仍为正式账套，再通过同一页面恢复测试账套并刷新确认。

## 适用经验门禁

- 已读取 `docs/experience-index.md`，命中 `docs/frontend-development.md#ERP 表格同步 Job 链路门禁` 和 `docs/login-access.md#ERP 金蝶账套登录连通性门禁`。
- 账套切换不得改回旧 `/erp/kingdee-table-auto-sync/**` 链路；现有自动同步继续使用 `infra/job` 和正式增量同步 Job。
- 当前连接必须来自后端持久化值，不能由前端默认值、按钮选中态或登录 Cookie 推断。
- 中文用户名和真实连接验证必须保持 UTF-8；任务文档、日志、测试输出不得记录密码或 Cookie。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；正式账套配置缺失或选择值非法时直接失败，不回退测试账套。
- `是否从根因和长期维护角度解决`：是；账套选择持久化与实际 `getEffectiveProperties()` 解析统一由后端配置服务负责，所有同步消费者自动复用。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- `doc/tasks/20260807-erp-connection-profile-switch/backend-api-evidence.md`
- `doc/tasks/20260807-erp-connection-profile-switch/frontend-feature-evidence.md`
- `doc/tasks/20260807-erp-connection-profile-switch/runtime-patch/`
- `output/playwright/erp-connection-desktop-test.png`
- `output/playwright/erp-connection-desktop-production.png`
- `output/playwright/erp-connection-mobile-test.png`
- `output/playwright/erp-profile-mobile-test.png`
- `output/runtime/int_main/erp-switch-runtime-audit/`

## Cleanup Keep

- `output/runtime/int_main/backend-runtime-control-20260807-erp-connection-switch.jar`

## Current Status

completed

实现、定向测试、运行配置、真实页面验证和任务清理均已完成；当前连接已恢复为测试账套。
