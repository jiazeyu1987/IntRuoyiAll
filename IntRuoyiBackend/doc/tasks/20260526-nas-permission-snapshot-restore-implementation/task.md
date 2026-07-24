# NAS 权限快照与 DCC 恢复实施

## 任务目标

在独立后端 worktree 中按已评审设计文档实施 NAS 权限快照与 DCC 权限恢复能力。主 agent 作为 reviewer 控制阶段门禁，必须先完成 Gate1 NAS ACL 读取能力验证，再进入 Gate2 DCC 运行时权限 enforcement 验证；两个 gate 均通过后才允许拆分正式开发。

设计依据：

- `D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260526-nas-permission-snapshot-restore-design/backend-api-design.md`
- `D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260526-nas-permission-snapshot-restore-design/data-model.md`
- `D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260526-nas-permission-snapshot-restore-design/bdd-tdd-subagent-plan.md`
- `D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260526-nas-permission-snapshot-restore-design/review-report.md`

## 阶段门禁

- Gate1：验证 SMBJ / NAS 服务账号能读取目录 ACL/security descriptor/DACL/SID/继承标记。Gate1 不通过时停止，不进入 Gate2。
- Gate2：验证或补齐 DCC 运行时文件浏览、预览、下载对目录 `canQuery/canPreview/canDownload` 的 enforcement。Gate2 不通过时停止，不进入正式恢复开发。
- 正式开发：快照表和模型、NAS ACL 快照采集、身份映射、恢复预览、显式应用恢复、校验报告。

## 里程碑

- M1：创建 worktree 与任务文档，记录 BDD/TDD/Subagent 规则。
- M2：Gate1 RED 测试：先写 NAS ACL 读取能力失败测试。
- M3：Gate1 GREEN 实现：最小实现 ACL 能力检测/读取接口，无法读取时 fail fast。
- M4：Reviewer Gate1 审查：测试证据、代码边界、无 fallback。
- M5：Gate2 RED/GREEN 与审查。
- M6：正式开发分波次执行并由 reviewer 放行。

## 预期验证

- 每个阶段必须记录 `BDD:`、`RED:`、`GREEN:`、`REGRESSION:` 或 `BLOCKER:` 到 `execution-log.md`。
- 实现 agent 与测试 agent 分离；测试 agent 不写生产代码，实现 agent 不写自己的验收测试。
- 后端仓库提交只包含本任务相关文件。

## 当前状态

- 状态：completed。
- 当前阶段：后端实现已由 review-fix-loop round 2 放行并提交。
- 已完成：后端 worktree、任务文档、主管状态文件初始化；Gate1 RED、GREEN、独立复核、infra 模块回归和测试服真实 NAS ACL 读取验证已完成；Gate2 DCC 运行时详情、预览、下载目录权限 enforcement RED/GREEN 与 reviewer 复核已完成；T4 已补齐 NAS ACL 快照/恢复 8 张表、DO、Mapper、runtime/test schema，并通过 reviewer 复核；T5 RED 已补齐 DCC NAS ACL 快照采集契约/持久化测试；T6 已新增快照采集 service 契约与实现，并集成目录任务 ACL 读取、快照保存和 ACL 读取失败阻断；review-fix-loop 修复并复核通过 descriptor 去重、路径一致性、canonical path key、snapshot header 计数和 taskItem.nasPath 一致性；T7 RED 已补齐身份映射 service 契约测试；T8 已新增 DCC NAS 主体映射 service 契约与实现，完成显式映射保存、目标主体 system API 验证、冲突 fail fast、未映射主体清单汇总与指定回归验证；本轮按 reviewer blocker 新增 shared descriptor 未映射主体计数 RED 回归，并修复 `listUnmappedPrincipals` 按每个 SUCCESS directory snapshot 引用累计 ACE；独立 reviewer round 2 已确认逻辑层、易用性层和 UI 层均可放行；T9 已新增并按 reviewer 反馈修正恢复预览/应用 service RED 测试，覆盖 preview 只读、未映射 SID 阻断、DENY blocker、unsupported accessMask blocker、匹配最新 planHash 的显式应用、API 返回 WAITING 但计划持久化状态保持合法 READY；T10 已新增 `DccNasPermissionRestoreService` 契约与实现，恢复预览基于已保存 CAPTURED raw ACL snapshot 只读生成计划，apply 仅创建 READY 恢复计划与 WAITING 计划项队列，不写 restore log 或 `dcc_directory_access_rule`；本轮 replacement repair 已消除 restore plan 构建中的 identity mapping N+1 与 runtime directory rule N+1，补齐 restore 专用 `ServiceException` 错误码抛出，完成 `taskId + idempotencyKey` apply 幂等一致性校验，并修正执行日志中 T9 RED / T10 GREEN 顺序；T9/T10 round 2 独立 reviewer 已 PASS；T10b 已新增恢复预览/应用 HTTP controller、apply req/resp VO、preview resp VO，GET/POST 路径与后端设计一致，apply command 携带当前登录用户；T10b round 1 reviewer blocker 已修复，preview API 现在返回 `runtimeEnforcementReady=true` 与 `runtimeEnforcementBlocker=null`；T10b round 2 独立 reviewer 已 PASS，full DCC 回归与 backend API evidence 校验均已通过；T10c RED 已新增恢复执行 service 测试，覆盖 READY plan + WAITING item 的 VALIDATE/APPLY/VERIFY 日志、目录规则替换、item/plan 完成状态，以及预览后当前目录规则 hash mismatch fail-fast；本轮 reviewer fix 已补强成功路径契约，明确要求目录旧规则 `delete*` 写调用先于目标规则 `insert*` 写调用，防止追加式实现误通过；T10c GREEN 已新增恢复执行 service 契约与实现，按 READY plan / WAITING item 执行 current hash 校验、delete 后 insert 替换、VALIDATE/APPLY/VERIFY 日志、item VERIFIED、plan COMPLETED，以及 current hash mismatch 阻断；同时补齐 apply 生成计划项的 `directoryId`、`expectedCurrentRuleHash`、`expectedAfterHash` 和 item `expectedAfterHash`。
- 本轮 hardening：已新增按 WAITING item 调用 `TransactionTemplate.execute(...)` 的短事务断言，并新增当前目录规则 `changeReason` 变化必须改变 preview `planHash` 的断言；组合 RED 命令进入测试阶段并按预期失败，GREEN 已修复为每个 WAITING item 通过 `TransactionTemplate.execute(...)` 执行，且 preview planHash 的 runtime directory rule payload 纳入 `changeReason`。
- 第三轮 contract：已按 `data-model.md` 将成功 restore log 状态测试合同修正为 `SUCCEEDED`，要求 hash mismatch VALIDATE FAILED 的 `beforeHash` 记录实际当前目录规则 hash，并新增已有 VERIFIED item + WAITING item 断点续跑累计完成计数测试；GREEN 已修复成功日志状态、hash mismatch 日志字段，并改为按 plan 下全部 item 状态累计 completed/failed count。
- 第四轮 contract：已新增并发 claim RED 契约，要求 `processWaitingRestorePlans()` 对扫描到的 READY plan 先调用 `DccNasAclRestorePlanMapper.claimReadyPlan(planId, startedAt)` 原子 claim 到 EXECUTING；claim 成功才处理 plan items，claim 返回 0 时必须跳过且不得查询 item、不得进入短事务、不得写目录规则/restore log、不得更新 plan final 状态。GREEN 已补齐 mapper 原子 claim default 方法，并在执行服务处理 item 前 claim，claim 失败直接跳过；指定目标测试、恢复 service 组合回归、宽回归和 `git diff --check` 均已通过。
- review-fix-loop round 1：reviewer 未放行的三个 blocker 已按 worker 包修复。恢复执行 service 现在支持 stale EXECUTING plan 原子 reclaim 后续跑，避免 claim 后永久跳过；处理前校验 plan item 存在、`directoryCount` 与 item 数一致且 item 状态可证明执行进度，不满足时 fail fast 到明确 FAILED；plannedOperations/restoreMode/replaceDirectoryRules/transaction 异常会转为可审计 item/plan FAILED 并写 restore log，validation 失败后仍不写运行时目录规则。主 reviewer 打回的长计划 active runner stale 风险已补充修复：每个 WAITING/APPLIED item 边界通过 CAS 式 `refreshExecutingPlanLease(...)` 刷新 lease，refresh 失败立即停止本轮，避免并发重复执行。主 reviewer 打回的 prerequisite 审计计数不自洽风险已补充修复：可定位 failed item 会纳入 plan summary failedDirectoryCount。目标测试、恢复 service 组合回归和宽回归均已通过。
- review-fix-loop round 2：reviewer 未放行的秒级精度 blocker 已按 worker 包修复。恢复执行 service 现在统一用 `truncatedTo(ChronoUnit.SECONDS)` 生成和传递 claim/reclaim/refresh lease timestamp；`PlanClaim.startedAt()`、`currentLeaseStartedAt` 和 refresh `refreshedAt` 均与 MySQL/H2 `datetime` 秒级精度一致。已补 RED 测试证明 READY claim、stale EXECUTING reclaim 和 item 边界 refresh 入 mapper 前 `getNano()==0`，且 claim/reclaim 后首个 refresh current lease 等于上一轮写入 DB 的秒级 lease。目标测试、恢复 service 组合回归、宽回归和 `git diff --check` 均已通过。
- review-fix-loop round 3：独立 reviewer 结论为 PASS。已确认 round 1 blocker、主 reviewer 补充 blocker 与 round 2 秒级精度 blocker 均修复；指定宽回归 50 个相关测试通过，backend API evidence 校验通过，最终 full DCC 回归 `mvn -pl yudao-module-dcc -am test` 通过（`yudao-module-dcc` Tests run: 274, Failures: 0, Errors: 0, Skipped: 0），`git diff --check` 无 whitespace error。保留非阻塞建议：后续真实库验收可补 mapper/schema 集成测试；若运营展示需要准确首次开始时间，可独立拆出 lease 字段；未来单 item 超过 stale 阈值时可考虑 final update 带 lease 条件。
- T10d backend API surface：已补齐 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot`、`GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot/items`、`GET /dcc/nas-permission/principals/unmapped`、`PUT /dcc/nas-permission/principal-mappings` 和 `GET /dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore/{restoreId}`。新增 snapshot 只读 query service 从现有快照/目录快照/ACE/身份映射表计算 summary/items/blocker，不返回 mock；principal mapping save command 携带 sourceAuthority/sourceSid/sourceName/accountName/accountType/active/changeReason/operatorUserId 并保持既有显式映射校验，`accountType` 缺失时在 insert 前 fail fast；主 reviewer 已移除旧参数顺序兼容构造器，接口 command 只保留 canonical constructor；restore status 对 COMPLETED/FAILED 最终 plan 读取 restore plan 审计摘要，非最终 plan 从当前 restore plan item 的 VERIFIED/FAILED/BLOCKED 状态计算 pollable completed/failed counts。
- T10d 验证：main reviewer no-fallback RED 已失败于 3 个预期点，GREEN 后 principal mapping/restore service 22 个目标测试通过；T10d 宽回归 58 个相关测试通过；`git diff --check` 与 backend API evidence 校验通过。review-fix-loop round 1 independent reviewer 打回的 3 个 blocker 已修复：principal mapping schema-required `accountType`、restore status 非最终计划真实进度计数、snapshot query service mapper-derived summary/items 测试；新增/扩展目标回归 36 个测试通过，主 reviewer 宽回归 66 个测试通过，`git diff --check` 与 backend API evidence 校验通过。
- T10d review-fix-loop round 2：independent reviewer 已 PASS，确认 round 1 三个 blocker 均关闭，未发现新的阻塞副作用；reviewer 复跑 36 个 focused tests 与 `git diff --check` 均通过。
- T10d final verification：`mvn -pl yudao-module-dcc -am test` 已通过，整体 Tests run: 290, Failures: 0, Errors: 0, Skipped: 0；backend API evidence 校验通过。
- T10d closeout preview：`task-closeout-cleanup --mode preview` 已运行并按预期 blocked；未执行 apply、未删除文件，原因是 linked worktree 不能 fast-forward merge 到 `int_main` 且本轮 T10d 改动尚未提交。
- 提交：`91c31a4457 任务: 补齐NAS权限恢复API接口`。
- 阻塞：暂无。收尾清理预览因任务分支不能 fast-forward merge 到 `int_main` 且主后端 worktree 有脏改动被阻塞，未执行删除或合并。
