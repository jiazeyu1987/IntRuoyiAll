# 第三方报工导入列表与进度修复

## Task Goal

修复报工页签选择“第三方报工”导入 `李萍.xlsx` 后，结果弹框显示成功但报工列表未新增、排产工单进度可能未增长的问题；确保导入确认后的报工记录、列表刷新和进度展示来自正式持久化链路。

## Current Status

in_progress

## Milestones

1. completed - 建立问题复现与现有链路定位。
2. completed - 编写第三方报工导入后列表/进度更新的回归测试并取得 RED。
3. completed - 实施最小正式修复，禁止 fallback、空成功或前端假刷新。
4. completed - 运行目标 GREEN、相邻回归和必要的静态/接口验证。
5. completed - 完成收尾记录、经验沉淀、cleanup、提交与推送；修复提交已进入当前 `int_main` HEAD。
6. in_progress - 将已修复版本按 code-only 范围发布到测试服务器 `172.30.30.58` 并验证运行态。

## Expected Verification

- 后端或前端目标回归测试先 RED 后 GREEN，覆盖“导入确认成功后新增报工记录并更新排产进度”。
- 前端静态合同或真实路径验证确认导入结果确认后会刷新正式列表/进度，而不是只关闭弹框。
- 若触及数据库 schema 或 SQL，必须核对当前迁移/Mapper/表结构并记录证据。
- 若真实 E2E 前置缺失，记录精确缺口和影响，不得用 API-only 冒充页面通过。
- 测试服修复发布只允许 `Environment=test`、`ServerHost=172.30.30.58`、`Component=intruoyi`、`SkipDatabaseSync`、`SkipMinioSync`；禁止正式服、备用服、mark-tested、promote-prod、promote-backup。

## Experience Gate Summary

- `docs/experience-index.md` 已存在并检索关键词：报工、第三方、导入、进度、feedback、工单。
- 适用经验：`docs/release-build-preflight-lessons.md#2026-07-19-release-migration-结构化字段与-dependsOn-后缀门禁`，近期 `20260718_mes_feedback_import_record_direct_progress.sql` 曾触发 release migration dependsOn 后缀门禁；本任务若新增或修改 SQL/迁移，必须按结构化 release-migration 元数据和 policy gate 执行。
- 适用经验：`docs/database-rules.md#只读资源池引用完整性门禁`，报工映射/资源读模型不得用默认成功、空名称或前端隐藏掩盖正式主数据缺失。
- 适用经验：`docs/test-release-preflight.md`，本轮为仅测试服 code-only 发布，必须使用冻结提交 release worktree，验证远端 `.env IMAGE_TAG`、实际镜像 tag、后端 health、前端 HTTP 和目标缺陷运行态。
- 适用经验：`docs/release-build-preflight-lessons.md#2026-08-01-codex-ps1-标准输入宿主门禁`，Windows 上 npm `codex.ps1` 使用末尾 `-` 读取 stdin 时必须由 PowerShell 7 `pwsh.exe` 执行；缺少宿主、参数丢失或 Codex 非 0 必须阻塞，禁止手工补 manifest。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；如需 fallback 必须先取得用户明确授权。
- `是否从根因和长期维护角度解决`：是；当前目标是定位正式导入持久化、列表刷新和进度计算链路。
- `是否存在临时补丁或绕过`：否；禁止用前端假新增、默认进度或 API-only 替代正式链路。

## Blockers

- 并发基线提交 `7186c11a2` 已把本任务后端实现、测试和初始任务文档纳入 HEAD；本任务剩余收尾只 stage 本任务文档、直接报工静态合同和长期经验文档。
- 真实 Playwright 页面导入未执行：本轮未启动本地前后端，也未确认可写测试租户/账号；不得把后端单测或静态合同冒充真实 E2E。
- GitHub 推送阻塞：当前分支 `int_main` 仍领先 `origin/int_main`；`git ls-remote origin HEAD` 因 `127.0.0.1:7890` 本地代理未监听失败，清空代理后直连 `github.com:443` 也连接失败。按项目规则，推送完成前不得标记 `completed`。
- 测试服当前仍运行旧包 `release-20260801-frozen-dcc-column-collation-r260801e-r1`，尚未包含本任务修复；需要本轮 code-only 发布。

## Final Verification

- 后端目标 GREEN、后端导入服务整类回归、前端直接报工结果静态合同、前端排产刷新静态合同均通过。
- `task-closeout-cleanup` preview/apply 均通过，无删除项，保留 task、execution log、verification report 和 bug-regression evidence。
- Closeout 尚未完成：等待 GitHub 连接恢复后推送当前分支，并验证不再 ahead。

## Cleanup Keep

- `doc/tasks/20260801-third-party-feedback-import-list-progress/bug-regression-evidence.md`
