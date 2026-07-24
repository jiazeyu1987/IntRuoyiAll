# 任务：电子签名治理增强文档与代码实施方案

## 目标

在独立 worktree 中，由主 agent 作为 reviewer，启动 4 个子 agent 分别编写以下文档，并在主 reviewer gate 通过后才允许后续代码实施：

- 长期防篡改留存证据：MinIO Object Lock / WORM、归档保留策略、恢复演练证明签名证据和归档长期可恢复。
- 签名周期性审阅：签名权限、锁定、失败记录、异常签名证据由质量人员定期复核页面或报表。
- CSV/质量体系材料：URS/FRS、风险评估、IQ/OQ/PQ、追溯矩阵、电子签名 SOP、培训记录、变更控制。
- 跨模块统一策略：DCC、eDHR、Showroom、IntAuth 的签名链路由统一策略源约束，避免规则漂移。

## 范围

- 后端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\ruoyi-vue-pro`
- 前端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\yudao-ui-admin-vue3`
- 分支：`codex/20260528-signature-governance-docs`
- 本轮交付以文档和后续代码实施计划为主，不直接改生产业务代码。

## 非范围

- 不实际开启 MinIO Object Lock、修改生产存储桶、执行恢复或触碰正式数据。
- 不修改 live 审核矩阵。
- 不新增 mock、fallback、兼容降级或默认成功路径。
- 不把法规符合性写成已认证结论；只能形成工程控制、验证和质量体系证据要求。

## 前置任务检查

- 根目录最近任务 `doc/tasks/20260528-fupan-project-memory-refresh/task.md` 状态为 `completed`。
- 后端最近任务 `ruoyi-vue-pro/doc/tasks/20260527-dcc-admin-e2e-repair/task.md` 状态为 `completed`。
- 前端最近任务 `yudao-ui-admin-vue3/doc/tasks/20260527-dcc-admin-e2e-repair/task.md` 状态为 `completed`。

## 里程碑

- [x] M1：创建成对 worktree 和任务文档骨架。
- [x] M2：4 个子 agent 分别写入独立主题文档。
- [x] M3：主 reviewer 按放行标准审查文档逻辑、接口、BDD/TDD 和副作用。
- [x] M4：必要时让对应子 agent 修订，最多两轮。
- [x] M5：生成最终 reviewer gate、测试报告、收尾清理预览并提交本任务直接改动。

## 放行标准

只有同时满足以下条件才可放行：

1. 文档可以支撑后续实现上述 4 个目标，并明确无副作用边界、阻塞项和回滚/移除策略。
2. 文档按 BDD + 严格 TDD + subagent-driven 方式组织，包含可执行 RED/GREEN/REGRESSION 验证路径。
3. 逻辑自洽，接口清晰，模块边界、数据模型、权限、配置、失败行为和验收证据可追踪。

## 预期验证

- `python C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root <task-dir>`
- `python C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root <task-dir>`
- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root <task-dir>`
- `rg -n "BDD:|RED:|GREEN:|REGRESSION:|Design Blockers|Test Blockers|Reviewer Decision" <task-dir>`

## 当前状态

- 状态：completed
- 当前阶段：完成
- 阻塞：无
- 最终验证：产品/系统/验收文档结构校验 PASS，关键 BDD/TDD/Blocker 证据检索 PASS，reviewer gate 为文档与实施计划放行。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260528-signature-governance-docs/docs/`
- `doc/tasks/20260528-signature-governance-docs/subagent-output/`
- `doc/tasks/20260528-signature-governance-docs/prd.md`
- `doc/tasks/20260528-signature-governance-docs/request-analysis.md`
- `doc/tasks/20260528-signature-governance-docs/dev-plan.md`
- `doc/tasks/20260528-signature-governance-docs/test-plan.md`
- `doc/tasks/20260528-signature-governance-docs/test-report.md`
- `doc/tasks/20260528-signature-governance-docs/reviewer-gate.md`
- `doc/tasks/20260528-signature-governance-docs/task-state.json`
