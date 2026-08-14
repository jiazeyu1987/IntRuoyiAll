# 20260803 FormCenter Route Missing E2E

## Task Goal

使用真实 Playwright 页面路径验证 `请求地址不存在:admin-api/form-center/templates/28/versions/V3.0` 已被修复，重点确认运行态 FormCenter 动态表单不再请求模板管理版本接口 `/admin-api/form-center/templates/{id}/versions/{versionNo}`。

## Milestones

- [completed] 确认本 worktree 端口、运行态和 E2E 前置条件
- [completed] 定位可触发 FormCenter 运行态抽屉的真实页面路径和数据
- [completed] 执行 Playwright 网络审计，断言目标错误请求未出现
- [completed] 记录 E2E 证据、限制和最终结论

## Expected Verification

- 真实浏览器访问本 worktree 前端入口。
- 登录本机默认测试身份，不记录密码或 token。
- 打开包含 FormCenter 动态表单槽位的 eDHR / 批次详情运行态页面。
- 监听网络请求，断言未出现 `/admin-api/form-center/templates/28/versions/V3.0` 或任意 `/admin-api/form-center/templates/{id}/versions/{versionNo}` 运行态请求。
- 若运行态前置缺失，记录明确 blocker，不用 API-only 冒充 E2E 通过。

## Current Status

ready_for_closeout

真实 Playwright E2E 已通过。验证使用 worktree 前端 `http://127.0.0.1:8094`、后端 `http://127.0.0.1:48094`，打开芋道源码租户批次 `900000000910` 的 FormCenter 动态表单任务 `7234`（模板 `28 / V3.0`），网络审计未出现 `/admin-api/form-center/templates/28/versions/V3.0`、未出现任意 `/admin-api/form-center/templates/{id}/versions/{versionNo}`，也未出现“请求地址不存在”响应。

本轮尝试过自动填写打开路径，但当前真实数据在 `task/open` 前置上返回 `eDHR 批次缺少唯一批记录路线`，因此最终采用 admin 真实页面“查看表单”抽屉路径验证前端修复点；该路径仍经过真实浏览器、真实登录和真实 eDHR 批次详情页面，不使用 API-only 冒充 E2E。

Cleanup preview 已执行但无法 apply：当前分支不能快进合并到 `int_main`，且主工作区 `E:\IntRuoyi` 存在脏改动。按项目收尾规则，本任务保持 `ready_for_closeout`，不标记 `completed`，避免覆盖或清理非本任务状态。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅验证已修复运行态不再依赖模板管理版本接口。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260803-form-center-route-missing-e2e/form-center-route-missing-real-e2e.cjs
- doc/tasks/20260803-form-center-route-missing-e2e/real-e2e-output/form-center-route-missing-real-e2e-result.json
- doc/tasks/20260803-form-center-route-missing-e2e/real-e2e-output/form-center-route-missing-real-e2e.png
