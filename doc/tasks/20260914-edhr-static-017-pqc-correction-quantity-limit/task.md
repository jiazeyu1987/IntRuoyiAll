# EDHR-STATIC-017 PQC 更正数量上限

## Task Goal

修复 EDHR-STATIC-017：PQC 检验更正入口不得接受损耗/报废数量大于实际检验数量的更正，并且不得低于逐件不合格/报废明细要求；非法输入必须在写入正式记录、逐件明细、PQC 记录、事件或签名前 fail fast 拒绝。

## Scope

- 仅处理 EDHR-STATIC-017。
- 不编辑共享缺陷总表或其它缺陷项。
- 不执行 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作或 git push。
- 2026-09-14 用户追加授权：提交并融合进 `int_main`。

## Milestones

- [x] 读取仓库规则、closeout 规则、bug-regression-fix-loop 技能和相关 eDHR/PQC 数量规则。
- [x] 建立任务文档并记录 BDD/TDD 约束。
- [x] 增加静态/单元回归，先证明更正入口缺少实际检验数量上限。
- [x] 最小化修复后端更正校验，并补齐前端写入前校验。
- [x] 执行定向非 E2E 验证与静态合同检查。
- [x] 将 EDHR-STATIC-017 切片融合到 `int_main` 并完成主线程定向复验。
- [x] 收尾记录 changed paths、验证结果、风险和 blockers。

## Expected Verification

- 定向后端回归：覆盖 `actualQuantity=5, scrapQuantity=10` 被拒绝。
- 定向后端回归：覆盖 `actualQuantity=5, scrapQuantity=1` 且逐件至少 2 条失败/报废明细时被拒绝。
- 定向后端回归：覆盖 `actualQuantity=5, scrapQuantity=2` 且逐件 2 条失败/报废明细时可继续进入现有写入流程。
- 静态前端合同检查：PQC 更正 payload 构建前校验损耗数量不得大于实际检验数量，且不得低于逐件失败/报废要求。
- 不运行 E2E，不写数据库，不启动/停止/重启服务。

## Design Constraint Checks

- No fallback: 不增加兼容分支、静默降级或默认成功。
- Fail fast: 数量关系不合法时在签名与任何正式记录写入前拒绝。
- Source parity: 更正入口与一线首次 PQC 提交保持相同数量关系边界。
- Piece detail floor: 损耗/报废数量不能低于逐件不合格/报废明细所需数量。
- Scope isolation: 不处理 EDHR-STATIC-015/016/018+，不编辑共享缺陷总表。
- User constraints: 本轮允许 git commit 和本地 `int_main` 融合；仍不执行 E2E、数据库写入、服务操作、远程操作或 git push。

## Cleanup Keep

- doc/tasks/20260914-edhr-static-017-pqc-correction-quantity-limit/bug-regression-evidence.md

## Current Status

blocked

- 2026-09-14: 用户追加授权提交并融合进 `int_main`；按主线融合门禁，仅将 EDHR-STATIC-017 allow-list 切片同步到 `E:\IntRuoyi`，不整合 C 盘 detached worktree 中混杂的其它 DCC/runtime 脏改。
- 2026-09-14: EDHR-STATIC-017 代码已在本地 `int_main` 生效并通过主线程定向复验；`task-closeout-cleanup` preview/apply 均无删除和 blocker。项目规则仍要求 push 后才能标记 `completed`，但本轮未获得 git push 授权。
