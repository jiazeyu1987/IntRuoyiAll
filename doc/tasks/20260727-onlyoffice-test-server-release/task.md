# 本地 OnlyOffice 修复测试服发布

## Task Goal

将 DCC OnlyOffice 本地下载基址修复发布到测试服务器 `172.30.30.58`，本轮仅执行测试服发布与运行态验证，不执行正式服、备用服、`mark-tested`、`promote-prod` 或 `promote-backup`。

## Milestones

- [x] 创建发布任务记录，读取测试服发布、服务器、worktree、PowerShell、数据库、后端与 CI/CD 门禁。
- [x] 锁定发布基线、releaseTag、目标环境和禁止动作。
- [x] 创建干净发布 worktree，避免从脏主工作区构建。
- [x] 修复发布前置 migration metadata 门禁阻塞，并完成本地回归验证。
- [x] 构建 release package 并上传 NAS，完成本地/NAS manifest 与 artifact hash 校验。
- [x] 发布到测试服；旧 releaseTag 在 required SQL 目标数据前置条件不满足时失败并判废。
- [x] 冻结部分发布状态并将测试服恢复到部署前实际运行版本。
- [x] 经业务确认 `ROUTE-XLSX-00002` 第 26 道工序不是合法业务数据，并补 test-only 正式数据修复迁移。
- [x] 修复 code-only 发布语义，使 `type=data` required SQL 不进入远端 MySQL 执行队列。
- [x] 使用新的 releaseTag 重建发布包并完成本地/NAS 完整性校验。
- [ ] 将新发布包重新发布到测试服。
- [ ] 记录最终验证证据和收尾状态。

## Expected Verification

- 目标服务器：`172.30.30.58`。
- 发布范围：`test` only，组件范围 `intruoyi`。
- 初始冻结基线：`origin/int_main` commit `9562dca4982007f36c302aaa99847a59d6a4c28e`。
- 发布分支：`codex/20260727-onlyoffice-test-release`。
- 已判废 ReleaseTag：`release-20260727-onlyoffice-test-r260727-1445`、`release-20260727-onlyoffice-test-r260727-1823`、`release-20260727-onlyoffice-test-r260727-1948`、`release-20260727-onlyoffice-test-r260727-codeonly-r1`、`release-20260727-onlyoffice-test-r260727-codeonly-r2`、`release-20260727-onlyoffice-test-r260727-codeonly-r3`、`release-20260727-onlyoffice-test-r260727-codeonly-r4`、`release-20260727-onlyoffice-test-r260727-codeonly-r5`。
- 重新发布 ReleaseTag：`release-20260727-onlyoffice-test-r260727-codeonly-r6`。
- 测试服后端 `http://172.30.30.58:48081/actuator/health` 返回 `UP`。
- 测试服前端 `http://172.30.30.58:8081/` 返回 HTTP 200。
- 运行态 release tag、后端/前端镜像、manifest sourceRepos 与本轮发布一致。

## Current Status

in_progress

## Resolved Blocker

- `release-20260727-onlyoffice-test-r260727-1948` 已通过稳定排序修复并越过原绑定迁移顺序问题，但在更早的 `20260709_mes_rt000006_batch_record_mapping.sql` 失败，错误为 `Missing RT000006 pressure pump route`。
- 测试库只读核对确认不存在 `id=922067` 或 `code=RT000006` 的路线，相关活动路线工序为 `0`，三类压力泵填写员有效角色也为 `0`；不是单纯 ID 或名称漂移。
- 用户已明确要求修复发布流程：发布到测试服务器时不要携带业务数据。正式方案是保持迁移元数据和结构类门禁，但 `publishScope=code-only` 时不执行 `type=data` required SQL；不为无关历史迁移补造测试库业务数据。
- 本轮失败状态已收口，测试服继续运行 `release-20260723-dcc-viewer-permission-r260723vp-r1`，后端、前端和 OnlyOffice 均健康。

## Prior Blocker

- 2026-07-27 测试服 `172.30.30.58` 部署 `release-20260727-onlyoffice-test-r260727-1445` 时，在 required SQL `20260717_mes_balloon_excel_device_workstation_binding.sql` 第 420 行失败，错误为 `balloon Excel target route process count mismatch`。
- 只读核对目标租户 `tenant_id=1` 得到 `ROUTE-XLSX-00001=24`、`ROUTE-XLSX-00002=26`，合计 50 条路线工序；迁移脚本固定期望 49 条，阻塞发生在业务数据前置条件与迁移契约不一致，而不是 OnlyOffice 修复代码编译失败。
- 用户确认 `ROUTE-XLSX-00002` 第 26 道工序不是合法业务数据；该 releaseTag 已判废，禁止复用重试、`mark-tested`、`promote-prod` 或 `promote-backup`。
- 失败收口已完成：本次 migration 与 release lock 均为 `FAILED`；测试服 `.env`、backend/frontend 容器和 `/release-info.json` 已恢复并保持 `release-20260723-dcc-viewer-permission-r260723vp-r1`。

## Current Repair

- 只读核对非法记录为 `mes_pro_route_process.id=900173`，`route=ROUTE-XLSX-00002`，`sort=26`，`process=B320/球囊测漏及全检`。
- 引用核对显示活动引用包括：`mes_pro_route_flow_process_config=1`、`mes_pro_route_schedule_config=1`、`mes_pro_route_use_process_config_legacy_20260709=1`、`mes_pro_schedule_order_process=11`；11 条排产工序快照 `reported_quantity=0`，可由正式迁移随同软删除。
- 新增 `20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.sql`，限定 `allowedEnvironments=test`，在 `20260717_mes_balloon_excel_device_workstation_binding.sql` 前执行，备份并软删除非法路线工序、前序链路、路线配置和派生排产快照。
- RED/GREEN 已完成：新增静态契约测试先失败于清理迁移缺失，补迁移后 `11 passed`；全量 migration policy gate 通过，`migrationCount=383`。
- `release-20260727-onlyoffice-test-r260727-1823` 复发后确认发布预检 FIFO 拓扑排序反转了 Manifest 稳定顺序；已改为按原始索引选择 ready 节点，相关发布回归 `117 passed`，migration policy gate 仍为 `383` 条通过。
- `release-20260727-onlyoffice-test-r260727-codeonly-r2` 在 NAS 上传期间被工具超时中断；本地包完整，但 NAS 仅有 3344 个文件，缺少 `resources`、`runtime-env` 和 `smoke`，因此该标签判废且不得复用。
- `release-20260727-onlyoffice-test-r260727-codeonly-r3` 已完成构建和 NAS 上传；Manifest v1 共 3373 个 artifact，本地与 NAS 逐项哈希校验缺失 0、mismatch 0，后端/前端 commit 均为 `8d940d17e99f3045b99018fac53491250289024d` 且 `dirty=false`。
- `r3` 部署时已明确跳过 `RT000006`、球囊路线清理及绑定等 `type=data` SQL，但后续仍执行了依赖被跳过 data migration 的 `20260720_dcc_obsolete_approval_bpm_seed`，因测试库缺少 `form_policy_type` 字段失败。
- 根因是 code-only 过滤未计算迁移依赖闭包；正式修复为同时跳过直接或间接依赖 `type=data` 的迁移，避免在缺少数据前置迁移时执行其 seed/menu/schema 子节点。
- 依赖闭包修复已通过 `4 passed` 目标测试和 `125 passed` 发布回归；使用 r3 真实 manifest/preflight 复算时，失败 seed、`RT000006` 和两条球囊数据迁移均未入队，独立 `20260721_form_action_policy_approval_mode` schema 仍入队。
- `release-20260727-onlyoffice-test-r260727-codeonly-r4` 已完成本地/NAS 包完整性校验并实际切换测试服 backend/frontend/OnlyOffice 镜像，但发布脚本最终 OnlyOffice 容器连通性校验使用嵌套 `sh -lc` 时丢失 URL 参数，误报 `ONLYOFFICE_PUBLIC_FILE_BASE_URL_UNREACHABLE`，发布锁已收口为 `FAILED`，该 tag 判废。
- 已修复 OnlyOffice 连通性校验：发布脚本直接执行 `docker exec intruoyi-onlyoffice curl -fsS --connect-timeout 5 <healthUrl>`，URL 通过 `ConvertTo-ShellSingleQuotedLiteral` 传递；目标测试和扩展发布回归通过。
- `release-20260727-onlyoffice-test-r260727-codeonly-r5` 本地/NAS 包完整且来源 commit 干净，但部署时 code-only 过滤后 APPLY 队列为空，PowerShell 将子表达式空输出绑定为 `$null`，`Sort-RequiredDatabaseSqlApplyItems -Items` 触发 null 参数错误。r5 未重启容器，`.env IMAGE_TAG` 已恢复到实际运行的 r4，发布锁已收口为 `FAILED`，该 tag 判废。
- 已修复空 APPLY 队列处理：先用 `$preflightApplyItems = @(Get-ReleasePreflightApplyItems ...)` 显式包装为空数组，再传入排序函数；目标测试和扩展发布回归 `126 passed`，后续必须用 r6 新 tag 重建发布包。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；code-only 对 `type=data` 的过滤是用户确认的正式发布范围，不改 migration 类型、不吞执行异常，类型映射缺失时 fail fast。
- `是否从根因和长期维护角度解决`：是；发布脚本按 manifest 中的 migration type 区分结构契约与业务数据迁移，使 `SkipDatabaseSync` / `SkipMinioSync` 与 code-only 实际执行边界一致。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 测试服发布门禁：必须冻结发布基线、验证 manifest/sourceRepos、publish-test 目标字段、远端 `.env IMAGE_TAG`、实际镜像、backend health、frontend HTTP 和 release-info；不得拼接不同 releaseTag。
- 服务器访问门禁：仅允许访问测试服务器 `172.30.30.58`，不得操作正式服 `172.30.30.57` 或备用服 `172.30.30.59`。
- Worktree 门禁：不得从脏主工作区直接构建；发布 worktree 位于 `D:\IntRuoyiWorktree\onlyoffice-test-release-20260727`。
- Migration metadata 门禁：build-release 前必须执行全量策略门禁；`type` 只允许 `schema/data/menu/config/permission/seed`，发现 `config-seed` 必须阻塞并修复。
- Code-only required SQL 门禁：必须从 manifest requiredSql 回查 migration type；`type=data` 不得进入远端 MySQL APPLY 队列，缺少 migrationId/type 映射必须 fail fast。
- Code-only 空队列门禁：如果 data 迁移及其依赖闭包过滤后没有任何 APPLY 项，必须按空数组继续发布，不得让 PowerShell 子表达式空输出绑定成 `$null`。
- OnlyOffice 远端健康校验门禁：容器内 URL 可达性校验不得用嵌套 `sh -lc` 拼接带引号 URL；优先直接 `docker exec <container> curl ... '<url>'`，并用静态测试防止命令被本地 PowerShell/SSH/远端 shell 拆参。
- PowerShell 门禁：不使用 `&&`；中文、JSON、SSH/stdin、日志证据使用 UTF-8；不得记录密码、token、私钥或连接串密钥。
- Git 门禁：当前主工作区 ahead/dirty 不作为本轮发布输入；发布分支只提交本任务文件。
