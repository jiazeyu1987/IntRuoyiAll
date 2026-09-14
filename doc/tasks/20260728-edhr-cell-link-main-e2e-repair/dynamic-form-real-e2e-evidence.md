# eDHR 动态表单单元格链接真实 E2E Evidence
- 状态：PASS
- 前端入口：`http://127.0.0.1:8081`
- 后端入口：`http://127.0.0.1:48081`
- 授权租户/账号：`测试租户/codexedhrcell01`；不记录密码。
- 批次/任务：`EDHRB-1784886992840` / task `5368` / instance `255`
- 临时规则/待办：rule `26`，workTask `2284`，target `5:3` -> `field6`
## BDD
- BDD: Dynamic route form opens with production work order prefill -> Given 测试租户动态 FormCenter 表单存在 PRODUCTION_WORK_ORDER.batchCode 链接规则且工单表 batch_code 为空、eDHR 执行 batch_code 有值 When 用户从批次详情点击打开填写 Then FormCenter 实例草稿和页面控件必须显示 eDHR 执行上下文批号。
## Result
- GREEN: task/open 返回 FormCenter 实例 `255`。
- GREEN: bpm_form_action_instance.form_data_json[`field6`] = `FIX-RULE-20260724-20260724175622`。
- GREEN: 页面动态表单输入控件显示 `FIX-RULE-20260724-20260724175622`。
- CLEANUP: {"workTaskDeleted":2,"ruleDeleted":1,"formDataRestored":1,"routeSnapshotRestored":1,"taskRestored":1}
