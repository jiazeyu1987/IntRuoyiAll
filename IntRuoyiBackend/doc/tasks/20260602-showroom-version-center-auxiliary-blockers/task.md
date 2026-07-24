# 任务：修复公司版本中心辅助版本异常误打断详情

## 任务目标

- 修复展厅公司版本中心中“查看版本”时，辅助版本异常把已选版本详情整体打断的问题。
- 保持 fail-fast，但错误归因必须精确：`selectedVersion` 可读时，`CURRENT_CONTENT` 或 `CURRENT_RELEASE` 损坏只能作为 blocker 返回，不能中断详情接口。
- 不引入 fallback，不修改租户业务数据；仅修复后端版本中心详情诊断行为。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：是
- 是否存在临时补丁或绕过：否

## BDD 场景

- BDD: 公司版本详情应独立于当前发布异常可读 -> Given 选中的公司历史版本存在 readable bundle 且当前发布版本指针损坏 When 管理端请求版本中心详情 Then 接口返回选中版本详情，并把当前发布异常标记为 `CURRENT_RELEASE` blocker
- BDD: 公司版本详情应独立于当前内容异常可读 -> Given 选中的公司历史版本存在 readable bundle 且当前内容版本损坏 When 管理端请求版本中心详情 Then 接口返回选中版本详情，并把当前内容异常标记为 `CURRENT_CONTENT` blocker

## 里程碑

- [x] M1：建立任务记录与 BDD 场景。
- [x] M2：补充 RED 回归测试，复现当前发布辅助版本异常误打断详情。
- [x] M3：最小修复版本中心详情诊断逻辑。
- [x] M4：完成 GREEN/REGRESSION 验证并记录证据。
- [x] M5：收尾清理并提交。

## 验证计划

- RED：`ShowroomVersionCenterServiceTest` 新增用例，证明当前发布版本指针损坏时详情接口仍被打断。
- GREEN：修复后同一用例返回 `selectedVersion`，并产生 `CURRENT_RELEASE` blocker。
- REGRESSION：既有 `detailShouldKeepSelectedCompanyReadableWhenCurrentContentBundleIsMissing` 与 `detailShouldKeepSelectedCompanyReadableWhenCurrentContentRevisionIsMissing` 继续通过。

## 当前状态

状态：已完成。

## Current Status

completed
