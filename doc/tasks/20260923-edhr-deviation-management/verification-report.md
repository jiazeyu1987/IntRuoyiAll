# 2026-09-29 集成复核说明
本报告下文为历史证据，不表示当前融合版本已完成全链路E2E。手动创建路径已按用户要求取消，旧夹具不计入正式来源验收。当前集成验证见 ../20260929-deviation-main-integration/verification-report.md。

# eDHR 偏差管理阶段验证报告

## 当前结论

P1–P4 已完成对应实现切片和阶段验证，并通过独立复核。偏差发起使用正式电子签名服务签署规范化正文，并在同一事务调用 GxP 审计追加；角色化策略 `edhr.deviation.create` 已登记并通过覆盖门禁。P5 已补齐修订历史、只读签名证据、NCR处置结果和处理读取错误态；前端类型检查已通过同版本依赖临时junction，真实浏览器 E2E 仍未授权执行；P6 尚未完成。

## 当前工作区与范围

- Worktree：`D:\IntRuoyiWorktree\piancha_0923`，分支 `codex/piancha-0923`，基线 `f46b0bdc20d80ee7fa6e5d6b86898bcae0d0b357`。
- 本轮实现到 P5 只读证据切片；P1/P2/P4及P5证据切片已验证，P3仍保留前端依赖/E2E限制，P6尚未完成。
- 未对正式服务、共享服务或主机 MySQL95 执行迁移；偏差迁移只在 `mysql:8.0.39`、`--network none` 的一次性隔离容器 `edhr_test` 执行首次和重复验证，容器及其匿名卷已删除。
- 未启动服务、未运行 Playwright E2E、未提交或推送。

## P1 验证结果

| 检查 | 结果 |
| --- | --- |
| P3/P4 Node 静态合同测试 | PASS，P3/P4各7项合同断言通过 |
| Maven 定向 reactor | PASS，最新P1/P2/P4组合84项通过；含偏差发起、处理签名、转审和既有NCR回归 |
| MySQL 8.0.39 迁移首次及重复执行 | PASS；4 张偏差表、2 项权限、临时过程已清理 |
| H2/MyBatis 偏差候选查询 | PASS；PQC推送前批次入选，跨租户/作废/已上市批次排除 |
| MySQL 序列并发 / 幂等reservation并发 | PASS；分别为24个唯一序列返回值、24竞争者一条幂等行 |
| MySQL 事务回滚和批次锁次序模拟 | PASS；显式回滚无残留，8轮创建先/4轮放行先均未出现已放行且偏差开放 |
| 偏差发起电子签名与审计事务 | PASS（4项H2集成测试；涵盖签名、正文哈希、重放不重复签、缺策略回滚） |
| GxP正式策略登记 | PASS；角色化 owner、REQUIRED签名、版本化策略和迁移重复执行均通过 |
| 完整 Java 服务运行于 MySQL 的端到端并发 | NOT RUN；MySQL实验验证实际 Mapper SQL/锁谓词，H2集成验证服务端开放偏差放行门禁 |
| 全库迁移策略门禁 | BLOCKED by pre-existing SQL `20260921_mes_edhr_nonconformance_review_materials_json.sql` 缺少 release-migration metadata；P1 六文件依赖闭包 PASS |
| API服务器 / E2E | API服务器和真实E2E未运行；前端类型检查已通过临时同版本依赖junction，真实E2E仍未授权 |

实际 BDD、RED/GREEN、SQL、容器安全边界和测试结果见 [execution-log.md](execution-log.md)、[execution-log.md](execution-log.md)、[test-report.md](test-report.md) 与 [test-report.md](test-report.md)。

## 独立复核与范围限制

独立复核确认公开上市放行入口存在批次锁顺序反转；修复后，放行最终化按“批次行→放行交易行”锁定，和偏差创建一致。独立复跑的 Node、Maven、文档及 API/数据库证据校验通过。候选批记录查询已通过 H2 的真实 Mapper SQL 测试，证明租户隔离、过滤作废/已上市放行，并保留未推送PQC批记录。最终定向 Maven 为46个测试通过，包含2个候选Mapper集成测试。

隔离 MySQL 存储过程模拟使用正式 Mapper 的批次锁顺序和筛选谓词，但并未调用完整 Spring 服务。编号年月格式和四位扩位由固定时钟单测验证，MySQL并发只验证mapper序列SQL。未推送PQC时的QA关键偏差不合格审批属于P4，P1仅证明批记录候选和偏差发起身份。

当前精确执行 GxP coverage gate -> PASS，`operations=9 annotations=8`；P4 migration dependency gate -> PASS，9项闭包；独立复核记录 P1/P2/P4/P5 相关选择测试 111/111 PASS，补充历史证据和NCR读模型后的定向选择测试继续通过；P3/P4静态合同和前端类型检查均PASS。完整 Spring 服务在 MySQL 上的端到端并发和浏览器E2E仍未执行。

## 结构化验证

执行 `python -X utf8 doc/tasks/20260923-edhr-deviation-management/validate-docs.py --skills-root C:\Users\BJB110\.codex\skills` -> PASS：17份任务Markdown、28项AC与BDD映射、阶段状态、Cleanup Keep、需求一致性及产品/系统/验收检查通过。

执行 backend-api-delivery 和 database-schema-delivery evidence validator 与 self-test -> PASS；`git diff --check` -> PASS（仅显示文件换行风格警告，无 whitespace error）。

## 当前独立复核与剩余范围

技术定位和登记已完成：operationId `edhr.deviation.create`，sourceLocator `cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrDeviationServiceImpl#create`，领域/对象/动作 `EDHR / EDHR_DEVIATION / CREATE`，状态信封 `ABSENT -> PRESENT`；策略版本 `20260924-edhr-deviation-01`、批准依据标识 `EDHR-DEVIATION-GXP-20260924`、owner角色 `ROLE_QA_QUALITY_OWNER`、`signaturePolicy=REQUIRED`、`retentionClass=GXP_BATCH_RECORD` 已写入配置和迁移。

PRD 已要求发起编制人电子签名；实现将签名绑定到规范化发起正文、偏差ID及批记录身份。批准人和责任人按用户要求使用权限角色码，所有批准节点使用电子签名；后续策略修改必须创建新版本，不改写历史事件。

P4独立复核已通过；P5已补齐修订历史、当前/失效签名状态、NCR处置读模型、真实签名人显示名和处理读取失败重试态，仍需在最终门禁中确认两个追溯宿主的真实页面路径。P6需完成最终28项验收映射复核、真实浏览器E2E（如获授权）及收尾门禁。不得将隔离迁移验证扩大为线上发布或完整 MySQL Java 事务证明。

## P6 Main-Agent Verification Update (2026-09-26)

### Code and Test Evidence

- Fixed the Vite SFC compile defect in `DeviationDetail.vue` by closing the handling-card header slot; added a regression contract in `IntRuoyiFronted/tests/e2e/edhr-deviation-detail-template-static.spec.cjs`.
- Removed duplicate `sortable="custom"` attributes from the deviation list because `sortColumnAttrs(...)` is the single sorting source for `UnifiedListTemplate`.
- Frontend contracts: P3, P4 and template/sort regression all PASS.
- Frontend checks: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` PASS; deviation-related ESLint PASS; `git diff --check` PASS.
- Backend targeted reactor: 92 tests PASS, 0 failures/errors/skips.

### Runtime and Migration Evidence

- Worktree runtime is the registered `int_main slot 13` pair: frontend `8094`, backend `48094`; backend health is `UP` and the paired Vite process is listening.
- Authorized local migration applied to `int-ruoyi-mysql`: four deviation tables, four NCR extension columns, ten permission rows and one `edhr.deviation.create` GxP policy row verified; repeat execution remains idempotent. No remote or production database was touched.

### Real Playwright Evidence

- `p6-20260925-r4` uses the real `芋道源码/admin` login and real frontend actions. It opens the deviation list, shows `全部/未处理/已处理`, displays the `没有偏差` empty state, opens the `发起偏差` form with batch, level, category, description, level basis and electronic-signature fields, and receives HTTP 200 for the deviation page and batch-option requests.
- A separate real-page run opens an existing batch-record detail and the formal `偏差` tab, which correctly shows `没有偏差` without a business error.
- A non-target avatar request to `test.yudao.iocoder.cn` returned HTTP 502 during the browser session; it is recorded as environmental noise and is not used as feature PASS evidence.

### Remaining Blocker

- Write-type E2E is now PASS. A product-name search found an enabled route work order; the real `打开/创建` page created task-owned batch `P6-DEV-20260926-CAP4-577008` / ID `900000001150`, after which Playwright completed deviation initiation, handling, five ordinary signatures and normal close.
- AOCI full-index reload was attempted twice after context compaction and timed out at the AOCI service boundary; this does not alter the code/test evidence above, but prevents claiming a fresh full AOCI cognition attestation in this run.

### Current Decision

P1, P2, P3, P4, P5 and P6 implementation evidence is complete and independently testable. The task is now `ready_for_closeout`; the task-owned batch fixture and write-type E2E gate are both complete. AOCI refresh timeout remains an infrastructure note only and does not block the product verification evidence.
- Runtime recheck on 2026-09-26 after a prior process interruption: restarted the registered `piancha_0923` pair, verified backend health `UP` and frontend root HTTP 200, then reran Playwright list/create-dialog verification. The new run reproduced the same non-target avatar 502 noise and the same real-data write blocker; no new deviation-page error appeared.

## P6 Real Write E2E Completion Update (2026-09-26)

- A real task-owned batch was created through the frontend `打开/创建` path using an enabled route work order: batch code `P6-DEV-20260926-CAP4-577008`, batch execution ID `900000001150`, formal batch execution code `EDHRB-1790401592564`.
- Real Playwright deviation initiation succeeded from the deviation page: deviation ID `1`, deviation code `PC-202609-0001`, linked batch execution ID `900000001150`, status `OPEN`, initiation signature ID `12576`, HTTP 200/business code 0.
- Real Playwright handling save succeeded after the JSON-field regression fix: handling ID `1`, content version `1`, investigation members `[]`, verification content saved, HTTP 200/business code 0.
- Real Playwright signed `PREPARER`, `VERIFIER`, `DEPARTMENT_OWNER`, `QA` and `QUALITY_OWNER` nodes with signature IDs `12577`–`12581`, each HTTP 200/business code 0.
- Real Playwright normal close succeeded with `NORMAL_COMPLETED`; final read-only database verification shows deviation `PC-202609-0001` is `CLOSED`, handling conclusion `CLOSED_LOOP`, verification result `PASS`.
- Non-target avatar 502 remains separately recorded as environmental noise; all deviation/handling target requests returned HTTP 200 and no pageerror occurred.

## Closeout Supersession

The historical entries above that say real E2E was unauthorized or P6 was pending are superseded by the current authorized runs recorded in `execution-log.md`: task-owned batch creation, deviation initiation, handling save, five ordinary signatures, normal close and final read-only trace verification all passed on September 26, 2026.

## Closeout Evidence

- Cleanup preview -> READY with `--worktree-closeout off`: formal task records kept, temporary evidence/output classified for deletion, no blocked paths.
- Cleanup apply -> APPLIED with `--worktree-closeout off`: temporary evidence and Playwright output removed; production code, SQL migrations, formal tests and task records retained.
- Linked-worktree merge/removal remains pending because the main worktree is dirty and contains unrelated concurrent changes. The task status therefore remains `ready_for_closeout`, not `completed`.

## P6 Critical Deviation Full-Chain Browser Verification

- PASS: Playwright created `PC-202609-0006` from the real deviation page and associated it with batch execution `900000001150`.
- PASS: QA electronic signature created `EDHR-NCR-20260926235142-900000001150`; the deviation closed with `TRANSFERRED_TO_NCR` and the batch displayed `冻结中` while review was pending.
- PASS: QA uploaded review material and completed `让步放行`; the real batch detail `偏差` tab showed the closed deviation and concession disposition.
- PASS: Backend root-cause fixes are covered by 8/8 NCR integration tests, 36/36 MES signature/deviation regression tests, P1 contract 6/6, P3/P4/template contracts and frontend lint/type checks already recorded in this task.
- Boundary: the task-owned manual batch has no active-order origin, so the three deeper production/PQC freeze action paths were not claimed as browser PASS; this is recorded as a scope limitation rather than a fallback or simulated success.

The task is ready for closeout cleanup. Git commit, merge, and worktree removal remain outside this run.
- Closeout cleanup preview/apply with `--worktree-closeout off` -> APPLIED; no blocked paths, formal task records and implementation assets retained. Linked worktree merge/removal remains pending because the main worktree is dirty and Git commit/merge was not authorized in this run.
