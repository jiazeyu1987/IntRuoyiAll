# Frontend Feature Evidence

## Feature Goal

在个人工作台“配置”页签内提供独立的“ERP表格自动同步”入口，让有配置权限的用户选择每日开始时间和 ERP 表格，保存后可回显，并查看正式同步水位和运行记录。

## Non-Goals

- 不修改 NAS 表格导出配置和类型。
- 不在前端复制 Kingdee 同步逻辑。
- 不通过 mock 数据或 API-only 代替真实页面验证。

## Requirements And Acceptance

- A1：配置页签显示独立的“ERP表格自动同步”子页签。
- A2：支持启用、选择时间、选择正式 ERP 表格并保存。
- A3：刷新或重新进入后按后端正式配置回显。
- A4：展示正式同步水位和最近运行记录。
- A5：运行记录使用中文触发来源、中文状态、可读日期时间和中文失败原因列名。
- A6：配置入口继续受 `mes:pro-batch-record-execution:golden-finger` 权限控制。

## UI Entry And Owned Files

- 页面入口：`/user/profile`。
- 用户路径：个人工作台 -> 配置 -> ERP表格自动同步。
- 组件：`src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue`。
- API wrapper：`src/api/erp/kingdeeTableAutoSync/index.ts`。
- 容器页：`src/views/Profile/Index.vue`。

## API And UI States

- 加载同步类型、配置、运行记录和水位时显示独立 loading 状态。
- 配置主加载失败显示不可关闭的错误 alert，并通过 `ElMessage` 显示真实错误。
- 保存和立即执行失败显示后端真实错误，不返回默认成功。
- 空配置默认禁用、时间为 `02:00:00`，但启用保存仍由后端正式校验。
- 同步类型由后端返回，前端不硬编码 NAS 类型或 mock 选项。

## BDD

- BDD: 保存并回显配置 -> Given 用户进入 ERP 自动同步页签，When 选择时间、商品和库存后保存并刷新，Then 页面回显启用、时间和两个选项。
- BDD: 权限边界 -> Given 用户不能查看配置页签，When 进入个人工作台，Then 不显示 ERP 自动同步入口。
- BDD: 可读运行结果 -> Given 已有正式运行记录，When 打开页签，Then 页面显示“自动调度”“成功”、可读日期时间和“失败原因”，不显示 `20`、13 位时间戳或 `failureMessage` 列名。

## TDD And Verification

- RED: `node tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> FAIL，组件和 API 初始不存在。
- RED: 可读展示合同新增后同一命令 -> FAIL，旧组件仍显示 `failureMessage`、状态数字和毫秒时间戳。
- GREEN: `node tests\e2e\profile-erp-table-auto-sync-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\profile-nas-table-auto-sync-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check` -> PASS。

## Responsive And Accessibility Checks

- 配置卡片限制最大宽度，复选框组可换行，避免窄屏选项互相挤压。
- Switch、时间选择器、复选框、tab 和按钮均保留 Element Plus 原生角色与键盘语义。
- Playwright 通过可见 `.el-switch` 操作开关，并读取隐藏 input 的 `aria-checked`/checked 状态验证。
- 页面无控制台错误。

## Real E2E

- 运行入口：前端 `http://127.0.0.1:8083`，后端 `http://127.0.0.1:48083`。
- 身份：本机 `芋道源码/admin`。
- 页面操作：登录 -> 个人工作台 -> 配置 -> ERP表格自动同步 -> 启用 -> 时间 `03:25:00` -> 勾选 `PRODUCT`、`STOCK` -> 保存。
- 页面结果：出现“ERP表格自动同步配置已保存”；刷新并重新进入后启用状态、时间和两个选项完整回显。
- 最终读回：本机开发库确认租户 1 配置、CRON `0 25 3 * * ?` 和两个启用类型与页面一致。
- 清理：通过页面恢复为禁用，保留时间和选项用于回显，不删除正式运行记录或水位。
- 可读展示复验：页面显示“自动调度”“成功”、格式化日期时间和“失败原因”，不再显示原始状态 `20`、13 位时间戳或英文列名。

## Blockers And Follow-Up

- 无前端交付 blocker。
- “立即执行一次”未在真实页面触发，避免产生真实 ERP 拉取副作用；配置保存、回显和只读运行记录链路已完成真实 E2E。
