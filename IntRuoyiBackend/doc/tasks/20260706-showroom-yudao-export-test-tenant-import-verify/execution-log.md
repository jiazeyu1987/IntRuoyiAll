# 执行日志：展厅芋道源码导出到测试租户导入一致性验证

## BDD
- BDD: 跨租户导出导入一致性 -> Given 芋道源码存在展厅产品数据 / When 从芋道源码导出资源包并导入测试租户 / Then 测试租户导入后的展厅产品、旧产品编号、产品主数据与导出源一致。
- BDD: 导入前置失败即停止 -> Given 登录、导出包、产品主数据或导入接口任一阶段失败 / When 执行跨租户导入验证 / Then 停止并报告明确失败原因，不继续比较。

## Evidence
- GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/login-access.md`、Playwright 技能与 task-closeout-cleanup 规则；本任务只在本机执行，芋道源码只读导出，测试租户执行用户授权的导入写入。

## RED
- RED: `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path /showroom/product --timeout 90000` -> FAIL，Playwright 默认 `chromium_headless_shell-1223` 启动时报 `Invalid file descriptor to ICU data received`；失败发生在浏览器启动前，不是账号、租户或展厅页面失败。
- RED: `node doc/tasks/20260706-showroom-yudao-export-test-tenant-import-verify/showroom-cross-tenant-verify.mjs` -> FAIL，脚本早期版本未校验下载文件 magic number，把源导出返回的 45 字节 JSON 错误体误当 zip 上传，测试租户导入 `/admin-api/showroom/product/import-excel` 返回 `code=500,msg=系统异常`；已修正脚本为非 `PK` zip 立即 fail fast。
- RED: `node doc/tasks/20260706-showroom-yudao-export-test-tenant-import-verify/showroom-cross-tenant-verify.mjs` -> FAIL，芋道源码导出 `/admin-api/showroom/product/export-excel?pageNo=1&pageSize=200` 返回 HTTP 200 但 body 为 `{"code":500,"msg":"系统异常","data":null}`，未生成合法 zip，停止导入与一致性比较。

## GREEN
- GREEN: `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path /showroom/product --timeout 90000` with `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe` -> PASS，芋道源码真实登录和展厅产品页前置通过。
- GREEN: `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /showroom/product --timeout 90000` with `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=C:\Program Files\Google\Chrome\Application\chrome.exe` -> PASS，测试租户真实登录和展厅产品页前置通过。
- GREEN: API error log query via logged-in `芋道源码/admin` -> PASS，最新 `/admin-api/showroom/product/export-excel` 异常日志 `id=25010` 明确返回 `SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_MISSING`。

## 实施记录
- 已创建任务文档并记录用户授权范围。
- 已创建跨租户验证脚本 `showroom-cross-tenant-verify.mjs`，使用真实浏览器登录、真实导出接口、真实导入接口和 workbook 对比逻辑。
- 已修正验证脚本：导出下载内容必须是 zip magic `PK`，否则直接报告源导出错误，禁止把错误 JSON 上传到测试租户。
- 已验证源租户产品主数据映射前置：展厅产品均存在 `product_master_id`，且 `showroom_product.product_code` 能解析到 MDM 产品主数据；当前导出阻塞不是产品主数据缺失。
- 已验证源租户封面和讲解音频文件元数据基础前置：产品封面、奖项封面、已发布讲解音频均能解析到 `infra_file` 元数据；当前导出阻塞发生在 INT 产品 ZH/EN 讲解完整性校验。

## Blockers
- BLOCKER: source-export -> 芋道源码导出失败，后端 API 错误日志 `id=25010` 显示 `SHOWROOM_PRODUCT_RESOURCE_PACKAGE_NARRATION_MISSING: INT product narration incomplete, missing=INT-1[ZH/EN],INT-15[ZH/EN],INT-81[ZH/EN],INT-86[ZH/EN],INT-97[ZH/EN],INT-129[ZH/EN]`。
- Impact: 无法取得合法展厅资源包，无法按用户要求执行“芋道源码导出 -> 测试租户导入 -> 一致性比较”。继续导入会违反 fail-fast 和无静默降级要求。
- Required next step: 先在芋道源码补齐上述 6 个 INT 产品的中文和英文讲解音频，或由用户明确批准调整导出规则；未补齐前本任务不能完成一致性验证。

## Closeout
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260706-showroom-yudao-export-test-tenant-import-verify --mode preview` from `ruoyi-vue-pro` -> PASS，preview 状态 `ready`，保留 `task.md` 与 `execution-log.md`，建议删除任务临时脚本和无效导出 zip。
- COMMIT: skipped -> 源租户导出被真实数据前置阻塞，验证未通过；按 Git 提交策略不提交未完成/阻塞任务。

## Reopen
- BDD: 补齐源租户产品讲解后复验 -> Given 源租户 6 个 INT 产品缺少 ZH/EN 讲解音频 / When 使用现有展厅讲解生成与发布接口补齐音频后重新导出并导入测试租户 / Then 源租户可生成合法资源包，测试租户导入成功且关键 workbook 数据一致。
- GREEN: user-authorization -> PASS，用户明确要求继续并补齐上述 6 个产品的中文和英文讲解音频；本轮允许对 `芋道源码` 这 6 个产品执行讲解音频补齐写入。


## 2026-07-06 17:46:31 - 冻结产品前置条件处理
- GREEN: source-narration-repair -> PASS, 芋道源码 6 个产品 INT-1/INT-15/INT-81/INT-86/INT-97/INT-129 已补齐并发布 ZH/EN 讲解音频，源租户导出资源包成功生成。
- RED: cross-tenant-import -> FAIL, 测试租户导入 `/admin-api/showroom/product/import-excel?sameProductAction=OVERWRITE` 返回 `SHOWROOM_PRODUCT_FROZEN: frozen product cannot be placed in hall`。
- GREEN: experience-preflight -> PASS, 本次高风险动作限定为测试租户 `aoteman` 通过既有业务接口解冻导入包内冻结展品；不修改芋道源码租户，不直接写库，不引入 fallback。
- BLOCKER-CANDIDATE: target-frozen-products -> 测试租户中 15 个导入包内产品为冻结状态，需先按业务接口解冻后才能执行导入摆放覆盖：INT-1, INT-64, INT-69, INT-70, INT-71, INT-72, INT-73, INT-74, INT-75, INT-81, INT-82, INT-83, INT-86, INT-97, INT-129。


## 2026-07-06 17:47:37 - 测试租户冻结产品解冻
- GREEN: target-unfreeze-blocking-products -> PASS, 通过 `/admin-api/showroom/product/unfreeze` 解冻测试租户导入包内 15 个冻结产品：INT-1, INT-64, INT-69, INT-70, INT-71, INT-72, INT-73, INT-74, INT-75, INT-81, INT-82, INT-83, INT-86, INT-97, INT-129。
- VERIFY: target-unfreeze-blocking-products -> PASS, 逐个查询 `showroom/product/page?keyword=<code>`，上述产品 `frozen=false`。


## 2026-07-06 18:00:48 - 旧产品编号导入回归修复
- BDD: 导入覆盖既有展品应写入旧产品编号 -> Given 产品列表包含 `旧产品编号` 且目标租户已有同展品编码 / When 使用 OVERWRITE 导入 / Then 展厅产品 `legacyProductCode` 更新为 Excel 中的旧产品编号并可被后续导出。
- RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldOverwriteLegacyProductCodeFromProductList -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, 期望 `product_legacy_overwrite` 但实际为 `null`，证明覆盖导入未写回 `旧产品编号`。
- GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldOverwriteLegacyProductCodeFromProductList -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 覆盖导入读取 `产品列表.旧产品编号` 并写入既有展品。
- Root Cause: `ShowroomApiRuntime#buildImportDraft` 覆盖既有产品时沿用 `currentDetail.legacyProductCode()`，未读取 `ShowroomProductExcelVO.legacyProductCode`。
- Fix: 新增 `resolveImportLegacyProductCode`，优先采用 Excel `旧产品编号` 非空值，空值时保留当前旧编号；未引入 fallback、降级或兼容旧格式分支。


## 2026-07-06 18:11:35 - 跨租户导入一致性验证完成
- GREEN: backend-restart -> PASS, `script/deploy/restart-int-ruoyi-local.ps1 -Component backend` 重启本地 48081 后端，并通过 `show-int-ruoyi-local-status.ps1 -Component backend` 验证 HTTP 200。
- GREEN: cross-tenant-export-import-verify -> PASS, `node doc/tasks/20260706-showroom-yudao-export-test-tenant-import-verify/showroom-cross-tenant-verify.mjs` 完成芋道源码/admin 导出、测试租户/aoteman 导入、测试租户再导出并比较 workbook。
- VERIFY: source-export -> PASS, 源导出包包含 Sheet：产品列表、产品主数据、奖项、讲解音频、关键词中英对照。
- VERIFY: target-import -> PASS, 产品导入 `totalRows=149, successCount=149, skippedCount=0, failureCount=0`；奖项导入 `awardTotalRows=46, awardSuccessCount=46, awardFailureCount=0`。
- VERIFY: cross-tenant-workbook-compare -> PASS, 产品列表 149/149、产品主数据 149/149、奖项 45/45、讲解音频 390/390、关键词中英对照 34/34 均 matched=true。
- Artifacts: 源包 `doc/tasks/20260706-showroom-yudao-export-test-tenant-import-verify/artifacts/source-yudao-showroom-product-resource-package.zip`；目标包 `doc/tasks/20260706-showroom-yudao-export-test-tenant-import-verify/artifacts/target-test-tenant-showroom-product-resource-package.zip`。
