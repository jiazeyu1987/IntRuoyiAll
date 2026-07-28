# IntRuoyi Backend Development Rules

## 触发场景

- 修改 `IntRuoyiBackend` 下的 Java、Spring Boot、Maven、接口、服务、Mapper、DO、配置、脚本或后端测试前，必须先读取本文件。
- 涉及 SQL、schema、菜单、权限、租户绑定或数据修复时，还必须读取 `docs/database-rules.md`。
- 涉及本机服务启动、端口或运行态验证时，还必须读取 `docs/local-runtime.md`。

## 项目边界

- 后端根目录：`E:\IntRuoyi\IntRuoyiBackend`。
- 使用 Java 17、Spring Boot、Maven 多模块结构。
- 主应用模块：`yudao-server`。
- 业务逻辑必须保留在所属模块内；跨模块移动或耦合必须有明确的设计理由和验证。
- 不得根据前端页面或历史实现猜测后端 schema、权限、接口或租户行为。

## 实施规则

- 先确认变更所属模块、现有 Controller/Service/Mapper/DO 边界和已有测试。
- 对功能、修复、重构和行为变更，先记录 BDD，再执行 RED -> GREEN -> REGRESSION。
- 缺少数据库、Redis、依赖、测试数据或运行配置时，必须 fail fast；不得切换数据源、返回 mock 成功或吞掉错误。
- 接口和服务错误必须通过真实响应、日志或测试暴露；不得用默认成功值掩盖失败。

## 验证方式

- 优先运行受影响模块的定向 Maven 测试，例如：
  - `mvn -pl yudao-module-mes -am test`
  - `mvn -pl yudao-server -am test`
- 如果指定测试类，记录 `-Dtest` 范围和 `surefire.failIfNoSpecifiedTests` 处理依据。
- 涉及 API 行为时，验证成功路径和失败路径。
- 涉及前端调用时，最后通过真实前端路径或已批准的 E2E 核对接口结果。

## eDHR 详情回填门禁

### 路线配置有值但详情接口为空

- Trigger: eDHR、批次详情、动态表单、损耗单、工艺路线绑定、填写人、`fillableUsers`、`routeBindingId`、配置页有值但详情接口为空。
- Preflight check: 先同时核对配置接口/表中的来源字段、执行任务快照字段、详情接口组装链路和既有优先级，不得只改前端显示文案。
- Blocker: 若详情任务没有可追溯的绑定 ID、快照字段或正式规则来源，必须阻塞并补齐后端数据链路；不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- Verification: 新增后端回归测试覆盖“仅路线绑定配置填写人”场景，并同时跑相邻优先级测试，确认有效工作任务和工序规则仍优先。
- Forbidden action: 禁止前端把 `未配置` 改成配置页名称、禁止把角色/部门 ID 当用户 ID、禁止用空列表兜底掩盖缺失来源。
- Evidence: 任务 `doc/tasks/20260724-edhr-route-form-filler-backfill/`，目标测试 `MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。

### 切换填写人快照读取边界

- Trigger: eDHR 批次执行填写页、“切换填写人”、协助填写人、`assistSwitchTasks`、`candidateUserSnapshot`、`getEdhrBatchExecution`、弹窗打开耗时过长。
- Preflight check: 先确认业务口径是否为批次执行创建后填写人固定；若固定，切换填写人候选必须来自执行详情返回的任务/填写人快照，而不是弹窗打开时重新拉取或重算全量批次详情；传统批记录打开链路还必须把批次任务 ID 写入执行记录并按 `batchExecutionId + taskId` 查询 active 执行记录，避免新批次复用旧执行详情。
- Blocker: 执行详情缺少可追溯任务快照、活动工作任务缺少 `candidateUserSnapshot`、或无法证明候选人来自创建时快照时，必须补齐后端详情链路；若 active 执行记录查询没有按批次和任务隔离，也必须阻塞；不得从当前登录人、角色、部门或空列表推断候选填写人。
- Verification: 运行 `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`，并配合前端 ESLint/`pnpm ts:check` 与 `mvn -pl yudao-module-mes -am "-DskipTests" compile`。
- Forbidden action: 禁止在切换填写人弹窗打开时调用全量 `getEdhrBatchExecution` 作为性能问题的替代方案；禁止用前端缓存、空列表兜底或吞异常掩盖缺失快照。
- Evidence: 任务 `doc/tasks/20260727-switch-filler-snapshot-loading/verification-report.md`。


## eDHR 批次任务配置来源门禁

### 当前配置与发布快照边界

- Trigger: eDHR 批次执行、路线发布快照、`routeSnapshotJson`、`batchUseConfigs`、记录本/批记录融合、当前路线配置缺失或陈旧绑定。
- Preflight check: 新建/返工批次前先同时检查当前 BATCH 工序配置是否存在、绑定是否归属启用工序配置、发布版本快照是否包含完整 `flowGraph.nodes` 与 `batchUseConfigs`。
- Blocker: 只要当前 BATCH 工序配置存在，就必须使用当前配置并严格校验绑定归属；不得因为当前绑定陈旧而静默回退到发布快照。
- Verification: 同时覆盖“当前配置存在优先当前绑定”“当前配置整体缺失时使用已发布快照”“陈旧绑定必须 fail fast”“legacy flat batchRecordReportId 快照可投影”的后端测试。
- Forbidden action: 禁止把发布快照作为通用 fallback；禁止用空绑定、默认 MAIN 或默认成功掩盖当前配置损坏。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`。

### 草稿 BATCH 快照读写对称边界

- Trigger: 路线草稿/候选版本、`routeSnapshotJson`、`batchUseConfigs`、`formBindings`、表单槽位、`flow-config/batch-record/save`、草稿保存后读回为空或仍报“系统异常”。
- Preflight check: 同时核对保存链路写入的候选快照字段、读取策略、版本生命周期状态和当前工序设置；一旦 DRAFT 草稿显式保存过 BATCH 绑定快照，DRAFT 读取必须优先返回该草稿快照，待审批/待发布版本仍按既有规则读取当前工序设置。
- Blocker: 显式保存后的 DRAFT `batchUseConfigs.formBindings` 读回被当前工序设置覆盖、读回为空、或无法区分 legacy 候选快照与本次草稿显式保存快照时，不得宣称草稿保存完成。
- Verification: 新增后端回归测试覆盖“显式保存后的 DRAFT 快照优先于当前绑定”，并同时跑完整相邻测试类，确认 PENDING_APPROVAL / READY_TO_PUBLISH 仍读取当前工序设置。
- Forbidden action: 禁止用当前工序设置作为显式保存草稿快照的 fallback；禁止用空绑定、默认 MAIN、前端隐藏或吞异常掩盖草稿快照读写不对称。
- Evidence: `doc/tasks/20260726-route-flow-v15-save-system-exception/verification-report.md`，`MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings`。

### 冻结快照附件负责人 JSON 类型边界

- Trigger: `batchRecordAttachmentOwners`、`PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID`、`批记录附件负责人配置无效`、已有批次冻结 `route_snapshot_json` 缺配置、路线版本发布后旧批次仍打不开。
- Preflight check: 先分别核对当前 ACTIVE 路线版本快照和目标批次冻结快照的 `$.configSnapshots.batchRecordAttachmentOwners`，同时检查 `JSON_TYPE` 必须是 `ARRAY`、`JSON_LENGTH` 必须等于业务要求数量；只看到配置接口返回列表不代表冻结快照可用。
- Blocker: ACTIVE 版本缺配置、批次冻结快照缺配置、JSON 被写成 `STRING` 而不是 `ARRAY`、影响行数不是精确目标行数、或缺少原始快照备份时必须停止，不得放宽打开已有批次的校验。
- Verification: 授权数据修复必须记录原始快照备份、回滚路径、`restoreRows/repairRows`、修复后 `JSON_TYPE=ARRAY` 与 `JSON_LENGTH`，再用真实页面 `打开/创建 -> 确认` 验证不再出现负责人配置错误。
- Forbidden action: 禁止把缺失负责人配置默认成功、禁止把 JSON 数组通过用户变量/字符串写成 JSON 字符串、禁止 API-only 或直接详情 URL 替代确认按钮 E2E。
- Evidence: `doc/tasks/20260727-batch-record-attachment-owner-config/verification-report.md`。
## eDHR 批记录版本治理规则运行态门禁

### 已发布版本治理证据与 Jimu 当前 JSON 边界

- Trigger: eDHR 打开填写、`openOrCreateByContext`、`1040750243`、批记录模板未确认填写规则、`CELL_RULE_RECONCILED`、已发布批记录版本、Jimu 报表 JSON。
- Preflight check: 先核对报表 `batchRecordVersionId`、版本 `APPROVED` 状态、migration item 中 `CELL_RULE_RECONCILED` 证据、blocking item 数量，以及当前 Jimu JSON 未确认单元格数量。
- Blocker: 版本未发布、缺少 `CELL_RULE_RECONCILED` 治理证据、存在 `BLOCKER` 或未确认 `CONFIRM_REQUIRED` 时，运行态必须继续 fail-fast，不得把当前 Jimu JSON 自动标记为已确认。
- Verification: 后端测试同时覆盖“已发布且治理通过时物化运行态规则”和“无治理证据的 legacy checkbox 仍阻塞”；真实 E2E 需打开当前填写任务并核验 execution snapshot 无未确认规则字段。
- Forbidden action: 禁止直接 SQL 修改 `jimu_report.json_str`、禁止跳过 `validateConfirmedCellRules`、禁止把 API-only 或历史 execution 直连当作打开填写成功。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/verification-report.md`。

### 批记录单元格链接预填落库边界

- Trigger: eDHR 批记录单元格链接、`PRODUCTION_WORK_ORDER.batchCode`、生产批号目标格为空、`/batch-record-cell-link/prefill`、`cell_values_json=[]`、只读预览缺少已配置链接值。
- Preflight check: 先区分“来源字段不存在”和“链接值未落库”：同时核对来源业务表字段值、启用链接规则、目标 execution 的 `cell_values_json`、创建/打开执行记录写边界和字段审计链，不得只看前端 draft hydrate。
- Blocker: 来源值存在且链接规则启用，但目标 execution 未保存到 `cell_values_json` 时，必须把修复收敛到创建/打开执行记录的后端落库链路；若字段审计系统写入证据缺失，也必须阻塞，不能直接 update 主表。
- Idempotency schema check: 自动落库写入字段审计前必须核对幂等键列长度；语义组合键可能超过 `varchar(64)` 时，使用稳定原始组合键的 SHA-256 作为保存和查询共用键，并同时测试写入路径与重复打开查询路径恰好生成 64 位小写十六进制。
- Verification: 后端回归需覆盖创建执行记录、打开历史空 DRAFT、重复打开幂等、目标已有人工值不覆盖、来源批号缺失 fail-fast，并复验字段审计 hash/head revision、审计批次数量和幂等键长度；真实 E2E 需同时断言打开任务响应、执行详情 `cellValuesJson`、页面目标输入值和重复打开不追加审计批次。
- Forbidden action: 禁止把 `/prefill` 返回值或前端 `hydrateDraftState` 当作已保存结果；禁止前端写空值兜底、查询接口隐式写库、直接 SQL 回填或绕过字段审计链。
- Evidence: `doc/tasks/20260727-edhr-cell-link-auto-persist-design/verification-report.md`；`doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/verification-report.md`。

## eDHR 批记录 Word 表格解析门禁

### 全局行形态优先于模板特例

- Trigger: 批记录 Word 导入、Route B/Route D 表格识别、packed 物料矩阵、操作明细区域、`生产自检`/合格标准/检验方法说明块、截图位置错位。
- Preflight check: 先用真实源 DOC 与最小合成表格复现结构偏差，定位到共享 parser/calibrator/row-type 规则；对 packed 宽单元格必须按视觉 token 处理续行，对短标题 + 长说明行必须按说明区行形态判断。
- Blocker: 缺少真实源 DOC、测试类硬编码本地 fixture 不存在、或 RED 不能稳定复现时，不得宣称修复完成；先记录缺失 fixture 和影响范围。
- Verification: 回归必须同时包含合成 RED/GREEN 和用户指定真实 DOC 样本；至少断言 packed 括号续行不新增物料项、后续物料不整体错位、操作明细区域不吞入后续说明块。
- Forbidden action: 禁止用表单名、工序名、文件名、压力泵模板名硬编码特例；禁止把缺 fixture 的结构测试当成业务逻辑失败；禁止只靠截图人工判断完成。
- Evidence: `doc/tasks/20260725-batch-record-global-table-position-fix/verification-report.md`。

### 批记录/路线导入真实 fixture 覆盖范围变更边界

- Trigger: 批记录 Word、Sheet1 Excel、路线导入、真实 fixture、`NoSuchFileException`、用户明确说“不需要覆盖这个”或取消真实样本覆盖。
- Preflight check: 先区分“业务仍要求真实样本覆盖但 fixture 缺失”和“用户明确变更验收范围取消该真实样本覆盖”；前者必须阻塞并取得权威原件，后者必须删除依赖缺失真实 fixture 的测试入口，同时保留不依赖真实文件的合成 fail-fast/契约测试。
- Blocker: 缺少用户明确范围变更、无法证明删除的测试只覆盖被取消的真实样本链路、或删除后完整目标套件仍有 failure/error 时，必须停止，不得宣称完成。
- Verification: 记录用户范围变更、删除/保留的测试清单，运行目标 parser/contract 定向测试和完整模块回归；完整回归必须 `BUILD SUCCESS` 且 0 failures/0 errors。
- Forbidden action: 禁止用 `@Disabled`、Maven excludes、assumptions、空夹具、合成 workbook 或桌面候选文件冒充权威真实 fixture；禁止把真实样本覆盖取消解释成业务 fallback。
- Evidence: `doc/tasks/20260727-edhr-notify-all-valid-candidates/verification-report.md`，用户明确取消 Sheet1 Excel 真实样本覆盖后，保留 `Sheet1RouteExcelParserTest` 合成 fail-fast 测试并通过完整 `mvn -pl yudao-module-mes test`。

### 旧版本 JSON 的 fillForm/edhrCellRule 读时刷新门禁

- Trigger: 批记录截图或只读预览仍显示已修复过的错位 checkbox、V14/V14.0 等既有版本复验、`sheetLayoutJson` 的 `text` 坐标正确但页面仍渲染旧控件。
- Preflight check: 同时审计 `text/value`、`fillForm.labelText/componentFlag/valueType` 和 `edhrCellRule.label/componentFlag/valueType`；不得只检查静态文本坐标。
- Blocker: 若业务列仍残留未确认 AUTO 规则的旧 checkbox / BOOLEAN / 串列 label，必须在共享单元格规则刷新链路中修复并持久化，不得用截图裁剪、前端隐藏或表单名特例绕过。
- Verification: 回归测试必须覆盖 stale `fillForm` 被刷新、已确认 MANUAL 规则不被覆盖、密集表格业务列优先使用上方列头；真实页面验证需同时断言目标业务列 offender 为 0 并保留截图。
- Forbidden action: 禁止只重新导入新版本就宣称既有版本已修复；禁止按产品名、工序名、文件名、压力表文本写清理逻辑；禁止把 API-only 审计替代真实前端截图验收。
- Evidence: `doc/tasks/20260726-batch-record-v14-layout-regression/verification-report.md`。

### Jimu fillForm 组件类型语义优先边界

- Trigger: Jimu 编辑页右侧“当前组件”与批记录单元格语义不一致、日期/签名日期单元格显示为“多行文本”或普通文本、`fillForm.componentFlag=input-textarea` / `input-text`、`记录人/日期` / `操作人/日期` / `复核人/日期` 等签名日期宽空白格。
- Preflight check: 先审计后端 `MesProBatchRecordReportJsonBuilder` 生成的 `fillForm.componentFlag`、`edhrSignature` 与相邻/同一行标签语义；Jimu 右侧当前组件以 `fillForm.componentFlag` 为准，只有 `edhrSignature` 元数据不足以显示电子签名控件；宽合并空白格不得在语义判断前被 `isWideBlankNarrativeArea` 直接归类为 textarea。
- Blocker: 如果无法用最小合成表格稳定复现组件类型误判，或无法证明普通叙述型宽空白格仍保持 textarea，不得宣称修复完成。
- Verification: 必须同时覆盖“签名日期宽空白格生成 `componentFlag=signature` 并保留 `edhrSignature`”和“普通高/合并叙述空白格仍生成 `input-textarea`”两个回归断言。
- Forbidden action: 禁止只改前端“当前组件”显示文案、禁止直接手工改 Jimu JSON、禁止按模板/产品/文件名硬编码日期格、禁止只把签名日期格退化成 `input-text` 或普通日期展示而丢失电子签名组件语义。
- Evidence: `doc/tasks/20260727-jimu-signature-date-cell-type/verification-report.md`。

## 业务审批策略按配置执行门禁

### 表单模板升版/作废审批模式以 published 策略为准

- Trigger: 表单模板导入升版、作废审批、`FORM_TEMPLATE_UPGRADE`、`FORM_TEMPLATE_OBSOLETE`、`form-template-upgrade-v1`、`Form template upgrade requires BPM approval`、业务审批策略切换 DIRECT/SIGNATURE_REQUIRED。
- Preflight check: 先核对 `bpm_business_approval_policy` 中目标 executor 的 published 策略模式；`DIRECT` 必须直接执行 executor 的直接生效逻辑，`BPM_REQUIRED` 必须有对应流程 key（升版 `form-template-upgrade-v1`、作废 `form-template-obsolete-v1`）并启动 BPM。
- Blocker: DIRECT 仍启动 BPM、BPM_REQUIRED 未启动 BPM、BPM_REQUIRED 流程 key 为空或错误、seed 强行改写已发布 DIRECT 策略、或回归只能靠手工改库时必须阻塞。
- Verification: 运行 `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test`、`python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py`，并复验 BPM_REQUIRED orchestrator 相邻测试。
- Forbidden action: 禁止把 DIRECT 当成降级或绕过强行拦截；禁止把 BPM_REQUIRED 静默直通、默认成功、前端隐藏错误、手工 update 单条数据或 seed 覆盖用户显式策略。
- Evidence: `doc/tasks/20260727-form-template-approval-mode-respects-policy/verification-report.md`。

## eDHR 放行负责人来源门禁

### 工序结束放行负责人必须来自 RELEASE_APPROVE

- Trigger: eDHR 放行负责人、放行预检、放行审批、电子签名放行、`releaseOwnerLabel`、`RELEASE_APPROVE`、`CLOSE`、工艺路线“工序结束 > 放行责任人”。
- Preflight check: 同时核对路线级 `RELEASE_APPROVE` 规则、候选人解析结果、工作台 `releaseSummary` 和正式放行授权；展示与授权必须共用 `RELEASE_APPROVE`，不能只看 `stageOwnerRole` 或关闭负责人。
- Blocker: 只配置 `CLOSE` 未配置 `RELEASE_APPROVE`、`RELEASE_APPROVE` 候选池为空、用户/角色无效或运行态仍显示“执行人”时必须停止，不得把关闭负责人、当前登录人、静态阶段角色或 `stageOwnerRole` 当作放行负责人。
- Verification: 后端回归覆盖 USER、ROLE_GROUP、角色成员可放行、关闭负责人不能越权和缺失配置 fail-fast；前端静态契约覆盖放行预检/审批阶段读取 `releaseSummary.releaseOwnerLabel` 且不兜底 `stageOwnerRole`。
- Forbidden action: 禁止新增数据库迁移修历史数据、禁止把 `CLOSE` 规则复用为放行授权、禁止前端用“执行人/QA/放行员”掩盖未配置、禁止吞掉候选人解析异常。
- Evidence: `doc/tasks/20260727-edhr-release-owner-from-end-config/verification-report.md`。

## 禁止做法

- 禁止跨模块复制业务逻辑来绕过现有服务边界。
- 禁止未核对 schema 就写运行 SQL。
- 禁止捕获异常后静默返回成功、空数据或默认数据。
- 禁止缺少依赖或测试数据时跳过验证并宣称完成。

## 2026-07-25 子表集合替换软删除唯一键门禁

- Trigger: 后端更新父表时先删除再重建子表集合，且子表存在 `case_id + sort`、`parent_id + code`、`tenant_id + key` 等唯一约束，并启用了 MyBatis Plus 逻辑删除。
- Preflight check: 先核对 mapper 删除方式、唯一索引字段、逻辑删除字段是否参与唯一索引；集合替换语义若要求同一唯一键可重建，删除必须释放真实唯一键占用。
- Blocker: 逻辑删除记录仍占用唯一键且后续插入使用相同 key 时，不得用 catch、重试、跳过插入、修改 sort 或前端规避来绕过。
- Verification: 新增或更新后端回归测试，覆盖同一父记录连续两次替换子表集合且第二次使用相同排序或业务键；目标 Maven 测试必须 PASS。
- Forbidden action: 禁止把集合替换失败归因于前端重复提交；禁止为了避开唯一键冲突引入随机排序、默认成功或软失败。
- Evidence: `doc/tasks/20260725-codex-test-method-target-table-rows/verification-report.md`，`CodexTestCaseServiceImplTest#updateCase_allowsRepeatedCheckpointReplacement`。
## 2026-07-25 Maven Reactor 兄弟模块验证门禁

- Trigger: 多模块 Maven 项目中当前模块依赖兄弟模块，出现缺方法、缺字段、DO/DTO builder 不一致、或测试编译引用 sibling module 新接口时。
- Preflight check: 先确认失败符号所属模块；若符号来自同 reactor 兄弟模块，必须用 `mvn -pl <module> -am ...` 重跑，让 Maven 同时构建依赖模块。
- Blocker: `mvn -pl <module> ...` 因未构建兄弟模块而失败时，不得直接判定为产品代码阻塞；必须复验 `-am` 后再给结论。
- Verification: 任务日志同时记录窄范围失败、`-am` 复验命令、PASS/FAIL 结果和影响模块。
- Forbidden action: 禁止用旧本地产物、跳过编译、API-only、或改 unrelated sibling 代码来掩盖 reactor 构建边界问题。

## 2026-07-27 Windows Maven 增量输出删除卡住门禁

- Trigger: Windows 上目标 Maven 命令长时间无输出，`jcmd <pid> Thread.print` 显示主线程停在 `IncrementalBuildHelper.beforeRebuildExecution` 和 `WinNTFileSystem.delete0`。
- Preflight check: 先确认 Maven PID、父进程、启动命令和是否属于当前任务；检查同仓并发 Maven，但不得停止其他任务进程。
- Blocker: 目标 Maven 超时且未生成 surefire 报告时，不得宣称测试通过；只允许停止当前任务启动的 Maven PID，并记录命令、PID 和诊断栈。
- Verification: 保持项目标准 Maven 参数重新运行目标测试，必须得到明确 `BUILD SUCCESS` 和测试计数；一次关闭增量编译后的全量编译失败不能替代标准参数复验。
- Forbidden action: 禁止强杀所有 Java/Maven 进程、删除其他任务构建产物、用静态检查冒充 JUnit 通过，或把 `-Dmaven.compiler.useIncrementalCompilation=false` 固化为产品构建 fallback。
- Evidence: `doc/tasks/20260727-remove-lfs-assets/verification-report.md`。
