# eDHR 批次执行真实路径 E2E Evidence

- Task ID: `fix-batch-record-fill-rule`
- 状态：FAIL
- 前端入口：`http://localhost:8081`
- 测试租户：`测试租户`；账号名 `aoteman`，密码从既有本机 E2E 配置读取且未写入证据。
- 工单：`TESTERPA9ED2D417434` / `925555`
- 路线：`E2E-OSF-20260721061819` / `922194`
- 批次号：`FIX-RULE-20260724-20260724093309`
- open-or-create 请求路线：`922194`

## BDD

- BDD: 规则确认后打开填写 -> Given 测试租户存在已绑定批记录模板的真实工单 When 用户从批次执行页打开/创建批次并点击打开填写 Then 后端 `task/open` 返回成功且页面进入 eDHR 填写页，不出现未确认填写规则误报。

## Result

- RED: Playwright 真实前端路径 -> FAIL，open-or-create 业务响应失败：eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录

1040750403 !== 0

