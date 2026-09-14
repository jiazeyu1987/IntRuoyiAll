# 金蝶生产补料单列表定时同步

## Task Goal

实现可定时将金蝶系统中的“生产补料单列表”同步到系统内，并沿用现有金蝶同步运行框架、租户边界、运行记录、水位和失败传播机制。

## Milestones

- [x] 读取项目后端、数据库、worktree、运行态和收尾规则
- [x] 创建隔离 worktree 并登记运行端口槽位
- [x] 梳理现有生产领料单/用料清单同步链路和金蝶字段合同
- [x] 先补 BDD 与 RED 测试，锁定补料单同步行为
- [x] 实现最小后端、数据库、任务和前端入口改动
- [x] 执行定向测试、技能 evidence validator 和端口/任务文档验证
- [x] 合并到 `int_main` 并补充个人中心配置页真实 E2E

## Expected Verification

- 定向后端测试覆盖补料单同步类型、定时 Job、客户端字段解析、主子表落库、租户 ID 显式写入和运行记录水位。
- 静态/前端测试覆盖 ERP 同步页可见补料单同步项，且不回退到旧 `kingdee-table-auto-sync` 接口。
- 数据库 schema 测试覆盖补料单主表/明细表、唯一业务键、索引和租户字段。
- `backend-api-delivery` 与 `database-schema-delivery` evidence validator 必须 PASS。
- 用户已于 2026-09-05 明确要求执行真实 E2E；需覆盖真实登录、补料单列表渲染、正式增量同步提交和 Job 执行日志终态。
- 用户反馈 `int_main` 页面未看到补料单后，需在 `int_main` 真实前端 `8081` 覆盖个人中心 `配置 / ERP表格自动同步` 表格中“生产补料单列表”可见。

## Current Status

ready_for_closeout

## Design Constraints Check

- 已读取 `docs/backend-development.md`、`docs/database-rules.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/local-runtime.md`、`docs/task-closeout-rules.md`。
- Worktree: `D:\IntRuoyiWorktree\kingdee-production-replenishment-sync`。
- Branch: `codex/kingdee-production-replenishment-sync`。
- Runtime profile: `int_main`, slot `26`, frontend `8160`, backend `48160`。
- 禁止 fallback：缺少金蝶正式 FormId、字段标识、账号权限、schema 或测试依赖时必须 fail fast。
- 租户边界：主表和明细表均必须显式写入当前租户 ID，不依赖数据库默认值或拦截器补值。
- 验证结论：定向 Maven 后端测试 21 个用例 PASS，前端静态合同 PASS，backend/database evidence validator PASS，真实 Playwright E2E PASS。
- 实现提交：`58ed56166`，提交信息 `feat: sync Kingdee production replenishment lists`。
- E2E 验证提交：`b89520cc4`，提交信息 `test: verify Kingdee replenishment sync e2e`。
- 完成门禁变更：用户于本轮明确说明“不用push”，远端推送不再作为本轮完成条件；当前保留本地分支 ahead 状态。
- int_main 合并：已按仓库规则先提交主干现存脏改动基线，再将 `codex/kingdee-production-replenishment-sync` 合并到 `int_main`。
