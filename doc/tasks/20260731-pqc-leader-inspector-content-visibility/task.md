# 20260731 PQC 组长查看检验员提交内容

## Task Goal

实现或修复 PQC 组长可在授权负责范围内查看每个 PQC 检验员提交内容的正式链路，不能只靠前端隐藏，也不能用模拟、空值或默认成功掩盖缺失数据。

## Milestones

- [ ] 梳理现有 PQC 提交、组长范围、提交内容详情与页面入口。
- [ ] 补充 BDD 场景和 RED 测试，证明当前 PQC 组长无法看到每个检验员提交内容。
- [ ] 实现最小正式方案，保持后端范围过滤和只读明细可追溯。
- [ ] 运行 GREEN 与相关回归验证，记录证据。
- [ ] 完成 cleanup 前状态和最终收尾记录。

## Expected Verification

- 后端或静态合同测试覆盖：PQC 组长能看到负责范围内每个 PQC 检验员的提交内容。
- 权限测试覆盖：PQC 组长不能查看负责范围外的提交明细。
- 若涉及前端页面，静态或组件测试覆盖页面展示提交人和提交内容字段。
- 验证不引入 fallback、模拟成功、前端本地越权隐藏或异常吞掉。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在正式提交内容与组长负责范围链路上解决。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：当前需求使用聚焦静态契约先 RED 后 GREEN，不用无关全量失败冒充本任务结果。
- `docs/e2e-rules.md#e2e-脚本入口存在性门禁`：本轮只声明静态合同与类型/后端定向测试通过，未把真实 Playwright E2E 写成已通过。
- `docs/powershell-memory.md#脏工作区基线门禁`：任务开始时工作区已有脏改动，已单独提交前线页面基线 `a9deae829`，本任务后续只选择性处理目标文件。
- `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`：Maven `-Dtest` 与 `-Dsurefire.failIfNoSpecifiedTests=false` 均整体加双引号执行。

## Workspace Baseline

- 初始 `git status --short --branch` 显示当前分支 `int_main...origin/int_main [ahead 10]`，且已有非本任务脏改动。后续若必须修改同一文件，将先按冲突规则处理。

## Closeout

- Cleanup preview/apply 已通过，保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、无阻塞、无 warning。
- 经验沉淀复核：本次“PQC 页签不能继续占位、静态合同不能冒充真实 E2E”的经验已由 `docs/frontend-development.md#前端静态契约隔离门禁` 和 `docs/e2e-rules.md#e2e-脚本入口存在性门禁` 覆盖，未新增长期经验文档。
- Push blocker：`git push origin int_main` 失败，GitHub 443 连接经 `127.0.0.1` 代理不可达；本地提交已完成但远端未同步，因此任务不能标记为 completed。
