# 第三方报工导入列表与进度修复

## Task Goal

修复报工页签选择“第三方报工”导入 `李萍.xlsx` 后，结果弹框显示成功但报工列表未新增、排产工单进度可能未增长的问题；确保导入确认后的报工记录、列表刷新和进度展示来自正式持久化链路。

## Current Status

in_progress

## Milestones

1. 建立问题复现与现有链路定位。
2. 编写第三方报工导入后列表/进度更新的回归测试并取得 RED。
3. 实施最小正式修复，禁止 fallback、空成功或前端假刷新。
4. 运行目标 GREEN、相邻回归和必要的静态/接口验证。
5. 完成收尾记录、经验沉淀、cleanup、提交与推送。

## Expected Verification

- 后端或前端目标回归测试先 RED 后 GREEN，覆盖“导入确认成功后新增报工记录并更新排产进度”。
- 前端静态合同或真实路径验证确认导入结果确认后会刷新正式列表/进度，而不是只关闭弹框。
- 若触及数据库 schema 或 SQL，必须核对当前迁移/Mapper/表结构并记录证据。
- 若真实 E2E 前置缺失，记录精确缺口和影响，不得用 API-only 冒充页面通过。

## Experience Gate Summary

- `docs/experience-index.md` 已存在并检索关键词：报工、第三方、导入、进度、feedback、工单。
- 适用经验：`docs/release-build-preflight-lessons.md#2026-07-19-release-migration-结构化字段与-dependsOn-后缀门禁`，近期 `20260718_mes_feedback_import_record_direct_progress.sql` 曾触发 release migration dependsOn 后缀门禁；本任务若新增或修改 SQL/迁移，必须按结构化 release-migration 元数据和 policy gate 执行。
- 适用经验：`docs/database-rules.md#只读资源池引用完整性门禁`，报工映射/资源读模型不得用默认成功、空名称或前端隐藏掩盖正式主数据缺失。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；如需 fallback 必须先取得用户明确授权。
- `是否从根因和长期维护角度解决`：是；当前目标是定位正式导入持久化、列表刷新和进度计算链路。
- `是否存在临时补丁或绕过`：否；禁止用前端假新增、默认进度或 API-only 替代正式链路。

## Blockers

- 初始 `git status` 显示工作区已有大量未提交改动，且包含 `IntRuoyiFronted/src/views/mes/pro/feedback/index.vue`。按项目规则需要先保存既有脏工作区基线并记录文件清单，再进行本任务实现。
