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


## eDHR 批次任务配置来源门禁

### 当前配置与发布快照边界

- Trigger: eDHR 批次执行、路线发布快照、`routeSnapshotJson`、`batchUseConfigs`、记录本/批记录融合、当前路线配置缺失或陈旧绑定。
- Preflight check: 新建/返工批次前先同时检查当前 BATCH 工序配置是否存在、绑定是否归属启用工序配置、发布版本快照是否包含完整 `flowGraph.nodes` 与 `batchUseConfigs`。
- Blocker: 只要当前 BATCH 工序配置存在，就必须使用当前配置并严格校验绑定归属；不得因为当前绑定陈旧而静默回退到发布快照。
- Verification: 同时覆盖“当前配置存在优先当前绑定”“当前配置整体缺失时使用已发布快照”“陈旧绑定必须 fail fast”“legacy flat batchRecordReportId 快照可投影”的后端测试。
- Forbidden action: 禁止把发布快照作为通用 fallback；禁止用空绑定、默认 MAIN 或默认成功掩盖当前配置损坏。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`。
## eDHR 批记录版本治理规则运行态门禁

### 已发布版本治理证据与 Jimu 当前 JSON 边界

- Trigger: eDHR 打开填写、`openOrCreateByContext`、`1040750243`、批记录模板未确认填写规则、`CELL_RULE_RECONCILED`、已发布批记录版本、Jimu 报表 JSON。
- Preflight check: 先核对报表 `batchRecordVersionId`、版本 `APPROVED` 状态、migration item 中 `CELL_RULE_RECONCILED` 证据、blocking item 数量，以及当前 Jimu JSON 未确认单元格数量。
- Blocker: 版本未发布、缺少 `CELL_RULE_RECONCILED` 治理证据、存在 `BLOCKER` 或未确认 `CONFIRM_REQUIRED` 时，运行态必须继续 fail-fast，不得把当前 Jimu JSON 自动标记为已确认。
- Verification: 后端测试同时覆盖“已发布且治理通过时物化运行态规则”和“无治理证据的 legacy checkbox 仍阻塞”；真实 E2E 需打开当前填写任务并核验 execution snapshot 无未确认规则字段。
- Forbidden action: 禁止直接 SQL 修改 `jimu_report.json_str`、禁止跳过 `validateConfirmedCellRules`、禁止把 API-only 或历史 execution 直连当作打开填写成功。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/verification-report.md`。

## eDHR 批记录 Word 表格解析门禁

### 全局行形态优先于模板特例

- Trigger: 批记录 Word 导入、Route B/Route D 表格识别、packed 物料矩阵、操作明细区域、`生产自检`/合格标准/检验方法说明块、截图位置错位。
- Preflight check: 先用真实源 DOC 与最小合成表格复现结构偏差，定位到共享 parser/calibrator/row-type 规则；对 packed 宽单元格必须按视觉 token 处理续行，对短标题 + 长说明行必须按说明区行形态判断。
- Blocker: 缺少真实源 DOC、测试类硬编码本地 fixture 不存在、或 RED 不能稳定复现时，不得宣称修复完成；先记录缺失 fixture 和影响范围。
- Verification: 回归必须同时包含合成 RED/GREEN 和用户指定真实 DOC 样本；至少断言 packed 括号续行不新增物料项、后续物料不整体错位、操作明细区域不吞入后续说明块。
- Forbidden action: 禁止用表单名、工序名、文件名、压力泵模板名硬编码特例；禁止把缺 fixture 的结构测试当成业务逻辑失败；禁止只靠截图人工判断完成。
- Evidence: `doc/tasks/20260725-batch-record-global-table-position-fix/verification-report.md`。

### 旧版本 JSON 的 fillForm/edhrCellRule 读时刷新门禁

- Trigger: 批记录截图或只读预览仍显示已修复过的错位 checkbox、V14/V14.0 等既有版本复验、`sheetLayoutJson` 的 `text` 坐标正确但页面仍渲染旧控件。
- Preflight check: 同时审计 `text/value`、`fillForm.labelText/componentFlag/valueType` 和 `edhrCellRule.label/componentFlag/valueType`；不得只检查静态文本坐标。
- Blocker: 若业务列仍残留未确认 AUTO 规则的旧 checkbox / BOOLEAN / 串列 label，必须在共享单元格规则刷新链路中修复并持久化，不得用截图裁剪、前端隐藏或表单名特例绕过。
- Verification: 回归测试必须覆盖 stale `fillForm` 被刷新、已确认 MANUAL 规则不被覆盖、密集表格业务列优先使用上方列头；真实页面验证需同时断言目标业务列 offender 为 0 并保留截图。
- Forbidden action: 禁止只重新导入新版本就宣称既有版本已修复；禁止按产品名、工序名、文件名、压力表文本写清理逻辑；禁止把 API-only 审计替代真实前端截图验收。
- Evidence: `doc/tasks/20260726-batch-record-v14-layout-regression/verification-report.md`。

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
