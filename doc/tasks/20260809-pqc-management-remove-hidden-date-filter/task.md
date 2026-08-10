# PQC 管理移除隐藏日期过滤

## Task Goal

- 修复 `PQC组长 > PQC管理` 显示“暂无筛选条件”但请求仍按当天 `submitDate` 过滤的问题。
- 默认无筛选条件时查询当前 PQC 组长正式负责人员范围内的全部历史提交。
- 仅当用户明确添加“提交日期”筛选时按该日期过滤。

## Milestones

- [x] M1：建立任务文档、BDD 场景、根因和验证边界。
- [x] M2：以失败测试锁定后端可选日期契约和前端无隐藏日期请求。
- [x] M3：实现最小前后端修复并完成定向回归。
- [x] M4：通过本机真实页面验证默认历史列表和显式日期筛选。
- [x] M5：完成证据校验、经验沉淀和任务收尾。

## BDD Scenarios

- BDD: PQC 管理默认展示历史提交 -> Given 当前 PQC 组长负责范围内存在跨多个提交日期的正式 PQC 事件 / When 用户进入 `PQC管理` 且页面显示“暂无筛选条件” / Then 请求不携带 `submitDate`，列表按正式分页返回负责范围内全部历史提交。
- BDD: PQC 管理显式按提交日期筛选 -> Given 用户在复合筛选器中添加“提交日期”并选择某天 / When 应用筛选 / Then 请求携带所选 `submitDate`，后端只返回该自然日内的正式 PQC 事件。
- BDD: 空日期不是默认当天 -> Given 客户端未提交日期参数 / When 后端处理提交列表分页 / Then 不生成时间起止窗口且不抛“提交日期不能为空”，其它人员范围、事件类型、租户和分页条件保持生效。

## Expected Verification

- 后端单元测试覆盖空日期查询全历史和显式日期生成 `[day, day+1)` 窗口。
- Mapper 静态合同覆盖时间条件仅在时间窗口非空时生效。
- 前端静态合同覆盖默认 `submitDate` 为空、无条件请求不携带日期、显式日期请求仍携带日期。
- 运行相关后端 JUnit、前端静态测试和 `pnpm ts:check`。
- 使用本机 `芋道源码/admin` 真实登录进入 PQC 管理：默认列表显示历史记录；添加 `2026-08-08` 提交日期后只显示该日 5 条记录。

## Applicable Gates

- `docs/backend-development.md#MES PQC组长人员范围与管理数据可见性门禁`：继续按唯一启用人员范围读取，日期筛选只影响事件 `server_submit_time`。
- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：请求中的有效筛选必须与页面可见筛选状态一致。
- `docs/e2e-rules.md`：真实页面使用本机入口、真实登录和 Playwright，API 仅用于只读支持证据。
- `docs/experience-index.md`：命中 `PQC管理 No Data`、`submitDate`、`server_submit_time` 和可见复合筛选经验路由。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；这是用户明确要求的正式可选日期查询契约。
- `是否从根因和长期维护角度解决`：是；同步修正 API、服务时间窗口、Mapper 和前端筛选状态。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：生产代码、标准测试、类型检查、真实页面验收、经验沉淀和任务清理均已完成；本机生效运行态与验收证据已保留。

## Cleanup Result

- `task-closeout-cleanup` preview/apply 均通过，无阻塞或警告。
- 已删除本任务聚焦测试中间文件、热补丁暂存目录、辅助脚本和失败的 v1-v3 运行包。
- 已保留 `task.md`、`execution-log.md`、`verification-report.md`、Playwright 验收证据和当前生效的 v4 运行包。
- 未执行 Git 提交、合并或推送；用户未授权这些操作。
