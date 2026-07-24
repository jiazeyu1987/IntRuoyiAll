# 任务：修复版本中心查看版本误切公开站点绑定租户

## 任务目标

- 修复管理端公司/产品版本中心“查看版本”在传入 `siteKey + stage` 后误切到公开站点绑定租户，导致当前登录租户自己的历史版本报 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。
- 保持公开 release 诊断能力，但已选版本与当前内容必须基于当前登录租户读取。
- 不引入 fallback，不修改业务数据；仅修复版本中心管理端读取租户边界。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：是
- 是否存在临时补丁或绕过：否

## BDD 场景

- BDD: 管理端查看版本应以当前登录租户读取已选版本 -> Given 当前登录租户存在公司历史版本 bundle 且同 `siteKey + stage` 公开绑定指向其他租户 / When 管理端请求版本中心详情 / Then 已选版本必须从当前登录租户读取成功，不得因绑定租户缺 bundle 而报 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`
- BDD: 管理端历史列表应以当前登录租户读取版本包 -> Given 当前登录租户存在历史版本 bundle 且公开绑定指向其他租户 / When 管理端请求版本中心历史 / Then 历史列表基于当前登录租户返回，当前线上版本信息只作为公开 release 诊断信息

## 里程碑

- [x] M1：建立任务记录与 BDD 场景。
- [x] M2：补充 RED 回归测试，复现绑定租户误读当前登录租户版本。
- [x] M3：最小修复版本中心管理端读路径的租户边界。
- [x] M4：完成 GREEN/REGRESSION 验证并记录证据。
- [x] M5：收尾清理并提交。

## 验证计划

- RED：新增 `ShowroomVersionCenterServiceTest`，证明绑定租户切到其他租户时 `getDetail/getHistory` 错误读取并抛 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`。
- GREEN：修复后同一用例返回当前登录租户的历史详情/历史列表。
- REGRESSION：现有辅助 blocker 场景继续通过。

## 当前状态

状态：已完成。

## Current Status

completed
