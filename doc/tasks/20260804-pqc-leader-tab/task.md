# 20260804 生产组长与 PQC 组长独立菜单页签

## Task Goal

将生产组长和 PQC 组长改为类似“批次执行”的两个独立主导航页面；不再作为 eDHR 批次页面内部 Tab，也不再通过组长工作台混合展示。

## Milestones

- [x] 定位现有 eDHR 内部组长 Tab、共享工作台和动态菜单边界
- [ ] 编写最小静态合同，锁定生产组长/PQC组长均为独立主导航页面
- [ ] 实现两个独立菜单页面并移除 eDHR 内部组长 Tab/路由
- [ ] 运行定向验证并记录 RED/GREEN/REGRESSION 证据
- [ ] 完成收尾检查、清理和最终状态记录

## Expected Verification

- 运行任务专用静态合同，覆盖 `QA → 生产组长 → PQC组长 → 批次执行` 菜单顺序。
- 证明生产组长/PQC组长分别使用 `/mes/pro/process-pool/production-leader` 与 `/mes/pro/process-pool/pqc-leader`。
- 证明 eDHR 内部 tabs 和旧 `/mes/pro/feedback/edhr-batch-*-leader` 路由不再承载组长内容。
- 运行相邻前端静态合同或 `pnpm ts:check`，若受历史无关问题阻塞则记录首个无关失败。
- 运行 `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-pqc-leader-tab/frontend-feature-evidence.md`。
- 运行 `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260804-pqc-leader-tab/database-schema-evidence.md`。

## Current Status

in_progress

- 用户已纠正旧口径：生产组长和 PQC 组长都必须是类似“批次执行”的独立主导航页面，当前正在重新执行 RED/GREEN。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，使用正式动态菜单、独立路由和角色锁定包装页表达入口边界。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：静态合同 PASS 与真实 E2E PASS 必须分开记录；本任务执行静态合同和 `pnpm ts:check`，未将静态合同冒充真实 Playwright。
- `docs/e2e-rules.md#Windows 换行与脚本行为同步`：更新 `tests/e2e/*static.spec.js` 时按稳定文件/组件/路由标记断言，不依赖坐标或截图。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 组长页继续读取 `pqcItemDetails/itemResults` 项目级明细，不恢复固定 `length/appearance/seal/pressure` 或 legacy `pqcPieceValues` 作为权威事实。
