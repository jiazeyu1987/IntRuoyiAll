# 活跃订单宽关键词搜索遗漏修复

## Task Goal

修复生产组长“新增活跃订单”弹窗输入宽关键词 `88` 时，已经满足正式路线、DCC 项目和 QA 条件的目标生产工单未出现在候选列表的问题。

## Milestones

- [x] M1：复现页面/API 症状并定位候选遗漏根因。
- [x] M2：记录 BDD 并新增确定性失败回归测试（RED）。
- [x] M3：实施最小正式修复并通过定向测试（GREEN）。
- [x] M4：完成相邻回归、运行态和 Playwright 真实页面验证。
- [x] M5：完成经验沉淀与任务清理收尾。

## Expected Verification

- 输入 `88` 时，`881MO090935`、`881MO090972`、`881MO090973`、`881MO090974` 均出现在候选列表且显示“符合要求”。
- 已取消工单即使匹配关键词也必须继续显示明确不可加入原因或按既有候选契约处理，不得被当作可加入。
- 候选上限不能在资格判定前无序截断，导致符合要求的工单被大量不符合要求的匹配项挤出。
- 不放宽正式路线、唯一 ACTIVE 版本、DCC 项目代码和已发布 QA 资格门禁。
- 定向 JUnit、相邻回归和 Playwright 真实页面验证通过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；修复候选搜索的排序/截断边界，不绕过资格校验。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- 活跃订单候选资格必须继续使用工单产品的唯一正式路线、唯一 ACTIVE 版本、精确 DCC 项目代码和已发布 QA，不得用名称、默认路线或旧版本规程补齐。
- 孤儿路线绑定不得参与有效路线唯一性；取消工单必须在加载路线、DCC 和 QA 前先行阻断。
- 宽关键词修复只能调整正式候选查询和截断顺序，不得放宽资格条件或改成前端本地补齐。
- Maven 定向测试使用 reactor `-am`，PowerShell 中 `-D` 参数整体加引号并记录 Surefire 结果。

## Current Status

completed

任务已完成。当前共享后端 JAR 经字节码核对包含本次修复，`/actuator/health` 为 `UP`；Playwright 真实页面输入 `88` 后四个目标工单均显示“符合要求”，精确搜索已取消工单仍显示“生产工单已取消”，且未执行“加入活跃订单”写操作。可复用经验已合并到现有后端经验文档，任务临时产物已按 preview/apply 清理。

## Cleanup Keep

- `doc/tasks/20260809-active-order-88-search-results/bug-regression-evidence.md`
- `doc/tasks/20260809-active-order-88-search-results/playwright-active-order-88.png`

## Cleanup Candidates

- `doc/tasks/20260809-active-order-88-search-results/current-runtime-inspection/`
- `doc/tasks/20260809-active-order-88-search-results/isolated-classes/`
- `doc/tasks/20260809-active-order-88-search-results/isolated-classes-failed1/`
- `doc/tasks/20260809-active-order-88-search-results/isolated-classes-failed2/`
- `doc/tasks/20260809-active-order-88-search-results/latest-runtime-inspection/`
- `doc/tasks/20260809-active-order-88-search-results/package-inspection/`
- `doc/tasks/20260809-active-order-88-search-results/qa-runtime-inspection/`
- `doc/tasks/20260809-active-order-88-search-results/runtime-inspection/`
