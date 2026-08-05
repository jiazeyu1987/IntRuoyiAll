# 个人工作台 NAS 表格自动同步

## Task Goal

在个人工作台的配置区域新增独立页签“NAS表格自动同步”，允许有配置页签可见权限的用户统一管理每天自动同步哪些 ERP 表数据、几点开始同步以及 NAS 输出规则；开发需先形成 BDD + TDD 设计文档，再按文档完成前后端、数据库、调度和 E2E 验证。

## Milestones

- [x] 创建独立 worktree 并确认主工作区脏改动不属于本任务。
- [x] 登记 worktree runtime slot，准备后续本地成对运行态验证。
- [x] 输出系统设计文档与 BDD/TDD 验收文档。
- [x] 编写 RED 测试，覆盖数据库、后端 API/Job、前端页签与权限边界。
- [x] 实现数据库迁移、后端服务/API/Job、前端页签和配置交互。
- [x] 跑通 GREEN 定向验证与真实前端路径 E2E。
- [ ] 完成收尾、经验沉淀、提交与推送。

## Expected Verification

- 系统设计文档结构校验通过。
- BDD/TDD 验收计划结构校验通过。
- 数据库迁移静态/策略测试通过。
- 后端目标 JUnit 或静态契约通过，覆盖配置保存、Job 绑定、立即执行、NAS 写入失败快失败。
- 前端静态契约或组件测试通过，覆盖“NAS表格自动同步”页签、配置页权限边界、API 调用和错误展示。
- worktree 成对前后端运行态通过真实 Playwright E2E，从个人工作台配置页签完成一次配置保存和一次手动同步/失败可见验证。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。

## Current Status

ready_for_closeout - NAS 表格自动同步设计、开发和定向验证已完成；真实 E2E 已在 `http://127.0.0.1:8088` / `http://127.0.0.1:48088` 通过，测试租户计划已清理为禁用，临时 E2E 结果目录已删除，实现提交 `1e4a61500` 已生成。cleanup apply / worktree 移除当前阻塞于 `E:\IntRuoyi` 主工作区 dirty 且当前分支暂不能 ff-only 合入 `int_main`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；已按正式 worktree slot 规则登记运行态端口，后续按 BDD/TDD 和正式调度链路推进。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- worktree 必须位于 `D:\IntRuoyiWorktree\` 下，且运行态必须通过 `scripts\runtime\reserve-worktree-slot.ps1` 登记 profile/slot/port，不得随机换端口。
- 当前 feature 必须使用 BDD + strict TDD，生产代码变更必须记录 RED/GREEN 证据。
- 个人工作台配置页签权限边界必须沿用现有“配置”页签可见性，后端接口仍需有正式 permission 校验，不得仅靠前端隐藏。
- NAS/ERP/调度失败必须快失败并可见，不得返回 mock 成功、空成功或默认成功。

## Cleanup Keep

- doc\tasks\20260805-profile-nas-table-auto-sync\docs\acceptance\bdd-scenarios.md
- doc\tasks\20260805-profile-nas-table-auto-sync\docs\acceptance\e2e-plan.md
- doc\tasks\20260805-profile-nas-table-auto-sync\docs\acceptance\tdd-plan.md
- doc\tasks\20260805-profile-nas-table-auto-sync\docs\acceptance\test-data.md
- doc\tasks\20260805-profile-nas-table-auto-sync\docs\system\backend-api-design.md
- doc\tasks\20260805-profile-nas-table-auto-sync\docs\system\config-security-deployment.md
- doc\tasks\20260805-profile-nas-table-auto-sync\docs\system\data-model.md
- doc\tasks\20260805-profile-nas-table-auto-sync\docs\system\frontend-design.md

## Cleanup Candidates

- IntRuoyiFronted\test-results\profile-nas-table-auto-sync-real
