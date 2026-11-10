# 问题 1：编辑项目用户查看引用文件

结果：修复完成，真实 Playwright UI 正反向验收通过。全部创建、审批、上传、授权、引用和查看由真实页面完成，无 API/DB 业务写。

## 业务链与边界

- `ProjectBrowserPanel.vue:128` 固定引用入口读取已保存引用身份，再以 `VERSION_HISTORY` 上下文打开正文；`project-browser.ts:169` 重新校验引用和接口权限。
- `DccControlledFileQueryServiceImpl.java:1539` 保留目标项目/文件夹/精确引用匹配；关系可预览、名称可见、正文预览统一使用 `DccControlledFileDetailAuthorizationGuard.canReadProjectReference`。
- `DccProjectReferenceReadAccess.java` 仅查本租户准确 controlledFileId/masterId 的既存引用，连接有效目标项目/文件夹，再确认用户对该目标项目可读且为 EDIT/OWNER。要求文件 ACTIVE、已受控、已生效、有正式 published artifact。
- `DccControlledFileDetailAuthorizationGuard.java:30` 另验证当前登录 actor 与正式 master 的项目/类别/类型/编号身份；正文 `VERSION_HISTORY` 或 `DAILY` 可按该准确引用读取。
- `DccControlledFileQueryServiceImpl.java:3496` 引用只读资格不提供附件下载；正文预览允许分支不扩展原件下载、打印、编制、检出或源项目整体访问。
- SUPERSEDED/OBSOLETE 仍走原有归档权限规则。本次没有扩大历史归档读取范围。

## 回归验证

新 `DccProjectReferenceReadAccessTest` 4 项通过，涵盖精确文件/租户/master/目标权限，删除引用、VIEW不足、失效文件夹/控制状态、身份错误及未认证拒绝；实际guard/query正向预览与负向编辑/下载资格。

既有 Query 307、Guard 46 已由主 Agent 集中跑过。修复新增依赖的旧 fixture 后，PreviewSourceBinding 67、TrainingOwnerProjection 14 通过；BodyReadCurrentSource 受影响分支聚焦通过。历史矛盾旧 fixture 拆为两项且均通过：ACTIVE 历史审批保持 exact artifact 和撤销 historic actor 拒绝；SUPERSEDED 保持无归档权拒绝、有 doc_control 归档读 exact artifact、撤销归档权拒绝。没有为测试放松生产权限。日志位于 `C:/Users/D01020/AppData/Local/Temp/word1106-review/agent1-history-split.log` 等本任务日志。

## 真实 UI 证据

环境：任务 worktree 8063/48063，租户芋道源码。管理员 admin 发起 WORD1106-REF-SOURCE / WORD1106-REF-TARGET 项目申请；任务自有 word1106Reviewer（仅既有项目审核员角色）审批。两个项目使用复制后的任务文件夹 DCC241016C22F1/F2。

受控上传源文件 WORD1106-REF-DOC，fileId `2054545668044084577`，A/1，ACTIVE；同源未引用负例 WORD1106-PRIVATE-DOC，fileId `2054545668044084578`。目标 F1/F2 各引用源文件一次。

普通账号 word1106Editor 仅有受控浏览/预览菜单，只有 TARGET 的项目 EDIT，无源项目/文件类型/全局编辑授权。授权前项目列表为空；授权后两个引用均打开真实 PDF，在页面正文搜索中命中 `WORD1106-REFERENCE-BODY`，带该用户水印。同源未引用文件直接详情仍显示无权访问；固定引用只读追溯成功。

证据目录 `output/word1106-e2e/issue1`：

- `editor-DCC241016C22F1-reference-body.png` 与 F2 同名截图：正文及搜索命中。
- `editor-unreferenced-source-file-denied.png`：目标 EDIT 授权后未引用源文件仍拒绝。
- `editor-reference-readonly-trace.png`：固定版本只读详情。
- `admin-projects-import-references-trace.zip`、`reviewer-project-approval-trace.zip`、`editor-positive-negative-trace.zip`：页面操作轨迹。
- `shared-data.json`：源/负例页面身份，已交接问题 2 继续取消自有引用并验收升版规则。

脚本：`e2e/issue1-session.cjs`、`issue1-projects.cjs`、`issue1-flow.cjs` 为本次真实UI会话使用的helpers；`issue1-acceptance.cjs` 是既存自有数据复验入口（需要两引用仍存在）。后者仅语法检查，未重复已通过的业务验收；问题2后续会移除这些引用。

未提交/推送，未修改原业务项目及其文件数据，未动原运行服务。原文件快照保存在本任务 `agent1-baseline` 目录。
