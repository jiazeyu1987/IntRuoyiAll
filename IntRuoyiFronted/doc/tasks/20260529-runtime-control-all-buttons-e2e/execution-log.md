# 执行日志：运行控制台全按钮真实数据 E2E（前端）

- BDD: 前端按钮真实交互 -> Given 用户进入运行控制台 / When 点击每个按钮和弹窗确认按钮 / Then 前端必须发起真实请求、展示真实成功或明确阻断信息。
- GREEN: frontend worktree isolation -> PASS，当前前端分支为 `codex/20260529-runtime-control-all-buttons-e2e`。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` -> FAIL，expected reason: 脚本不存在。
- RED: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` -> FAIL，test-script reason: loading、响应捕获、Element Plus radio/弹窗关闭、候选阻断兼容不足。
- GREEN: `node tests\e2e\runtime-control-all-buttons-real.e2e.js` -> PASS，39 checks；测试租户执行真实按钮写入与门禁，`芋道源码/admin` 执行最终安全验证。
