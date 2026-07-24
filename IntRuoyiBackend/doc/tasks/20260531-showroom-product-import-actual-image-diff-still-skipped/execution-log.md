# 执行日志：排查实际封面图片不同但导入仍提示相同

BDD: 实际图片内容不同必须算变化 -> Given 系统当前封面实际文件内容与 Excel `产品图` 嵌入图片字节不同 / When 导入该产品行 / Then 产品不能进入跳过无变化列表。

BDD: 仅 URL 或文件名相同不能代表图片相同 -> Given 当前封面 URL 与导入封面 URL 文本相同或相似 / When 实际文件内容不同 / Then 导入必须识别为变化并发布新版本。

BDD: 旧导入封面 URL 已匹配内容也要刷新地址 -> Given 当前封面使用旧式 `product-编码-imported-cover.png` 地址且该地址可能被前端缓存 / When Excel 嵌入图片内容与当前文件内容相同 / Then 导入仍发布一次新版本并把封面改为带内容哈希的导入封面 URL。

REPRO: 用户截图显示导入 `产品资料正式版.xlsx` 后仍返回 `总行数：160 / 成功发布：0 / 跳过无变化：160 / 跳过产品：product_001...`，用户确认实际图片不同。

## Bug

导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 时，`product_001` 在界面上仍被提示为“跳过无变化”，用户看到的系统封面与 Excel 产品图不一致。

## Expected

如果 Excel 嵌入图片和当前系统封面实际内容不同，必须发布新版本；如果实际内容相同但当前封面仍是旧式固定导入 URL，必须把封面规范化为带内容哈希的 URL，用新版本刷新浏览器缓存地址。

## Reproduction

- 本地前端 `http://localhost:8081` 的运行时配置指向 `http://localhost:48081`，当前 48081 有本地后端进程监听。
- 真实 Excel 中 `product_001` 产品图 SHA-256 为 `b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`。
- 当前租户 1 的 `product_001` 仍保存旧式封面 URL：`/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-product_001-imported-cover.png`。
- 该旧式 URL 当前返回的文件字节 SHA-256 已经是 `b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`，但旧 URL 可被浏览器缓存为旧图，因此继续跳过无法刷新界面可见封面。

## Root Cause

前序修复已改为按实际文件字节比较导入图片和当前封面，所以服务端确认内容相同后会保留当前 `cover_image` 字段。历史数据中存在旧式固定导入封面 URL：`product-编码-imported-cover.png`。这种 URL 曾被重复写入不同内容，浏览器或前端缓存会把相同 URL 继续显示为旧图片。由于导入未把旧式 URL 规范化为带内容哈希的新 URL，用户看到图片不同，但导入结果仍提示“相同”。

RED: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增 `importProductExcelShouldCanonicalizeLegacyImportedCoverUrlEvenWhenContentMatches` 后编译失败，缺少 `ShowroomProductCoverImageService#importedCoverImageUrlMatchesContentHash`，证明当前实现没有旧式 URL 内容哈希判定入口。

## Fix

- `ShowroomProductCoverImageService` 新增 `importedCoverImageUrlMatchesContentHash`，用于判断当前导入封面 URL 是否已经是匹配图片内容的哈希 URL。
- `ShowroomApiRuntime#resolveImportCoverImage` 在实际字节相同但当前 URL 是旧式 `product-编码-imported-cover.png` 且未匹配内容哈希时，重新上传导入图片并返回带哈希的 URL，使 `cover_image` 字段产生变化并发布新版本。
- 保留既有行为：普通当前封面内容相同仍跳过；实际图片内容不同仍上传并发布新版本。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `Tests run: 47, Failures: 0, Errors: 0, Skipped: 0`。

## Verification

- 覆盖旧式固定导入封面 URL 即使内容匹配也发布新版本并换成哈希 URL。
- 覆盖封面服务只认可 `product-编码-imported-cover-<hash>.ext` 形式为已规范化 URL。
- 回归覆盖实际产品图不同会发布、当前封面内容相同且非旧式导入 URL 会跳过。
- bug-regression 证据校验通过：`Bug regression evidence is valid.`。
- task-closeout-cleanup 预览通过：`delete: <none>`，`blocked: <none>`。
- `mvn -pl yudao-server -am -DskipTests package` 通过，新的本地后端包已生成。
- 本地 48081 已切换到 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260531-233551-import-cover-fix.jar`。首次启动因 DCC 下载加密缺少必填配置失败；补齐 `yudao.dcc.download.encryption.*` 本地运行参数后启动成功，`GET http://127.0.0.1:48081/admin-api/system/auth/get-permission-info` 返回 HTTP 200。

## Blockers

无。
