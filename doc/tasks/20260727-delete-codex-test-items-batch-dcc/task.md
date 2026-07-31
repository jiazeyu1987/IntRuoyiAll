# 删除批记录/文控测试管理测试项

## Task Goal

删除当前测试管理中 `project` 为 `批记录` 或 `文控` 的测试项，并保留删除前后可核验证据，避免误删其他项目测试项。

## Milestones

1. [x] 核对测试管理表结构、目标数据来源和当前目标记录清单。
2. [x] 执行最小范围删除，仅处理当前租户 `project in ('批记录','文控')` 的测试项及其直属检查点。
3. [x] 复核目标项目测试项已删除，其他项目测试项未被误删。
4. [x] 记录验证报告与收尾状态。

## Expected Verification

- 删除前记录目标项目测试项名称、ID、项目字段和检查点数量。
- 删除后查询确认 `project` 为 `批记录` 或 `文控` 的未删除测试项数量为 0。
- 查询确认非目标项目测试项仍存在，且目标项关联检查点已随测试项删除或不再可见。

## Current Status

completed

## Result Summary

- 当前本地租户 `tenant_id=1` 删除 `批记录` / `文控` 测试项 10 个，直属检查点 32 个。
- 删除后当前租户 `批记录` / `文控` 未删除测试项为 0，直属检查点为 0。
- 非目标项目仍保留 8 个：`工艺路线` 4 个、`智能排产` 4 个。
- `tenant_id=122` 下仍有 2 个 `文控` 测试项，属于跨租户数据，不在当前 `芋道源码/admin` 页面上下文删除范围内。
- 证据文件：`doc/tasks/20260727-delete-codex-test-items-batch-dcc/artifacts/delete-batch-dcc-codex-test-items-summary.json`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按测试管理数据契约精确删除目标项目测试项，为后续按“测试节点”重建数据结构腾出干净基线。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### Codex Runner 目标测试项存在性门禁

- Trigger: 删除、调整或执行系统管理-测试管理中的 Codex Runner 测试项。
- Preflight check: 在删除前通过真实数据源核对目标测试项名称、状态、租户和删除标记。
- Blocker: 目标测试项不存在、租户不匹配、或删除条件无法精确限定到 `project` 字段时必须停止。
- Verification: 删除后用同一数据源复核目标测试项不可见且非目标测试项仍存在。
- Forbidden action: 禁止用模糊关键词误删其它测试项；禁止把空领取或页面空列表当作删除成功。
- Evidence: `docs/experience-index.md` 路由到 `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁`。

### 测试管理 schema 迁移门禁

- Trigger: 修改或运行 `system_codex_test_case`、测试项分页、测试管理页面相关接口。
- Preflight check: 删除前核对当前数据源存在 `system_codex_test_case.project` 及检查点关联字段。
- Blocker: 当前库缺少项目字段、检查点关联字段或无法确认删除范围时必须停止。
- Verification: 记录 schema 核对结果、目标记录清单、删除影响行数和删除后复核结果。
- Forbidden action: 禁止用前端隐藏错误、默认 project、吞掉数据库异常、mock 成功或 API-only 空结果替代真实数据核验。
- Evidence: `docs/database-rules.md#测试管理-schema-迁移门禁`。
