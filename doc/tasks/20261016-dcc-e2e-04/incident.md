# C04跨例账号岗位误写事件

状态：已恢复，并以当前真实页面重新核验。目标用户ID910426，username=DCC241016C02NoSupervisor，昵称DCC241016C02缺主管编制，部门DCC241016C02Dept。C04脚本误加本例岗位，属于跨例影响；随后恢复不抵消事件本身。

| 事实 | 正式操作日志ID | 时间（Asia/Shanghai） | 页面可见变化 |
|---|---|---|---|
| 原账号创建 |11604|2026-10-16 21:42:32|创建目标用户，业务编号910426 |
| 原部门设置 |11610|2026-10-16 21:53:53|空→DCC241016C02Dept |
| 原岗位登记 |11629|2026-10-17 00:51:22|添加DCC241016C02验证岗位 |
| C04误改 |11632|2026-10-17 01:04:08|添加DCC241016C04验证岗位 |
| 恢复 |11638|2026-10-17 01:05:25|仅删除DCC241016C04验证岗位 |

以上为SYSTEM操作日志主键，来自系统管理→审计日志→操作日志，以业务编号910426筛选后逐条打开详情读取；不是未读取的GxP事件ID。原岗位为ID17/code DCC241016C02Post；误加岗位为ID18/code DCC241016C04POST。岗位编号/编码对应证据为r3-e537-ui-003-post-page.txt、r3-e537-ui-004-task-post-created.txt。

## 当前状态独立回读

1. 在真实用户管理筛选框查询精确username=DCC241016C02NoSupervisor，等待自然GET /system/user/page该username请求完成、HTTP200及加载结束。唯一行同时显示ID910426、精确username/昵称、部门DCC241016C02Dept、角色DCC241016C02编制、岗位仅DCC241016C02验证岗位。无C04岗位。
2. 按Root新增明确只读要求，打开该唯一行编辑弹框；等待自然GET /system/user/get?id=910426、HTTP200及弹框加载结束，再核对nickname=DCC241016C02缺主管编制。岗位标签精确集合为[DCC241016C02验证岗位]，不存在C04。截图后点击取消，未填写或保存任何字段；监听user/update请求为空。
3. 当前状态来自本次真实页面，不从历史审计推断。未发现未恢复字段；误改/恢复两条操作摘要均仅列岗位增/删，没有其他字段差异。未进一步编辑其他例账号。

证据文件均位于本任务目录：

- incident-audit-details.json：11604/11610/11629/11632/11638完整可见审计详情。
- incident-current-identity-readback.json：精确username查询及当前唯一身份行。
- incident-current-edit-dialog-readback.json：GET id、昵称、岗位精确集合、更新请求0。
- incident-readonly-ui-008-910426-restored-posts-edit-dialog-readonly.png：当前编辑弹框截图；同名txt为DOM。
- incident-readonly-ui-004-audit-11629.png、005-audit-11632.png、006-audit-11638.png：原/误改/恢复审计截图（完整文件名前缀均为incident-readonly-ui-）。
- ui-journal-incident-readonly.jsonl：本次所有真实UI操作；trace登记于fixture-manifest.json。

## 脚本原因与防复发

原ui-journal-r3-e537.jsonl第13行在部门点击后即定位目标行、打开用户弹框并操作岗位；没有等待精确username查询响应，没有把表单GET id与预期id比对，也没有在保存前确认已加载昵称。这些脚本身份边界缺失，使加载期间的错误目标未被保存前拦截。没有独立证据证明产品row-key缺陷，本例不提出或修改产品行key。

后续用户保存使用ui.saveOwnUser({username,id,nickname})：账号必须属于本例manifest及C04前缀；必须有新完成的精确username列表请求、唯一精确username/id行、新完成且同id的GET详情；表单加载结束且nickname相同。成功保存后消费本次检查点，下一次必须重新查询。维护停止标志为真时拒绝保存。用户页直接确认/保存命令由runner拒绝，须走身份检查入口；目标行不按分页nth选择。

修改仅限本任务ui-session.js、user-save-identity.js及其测试，未改生产代码。13项身份边界测试PASS：包含跨例账号、相似查询词、旧查询、多目标行、DOM行身份错位、错误GET id、加载未完、昵称不符及维护停止的拒绝场景。测试未进行真实用户保存，不制造新写入。

## BDD及保留

- Given 原C02岗位已由事件11629设置，C04误加记录11632且已由11638删除仅C04岗位。
- When 真实UI只读查询审计，再以精确username唯一行及GET id一致的编辑弹框核验。
- Then 原C02岗位保留，C04岗位不存在；user/update请求0，取消弹框并关闭浏览器，保持维护停止及原业务草稿。

原业务草稿2054545668044084183未变更，8PASS/1FAIL/3BLOCKED保持。等待Root运行态恢复后仅续原稿。
