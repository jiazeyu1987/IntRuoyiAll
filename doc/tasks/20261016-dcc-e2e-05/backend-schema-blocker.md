# C05-PRE-01 — 新运行 Jar 与运行库字段不匹配

> 修复前历史证据。PID43284 恢复轮中，本例通过真实配置账号重开类别确认保存和 APPLICATION_CUSTOM 来源已恢复；P1 也已真实审批 COMPLETED。本文件旧 RED 保留，不再作为当前阻塞。当前新阻塞见 rounds/unblock-pid43284/verification-report.md 中 C05-PRE-03（流程定义合同不匹配）。

## Given / When / Then

- Given `int_qms` 8061/48061，Root 证实 PID 24528、Jar SHA256 `b0bb53fbf20546696140e7c93754a07b40527596e94ce0185035782bb4127071`；本例独立配置账号已实际登录，具备类别管理、目录管理及查询菜单权限。
- Given 本例五级路径 `技术文档/设计和开发策划阶段/DCC241016C05自定义其他/DCC241016C05自定义章节/DCC241016C05自定义正文` 和私有目录已经真实页面核验。
- When 通过“文控权限 -> 类别列表 -> 新增类别”填写 `DCC241016C05CUSTOM`、`DCC241016C05其他类别`，选择“本次申请自定义（其他）”、上述分类和私有目录，点击“确定”。
- Then 应成功保存并从正式类别列表/类型映射页辨认同一类别；实际对话框显示“系统异常”且不关闭，未取得页面成功证据。

## RED

命令：`node doc/tasks/20261016-dcc-e2e-05/fixture-setup.real.e2e.mjs --business-only`。真实配置账号页面执行失败，执行工具观察 shell exit 1；业务 C05-A/B/C/D 未运行。

实际页面：[类别系统异常截图](fixture-evidence/A1792120341210-observer-failure.png)；[同一配置阶段 trace](fixture-evidence/A1792120341210-observer-configuration-trace.zip)。截图显示正式生命周期阶段，不能与此前误用自定义阶段名称而被正确拒绝的 fixture 参数混淆。

后端只读日志证据：`output/runtime/int_qms/logs/dcc24-r8/application.log` 记录 2026-10-16 09:45:50.926 的 `DccFileCategoryMapper` 错误：`Unknown column 'approval_source_mode' in 'field list'`，对应 `dcc_file_category`。同一日志还记录 `DccControlledFileMapper` 的 `Unknown column 'draft_approval_source_mode' in 'field list'`。归档 `application.log.2026-10-16.3.log` 08:18:14.793 也有前一缺列错误。这里只读取已有日志，没有执行 SQL、接口验证或迁移。

页面系统异常与类别 Mapper 缺列相符；本线程没有执行 SQL 独立盘点 schema，也不推断其它列或迁移均已齐备。health UP 只证明服务在线。

## GREEN / 复验边界

GREEN 为 BLOCKED。按本轮“不迁移数据库、不做生产修复”的指令，保留失败现场交 Root 安排运行库/运行 Jar 匹配处理。修复后重新用相同本例配置账号与正式页面确认类别保存、类型映射和类别来源，再准备/核验 P1，才可标本例 fixture UI_VERIFIED 并执行 C05-A/B/C/D；不能用 API/SQL 造类别或把管理员当业务审批人。

独立的项目准备也未提交：正式项目申请页面可见“项目审核员，4 名候选”；角色管理界面按精确 `dcc_project_reviewer` 在可见分类下未找到对应角色行，不能仅凭同名选项为本例审批人分配。未改变既有共享角色定义、成员或权限。
