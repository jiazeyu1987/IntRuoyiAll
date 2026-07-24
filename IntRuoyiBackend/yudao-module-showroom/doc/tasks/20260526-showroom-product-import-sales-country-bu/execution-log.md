# 执行日志

## 本轮记录

- 2026-05-26 文档 worker：只读检查验收 Excel、后端、管理前端、Website 影响面。
- 2026-05-26 文档 worker：创建任务文档与 BDD/TDD 计划证据。
- 2026-05-26 文档 worker：按 reviewer Gate 1 反馈补充 `产品列表` 完整表头替换关系、所属公司校验约束、非导入范围和新表头 RED 计划。
- 2026-05-26 reviewer：复核文档 Gate 1/2，校正生命周期契约为现有接口值 `研发中 -> R_AND_D`，确认可进入严格 TDD 实现阶段。
- 2026-05-26 文档 worker 阶段未修改生产代码、测试代码、配置或资源文件，未执行 commit。
- 2026-05-26 后端实现：更新 Excel VO、导入/导出映射、所属公司校验、字段显示标签、批量补齐在售国家 API、产品叙事提示词与后端回归测试。
- 2026-05-26 前端/Website worker：更新管理端字段文案、列表状态列、批量入口、Website mock 与展示测试。
- 2026-05-26 reviewer：复跑后端、管理端、Website 目标验证；记录真实 Excel 检查、`pnpm ts:check` 阻塞和真实本地 E2E 阻塞。
- 2026-05-27 reviewer：重启本任务 worktree 的后端与管理端前端，完成真实测试租户 Playwright 导入验证；发现并修复导入发布路径误触发音频生成的回归。

## BDD 场景

BDD: 导入产品列表读取替换后表头 -> Given 验收 Excel `产品资料修改版.xlsx` 的 `产品列表` 只使用 `展品编码`、`产品名-中文`、`产品名-英文`、`展柜名称`、`持证公司`、`在售/在研`、`BU`、`在售国家`、`适应症`、`型号规格`、`注册证信息`、`奖项`、`原材料表单` 表头 / When 展厅产品管理执行真实导入 / Then 系统必须按新表头导入产品编码、中英文名、生命周期、BU、在售国家、适应症、型号规格和注册证信息，不能依赖旧表头标签或 fallback。

BDD: 导入产品列表读取 BU 与在售国家 -> Given 验收 Excel `产品列表` 表头包含 `BU` 和 `在售国家` / When 展厅产品管理执行真实导入 / Then `BU` 必须写入 `fields.pipeline_layout`，`在售国家` 必须写入 `fields.core_selling_points`，并生成新的产品版本或跳过无变化行，不能通过旧表头兼容或 fallback 掩盖失败。

BDD: 持证公司 mismatch 失败可见 -> Given Excel `持证公司` 与当前产品已有所属公司展示名不一致 / When 执行产品导入 / Then 导入必须失败并指出产品编码、Excel 公司名、当前所属公司；如果实现沿用 currentDetail 的 `owner_company_id`，该行为必须是明确的 no-fallback 校验，不能静默忽略 Excel 公司名。

BDD: 在售在研解析为生命周期 -> Given Excel `在售/在研` 为 `已注册` 或 `研发中` / When 执行产品导入 / Then 分别写入 `lifecycleStage` 的 `REGISTERED` 或 `R_AND_D`；其他值必须失败可见。

BDD: 非产品基础信息列不被静默导入 -> Given Excel 包含 `展柜名称`、`奖项`、`原材料表单` / When 当前实现未支持展柜映射、奖项或原材料导入 / Then 导入契约或响应必须明确这些列不在当前产品基础信息导入范围，不能静默宣称导入成功，也不能把它们作为其他字段 fallback。

BDD: 导入模板和导出文件使用新表头 -> Given 产品管理用户下载导入模板或导出产品文字资料 / When 打开 `产品列表` 工作表 / Then 表头必须包含 `BU`、`在售国家`，并不得把旧语义文案作为最终业务标签输出。

BDD: 基础信息展示新业务标签 -> Given 产品详情字段包含 `pipeline_layout`、`core_selling_points` 及英文变体 / When 管理前端打开产品基础信息或版本中心 / Then 页面标签必须显示 `BU`、`在售国家`、`BU`、`Countries on Sale`，保存时仍提交原 fields key，不能新增数据库列或 API 兼容字段。

BDD: 列表状态按在售国家判断资料完整 -> Given 产品列表返回 `fields.core_selling_points` 和 `fields.core_selling_points_en` / When 管理前端渲染状态列 / Then 状态列必须以 `在售国家` 对应文案展示中英文资料状态，缺失时仍暴露 MISS，不允许静默降级为完整。

BDD: 发布包和 Website 展示新标签 -> Given 产品发布包包含公开字段 `core_selling_points` 或 `pipeline_layout` / When 发布包投影到 legacy Website 配置并由 Website 前台读取 / Then Website `bilingualPublicFields` 保持 fieldCode 不变，但 `labelZh/labelEn/valueZh/valueEn` 必须按 `在售国家` 或 `BU` 展示。

BDD: AI/批量补齐不得写错语义 -> Given 现有批量内容生成链路仍可能写入 `core_selling_points` / When 用户触发相关功能 / Then 实现前必须明确改为 `在售国家` 补齐或移除/禁用该入口，不能把卖点内容继续写入 `在售国家` 字段。

BDD: 缺少必需表头时失败可见 -> Given 导入文件缺少 `展品编码`、`产品名-中文`、`产品名-英文`、`持证公司`、`在售/在研`、`BU`、`在售国家`、`适应症`、`型号规格` 或 `注册证信息` 表头 / When 执行导入 / Then 导入必须失败并返回清晰错误，指出缺少的表头和影响，不得静默按空值、默认值或旧字段处理。

BDD: 产品 Excel 导入只处理文字字段和发布投影所需引用 -> Given 产品已有已发布中英文讲解稿、音频和封面 / When 测试租户从产品管理导入 `产品资料修改版.xlsx` 并发布有变化产品 / Then 系统只能更新 Excel 覆盖的文字字段并保留封面，不得生成新音频、不调用封面生成、不调用 AI 补写在售国家；为满足 Website 当前版本包契约，可沿用已发布讲解稿文本和音频引用到新产品版本，但不得创建新的音频文件或改写讲解稿内容。

## RED 计划

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReadReplacementProductListHeaders test` -> FAIL, expected reason: 当前 `ShowroomProductExcelVO` 仍绑定旧 Excel 表头；只含替换后表头的 `产品列表` 无法把 `展品编码`、`产品名-中文`、`产品名-英文`、`持证公司`、`在售/在研`、`BU`、`在售国家`、`注册证信息` 映射到既有属性和 fields。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReadReplacementProductListHeaders" test` -> FAIL, actual reason: 新表头测试先经过测试依赖补齐后进入真实 RED，导入成功数 `expected: <1> but was: <0>`，证明旧 VO 无法读取只含替换后表头的 `产品列表`。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldFailOnOwnerCompanyMismatch test` -> FAIL, expected reason: 当前导入从 currentDetail 保留 `owner_company_id`，尚未证明 `持证公司` 与当前产品所属公司不一致时会失败可见。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldParseRegisteredAndInDevelopment test` -> FAIL, expected reason: 当前生命周期解析尚未覆盖 `在售/在研` 新表头下的 `已注册` 与 `研发中`。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldExposeUnsupportedNonBasicInfoColumns test` -> FAIL, expected reason: 当前导入尚未明确 `展柜名称`、`奖项`、`原材料表单` 的非导入范围或 fail-fast 行为。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest test` -> FAIL, expected reason: 当前字段显示元数据仍把 `pipeline_layout`、`core_selling_points` 显示为旧语义标签，未输出 `BU`、`在售国家`、`Countries on Sale`。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest test` -> FAIL, expected reason: 发布字段和版本中心样例仍断言旧语义标签或示例值，Website projection 无法证明新标签。

RED: `node scripts/showroom-admin-product-list.test.mjs` -> FAIL, expected reason: 产品列表状态列仍使用 `zh_core_selling_points` / `en_core_selling_points` 的旧显示文案，未改成 `在售国家` 对应状态。

RED: `node scripts/showroom-admin-version-center.test.mjs` -> FAIL, expected reason: 版本中心测试数据仍期望旧语义标签，未验证 `core_selling_points` 的新展示标签。

RED: `pnpm test -- --run` in `Website` -> FAIL, expected reason: Website mock 与测试仍显示旧语义标签，未验证 `在售国家` / `Countries on Sale`。

RED: Playwright E2E at `http://localhost:8081` -> FAIL, expected reason: 使用测试租户导入验收 Excel 后，管理前端基础信息、列表状态或 Website 前台仍未显示新业务标签；如果导入前置服务不可用，记录缺失服务和影响。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` -> FAIL, actual reason: 真实 E2E 暴露旧导入路径复用 `publishProduct(...)` 后，集成测试补充 `verify(narrationService, never()).generateAudio(anyLong())` 立即失败；调用栈为 `ShowroomApiRuntime.generateProductNarrationAudioPair(...)`，证明导入会生成 `product-*-ruoxi.wav` 音频并带来副作用。

## GREEN 计划

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS，包含替换后表头导入、所属公司 mismatch、`在售/在研` 解析、非产品基础信息列范围验证。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` -> PASS，6 tests passed。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest test` -> PASS

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest test` -> PASS

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomFoundationContractTest,ShowroomReleaseProductDetailAssemblyTest,ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest,ShowroomProductNarrationRegressionTest" test` -> PASS，40 tests passed。

GREEN: `node scripts/showroom-admin-product-list.test.mjs` -> PASS

GREEN: `node scripts/showroom-admin-version-center.test.mjs` -> PASS

GREEN: `pnpm test -- --run` in `Website` -> PASS

GREEN: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` in `yudao-ui-admin-vue3` -> PASS，reviewer rerun 48 tests passed。

GREEN: `node tests\e2e\showroom-product-toolbar-layout.spec.js` in `yudao-ui-admin-vue3` -> PASS，reviewer rerun。

GREEN: `pnpm test -- --run` in `Website` -> PASS，reviewer rerun 8 files / 73 tests passed。

GREEN: `npx playwright test kiosk-detail.spec.js` in `Website` -> PASS，reviewer rerun 2 tests passed。

GREEN: `git diff --check` in `ruoyi-vue-pro` / `yudao-ui-admin-vue3` / `Website` -> PASS，only line-ending warnings reported.

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test` after text-only import publish fix -> PASS，6 tests passed，覆盖不生成音频、不调用封面生成、不调用 AI 补写在售国家。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomFoundationContractTest,ShowroomReleaseProductDetailAssemblyTest,ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest,ShowroomProductNarrationRegressionTest" test` -> PASS，40 tests passed。

GREEN: `pnpm ts:check` in `yudao-ui-admin-vue3` with `NODE_OPTIONS=--max-old-space-size=16384` -> PASS。

GREEN: Playwright E2E at `http://localhost:8081` using test tenant `测试租户` / `aoteman` and `产品资料修改版.xlsx` -> PASS，接口返回 `totalRows=164`、`successCount=164`、`skippedCount=0`、`failureCount=0`。

GREEN: backend log check for the same real import -> PASS，仅出现 `/admin-api/showroom/product/import-excel` start/end log；未出现 `product-*-ruoxi.wav`、`Native memory allocation` 或 `SHOWROOM_AUDIO_GENERATION_FAILED`。

REGRESSION: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS；本地 48081 运行 jar `output/runtime/backend-20260527-showroom-sales-country-bu.jar`，`/actuator/health` 返回 200。

REGRESSION: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260526-showroom-product-import-sales-country-bu --mode preview --workspace <yudao-module-showroom> --worktree-closeout off --json` -> PASS，保留 `task.md`、`execution-log.md`、`backend-api-evidence.md`、`frontend-feature-evidence.md`、`verification-report.md`，无删除项、无阻塞项。

## 回归计划

- 后端回归：`mvn -pl yudao-module-showroom test`
- 管理前端回归：`pnpm ts:check`，以及受影响 showroom-admin 脚本测试。
- Website 回归：`pnpm test -- --run`，必要时补充 `npx playwright test tests/kiosk-detail.spec.js`。
- 最终校验：只用 API 做导入后的字段值核对；前端流程必须由 Playwright 操作真实页面完成。

## 阻塞与风险

- 若测试环境、登录账号或前端入口不可用，必须记录具体缺失前置条件和影响，不能 mock 成功。
- `奖项`、`原材料表单` 当前只在 `产品列表` 中作为非产品基础信息列记录；若 reviewer 要求导入 `奖项`、`原材料` 工作表，需另起明确范围或扩展本任务测试。
- `展柜名称` 当前不作为产品基础信息字段；若实现 worker 选择校验展柜映射，必须有确定性规则和测试，不能作为产品匹配 fallback。
- 若业务决定保留批量补齐入口，必须重新定义它对 `在售国家` 的行为，否则应禁止它继续写入 `core_selling_points`。
- RESOLVED: `http://localhost:8081` 已切换到本任务 worktree 的管理端 Vite；`http://127.0.0.1:48081/actuator/health` 已返回 200。
- RESOLVED: `pnpm ts:check` in `yudao-ui-admin-vue3` with `NODE_OPTIONS=--max-old-space-size=16384` -> PASS。
- RESOLVED: 真实测试租户 Playwright 导入 `产品资料修改版.xlsx` -> PASS，164 行全部成功发布。

## 真实 Excel 检查证据

- READ: `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版.xlsx` -> exists, size 42134 bytes。
- READ: workbook sheets -> `产品列表`、`奖项`、`原材料`。
- READ: `产品列表` row 1 -> `展品编码 | 产品名-中文 | 产品名-英文 | 展柜名称 | 持证公司 | 在售/在研 | BU | 在售国家 | 适应症 | 型号规格 | 注册证信息 | 奖项 | 原材料表单`。
- READ: `产品列表` row 2 -> `product_001`，`持证公司=瑛泰`，`在售/在研=已注册`，`BU` 为空，`在售国家=中国`。
- Impact: 导入契约允许 `BU` 为空，必须读取 `在售国家` 并写入 `core_selling_points`。

## 收尾状态

实现与目标自动化验证、真实测试租户 Playwright 导入验证均已完成；Reviewer Gate 3 当前可放行，待完成提交与收尾清理预览。

## 前端/Website worker 证据（2026-05-26）

BDD: 管理端基础信息展示新业务标签 -> Given 产品详情字段仍使用 `pipeline_layout`、`core_selling_points` 底层 key / When 用户打开产品基础信息或版本中心 / Then 管理端显示 `BU`、`在售国家`、`Countries on Sale`，不得把旧语义作为目标业务标签。

BDD: 管理端批量补齐入口调用在售国家接口 -> Given 后端批量入口改为 `/showroom/product/batch-generate-sales-countries` / When 用户点击产品列表批量补齐按钮 / Then 前端按钮、事件、loading、API wrapper 与提示文案均使用“在售国家”语义，不再调用旧 `batch-generate-selling-points` 路径。

BDD: Website 展示新业务标签 -> Given Website 发布配置包含 `pipeline_layout` 与 `core_selling_points` / When 前台产品详情以中英文渲染公开字段 / Then 显示 `BU`、`在售国家 / Countries on Sale` 和国家/地区示例值，不显示旧业务标签。

RED: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` in `yudao-ui-admin-vue3` -> FAIL，旧实现仍显示 `管线布局`、`核心卖点`、`一键卖点`，且状态列缺少 `在售国家` 文案；同时首次运行 `scripts/showroom-admin-product-list.test.mjs` 暴露当前 worktree 未安装 `@vue/compiler-sfc`。

RED: `node tests\e2e\showroom-product-toolbar-layout.spec.js` in `yudao-ui-admin-vue3` -> FAIL，旧实现工具栏按钮仍为 `一键卖点` 且事件仍为 `batch-generate-selling-points`。

RED: `pnpm test -- --run` in `Website` -> FAIL，当前 Website worktree 缺少 `node_modules`，`vitest` 不可用；已按锁文件执行 `npm ci --ignore-scripts` 后补齐测试依赖，再进入 GREEN 验证。

GREEN: `node --test scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-frontend.test.mjs` in `yudao-ui-admin-vue3` -> PASS，48 tests passed。

GREEN: `node tests\e2e\showroom-product-toolbar-layout.spec.js` in `yudao-ui-admin-vue3` -> PASS。

GREEN: `pnpm test -- --run` in `Website` -> PASS，8 test files / 73 tests passed。

GREEN: `npx playwright test kiosk-detail.spec.js` in `Website` -> PASS，2 tests passed，Website 产品详情英文路径展示 `BU`、`Countries on Sale`。

REGRESSION: `pnpm ts:check` in `yudao-ui-admin-vue3` -> PASS，使用 `NODE_OPTIONS=--max-old-space-size=16384` 完成全仓类型总检。早期默认 heap 与 8GB heap OOM、auto-import 类型链路缺失的失败记录已由依赖安装与类型生成链路恢复后解除，不作为当前阻塞。

REGRESSION: static old-label scan in all three worktrees -> PASS。生产代码、管理前端与 Website 当前业务展示未发现旧语义继续作为目标标签；旧词仅保留在历史任务文档、本任务旧语义说明、RED 失败证据或负向断言中。
