# 执行日志

## 2026-08-07

- User intent: 解释为什么测试服 `wangsiyu` 进入文控中心后，在受控浏览目录 `作废保留` 仍显示“无权限或无匹配当前有效文件”。
- Scope: 测试服务器 `172.30.30.58`，只读诊断。
- Rules read: `docs/server-access.md`, `docs/login-access.md`, `docs/database-rules.md`, `docs/e2e-rules.md`, `docs/powershell-encoding.md`。
- Skill used: `security-privacy-compliance-review`，已读取 `references/security-review-contract.md`。
- BDD: controlled browser visibility -> Given `wangsiyu` 已有文控菜单入口, When 选择目录 `作废保留` 且浏览页请求当前有效版, Then 列表是否可见由目录下 ACTIVE 当前有效文件和当前查看矩阵权限共同决定。
- Backend evidence: `DccControlledFileMapper.buildBrowserSummaryQuery` 在 `latestVersionOnly=true` 且未指定状态时只筛选 `ACTIVE`；前端受控浏览请求固定传 `latestVersionOnly=true`，当前目录搜索固定传 `includeDescendantDirectories=false`。
- Backend evidence: `DccControlledFileQueryServiceImpl.listControlledFileBrowserCandidates` 对候选文件执行 `canAccessQuery`，普通 ACTIVE 文件最终要求申请人本人、目录管理员或当前类别查看矩阵命中；`DccControlledFileViewMatrixAccessService` 对无启用规则的类别返回 `VIEW_MATRIX_NOT_CONFIGURED` 阻断风险并解析不到用户。
- RED: 首次只读 SQL -> FAIL, `ERROR 1267 Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT)`；未产生写入，随后按真实列排序规则重试。
- RED: 角色权限只读 SQL 初版 -> FAIL, `ERROR 1054 Unknown column 'm.tenant_id' in 'on clause'`；未产生写入，随后按真实 `system_menu` schema 去掉不存在字段条件重试。
- GREEN: 真实测试库 schema 核对 -> PASS, `dcc_file_directory`、`dcc_controlled_file`、`dcc_controlled_file_master`、`dcc_category_view_matrix_rule`、用户/角色关系表均存在且字段可用。
- GREEN: 目录与当前有效文件核对 -> PASS, 有效目录 `909083` 路径为 `质量管理/1. QMS documents/3 RE 扫描件/作废保留`；目录及子目录共 1 条文件，文件 `2054545668044051396` 为 `ACTIVE`、`published_file_id=9198354889671`，且 `dcc_controlled_file_master.current_active_controlled_file_id` 指向该文件。
- GREEN: 用户权限与查看矩阵核对 -> PASS, `wangsiyu(id=910250)` 当前有效角色为 `approval_center_entry(910295)`、`wenkong_no_download(910417)`；目录管理/下载危险权限计数为 0；文件类别 `其他(906104)` 的启用查看矩阵规则数为 0。
- Root cause: `wangsiyu` 能进入文控页签只证明菜单权限已恢复；当前文件申请人为 `1`，账号不是申请人，且无目录管理员权限；其文件类别 `906104` 未配置任何启用查看矩阵，所以后端不会把该 ACTIVE 文件放入受控浏览结果，页面显示“无权限或无匹配当前有效文件”。
- Experience consolidation: 已将通用的 DCC 受控浏览排查顺序和“菜单入口不等于文件可见权限”门禁合并到 `docs/e2e-rules.md` 的 `DCC 受控浏览当前有效版与权限隔离门禁`，未新建长期经验文档。
- Scope boundary: 本次只读诊断未修改测试服 MySQL、Redis、代码或运行态；未执行修复。
- Closeout preview: PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，无删除项、阻塞项或警告。
- Closeout apply: PASS，无删除路径；当前为主工作区 `int_main`，无 worktree 合并或移除操作。
