# 执行日志：运行控制台全按钮真实数据 E2E（后端）

- BDD: 后端运行动作真实链路 -> Given Playwright 从运行控制台触发按钮 / When 后端接收运行控制台请求 / Then 返回真实执行结果或明确门禁阻断，不得 mock 成功。
- GREEN: backend worktree isolation -> PASS，当前后端分支为 `codex/20260529-runtime-control-all-buttons-e2e`。
- GREEN: backend runtime participation -> PASS，`runtime-control-all-buttons-real.e2e.js` 通过 `http://127.0.0.1:48081` 触发真实运行控制台接口。
- GREEN: release operation guard -> PASS，不存在的 `e2e_missing_*` ReleaseTag 创建真实 operation 后失败并写日志，未部署测试服或正式服。
- GREEN: rollback/restore candidate guard -> PASS，服务端候选缺 manifest/checksum/rehearsal/snapshot 时按钮禁用并展示阻断原因。
