# PQC 规程任务生成与数量门禁修复

## Task Goal

修复 AC-M12 至 AC-M15 暴露的 PQC 规程发布、任务生成、数量一致性、上午/下午巡检身份隔离、末检不适用依据和放行完整性缺口，确保系统按发布 QA 规程生成并校验正式 PQC 任务。

## Milestones

- [x] M1：建立 RED 证据，覆盖计划数量不一致、多件样本截断和前端数量可改写。
- [x] M2a：补齐后端 PQC 提交数量严格校验，提交实际数量、计划数量和逐项样本数不一致时 fail fast。
- [x] M3a：补齐前端提交侧数量不可改写/不截断策略。
- [x] M2b：补齐正式 PQC 任务生成器、上午/下午巡检生成身份和适用末检任务生成。
- [x] M2c：补齐放行完整性预期 PQC 任务集合校验。
- [ ] M2d：补齐末检不适用依据的正式持久化模型。
- [ ] M4：运行目标后端、前端静态合同和必要回归验证。
- [ ] M5：记录验证报告、经验沉淀、清理与提交推送。

## Expected Verification

- 后端目标 JUnit：`MesFrontlinePqcContextServiceTest`、`MesOrderReleaseCompletenessServiceTest`、新增/更新 PQC 任务生成相关测试。
- 后端静态合同：`node yudao-module-mes\src\test\js\mes-pqc-task-generation-static.spec.cjs`，覆盖生成器必须查已发布规程、AM/PM 巡检身份分离、`301×5%` 向上取整为 `16`、重复身份 fail fast，以及放行完整性必须按工序快照检查 FIRST/PATROL AM/PATROL PM/FINAL 预期任务集合。
- 前端静态合同：覆盖 PQC 检验数量不可手工偏离任务计划数量、样本提交不截断多件值、QA 规程页面不再声称“仅预览未接入”。
- 结构检查：`git diff --check`，任务文档 UTF-8 可读。
- 若全量运行态或真实 E2E 前置缺失，按项目 fail-fast 记录 blocker，不用 mock/API-only 代替。

## Current Status

blocked：已完成并静态验证前端数量锁定/不截断、后端提交侧数量 fail-fast、正式 PQC 任务生成器、上午/下午巡检身份分离、`301×5%` 向上取整为 `16`、适用末检任务生成和放行完整性预期任务集合校验；后端 Maven/JUnit 在当前机器持续受 JVM native memory/pagefile、并行 Maven 占用与超时阻塞，且末检“不适用”显式依据仍无正式字段/表/接口模型，不能安全标记 AC-M12 至 AC-M15 全量 `ACCEPTED`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；所有缺规程、缺任务、数量不符、重复任务和末检依据缺失均应 fail fast。
- `是否从根因和长期维护角度解决`：是；目标是补齐正式规程/任务/放行数据链路，而不是前端文案或默认成功。
- `是否存在临时补丁或绕过`：否；若验证前置缺失，将记录 blocker 而不是扩大 mock 或 API-only 验收。

## Applicable Experience Gates

### MES PQC 项目级检验快照门禁

- Trigger: PQC 填写、PQC 组长复核、QA 检验规程、检验设备、设备编号、接收标准、检验方法、参数上下限、`itemResults`、`rawPayload.pqcPieceValues`、`pqcItemDetails`。
- Preflight check: 修改 PQC 链路前先核对发布 QA 规程项目、项目级设备、接收标准上下限、单位和精度字段；提交契约必须以结构化 `itemResults[]` 为业务事实。
- Blocker: 客户端可改写标准/方法、后端把 raw payload 当权威、缺发布规程项目或设备主数据时默认成功，必须停止。
- Verification: 后端回归覆盖 schema、项目设备、`itemResults` 提交和明细冻结；前端覆盖填写页项目入口和组长页读取结构化明细。
- Forbidden action: 禁止用固定四项字段、默认上下限、空标准、raw payload 或 API-only 展示替代正式项目级快照。
- Evidence: `docs/backend-development.md#mes-pqc-项目级检验快照门禁`。

### QA 规程配置状态必须来自产品级规程记录

- Trigger: QA 规程配置页、DCC 项目代码对应产品、`project-statuses`、`mes_qa_inspection_regulation.product_id`。
- Preflight check: 配置状态必须由后端按产品 ID 查询 QA 规程记录并返回，前端只能展示和错误处理。
- Blocker: 前端常量、样例模板、空状态或接口失败被当作配置状态来源时必须停止。
- Verification: 后端回归覆盖已配置/未配置产品按请求顺序返回；前端静态契约覆盖正式 API 和失败可见错误。
- Forbidden action: 禁止用前端文案、压力泵样例模板、API-only 展示或吞错误替代后台 QA 规程配置事实。
- Evidence: `docs/backend-development.md#mes-pqc-项目级检验快照门禁`。
