# E2E-14 组织准备对象误匹配与恢复证据

## 当前影响

非本例账号 A4020140（用户编号 910170，商孟莹）已恢复：误加岗位由审计 11544 确认删除，部门于 2026-10-16 19:54:26（上海时间）通过真实前端单次保存清空。审计 11569 精确为“更新了用户【商孟莹】: 删除了【部门】：【DCC241016C14部门】”；重新查询、重新打开后部门为空，角色、岗位、昵称、性别、联系方式、备注、状态、锁定状态和创建时间与保存前一致。

恢复运行包 SHA256 为 caafeb2e008f0980d5c499448628bace9f87a2a0c69344000135e2ba650427ff（PID 43284 / 48061，Root 提供 health UP）。最终恢复脚本 exit0，saveClicks=1、userUpdateRequests=1；证据见 department-recovery-result.json 及 department-recovery/2026-10-16T11-54-18-510Z-apply/。没有通过 API/SQL、重置密码或权限变更恢复。

恢复前一次运行在列表唯一性断言停止，saveClicks=0、userUpdateRequests=0。随后将等待条件限定为自然查询请求的 username=A4020140，等待首次列表加载结束、筛选应用及目标查询完成；唯一行及 ID/用户名/昵称断言保持不变，没有放宽为首行或部分匹配。

基线恢复后已继续本例前置，源 PDF 已完成真实上传和预览，尚未提交正式受控文件，没有执行主管/会签/批准/文控签名；当前 route-preview 因 EXECUTION_CONTRACT_UNREGISTERED_DEFINITION 阻塞，14-A—14-D 均为 BLOCKED，详见 route-preview-blocker.md。

## 真实证据

- 组织准备脚本试图定位 DCC241016C14D 行，换页期间实际打开了账号 910170 的编辑弹窗。脚本当时没有在更改字段前复核弹窗昵称，这是脚本缺口。
- live-evidence/25-user-organization.zip 保留了目标行选择与实际弹窗不一致的页面帧；提取帧 live-evidence/org-targeted-review/12-1800.jpeg 显示实际昵称为商孟莹。
- 操作日志 11529：2026-10-16 09:42:16（页面原文时间），部门从空修改为 DCC241016C14部门；岗位添加了 DCC241016C14签名岗位，业务编号 910170。证据：live-evidence/37-audit-original-and-changed-organization.png 和 live-evidence/ui-actions.jsonl。
- 同页日志 11524—11528 分别对应本例 U/S/L/C/A 账号，未发现它们之外的其他已保存组织误改。
- 操作日志 11544：2026-10-16 10:20:51（页面原文时间），已删除上述误加岗位。用户列表回读岗位为 -，部门仍为 DCC241016C14部门。证据：live-evidence/39-incident-post-restored-dept-pending.png、live-evidence/40-incident-partial-recovery.zip。
- 正式用户编辑页的部门选择器没有可见清空控件；hover 后 .el-select__clear:visible 数量为 0。证据：live-evidence/38-incident-recovery-entry.png。没有调用接口、SQL、组件方法或写存储绕过。

## 已修复的脚本约束

- 账号查询改为真实筛选框按完整用户名查询，并等待该页面自然查询请求结束、加载层消失、唯一用户名可见；不再通过快速翻页找目标账号。
- 保存前用固定本例身份表核对精确昵称；打开非本例账号或错误的本例账号立即停止，不改字段。
- 纯函数自检证明：D 对应商孟莹时拒绝；D 对应本例主管 S 时拒绝；D 对应本例文控 D 时接受。此项仅证明脚本门禁，不代表页面身份问题或业务 E2E 已修复。

## BDD / RED / GREEN 边界

Given 用户列表正在换页或异步换数据，目标行显示本例 D；When 点击修改并打开编辑弹窗；Then 弹窗身份必须仍是该 D，脚本须在任何输入前验证完整身份。

RED：真实 trace 和日志 11529 证明脚本在身份不一致时保存，影响一个基线账号。

GREEN：脚本精确身份门禁自检通过、语法检查通过。未重放写入型组织准备，也未修改生产源码。

静态风险：IntRuoyiFronted/src/views/system/user/index.vue 的 el-table 未配置稳定 row-key；这与换页行事件错配现象相关，但没有把单独字符串缺失当作已证明的根因。UserForm.vue 的部门树选择器没有显式 clearable。这两项交 Root 复核与安排修复。

## Root 后续动作

1. 已完成账号 910170 部门恢复及真实页面、审计复核；基线账号恢复链路关闭，不再对其执行写入。
2. 部门清空正式入口及后端显式 null、遗漏值审计已按 Root 授权修复，6 项专属测试与 71 项原用户服务回归全部通过；列表身份风险未在本次扩大修复。
3. 恢复完成后，再继续本例 D 组织关系核对、文件前序审批与 14-A—14-D。

## 证据与凭据

页面历史审计存在与本例无关的敏感密码哈希记录，保存的文本和相应截图已脱敏，不在本报告复制。19 份 trace 已移除 console/pageError 原始载荷，验证 network payload 为空；详见 evidence-sanitization.json。

临时账号凭据仅保存于当前 Windows 用户 DPAPI 加密文件；该文件已由本目录 .gitignore 排除，不属于交付证据。浏览器已全部关闭。
