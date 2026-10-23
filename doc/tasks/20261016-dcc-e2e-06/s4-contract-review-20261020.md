# E06 S4 合同与提交边界后续复核

日期：2026-10-20（Asia/Shanghai）。本记录为 Root 最新明确授权后的合同复核；只读需求、源码及既有证据，没有新 E2E、测试运行或构建。

## 后续业务结论

E2E-06 业务合同已由本轮 S1/S2/S3、S4 双页隔离及 P2 最终确认取消证据覆盖，后续复核结论为 **BUSINESS_CONTRACT_COVERED / PASS**。没有发现已证实的跨项目残留提交产品缺陷。服务端非法 HTTP 输入分支是额外防御边界，正常正式 UI 不能构造；本轮为 **NOT_APPLICABLE_TO_FORMAL_UI / NOT_TRIGGERED**，不再作为 E06 业务 BLOCKED。

该结论来自 Root 本轮明确指令：“业务合同已由S2/S4 UI隔离+确认取消覆盖；服务端非法HTTP分支……不再保留为E2E业务BLOCKED”。这不是宣称非法 HTTP 被成功发出或拒绝，也不是重新执行 S4 获得 PASS。历史 result/handoff 的 S4 BLOCKED、3 PASS/1 BLOCKED 计数、首次失败和 trace 保持原样；追加的机器记录为 `s4-contract-root-review-20261020.json`。

## 原合同与解释依据

| 依据 | 精确位置 | 对本例的约束 |
|---|---|---|
| 八聊统一需求 | `doc/tasks/20261011-dcc-eight-thread-management/requirements.md:3`、`:51`—`:55` | 本文明确覆盖历史 shared-contract/旧设计；R08规定“只选同项目可看生效受控版”，关联固定所选版本，详情/目录只读。R01第10行规定切换确认后清除不适用旧关系。 |
| 正式 E2E 合同 | `doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md:27` | Given P1/P2可区分版本；When P1选关联、切P2核对并重新选择、尝试旧页原选择；Then 本项目候选、旧关联不能带入、“任何残留跨项目选择均被拦截”、固定合法所选版本。没有指定必须发送非法 HTTP 或必须由服务器返回拒绝。 |
| 本例历史子用例 | `task.md` 的 Parameterized Subcases / S4补验计划 | 为真实页面路径划分了 S4，后续把“HTTP拒绝未触发”保留为 BLOCKED。它不能增加一项原合同没有的“必须绕过前端”业务完成条件；历史结果仍保留。 |
| 历史跨项目设计 | `docs/dcc-parallel-delivery/task-d-relations.md:8`、`:32`；`docs/dcc-minimal-main-flow/implementation.md:206` | 曾规划跨项目/同产品关系，不用于放宽本轮同项目规则。R08唯一新需求声明与W04任务第4行、验证报告第29行明确记录了旧用例允许跨项目/待生效与R08冲突、已按R08调整。不能据旧设计判当前隔离为功能缺陷。 |

目前仓库可定位的是上述统一需求及正式用例；本次没有取得原始聊天完整逐字稿，不能扩写不存在的原话或推断“必须非法HTTP”。Root本轮已经明确了正式用例的复核口径。

## 已有本轮 UI 证据（未重跑）

证据根目录：`runs/rerun-20261020-r2-round1/`。

| 合同要求 | 已有直接证据 |
|---|---|
| P1只选本项目，合法关联固定版本 | `result.json` 的 relationSelections：目录与本项目搜索均核对，P2候选排除；`DRAFT1-fixed-relation.json`及panel截图：新草稿2054545668044084332固定P1CAND A/1，刷新和受控浏览精确行重开一致。 |
| 项目切换不能带入旧关联 | `S2-before-switch.json`、`S2-switch-confirmation.json`、`S2-after-switch.json`及截图：P1关联1→P2关联0，编号/名称为空、正文预览清空，P2文件夹重新可选；正常切换自带session-cleanup HTTP200。 |
| 旧页面不能变成P2携带P1 | `S4-two-tab-boundary.json`、`S4-second-tab-P2.png`、`S4-old-tab-P1-context.png`：新页选P2，旧页仍为P1且关联窗口上下文仍P1。旧页有效的P1选择不是P2跨项目提交。 |
| 完整合法输入进入确认时仍不带旧关系 | `S4-P2-empty-relations-confirmation.json`及截图：补齐P2文件夹/类型/私有矩阵/实际PDF/编号版本/生效方式和必要属性后，最终确认显示项目P2、“关联文件：未关联”、无P1CAND。`S4-P2-confirmation-cancelled.json`：真实取消保留填写，动作区间submit/working写请求0。此项已超出仅缺必填校验的弱证据。 |
| P2重新合法选择可持久化 | `DRAFT2-fixed-relation.json`及panel截图：新草稿2054545668044084335固定P2CAND B/1，刷新及受控浏览重开一致。 |

执行器首失败2次及S3一次新context校准仍见历史handoff/checkpoint/failure-7、failure-11，不改写为首轮无失败。`process-closure.json`记录两runner exit0、context/browser await关闭、runner及已记录Chromium后代进程0。本次没有打开浏览器或启动runner。

静态UI防线与证据一致：`upload/index.vue:1640` 的 `handleProjectCodeChange` 在接受切换后清空 `relatedControlledFileIds`、`selectedUploadRelations` 并关闭/清除选择上下文（1696—1699）；`persistUploadRelations:1794` 再核对contextKey、租户、项目、ACTIVE及可执行性。`relations/DccFileSelector.vue:54`、`:77`限定来源项目查询，`:28`禁用跨项目候选；`selector-state.ts:19`、`:27`、`:39`、`:79`在reset/select/save重复身份检查。无需改页面允许非法状态来证明拦截。

## 实际服务端调用链与校验

以下均为 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/` 下当前源码静态事实。

1. 新申请正式提交：`controller/admin/file/DccControlledFileController.java:339` `submitControlledFile`（权限+Validated+事务）→ `publicUploadPlacementService.create` → `DccControlledFileWorkflowServiceImpl.submitNewWithDerivedStorage:662` → `submitControlledFileIdempotently:1525` → 私有 `submitControlledFile:1571`。
2. 公共矩阵路径：创建WORKING记录后，在 `WorkflowServiceImpl:1609` 调用私有 `validateAndBindRelatedFiles:1031`，发生在绑定正文/附件、状态转换、保存审批路线、`createApprovalProcess:1619`之前。不可写成“所有文件insert之前拒绝”：源文件insert在1603，靠外层事务回滚，关系insert和BPM创建尚未发生。
3. 本例APPLICATION_CUSTOM路径：`submitControlledFile:1573`→`submitNewCustomApplication:1633`→`createWorkingControlledFileIdempotently:890`→同一私有校验`:930`，通过后才进入`submitWorkingIteration:1055`。保存草稿 `/working`→`createWorkingWithDerivedStorage:669`也走`:890`及`:930`。没有绕过项目校验的独立custom提交分支。
4. `WorkflowServiceImpl.validateAndBindRelatedFiles:1031` 对非空集合要求源项目编辑/负责人资格，逐一要求目标存在且名称可见；随后委托 `DccControlledFileRelatedFileServiceImpl.validateAndBindRelatedFiles:92`。
5. 关系服务先规范化ID（`:259`），拒绝无项目、非法/自身ID及重复；再核对源记录租户/项目/Master，锁源Master及可写快照（`:315`）；逐一 `requireSelected:309` 和 `requireLatest:333` 校验租户、精确版本/Master身份。`:110`—`:114`要求目标受控、可执行、非待生效、ACTIVE、`file.projectId == projectCodeId`，并排除自身Master及不匹配latest身份。所有候选验证完毕后才进入`:119`的insert循环，不会先写合法项再检查后续非法项。
6. 不同项目抛出 `CONTROLLED_FILE_RELATED_FILE_INVALID`，定义为 `ErrorCodeConstants.java:372`，业务错误码 **1080000302**。这是ServiceException业务码，不等于HTTP状态403/400；本例没有真实非法HTTP响应，不猜具体HTTP映射。

源码存在服务器独立校验，未发现“仅靠前端”的跨项目写入缺口。事务注解和调用顺序支持失败回滚的设计判断；本次不将其等同于已实测整条Controller事务、数据库行数或HTTP响应。

## 直接相关既有单测与覆盖限制

测试源码位于 `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/`。

| 方法 | 已有断言 | 边界 |
|---|---|---|
| `DccCurrentFileRelationsTest.crossProjectAndPendingSelectionsAreRejectedBeforeAnyRelationInsert:88` | 循环两个非法目标：另一项目ACTIVE、同项目待生效；每次精确ServiceException码1080000302；`related.insert`总次数0。 | H2+TransactionTemplate，目标resolver、关系/文件mapper和权限策略为mock；直接调用关系服务，未经过HTTP或完整工作流。单个非法目标，不是多项混合事务用例。 |
| `sameProjectActiveSelectionPersistsExactVersion:100` | 同项目ACTIVE正例仅insert一次，目标201、项目1、快照A/1。 | 只证明精确关系写参数，不是本轮页面持久化证据的替代。 |
| 同类其余7方法`:52/61/70/80/113/118/124` | 固定所选版本、源版本不随current变化、作废保留追溯/正文拒绝、禁止自由编辑及零写、缺provenance/外来Master/坏快照拒绝。 | 不新增跨项目HTTP覆盖。 |
| `DccControlledFileWorkflowServiceImplTest.relatedWriteRequiresProjectEditPermissionBeforeBinding:439`、`relatedWriteRejectsAnInvisibleTargetBeforeBindingAnyRelation:448`、`relatedWriteAllowsNameOnlyTargetsWithSeparateProjectEditPermission:461` | 项目无编辑权限/存在不可见目标时不委托关系服务；名称可见且项目可编辑时才委托。 | ReflectionTestUtils调用私有辅助方法，关系服务mock，不证明Controller端到端项目拒绝。 |
| `createWorkingControlledFile_postInsertFailureMustNotReplayItsOwnRow:1225` | relation/ticket/relation-duplicate失败原样抛出，不创建BPM。 | 关系服务注入异常，未测真实跨项目校验或实际DB源记录回滚。 |

既有执行证据：`doc/tasks/20261016-dcc-e2e-10/fix-green-evidence/DccCurrentFileRelationsTest.xml`：9 tests、0 failures/errors/skipped；包含上述跨项目拒绝及正例方法。Root历史 `e2e06-root-review.json` 和 `e2e10-rework-matrix-test-summary.json`确认其作为独立后端证据。它是历史GREEN；本次只解析/阅读，**未重新运行9项，也未声称已对本轮运行JAR做HTTP拒绝验证**。本轮runtime-copy证明的8个类不包含本关系服务，不能拿该类清单替代关系服务的当前二进制指纹证明。

## 最小补验建议与产品缺陷判断

- 对本轮E06业务验收：**无需补跑**。按Root本轮批准口径，保留现有UI证据和历史原计数，后续review确认业务合同覆盖即可；没有需要为S4修改的产品代码，也没有证实的跨项目漏洞。
- 若未来单独要求服务器信任边界的完整事务证据：另立后端定向测试范围，在真实Spring事务中经公共submit/working入口传入P2源+P1目标，校验业务码、无关系/源残留、无BPM及合法混合列表全或无；公共矩阵和APPLICATION_CUSTOM均覆盖，另有同项目ACTIVE精确版本正例。此为可选后端集成测试建议，不是本轮未完成门禁，不称作E2E，也不在当前执行。
- 若一定要求真实页面生成非法HTTP，必须先发现可重复的合法UI入口/时序能实际保留不一致状态；目前没有该证据。不得放宽页面、注入状态、直接API或mock来伪造E2E。

本次不构建、不运行测试、不操作数据库/服务/Git、不创建子Agent，不改产品、共享台账或任何历史run文件。工程closeout受原Git/清理禁止约束而独立blocked，与本次E06业务复核PASS分别记录。
