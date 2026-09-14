# 智能排产测试管理测试项补充

## Task Goal

仿照测试管理中“排产工单手动重排样例测试项”的自然语言测试项形态，为智能排产模块补充可由 Codex Runner 执行的业务测试项种子，并用静态合同验证测试项、检查点和无 fallback 约束。

## Milestones

- [x] 梳理现有测试管理表结构、手动重排样例和智能排产 E2E 覆盖点。
- [x] 先新增失败合同，约束智能排产测试项种子必须存在且检查点完整。
- [x] 新增正式 SQL 迁移种子，覆盖智能排产关键业务路径。
- [x] 运行定向验证并记录 RED/GREEN/REGRESSION 证据。
- [ ] 完成收尾记录、清理预检和最终状态更新。

## Expected Verification

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_codex_smart_scheduling_test_items_seed.py -q`
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_codex_test_management_migration.py IntRuoyiBackend\script\tests\test_codex_smart_scheduling_test_items_seed.py -q`

## Current Status

ready_for_closeout

## Current Blockers

- Git closeout is blocked by unrelated concurrent workspace changes and local commits on shared branch `int_main`; do not stage, baseline, commit, push, or clean files outside this task without user coordination.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过迁移种子沉淀测试管理可见的智能排产测试项，而非临时运行一次脚本。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260726-codex-smart-scheduling-test-items/task.md`
- `doc/tasks/20260726-codex-smart-scheduling-test-items/execution-log.md`
- `doc/tasks/20260726-codex-smart-scheduling-test-items/verification-report.md`

## 经验门禁

### Codex Runner 自动测试门禁

- Trigger: 新增或验收 `系统管理 > 测试管理` 中由 Codex Runner 调用 Playwright 的自然语言测试项。
- Preflight check: 测试项必须有明确自然语言方法、目标测试数据归属、检查点和 `parallelSafe=false` 写入保护。
- Blocker: Runner 前置条件、目标租户、凭据映射或业务测试数据缺失时，真实执行必须阻塞，不能伪造通过。
- Verification: 记录测试项种子、检查点、执行命令、失败/通过证据和必要的只读核验。
- Forbidden action: 禁止用 API-only、mock 截图、默认成功、Runner 离线跳过或顺序执行降级冒充真实 E2E 通过。
- Evidence: `docs/e2e-rules.md#codex-runner-自动测试门禁`。

### Codex Runner 目标测试项存在性门禁

- Trigger: 用户要求运行或维护测试管理中的具体测试项名称。
- Preflight check: 先确保 `system_codex_test_case` 中存在启用、未删除、租户匹配的目标名称。
- Blocker: 目标测试项不存在、被删除、禁用或租户不匹配时停止；不得自动改跑其它测试项。
- Verification: 通过静态合同约束迁移中目标测试项名称、启用状态和检查点。
- Forbidden action: 禁止用模糊关键词误选其它测试项，禁止临时造数后宣称既有测试项可执行。
- Evidence: `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁`。

### Element Plus 表格选择门禁

- Trigger: 智能排产或手动重排测试项需要在 `el-table` 勾选排产工单行。
- Preflight check: 测试方法必须要求按页面可见业务唯一文本定位目标行，并在写入前断言已选集合。
- Blocker: 选中集合缺失目标行、包含额外行或误点表头复选框时停止。
- Verification: 检查点应覆盖选中范围、写入请求范围和最终 UI/API 状态。
- Forbidden action: 禁止用表头全选、数组下标、API-only、直接 SQL 或坐标猜测绕过可见业务行定位。
- Evidence: `docs/e2e-rules.md#element-plus-表格选择门禁`。
