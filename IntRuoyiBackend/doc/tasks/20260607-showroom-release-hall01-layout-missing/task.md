# 任务：排查 hall_01 发布缺少画布布局

## 任务目标

确认并修复 `SHOWROOM_RELEASE_HALL_BLOCKED: hallId=1 hallCode=hall_01 reason=SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout is required` 的触发原因。用户已明确授权将 `芋道源码/admin` 租户中布局信息为 `NULL` 的展柜产品映射写入后端默认布局信息。

## Previous Task Check

- 上一后端任务：`doc/tasks/20260606-runtime-console-build-test-backup-release/task.md`。
- 检查结果：`Current Status` 为 `completed`。

## BDD 场景

- BDD: 发布阶段阻止缺失画布布局的展柜 -> Given 展柜 `hall_01` 存在产品映射但任一映射缺少 `layoutX/layoutY/layoutWidth/layoutHeight` / When 用户手动发布展厅 / Then 后端抛出 `SHOWROOM_RELEASE_HALL_BLOCKED`，不得生成默认布局。
- BDD: 授权后修复空画布布局 -> Given 用户明确授权修复 `芋道源码/admin` 租户空布局映射 / When 只读确认缺失展柜均为整柜布局为空并执行默认布局修复 / Then 缺失展柜的每条产品映射写入完整 `layout_x/layout_y/layout_width/layout_height`，已存在完整布局的展柜不被覆盖。

## 里程碑

- [x] M1：创建任务文档并确认上一任务状态。
- [x] M2：只读核对 `hallId=1 / hall_01` 的租户、产品映射数量和布局字段完整性。
- [x] M3：定位发布失败的代码路径和数据前置条件。
- [x] M4：记录结论、验证证据和修复边界。
- [x] M5：按用户授权只修复 `芋道源码/admin` 租户整柜空布局映射。
- [x] M6：只读校验所有参与发布的展柜映射布局完整性。

## 预期验证

- 只读 SQL 或后台 API 证明 `hallId=1 / hall_01` 至少一条产品映射缺少完整画布布局。
- 代码路径证明发布阶段调用 `ShowroomHallCanvasLayoutPolicy.requireCanvasLayout` 并按 fail-fast 策略阻止发布。
- 修复前只读统计 `tenant_id=1` 所有展柜布局完整性。
- 修复后只读统计 `tenant_id=1` 所有展柜布局完整性，确认缺失布局数为 0。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是，仅限用户明确授权的数据修复范围：将 `芋道源码/admin` 租户中整柜布局为空的展柜产品映射写入与后端默认布局策略一致的矩形布局。触发条件：展柜内映射全部缺失布局；风险：默认布局不是人工精排；移除/回滚策略：后续在展柜管理画布中人工调整并保存会覆盖默认布局。
- `是否从根因和长期维护角度解决`：是。保留发布阶段 fail-fast 契约，不改代码降级；通过补齐正式持久化数据满足发布前置条件。
- `是否存在临时补丁或绕过`：否。

## 只读诊断结果

- `hallId=1` 属于 `tenant_id=1`，即 `芋道源码/admin` 租户。
- `hallCode=hall_01`，展柜名称为 `心内介植入展柜`。
- 该展柜共有 24 条 `showroom_hall_product` 产品映射。
- 24 条映射的 `layout_x/layout_y/layout_width/layout_height` 全部为 `NULL`。
- 发布阶段调用 `ShowroomHallCanvasLayoutPolicy.requireCanvasLayout`，缺少任一布局字段即抛出 `SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout is required`，外层包装为 `SHOWROOM_RELEASE_HALL_BLOCKED`。

## Blocker

- 阻塞项：`芋道源码/admin` 租户的 `hall_01` 尚未保存正式画布布局。
- 影响：当前不能手动发布展厅；后端按无 fallback 契约正确阻止发布。
- 解除条件：在 `芋道源码/admin` 的展柜管理中为 `hall_01` 保存完整画布布局，且所有参与发布的展柜均保存完整布局后，再重新手动发布。
- 权限边界：未经用户明确授权，不写入 `芋道源码/admin` 租户数据。

## User Authorization

- 2026-06-07：用户明确要求“芋道源码/admin租户的展柜的布局信息是null的都改成默认的布局信息”。

## 修复结果

- 修复前：`tenant_id=1` 共 8 个展柜；`hall_03 / 外周介植入展柜` 已有完整布局，其余 7 个展柜为整柜布局为空，共 137 条产品映射缺少布局。
- 修复动作：按后端 `ShowroomHallCanvasLayoutPolicy` 的默认网格算法，为这 7 个整柜布局为空的展柜写入 `layout_x/layout_y/layout_width/layout_height`；未覆盖已完整的 `hall_03`。
- 修复后：8 个展柜全部 `complete_count = mapping_count`，`all_null_count = 0`，`partial_count = 0`，`REPAIR_TARGET_COUNT=0`。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260607-showroom-release-hall01-layout-missing/bug-regression-evidence.md`
