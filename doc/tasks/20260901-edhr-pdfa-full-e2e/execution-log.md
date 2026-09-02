# Execution Log

## Scope

- Worktree: `D:\IntRuoyiWorktree\20260901-edhr-pdfa-full-e2e`
- Branch: `codex/20260901-edhr-pdfa-full-e2e`
- Runtime profile: `int_main` 附加 worktree，前端 `8087`，后端 `48087`。
- Credential label: `芋道源码/admin`，明文密码不写入日志。

## BDD Scenarios

BDD: 管理员补齐 PDF/A 链路权限 -> Given 管理员从真实登录页进入权限管理 When 为任务执行账号分配批记录最终放行和最终归档所需正式角色菜单 Then 重新登录后工作任务看板显示相应待办且无越权菜单扩散

BDD: 创建可归档批次 -> Given 任务自有模拟订单具备完工生产、检验、补料和四份材料数据 When 用户从批次执行真实页面发起模拟准备 Then 系统创建带任务标识的批次并满足管理者代表放行前置条件

BDD: 管理者代表最终放行 -> Given 批记录、过程检验、损耗、物料平衡和附件均完整 When 管理者代表在候选审核待办中填写签署证据并确认放行 Then 批次进入最终归档待办且放行签名和时间可追溯

BDD: 生成 PDF/A 最终归档 -> Given 最终归档待办由有权限的归档责任人打开 When 责任人点击生成归档 Then 系统生成不可变归档文件并显示 PDF/A 校验通过

BDD: 历史追溯下载打印 -> Given 批次已完成最终归档 When 用户从历史追溯列表打开该批次 Then 页面只读展示表单、签名、附件和放行信息，并可下载及打印同一归档 PDF

## TDD Evidence

- RED: `node tests/e2e/edhr-pdfa-simulation-bootstrap-real-flow.e2e.js` -> FAIL，管理者代表真实页面提交返回 `eDHR 批次执行不存在`；后端回归 RED 进一步证明空 `batchExecutionId` 被传给四份材料门禁。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，32 个测试通过，其中新增“从放行事务恢复批次编号”回归通过。
- RED: Stage4/Stage5 真实页面链路依次暴露 `MATERIALS_RECHECK_REQUIRED`、缺少权威 `entryType`、Flow6 回填收据缺少正式结果 ID/单一来源、管理者候选来源类型不一致，以及最终放行后批次仍停在状态 20。
- GREEN: `python -X utf8 -m pytest script/tests/test_stage4_simulation_attachment_hash.py -q` -> PASS，2 个合同覆盖附件归属标记重算哈希、Flow6 正式回填 ID/来源写入与收据哈希持久化。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesReleaseAuthoritativeContextPortImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 个权威上下文测试通过。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProductionReleaseManagerApprovalServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 个管理者代表候选与审批测试通过。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，33 个测试通过，新增“最终批准后关闭批次并创建归档待办”回归通过。
- RED: `python -X utf8 -m pytest script/tests/test_stage4_simulation_attachment_hash.py -q` -> FAIL，Stage4 独立模拟收据哈希按插入前时间计算，数据库持久化后 `completed_at` 发生规范化，导致生产链路报 `STAGE4_INDEPENDENT_COMPLETION_RECEIPT_PERSISTENCE_MISMATCH`。
- GREEN: `python -X utf8 -m pytest script/tests/test_stage4_simulation_attachment_hash.py -q` -> PASS，3 个合同通过，新增覆盖“插入后重读收据、按持久化值重算哈希并更新，再用于下游 source credential”。
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，独立后端运行包重建成功。
- GREEN: `node tests/e2e/edhr-pdfa-simulation-bootstrap-real-flow.e2e.js` -> PASS，真实页面完成 Stage4/Stage5 准备和管理者代表放行，生成批次 `900000001025`、批次号 `STAGE4-BATCH-STAGE4DOSSIEAF98370BEF8D`、归档待办 `2438`。
- GREEN: `pnpm e2e:edhr:final-archive-task` -> PASS，归档责任人从真实工作任务看板处理最终归档待办，归档状态 `SEALED`，批次状态 `40`，待办状态 `DONE`，下载字节数 `31516`。
- GREEN: 历史追溯真实页面校验 -> PASS，`/mes/pro/feedback/edhr-batch-history` 可按批次查询已归档记录，显示统一时间线、归档版本、下载和打印入口，页面未暴露保存、提交、放行、生成归档、编辑或删除按钮。
- GREEN: PDF/A 文件校验 -> PASS，下载产物 `history-final-archive-latest.pdf` 为 4 页、PDF 1.4、含 Metadata Stream 和 1 个 OutputIntent，元数据标识 `PDF/A-1b`，正文可抽取且包含目标批次号；4 页渲染为 PNG 后完成视觉检查。
- GREEN: 数据库只读终态核验 -> PASS，批次 `900000001025` 状态 `40` 且有关闭聚合哈希；归档 `33` 为 `SEALED`、`BATCH_FINAL_PDF`、`PDF/A-1b`、`VALID`，对象锁 `COMPLIANCE`、`objectLock=true`、`legalHold=true`；待办 `2438` 为 `ARCHIVE/DONE`。
- GREEN: 经验沉淀 -> PASS，已在 `docs/e2e-rules.md#E2E 显式目标环境变量门禁` 记录显式目标 env 键名复核与下载产物验证门禁，并在 `docs/experience-index.md` 增加关键词。
- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260901-edhr-pdfa-full-e2e --mode preview` -> BLOCKED，预览可识别待删除临时证据，但 apply 需要提交/快进融合，当前主工作区 dirty 且本轮未授权 Git commit/merge/push。
- GREEN: `git commit -m "feat: complete edhr pdfa archive flow"` -> PASS，任务实现、回归测试、任务记录和经验沉淀已提交；rebase 后提交为 `ec3aff5db`。
- GREEN: `git rebase int_main` -> PASS，任务分支已更新到当前 `int_main` 之上；`git rev-list --left-right --count int_main...HEAD` 输出 `0 1`。
- BLOCKED: ff-only merge into `E:\IntRuoyi` -> NOT RUN，主工作区 `git status --short --branch` 显示 `docs/powershell-memory.md` 已修改，并存在 `.pytest-temp/`、`LOG_FILE_IS_UNDEFINED`、`resource/...` 等未跟踪项；按收尾规则目标主工作区 dirty 时禁止合入、清理或删除 worktree。

## Worktree Initialization

- 已读取 `docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/database-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md` 与 `docs/task-closeout-rules.md`。
- 目标绝对路径已验证位于 `D:\IntRuoyiWorktree\` 下。
- 新分支和 worktree 已从 `int_main` 当前提交 `c9466a902` 创建。
- 已通过 `scripts/runtime/reserve-worktree-slot.ps1` 原子登记 `slot=6`，专属端口为 `8087/48087`。

## Issues

- 2026-09-02 首次后端回归执行因 `MesProEdhrReleaseServiceImplTest` 未注册新增的 `MesProEdhrNonconformanceReviewService` 测试依赖而在 Bean 创建阶段失败；已补齐测试上下文依赖，准备重新执行业务 RED。
- 2026-09-02 真实链路暴露 Stage4 独立完成收据哈希与数据库持久化值不一致；已以 RED/GREEN 修复为“插入后重读并按持久化值封存哈希”。
- 2026-09-02 最终归档 E2E 首次使用旧环境变量名导致缺少真实目标前置；已核对当前脚本实际读取的 env 键名后复跑通过，未记为产品缺陷。
