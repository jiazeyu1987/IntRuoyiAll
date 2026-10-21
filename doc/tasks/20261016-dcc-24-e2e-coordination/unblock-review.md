# 前置修复 review（持续更新）

## 已核实

- 环境：原 QMS MySQL/Redis 容器退出，前后端停机。恢复原容器/数据卷及已核对 Jar 后，8061/48061 在线、health UP、官方真实 UI 登录 PASS。证据 unblock-runtime-health.json、login-preflight-result.json。
- 数据库：用户明确授权后，固定五项正式增量在副本与实际库分别首次/重复执行均PASS。完整依赖20项与全SQL policy PASS，直接依赖23项字段事实PASS，目标16项实际schema从RED转GREEN。原45211行的原列摘要完全一致、旧账本未变；实际库执行前已停QMS后端、确认无其它数据库会话并刷新备份。待新包启动后真实页面复验。
- E2E-I05 登录：E2E09修复租户选中值和导航等待，重新真实登录通过；账号准备仍须逐项完成，不等于草稿业务通过。
- E2E-I03 目录：C10 id913887、C19 id913890 已保存且启用，租户1。真实页面目录children/search/tree业务码均0；admin id1有4条生效的项目文件指派，现有服务及测试明确其范围优先于管理角色，所以范围外目录不可见。不得为测试改变原有指派或移除授权校验。C10/C19已获通知以任务自有、无项目指派的最小配置账号继续。真实页面全局500另来自公共待办与类别缺列，不再归因为目录SQL失败。
- I03证据：directory-scope-inspection.json（只读辅助核验），E2E17/root-directory-diagnosis.json 与截图（真实前端归因）。Root一度误把C19线程ID记为C17，已明确纠正；只读诊断由C17保存在自己的目录，无修改C19资产。

## 已修复并验证

- E2E-I04 目录首次打开：E2E12唯一负责directories/index.vue。方案将首次打开请求保留至实际组件mounted，后续已挂载时直接打开；定向生命周期RED/GREEN和真实首次点击/取消重开PASS。Root已review最小差异、result及截图，接受FIXED_VERIFIED；原12完整会签业务继续验证。
- E2E-I01 用户910170部门恢复：明确null清空和省略部门审计分别取得真实RED；最终6项专属+71项用户服务回归全部PASS，前端4项PASS。最终package成功并部署PID43284/48061，health和官方真实UI登录PASS。E2E14精确过滤并完成单次页面保存，重新打开部门为空、岗位仍为空、其它页面字段不变，真实审计11569仅删除误加部门。Root已review department-recovery-result.json（RESTORED_VERIFIED，saveClicks=1，userUpdateRequests=1）及03-restored-reopened.png。最初查询未唯一时保护拦截，零写入，未盲目重复保存。
- E2E-I02 页面GREEN：E2E20本例Manager/项目283真实上传页复验，upload-options业务码0、24个可选择类型、typeErrorVisible=false，sourceUploads=0/controlledFileBusinessWrites=0。证据E2E20/artifacts/green-upload-options/2026-10-16T11-43-48-641Z/result.json和截图/trace。原缺列前置已解除，完整20权限业务不因此升级PASS。
- 24原线程均已发送恢复通知，显式model=gpt-6.1-sol/thinking=high，见unblock-continuation-receipts.json。C10/C19仍需完成专用配置身份的目录回读，不以诊断代替该身份实测。

## 结论边界

本报告为环境及准备阻塞修复，不是24例业务E2E全部通过。保留历史问题快照 e2e-issues-20261016.md/json；所有修复状态以本轮实际证据为准。未提交/推送、未操作远端、未直接API/SQL恢复账号或创建业务fixture。
