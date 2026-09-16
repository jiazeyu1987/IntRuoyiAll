# 一键式无数据程序发布 P1

## Task Goal

在当前 `int_main` 基线上完成且仅完成 P1 发布合同收敛：固定标准程序包为 `app-release`，冻结两个 Git root 与三个 source role，明确客户端与服务端参数所有权，并建立构建前测试顺序、二次源码冻结和三语言双摘要合同。

## Milestones

- [x] P1-M1：建立维护仓与 IntRuoyi 根仓两个独立任务 worktree，并审查验证线差异。
- [x] P1-M2：先补 T01-T03/T15 失败测试并记录 RED。
- [x] P1-M3：最小实现 P1-AC1 至 P1-AC5 合同并记录 GREEN。
- [x] P1-M4：运行 P1 相关回归、端口 guard、staged 清单和 diff-check，并分别提交任务分支。

P2-P5 不在本次执行范围，不实现完整状态机、持久化 workflow、环境锁、发布 UI 或真实部署。

## Expected Verification

- Maven 定向合同：`ReleaseWorkflowContractTest`、`ReleaseSourceFreezeTest`、`ReleaseWorkflowGateOrderTest`、`ReleaseDigestVectorTest`。
- 前端静态合同：客户端请求不能提交 releaseTag、publishScope、host、NAS、MinIO、repo roots 或 LocalCacheRoot。
- 维护脚本合同：标准范围固定 `app-release`；唯一权威 `manifest.json`；构建前和打包前二次 Git 冻结；后端、前端、脚本测试先于昂贵构建。
- Python、Java、PowerShell 固定双摘要向量一致；反斜杠路径和大小写折叠冲突拒绝。
- 当前 app worktree 的端口 guard、任务 owned diff-check 和 Git staged 清单通过。

## 经验门禁

- Trigger: 标准发布合同。Preflight check: 固定 `app-release` 且数据备份/恢复不进入 workflow。Blocker: 客户端可选 `with-data` 或传基础设施参数。Verification: Java/前端/脚本合同。Forbidden action: 兼容读取第二份 manifest。Evidence: `execution-log.md`。
- Trigger: 来源冻结。Preflight check: 维护仓与 IntRuoyi 根仓分别记录批准 commit，maintenance/backend/frontend 三个 role 可追溯。Blocker: HEAD 漂移或 dirty。Verification: 构建前、打包前各校验一次。Forbidden action: 使用主工作区未提交内容。Evidence: `execution-log.md`。
- Trigger: 进入昂贵构建。Preflight check: 后端、前端、脚本测试已全部 PASS。Blocker: 任一失败或跳过。Verification: 命令顺序合同。Forbidden action: 先 Maven package、前端 build 或 Docker build。Evidence: `execution-log.md`。
- Trigger: 计算制品摘要。Preflight check: 路径规范化、大小写折叠唯一、原始 bytes SHA-256、固定 UTF-8/LF 序列。Blocker: 三语言结果不同或非法路径。Verification: 固定向量。Forbidden action: 语言/区域相关排序或路径兼容降级。Evidence: `execution-log.md`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以单一合同、服务端参数所有权和可执行门禁收敛发布输入。
- 是否存在临时补丁或绕过：否；验证线变更按当前主线逐文件融合，不整链 cherry-pick。

## Current Status

in_progress

P1 已完成。主线程后续进入 P3 后发现应用仓 `release_preflight_plan.py` 尚未接受标准 `app-release` scope，多个 required SQL 对测试服真实数据状态存在硬编码或空基线缺口。本 worktree 已补齐 `app-release` 迁移预检合同、活跃路线菜单解析、璞慧排产管理员菜单父级兼容、清洗工序参数空规则 no-op、光固 I/II 空来源 no-op、C00 文本比较显式 collation、B04091/B09353 清洗温度空候选 no-op、IDI QA 旧源空基线 no-op、压力泵同名物料已绑定/`product_master_id IS NULL` 的 no-op 合同，以及旧表单模板 Jimu 布局从正式识别字段构建的迁移合同。R53 Jimu 布局修复已提交为 `c82ee4841` 并进入 R55 包；R55 随后暴露当前正式 `mes_pro_batch_record_version` 缺少 `child_form_member_count/child_form_member_hash` 的迁移 schema 缺口。现已按真实 21 列契约修复并提交为 `3098b3319`，静态 RED/GREEN 与测试服只读 preflight 已通过；R55 不复用。

独立代码审查进一步确认六项通用机制尚未完成：来源选择没有绑定预期 maintenance/backend/frontend commits；schema rehearsal 排除 data 和 target-preflight 迁移；应用迁移测试入口固定单文件；阶段状态不是后台真实事件；取消/heartbeat/recovery 未接底层 operation/process；发布脚本仍含 migrationId 业务特例。审查同时纠正 R55 证据：未知列错误前已有 definition INSERT，不能宣称数据库完全零写入。当前阻塞在通用机制补齐和新 releaseTag 真实验证，不得直接生成 R56。

已先补来源批准 commit 持久化及执行器参数绑定（应用提交 `e109b707c`），并在维护脚本中加入按冻结迁移差异自动发现测试、缺失即 `MIGRATION_TEST_MISSING` 的构建前门禁；随后补齐运行中 operation 的底层取消/进程终止确认，以及统一迁移元数据（顺序、session profile、批准 hook、结果断言）合同。相关 Java 79 tests、应用迁移合同 25 tests、维护回归与脚本 AST 已通过。后台阶段事件、heartbeat/recovery scheduler、统一迁移隔离 rehearsal 与完整 publish-test 仍待完成。

本轮接手后确认主维护仓日志停在 `2026-09-15 21:21:57` 不是仍在运行，而是主工作区日志落后；维护 worktree 记录 R60 已在昂贵构建前失败，R61/R62 仅生成 required-sql 或局部 preflight 证据、没有 `manifest.json` 或镜像包，且当前无对应发布/Maven/Docker 进程，按中断半成品判废。已补应用侧通用后台机制：scheduler 先自动 reconcile 已终态底层 operation，再只对日志无新推进且 heartbeat 超时的 workflow 触发 fail-closed recovery；运行中且日志有推进的长构建用日志 mtime 刷新 workflow 心跳，避免按钮页面卡在旧状态或误杀长任务。R63 进一步暴露 `20260829_mes_old_form_template_binding_switch.preflight.sql` 第一个 `NOT EXISTS` 少闭合一层括号，导致目标只读 preflight 2/17 后 MySQL 1064；现已增加所有 target-preflight 外层 `SELECT CASE` 的括号闭合通用合同，并修复该 SQL。定向回归 12 PASS，测试服只读单文件和完整 17/17 target preflight PASS。下一步提交应用修复，再用全新 releaseTag 重新 build-release -> publish-test 验证，不能复用 R61/R62/R63。

2026-09-16 静态审查进一步判定当前按钮调用链不能放行：`app-release` scope 与底层 action 断裂，按钮仍调用应用仓旧脚本，旧 `/actions` 可绕过 workflow 授权，稳定等待态会被心跳误杀，测试验收 lease 会泄漏，底层进程先于 workflow 绑定启动，阶段失败显示不真实，前端固定操作排序第一条 workflow。当前切片先修这些通用按钮机制；在修复和 RED/GREEN 回归通过前暂停 R80 publish-test，不继续生成或发布新的 releaseTag。

静态审查纠偏切片已完成本机 RED/GREEN：后端 `RuntimeControlServiceImplTest,ReleaseWorkflowOrchestratorTest` 80 tests PASS，前端静态合同 PASS，`pnpm ts:check` PASS。当前已固定 `app-release`、维护仓发布脚本、workflow 上下文授权、派发前 operation 绑定、稳定等待态 heartbeat、按环境 lease 生命周期和前端显式 workflow 选择；R80 仍按不可复用处理，下一步需提交应用修复后用全新 releaseTag 重新 build-release -> publish-test 验证。

2026-09-16 17:40 静态复查后进入 R1-R5 纠偏切片：修复来源 tuple 去重、超时恢复后 build lease 释放、派发后未知状态隔离、正式发布只读准入前置、以及 build-release 真实阶段/失败定位。修复前不生成或发布新的 releaseTag；仍不执行正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup`、MinIO 数据同步或全量数据库复制。

R1-R5 纠偏切片已完成本机 GREEN：ReleaseWorkflow 定向 clean JUnit 21 tests PASS，ReleaseWorkflow/RuntimeControl 周边回归 58 tests PASS，前端 `pnpm ts:check` PASS。当前代码只完成本机通用按钮机制修复；R81 仍绑定旧应用提交，不作为发布成功证据。下一步需先分别提交应用仓和维护仓修复，再用全新 releaseTag 从同一组新提交重新 build-release -> publish-test 验证。

S1-S4 纠偏切片已完成本机 GREEN：恢复态列表/详情只读返回，取消写阶段保持隔离租约，测试验收显式 PASS/FAIL 分支，`mark-tested` 使用服务端认证操作者；维护脚本同步显式 `TestResult=PASS` 凭证合同；通用直接 mark-tested 前端 payload 也已固定传 `testResult=PASS`。应用后端 102 tests、前端静态合同、`pnpm ts:check` 与维护脚本 86 tests 均通过。下一步提交两仓修复后使用全新 releaseTag 重建并继续测试服 publish-test 与运行态/按钮验收。
