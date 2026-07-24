# 执行日志：电子签名治理自动回填

BDD: 长期留存选择真实文件签名候选 -> Given 系统存在真实文件签名记录 / When 用户在长期留存页选择候选 / Then DCC回执来源ID、对象Key、版本ID、SHA256、证据Hash 自动回填，不要求用户手工编写。

BDD: 周期复核选择真实投影样本 -> Given 系统存在真实签名记录 / When 用户在周期复核页选择样本 / Then 来源表、来源ID、来源Hash、动作、含义 自动回填。

BDD: CSV质量包选择发布候选 -> Given 系统能提供真实签名治理候选 / When 用户在CSV质量包页选择候选 / Then Release ID、材料证据、追溯证据、QA签名证据 自动回填；缺候选时显式提示。

BDD: 技术证据字段禁止手填 -> Given 用户打开电子签名治理页 / When 字段表达 ID、Hash、ObjectKey、Ref 或审计事件 / Then 字段必须由真实来源自动回填或保持禁用，用户不能手工填写。

INFO: scope -> 优先复用真实签名记录；不造假 ID、Hash、对象 Key；无候选时显式提示。

RED: node scripts\signature-governance-page-contract.test.mjs -> FAIL, 页面缺少真实签名候选加载与自动回填入口。

RED: node tests\e2e\signature-governance-e2e-static.spec.js -> FAIL, E2E helper 仍要求人工提供 DCC 对象Key、Hash 与复核来源字段。

RED: mvn -pl yudao-module-dcc -Dtest=DccElectronicSignatureManagementServiceTest#getSignaturePage_enrichesControlledFileAndActorMetadata -DfailIfNoTests=false test -> FAIL, DCC 签名响应缺少 sourceObjectKey/sourceVersionId/controlledCopyObjectKey/controlledCopyVersionId。

GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS

GREEN: node tests\e2e\signature-governance-e2e-static.spec.js -> PASS

GREEN: npm run ts:check -> PASS

GREEN: mvn -pl yudao-module-dcc -Dtest=DccElectronicSignatureManagementServiceTest#getSignaturePage_enrichesControlledFileAndActorMetadata -DfailIfNoTests=false test -> PASS

GREEN: experience-preflight -> PASS, 真实 E2E 仅使用测试租户 `测试租户/aoteman` 访问本机 `http://localhost:8081`，不写入业务数据，不操作正式服。

GREEN: Playwright real E2E no-candidate path -> PASS, 登录测试租户 `测试租户/aoteman` 后访问 `/signature-governance/retention`、`/signature-governance/periodic-review`、`/signature-governance/csv-package`，真实调用 `/admin-api/dcc/electronic-signatures/page?pageNo=1&pageSize=20`，页面非 404，候选加载入口可见，接口返回 `total=0` 时页面显式提示无真实文件签名样本。

BLOCKER: Playwright real E2E auto-fill-with-row -> 测试租户 `tenant_id=122` 当前 `dcc_controlled_file_signature` 共 242 条均为 `deleted=1` 且 `evidence_status=HISTORICAL_UNBOUND`，正常分页接口不会返回软删记录；缺少可验证自动回填成功的未删除真实 DCC 签名记录。

RED: node scripts\signature-governance-page-contract.test.mjs -> FAIL, DCC回执来源ID仍可手工编辑。

GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS, 技术证据字段已禁用，eDHR归档、CSV发布、验证包、培训执行、变更记录来源加载函数存在。

GREEN: node tests\e2e\signature-governance-e2e-static.spec.js -> PASS, E2E helper 不再要求手工传入对象Key、Hash、来源ID、追溯Ref 等技术环境变量。

GREEN: Playwright real E2E disabled-field path -> PASS, 登录测试租户 `测试租户/aoteman` 后访问 `/signature-governance/retention` 与 `/signature-governance/csv-package`，页面非 404，责任人、DCC样本、eDHR样本、DCC对象Key、eDHR对象Key、恢复对象Key、预期SHA256、Release ID、文档ID、追溯证据、培训ID、变更ID、签名证据均为 disabled；点击加载按钮后真实调用 `/dcc/electronic-signatures/page`、`/mes/pro/batch-record-execution-archive/page`、`/mes/pro/edhr-release/page`。

BLOCKER: npm run ts:check -> FAIL, unrelated dirty file `src/views/mes/pro/task/calendar/index.vue` 存在 `Property 'id' does not exist on type 'ProScheduleCalendarIssueItemVO'`，非本次电子签名治理改动文件。

BDD: 治理字段尽量自动生成 -> Given 用户进入长期留存、周期复核、CSV质量包 / When 系统存在文件主配置、统一策略和真实业务来源 / Then 除真实样本选择外，配置、状态、负责人、周期、模块等字段均由系统生成或禁用为空，不允许手填。

RED: node scripts\signature-governance-page-contract.test.mjs -> FAIL, Endpoint 等治理配置字段仍可编辑。

GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS, 治理配置、周期复核、CSV状态/Owner/批准人等字段均禁用，并接入自动生成函数。

GREEN: node tests\e2e\signature-governance-e2e-static.spec.js -> PASS, E2E helper 不再要求留存、复核、CSV 的手工环境变量。

BLOCKER: npm run ts:check -> FAIL, only remaining error is unrelated dirty file `src/views/mes/pro/task/calendar/index.vue` missing `ProScheduleCalendarIssueItemVO.id`.

BLOCKER: Playwright real E2E auto-generation path -> 登录后等待跳转超时，未完成本轮真实浏览器验证；不得标记为通过。

BDD: 自动生成字段友好展示 -> Given 用户进入长期留存、周期复核、CSV质量包 / When 字段来自文件主配置、真实签名样本、统一策略或业务候选 / Then 页面不展示一排禁用输入框，而以来源卡、只读摘要和明确操作区展示自动生成结果。

RED: node scripts\signature-governance-page-contract.test.mjs -> FAIL, 契约仍要求旧表单字段文案，无法保护新的来源卡与只读摘要布局。

GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS, 10 tests，长期留存、周期复核、CSV质量包已采用 workbench/source-strip/generated-grid/readonly-item 布局，且契约禁止退回 disabled 表单。

GREEN: node tests\e2e\signature-governance-e2e-static.spec.js -> PASS, 静态 E2E helper 不依赖页面内手填字段。

BLOCKER: npm run ts:check -> FAIL, Node 默认堆内存 OOM。

BLOCKER: $env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check -> FAIL, only remaining error is unrelated dirty file `src/views/mes/pro/task/calendar/index.vue(1874,37): Property 'id' does not exist on type 'ProScheduleCalendarIssueItemVO'`.
