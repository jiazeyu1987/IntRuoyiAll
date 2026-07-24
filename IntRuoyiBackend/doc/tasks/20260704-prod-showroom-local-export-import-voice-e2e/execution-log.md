# 执行日志

BDD: 正式服导入本机导出的 INT 产品资源包后保留产品语音 -> Given 本机导出的展厅产品 zip 中产品编号为 INT-* 且具备中文/英文产品语音, When 通过正式服后台真实页面导入 zip, Then 正式服产品当前版本可以查询到对应中文/英文产品语音且 source revision 与产品当前版本一致。

BDD: 本地导出包缺少 INT 产品语音时不得导入正式服 -> Given 本机导出的 zip 中任一 INT-* 产品缺中文或英文语音, When 准备执行正式服导入, Then 导入动作阻塞并记录缺失编号清单，不把不完整包上传正式服。

GREEN: experience-preflight -> PASS, 已读取 PowerShell、登录、服务器访问、发布备份恢复经验；用户已明确授权本次正式服 E2E 导入验证；正式服健康检查可访问；后续高风险动作只限导入用户指定验证包与只读核验。

当前状态: 正在识别本地导出、正式导入和版本核验脚本路径。
GREEN: official-login-preflight-local -> PASS, 使用系统 Chrome 完成本机 `芋道源码/admin` 到 `/showroom/product` 真实登录预检。
GREEN: official-login-preflight-prod -> PASS, 使用系统 Chrome 完成正式服 `芋道源码/admin` 到 `/showroom/product` 真实登录预检。
RED: local-product-resource-package-voice-alignment -> FAIL, 本机真实页面导出 zip 成功，但解析后 `productCount=149`、`intProductCount=149`、`workbookProductNarrationCount=0`、`manifestProductNarrationCount=0`、`assets/narration/product/` 为空；`INT-12` 等全部 INT 产品缺中文/英文语音。

BLOCKER: prod-import-e2e -> 本机导出的 zip 不满足“INT-* 产品必须同时具备中文/英文产品语音”门禁，已阻断正式服上传，未执行正式服导入写入动作。

GREEN: prod-int12-current-version-readonly -> PASS, 正式服真实登录态只读查询确认 `INT-12` 的 `productId=744`、`currentRevisionId=5196`、`revisionNo=1`、中文名 `球囊扩张压力泵`。

RED: prod-int12-current-version-narration-readonly -> FAIL, 正式服 `INT-12` 当前版本 `5196` 下，中文与英文 `PRODUCT/PUBLIC` 语音接口均返回 `SHOWROOM_TARGET_NOT_FOUND: narration not found`，不存在可与当前版本对齐的语音版本。

FINAL: 本次按用户要求完成本机导出与正式服真实登录前置核验；正式导入因本地导出包缺产品语音被安全阻断。需要先在本机为 149 个 INT 产品补齐并发布中英文产品语音，再重新导出 zip 进行正式服 E2E 导入。
BDD: 本机补齐 INT 产品讲解稿和语音后导出完整 zip -> Given 本机 149 个 INT 产品缺少产品语音, When 先执行一键讲解补齐中英文讲解稿再执行一键语音生成并发布音频, Then 本机导出的 zip 中每个 INT 产品都有中文/英文 PRODUCT 语音。

INFO: user-authorized-generation-and-prod-import -> 用户要求“先生成所有语音,然后导出,然后正式服导入测试”，继续本机写入生成与正式服导入测试链路。
RED: node scripts/generate-local-int-product-voices.mjs -> FAIL, 登录页租户输入框重复导致 Playwright strict mode violation。
GREEN-CANDIDATE: 修正登录选择器为可见输入框后重跑本地全量语音生成。

GREEN-CANDIDATE: 修正本地生成脚本登录流程为已验证的可见 login-form + tenant combobox 路径。

GREEN-CANDIDATE: 本地语音生成接口改为直连后端 http://127.0.0.1:48081，登录仍走真实前端 8081。

GREEN: node scripts/verify-local-all-int-product-voices.mjs -> FAIL, 本地全量 INT 产品语音仍缺失：ZH 缺 INT-83、INT-132..INT-166；EN 缺 INT-83、INT-131..INT-166，正式服导入暂停。

GREEN-CANDIDATE: 仅针对剩余缺口 INT-83 重新生成当前版本中英文产品语音，然后全量复验。

GREEN-CANDIDATE: INT-83 当前版本缺讲解稿，先生成讲解稿再生成语音并全量复验。

GREEN-CANDIDATE: 使用单品同步接口为 INT-83 当前版本生成讲解稿和中英文语音。

## 2026-07-04 18:43:24 +08:00 正式服导入验证继续

GREEN: experience-preflight -> PASS, 已按 docs/powershell-memory.md、server/login 授权上下文与任务记录执行；用户已明确要求正式服导入测试。本次正式服写入仅允许使用已校验完整的本机 INT-only zip，并强制覆盖导入；若 zip 语音不完整或请求未包含 OVERWRITE，脚本必须失败停止。
GREEN: local all INT voice completeness -> PASS, 本机 163/163 个 INT 产品已具备中文与英文语音，缺失/版本错配清单为空。
GREEN: prod import runner guard -> PASS, 导入脚本已强制选择覆盖导入并校验请求包含 OVERWRITE；正式服语音验证已加入 audienceType=PUBLIC。
## 2026-07-04 18:53:32 +08:00 本机批量讲解任务状态复位

GREEN: local stale narration script task reset -> PASS, 仅复位本机 infra_config 中 showroom.product.batch-narration-script.* 任务状态；原因是 INT-83 单品同步已补齐语音，但上次批量异步任务因缺 TenantContext 留下 productId=null 的失败状态，导致前端状态 normalizer 报错并中断 E2E。未修改产品、版本、语音或正式服数据。
## 2026-07-04 19:09:43 +08:00 INT-83 语音发布

GREEN: local INT-83 narration publish -> PASS, 使用本机真实登录态将 narrationVersionId=6687(ZH)、6688(EN) 按 submit -> supervisor approve -> gaoxin approve -> publish 推进到 PUBLISHED；sourceRevisionId=5676，audioFileId 分别为 9198354914803/9198354914804。
## 2026-07-04 19:12:57 +08:00 导出脚本单次下载修正

GREEN: prod import runner export guard -> PASS, 导出脚本已改为只等待浏览器真实 download 并 saveAs，不再点击导出后额外 fetch 同一 export-excel URL，避免两个并发 600MB 级 zip 构建触发本机后端 Java heap OOM。
## 2026-07-04 19:17:39 +08:00 导出脚本单次响应体修正

GREEN: prod import runner export response guard -> PASS, 前端导出为 XHR/Blob 流程不会触发 Playwright download；脚本改为点击真实导出按钮后只等待同一次 export-excel 响应并保存 response.body，不再二次请求。
## 2026-07-04 19:21:46 +08:00 本机后端堆内存调整

GREEN: local backend heap restart -> PASS, 48081 本机后端已按原 jar/参数重启并追加 -Xmx4g，用于验证完整产品语音资源包导出；stdout=D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260704-192044-xmx4g.out.log，stderr=D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260704-192044-xmx4g.err.log。

GREEN: mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#exportProductExcelShouldScopeProductNarrationsToExportedProductRows test -> PASS, 导出端只把本次产品列表中的 INT-* 产品语音写入 zip，未入展柜/未导出的 INT-99 产品语音不会进入讲解音频 sheet 或 manifest。
GREEN: prod import runner stream export token decode -> PASS, 本机导出脚本改为真实登录后读取 ACCESS_TOKEN/tenantId 并递归解包 WebStorageCache 的二次 JSON 值，再用流式下载避免 600MB zip 进入 Playwright response.body。

GREEN: prod import runner click guard -> PASS, 正式服导入按钮被搜索框 pointer intercept 时改为 force click；支持 REUSE_LOCAL_EXPORT=1 复用已通过本地包校验的 zip，避免重复 600MB 导出。

GREEN: prod import and readonly voice version check -> PASS, 正式服导入请求已执行；只读核验 INT-12/INT-1/INT-10 当前产品版本均存在 PRODUCT/PUBLIC 中文与英文语音，sourceRevisionId 与 currentRevisionId 对齐，音频字段存在。详见 artifacts/prod-voice-version-readonly-final.json。

FINAL: 正式服产品 zip 导入导出语音对齐验证完成 -> PASS，本机 zip=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260704-prod-showroom-local-export-import-voice-e2e\artifacts\local-showroom-product-resource-package.zip，size=633807921 bytes；正式服只读核验文件=artifacts/prod-voice-version-readonly-final.json；结果摘要=artifacts/prod-local-export-import-voice-result.json。
