# 20260904 Registration Upload E2E Verify

## Task Goal
修复并继续验证 `e2e_test/registration/upload/registration-certificate-upload-e2e-acceptance.md` 中注册证上传真实 E2E，重点处理 E2E-3 注册经理待办审批失败点，并记录所有失败场景分析结果。

## Milestones
- [x] 读取项目 E2E、登录、本地运行、worktree 与收尾规则。
- [x] 固化 E2E-3 已确认口径：页面展示真实姓名 `王立轩` 属于正确表现，账号仅作为登录身份标签。
- [x] 复现 E2E-3 当前失败点并补回归测试。
- [x] 实施最小修复并完成定向验证。
- [x] 将 E2E-3 修复融合进 `int_main` 并完成定向验证。`n- [ ] 继续真实页面 E2E 验证并记录失败场景分析。

## Expected Verification
- 文档静态校验：上传验收文档不得要求页面展示登录账号替代真实姓名。
- 后端/前端定向回归：覆盖 E2E-3 失败根因对应代码路径。
- Playwright 真实页面验证：使用芋道源码租户、申请人 `wanglixuan` 与注册经理 `chudongchuan` 完成 E2E-3 或记录准确 blocker。

## Current Status
in_progress

2026-09-05 18:58：E2E-3 修复已融合进 `int_main` 工作区，基线提交 `3bbbdae13` 已先保存主干原脏改动；BPM/DCC/文档定向验证通过。真实页面 E2E 仍需在 48081 运行态加载新后端后继续。

## Design Constraints Check
- E2E 必须由 Playwright 操作真实前端页面完成，API 仅用于只读核验。
- 不记录密码、token、cookie 或连接密钥。
- 不修改无关脏改动，不重启 `int_main` 后端，除非另获明确授权。
- 无 fallback、无 mock、无静默切换账号/租户/环境。

