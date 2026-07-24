# 任务：执行一次展厅手动发布并核对结果

## 任务目标

- 使用真实前端入口执行一次“手动发布展厅”。
- 仅使用测试租户，确认当前本机最新 IntRuoyi 前后端上发布是否成功。
- 记录前端提示、接口响应和 `current release` 是否切换。

## 非目标

- 不修改前后端代码。
- 不使用 `芋道源码` 租户执行发布。
- 不直接改数据库修正数据，除非用户后续明确要求。

## 前序任务检查

- 已检查上一同仓任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-manual-release-super-admin-visibility\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成，不阻塞本次真实发布核查。

## 里程碑

- [ ] M1：建立任务记录并确认前后端本机入口可用。
- [ ] M2：使用测试租户通过真实前端执行一次“手动发布展厅”。
- [ ] M3：核对前端提示、接口返回和 `current release` 是否变化。
- [ ] M4：更新任务文档与执行日志。

## 预期验证

- 前端入口：`http://127.0.0.1:8081/showroom/company`
- 后端核对：`http://127.0.0.1:48081/showroom/release/current`
- 真实前端操作使用 Playwright

## 当前状态

状态：已完成

## Current Status

Completed

## Completed Work

- 已通过真实前端入口 `http://127.0.0.1:8081/showroom/company` 使用测试租户执行“手动发布展厅”。
- 已记录第一次因 `showroom_release_asset.uk_showroom_release_asset` 唯一键冲突失败的结果。
- 在后端修复并重启后，已再次通过真实前端复测，确认手动发布成功并切换了 `current release`。

## Final Verification

- 发布前 `current release`：`20260524T100623Z-316b86ad1758`
- 第一次真实前端发布 -> FAIL，报 `product-2-preview ... uk_showroom_release_asset` 唯一键冲突
- 后端修复后再次真实前端发布 -> PASS
- 发布后 `current release`：`20260524T163916Z-e03a7b68bf1a`
