# eDHR 批次执行真实路径 E2E Evidence
- Task ID: `20260727-edhr-cell-link-auto-persist-implementation`
- 状态：PASS
- 前端入口：`http://127.0.0.1:8086`
- 后端入口：`http://127.0.0.1:48086`
- 授权租户/账号：`芋道源码/admin`；密码由登录页本机默认值提供，脚本和证据不记录明文密码。
- 数据来源：`int-ruoyi-mysql/ruoyi-vue-pro`
- 批次执行：`EDHRB-1785116357526`，任务 ID `6666`，执行 ID `1571`
- 单元格链接规则：ruleId `12`，source `PRODUCTION_WORK_ORDER.batchCode`，target `3:3`
- 临时责任人切换：workTaskId `2227`，原责任人 `wangxin` -> `admin`；回滚影响行数 `1`。
## BDD
- BDD: 数据库夹具发现 -> Given 本机数据库存在授权租户 admin 与非作废 eDHR 批次任务 When 执行真实 E2E Then 脚本从数据库读取批次、任务和执行 ID，不要求人工注入工单或批次环境变量。
- BDD: 打开工序任务 -> Given 批次详情存在可打开任务 When 用户点击打开填写 Then 前端调用真实 `/mes/pro/edhr-batch-execution/task/open` 并进入既有 eDHR 执行页。
- BDD: 单元格链接自动落库 -> Given 批记录存在生产工单 batchCode 链接规则 When 用户打开执行记录 Then `task/open` 返回 `cellLinkAutoPersist`，详情接口包含已保存单元格值，页面输入框显示相同值。
## Result
- GREEN: 真实前端详情页打开填写路径已完成。
- GREEN: task/open 返回 cellLinkAutoPersist，状态 `NO_CHANGE_ALREADY_APPLIED`，目标单元格 `3:3`，值 `34126020001`。
- GREEN: 执行详情 cellValues 包含目标单元格保存值；页面输入控件显示值 `34126020001`。
- GREEN: 数据库执行记录 `field_audit_revision=1`，`cell_values_json` 包含目标格保存值。
- GREEN: `CELL_LINK_AUTO_PREFILL` 自动预填审计批次恰好 `1` 条，`idempotency_key` 长度为 `64`。
- GREEN: 重复执行真实 E2E 后仍返回 `NO_CHANGE_ALREADY_APPLIED`，未追加第二条自动预填审计批次。

## Cleanup
- 临时责任人已恢复为用户 ID `810`（`wangxin`）。
- 工作任务 `2227` 最终状态为 `TODO`，更新人为 `codex-e2e-rollback`。
- 任务自有隔离运行态 `8086/48086` 已停止并释放，原 E2E worktree、分支和 slot 5 已清理。
