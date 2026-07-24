# 任务：eDHR Phase 6 模块去重与后台下沉清理（后端）

- Task ID: `20260701-edhr-phase6-module-dedup`
- Workspace: `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_phase\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

配合前端 Phase 6 去重，确认后端 eDHR API 是否存在可安全删除或必须保留为专业后台能力的重复接口；默认不删除后端接口，除非前端入口、菜单、服务调用和测试均证明无业务职责。

## Previous Task Check

- 上一个后端任务：`20260701-edhr-phase5-admin-downscoping`
- 状态：`completed`

## 经验门禁

- 命中 `docs/powershell-memory.md`。
- 命中 `docs/worktree-memory.md`。
- 命中 `simplify-codebase`：后端删除必须先证明调用面与业务职责均消失。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 后端 API 不因前端下沉误删 -> Given 一个 eDHR 专业页从主流程下沉为后台页 / When 执行 Phase 6 去重 / Then 后端专业查询或配置接口仍保留，除非证明无任何入口和调用。`

## Milestones

1. M1：建立 Phase 6 后端去重任务台账。`completed`
2. M2：扫描后端 eDHR API 面。`completed`
3. M3：标记后端接口保留/候选删除判断。`completed`

## Expected Verification

- 后端若无代码删除，则以接口职责矩阵和既有 Phase 1-5 测试证据为准。
- 若删除任何接口，必须补受影响 controller/service 测试并运行。

## Current Blockers

- 暂无。当前后端无已证明可安全删除的接口，本轮不删除。

## Final Verification

- 后端本轮无生产代码变更，不新增后端测试。
- 前端真实 E2E 已验证批次详情主流程入口、`/get` 与 `/workbench` 接口仍可正常访问。
- 结论：放行、审计、权限、模板、记录簿等后端接口仍属于专业后台/管理能力，不因前端下沉而删除。
