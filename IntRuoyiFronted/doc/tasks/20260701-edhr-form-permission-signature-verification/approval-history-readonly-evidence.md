# eDHR 审批历史只读真实路径 E2E Evidence

- Task ID: `20260701-edhr-form-permission-signature-verification`
- 生成时间：2026-07-02T00:51:51.567Z
- 前端工作目录：D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3
- 固定前端入口：`http://localhost:8081`
- 固定测试租户：`测试租户`
- 默认账号名：`aoteman`；密码由 `EDHR_APPROVAL_HISTORY_PASSWORD` 注入，不写入仓库证据。
- 测试性质：只读真实 UI E2E，仅验证既有历史通过/驳回记录展示链路，不创建、不提交、不审批、不归档。
- 当前状态：PASS

## BDD

- BDD: 历史审批通过详情只读验证 -> Given 测试租户存在真实已关闭 eDHR 执行记录 / When 用户通过真实前端打开审批详情页 / Then 页面加载审批详情、追踪时间线、签名记录且展示 APPROVE 证据。
- BDD: 历史审批驳回详情只读验证 -> Given 测试租户存在真实已驳回 eDHR 执行记录 / When 用户通过真实前端打开审批详情页 / Then 页面加载审批详情、追踪时间线、签名记录且展示 REJECT 和驳回原因证据。
- BDD: 缺少真实前置即阻塞 -> Given 缺少登录密码、真实历史记录、菜单权限或前端入口 / When 执行 E2E / Then 写入 BLOCKED/FAIL 证据，不使用 mock、不改数据、不接口绕过。

## Steps

- 历史审批通过详情只读验证: PASS
  - executionId: `761`
  - executionCode: `BRE202606242357012100761`
  - statusLabel: 已关闭
  - requiredAction: APPROVE
  - screenshot: D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-approval-history-readonly\approved-history-761.png
- 历史审批驳回详情只读验证: PASS
  - executionId: `760`
  - executionCode: `BRE202606242356423200760`
  - statusLabel: 已驳回
  - requiredAction: REJECT
  - rejectReason: E2E-REJECT-1782316557196-T1
  - screenshot: D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\edhr-approval-history-readonly\rejected-history-760.png
