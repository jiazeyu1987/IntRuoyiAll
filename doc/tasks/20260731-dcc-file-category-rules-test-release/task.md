# 测试服 DCC 文件类别规则发布与批量识别

## Task Goal

- 将已合入 `int_main` 的 DCC 文件类别规则能力发布到测试服务器 `172.30.30.58`。
- 在测试服使用官方批量识别链路刷新 DCC 项目代码关联文件的阶段/文件类型映射。
- 不直接修 SQL、不修改生产环境、不引入 fallback 或静默降级。

## Milestones

1. 建立测试服执行任务记录，并完成发布、登录、服务器、数据库和经验门禁预检。
2. 只读确认测试服当前运行态、登录权限、DCC 项目代码入口、类别规则与候选影响面。
3. 发布当前 `int_main` 到测试服务器，并验证 manifest、镜像 tag、后端健康和前端 HTTP 200。
4. 使用官方批量识别任务执行 `FILE_CATEGORY / GLOBAL / OVERWRITE_ALL`，并轮询到终态。
5. 复查候选数量、失败/冲突/歧义数量和 DCC 项目代码详情页面阶段/文件类型分组。
6. 完成验证报告、经验沉淀、提交与推送。

## Expected Verification

- 发布前 `git status --short --branch --untracked-files=all` 干净或已完成授权基线提交。
- 测试服后端 `http://172.30.30.58:48081/actuator/health` 返回健康状态，前端 `http://172.30.30.58:8081/` 返回 HTTP 200。
- 迁移后存在并启用 DCC 文件类别匹配规则，且缺失规则必须 fail fast。
- 批量识别任务终态成功，`failedCount/conflictCount/ambiguousCount` 为 0；若不为 0，导出失败明细并阻塞。
- Playwright 真实页面抽样核对 DCC 项目代码详情阶段与文件类型分组。

## BDD Scenarios

- Given 测试服类别列表已有启用类别和阶段映射，When 执行全局文件分类，Then DCC 项目代码详情的阶段列表与文件类型列表按类别规则聚合。
- Given 测试服存在可识别但未分类或分类不匹配的 DCC 项目代码关联文件，When 批量识别完成，Then 可识别文件不再停留在“未分类文件类型”。
- Given 官方批量识别返回失败、冲突或歧义，When 任务进入终态，Then 记录失败明细并阻塞，不直接 SQL 修数。

## Current Status

- in_progress
- 当前阶段：发布前置门禁已通过，等待构建新的 `release-20260731-dcc-file-category-rules-test-r2`。
- 测试服仍运行 `release-20260729-sqlfix-test-r260729d-r1`，尚未创建批量分类任务，尚未改写 DCC 数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，使用已合入的正式后端分类规则与批量识别链路。
- 是否存在临时补丁或绕过：否。

## Authorization

- 用户已授权测试服执行与必要的发布/批量识别操作。
- 凭据只用于登录与授权校验，不写入日志、文档或提交。

## Release Scope

- Target: test server `172.30.30.58` only.
- Release tag: `release-20260731-dcc-file-category-rules-test-r2`.
- Component: `intruoyi`, code-only, `SkipDatabaseSync=true`, `SkipMinioSync=true`.
- Source: clean release clone `D:\IntRuoyiWorktree\dcc-file-category-rules-test-release`, commit `e9eca0b3`.
- Forbidden: production, backup deployment, `mark-tested`, `promote-prod`, `promote-backup`, direct SQL repair, failed-package reuse.

## Experience Gates

- 测试服发布门禁：仅允许 `Environment=test`、`ServerHost=172.30.30.58`、`RemoteAppDir=/opt/intruoyi/runtime`；发布后必须核对 operation、manifest、远端 `.env IMAGE_TAG`、实际镜像 tag、后端 health、前端 HTTP 200 和真实页面证据。
- DCC 文件类别规则种子门禁：发布/迁移后必须确认启用类别、同租户同名唯一、规则表唯一键、OQ/PQ 与零配件图纸 seed 完整；禁止直接 SQL 修 `dcc_controlled_file`，禁止把 `AMBIGUOUS` / `UNCLASSIFIED` 当成功。
- 登录与 E2E 门禁：远端测试服登录已获授权；密码、token、连接密钥不得写入日志或提交；Playwright 必须走真实页面，API 仅作只读辅助或最终核验。
- PowerShell / Git 门禁：命令不使用 `&&`；中文读写使用 UTF-8；提交前必须复查 staged 清单、`git diff --check`、推送后确认不领先 `origin`。
- CI/CD 证据契约：记录环境、目标主机、构建/发布/回滚命令、所需密钥归属、artifact/release 输出、GREEN 验证和 blocker。
