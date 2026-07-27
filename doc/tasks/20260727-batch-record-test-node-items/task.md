# 批记录 6 个测试节点写入测试管理

## Task Goal

按用户确认的 6 个批记录测试节点：`解析`、`版本治理`、`绑定快照`、`批次任务`、`填写审批`、`归档追溯`，分别拆解测试方法项和测试目标项，并写入当前本机测试管理数据。

## Milestones

1. [x] 核对测试管理 schema、当前租户和现有批记录测试项状态。
2. [x] 设计 6 个节点对应的测试方法项和测试目标项。
3. [x] 写入当前租户测试管理数据，保证节点测试项可检索且检查点完整。
4. [x] 复核写入结果和非目标项目数据未受影响。

## Expected Verification

- 当前库 `system_codex_test_case.project` 和 `system_codex_test_checkpoint.case_id` 等字段存在。
- 写入前当前租户批记录测试项为 0 或不包含本次 6 个节点。
- 写入后当前租户 `批记录` 项目存在 6 个节点测试项，每项包含测试方法项和测试目标项。
- 非目标项目测试项数量不被本任务修改。

## Current Status

ready_for_closeout

## Result Summary

- 当前租户 `tenant_id=1` 已写入 6 个 `批记录` 节点测试项：解析、版本治理、绑定快照、批次任务、填写审批、归档追溯。
- 每个节点 3 个测试方法项、4 个测试目标项，共 18 个方法项、24 个目标检查点。
- 所有节点均为 `ENABLE`、`SEQUENTIAL`、`parallelSafe=false`。
- 非目标项目数量保持：`工艺路线` 4 个、`智能排产` 4 个。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先以现有测试管理数据契约落地 6 个节点，为后续增加正式 `测试节点` 字段或页面维度提供稳定内容基线。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 测试管理 schema 迁移门禁

- Trigger: 修改或运行 `system_codex_test_case`、测试项分页、测试管理页面相关接口。
- Preflight check: 写入前核对当前数据源存在 `system_codex_test_case.project` 及检查点关联字段。
- Blocker: 当前库缺少项目字段、检查点关联字段或无法确认写入租户范围时必须停止。
- Verification: 记录 schema 核对结果、写入目标、影响行数和写入后复核结果。
- Forbidden action: 禁止用默认 project、吞掉数据库异常、mock 成功或 API-only 空结果替代真实数据核验。
- Evidence: `docs/database-rules.md#测试管理-schema-迁移门禁`。

### Codex Runner 目标测试项存在性门禁

- Trigger: 新增、调整或执行系统管理-测试管理中的 Codex Runner 测试项。
- Preflight check: 写入前通过真实数据源核对目标测试项名称、状态、租户和删除标记。
- Blocker: 目标租户不明确、目标项冲突无法幂等更新、或写入后检查点不完整时必须停止。
- Verification: 写入后用同一数据源复核 6 个节点测试项可见且字段完整。
- Forbidden action: 禁止用模糊关键词误选其它测试项；禁止在缺少测试方法和目标项的情况下临时造数。
- Evidence: `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁`。
