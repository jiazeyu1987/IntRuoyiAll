# 任务：修复展厅产品资源包导入 target_market 超长系统异常

- Task ID: 20260703-showroom-product-import-target-market-overflow
- Created: 2026-07-03
- Current Status: completed

## Current Status

completed

## Task Goal
修复测试服导入展厅产品资源包时因 `showroom_product_revision.target_market` 字段长度不足导致的数据库截断和前端“系统异常”，使导入失败/成功行为可预期、可回归。

## Milestones

1. 复现并记录测试服导入异常根因。completed
2. 补充 BDD/TDD 回归，覆盖长 target_market 导入。completed
3. 修复数据库 schema 与本地测试表定义。completed
4. 运行目标测试、schema 门禁和测试服真实导入验证。completed
5. 提交当前任务改动并说明测试服处理方式。completed

## Expected Verification

- 目标后端测试覆盖长 `target_market` 导入不再触发字段截断。
- SQL/schema 门禁验证通过。
- 测试服真实页面导入 `showroom-product-resource-package.zip` 成功，产品/奖项失败数均为 0。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：PowerShell 中文与多行脚本必须走 UTF-8 安全路径。
- 已读取 `docs/experience-index.md`：涉及测试服/真实导入需记录 experience-preflight。
- 已读取 `docs/server-access.md`：测试服操作以只读定位为主，写入/发布需明确记录目标。
- 已读取 `docs/login-access.md`：真实 E2E/Login 需按标准入口，当前使用测试服 `芋道源码/admin` 登录并走真实产品管理导入入口。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，schema、初始建表、H2 测试表和发布迁移契约已对齐。
- 是否存在临时补丁或绕过：否。测试服已直接执行同等 schema 扩容以恢复当前环境，仓库已新增正式迁移脚本防止后续环境漂移。

## Final Verification Result

- RED：目标 Maven 测试在 H2 `target_market varchar(255)` 下复现 `Value too long`。
- GREEN：目标 Maven 测试通过，SQL/release 合同测试通过。
- TEST-SERVER：真实页面导入资源包通过，返回 `totalRows=150`、`failureCount=0`、`awardTotalRows=46`、`awardFailureCount=0`。

## Current Blockers

- 暂无。
## Cleanup Keep

- doc/tasks/20260703-showroom-product-import-target-market-overflow/database-schema-evidence.md
- doc/tasks/20260703-showroom-product-import-target-market-overflow/verify-test-server-import.cjs
