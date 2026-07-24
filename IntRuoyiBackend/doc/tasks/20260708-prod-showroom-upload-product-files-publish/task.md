# 正式服展厅产品文件上传与发布

## 任务目标

- 按 `C:\Users\BJB110\Desktop\zhanting\展厅文件与产品编号对应关系.xlsx` 的映射，将本地文件上传到正式服对应产品。
- 文件必须绑定到对应产品里的正确分类/资源位置，保留一对多关系。
- 全部上传成功后，通过正式后台手动发布展厅。
- 手动发布成功并完成正式 Website 验证后，目标才算完成。

## 里程碑

- [x] M1 建立任务记录并完成正式服/上传/发布经验门禁。
- [x] M2 确认正式服产品、分类/资源字段、上传接口和发布接口。
- [x] M3 备份正式服并执行最小安全上传/绑定。
- [x] M4 完成全部 27 个文件、81 条产品绑定并核验正式数据。
- [x] M5 手动发布展厅并验证 Website release。

## 经验门禁

- PowerShell/中文/远端命令：已读取 `docs/powershell-memory.md`，中文路径、Excel、远端命令和输出必须显式 UTF-8。
- 正式服访问：已读取 `docs/server-access.md`；本目标由用户明确要求正式服上传和发布，写入范围限定为展厅产品文件上传、产品资源绑定、手动发布和验证。
- 备份恢复：已读取 `docs/release-backup-restore.md`；正式服写入前必须先做数据库/对象相关备份或确认已有可用备份点，不得覆盖整库或清理 NAS。
- 登录/E2E：已读取 `docs/login-access.md`；正式服后台验证必须使用已授权正式环境入口和 `芋道源码/admin` 真实路径，不得静默切换租户。
- 展厅发布验收：已读取 `docs/agent-memory/project-error-prevention.md`，发布成功后必须验证 Website 前台真实 release，不只看后台接口成功。
- 受保护资源：默认 `infra_file_config.id=28`、bucket `yudao`、`showroom/%` 媒体 URL 受保护；上传不得改写该配置到非默认域。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，优先复用正式后台业务接口和既有展厅发布链路，禁止直接 SQL 拼文件关系。
- 是否存在临时补丁或绕过：否。

## 预期验证

- 上传前：正式服产品编码/旧编码映射可定位到 69 个目标产品，文件分类/资源字段明确。
- 上传后：27 个本地文件在正式服生成可访问 URL/文件记录，81 条产品绑定完整，无空绑定、无错租户、无本机路径入库。
- 发布后：正式后台手动发布返回成功 release，Website 根页面和目标产品详情可访问，控制台/响应无展厅 release 完整性错误。

## Current Status

completed

- 已解析 Excel 映射并完成正式服 27 个文件、69 个目标产品、81 条产品附件绑定关系处理。
- 已完成 `.m4v/.m4V` 上传策略修复、附件-only 产品发布复用已发布讲解音频修复，并部署到正式服。
- 正式服产品修订已全部发布；展厅 release 发布因产品附件被整文件读入 release 内存触发 `Java heap space`，已完成本地根因修复与 RED/GREEN 回归验证。
- 下一步：发布 release OOM 修复到测试服和正式服后，重新执行正式服展厅 release 发布并验证 Website。

## Final Status

completed

- Excel 映射已完成解析并按正式服业务接口处理：81 条映射、27 个唯一文件、69 个目标产品，正式产品附件绑定与产品修订发布已完成。
- 后端已修复并发布三项根因：`.m4v/.m4V` 上传策略、附件-only 产品发布复用既有讲解音频、展厅 release 发布大附件不读正文不内嵌 release asset。
- OOM 修复发布包 `release-20260708-showroom-release-oom-6c7a613b9d` 已完成 build-release、测试服部署、mark-tested、正式服 preflight dry-run 与正式服部署。
- 正式服后端运行态已确认：`IMAGE_TAG=release-20260708-showroom-release-oom-6c7a613b9d`，`intruoyi-backend:release-20260708-showroom-release-oom-6c7a613b9d`，`/actuator/health` 返回 `UP`。
- 正式展厅 release 已发布成功：releaseId=`20260707T203144Z-be276b74dfa8-70f3eea512e2`，manifestHash=`b242c1d4e9bd894ea27d3d3b3e9de28ff374d3ebb18ac5b809d12e85d6aaae7c`，documentCount=194，assetCount=612。
- Website 验证通过：`http://172.30.30.57:8083/`、`/showroom`、`/showroom/` 均 HTTP 200，JS/CSS 静态资源均 HTTP 200。
- 产品详情附件抽查通过：website-index 中 147 个产品，抽样 20 个产品详情均有附件 URL；附件 URL 指向 `/admin-api/infra/file/28/get/showroom/product-attachments/...`，无本机路径、无 `assetId`、无 `contentHash`。

## Final Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomReleaseWebsiteIndexAssemblyTest#shouldPublishProductDetailAttachmentsAsUploadedFileReferencesWithoutReadingBinaryContent test` -> PASS。
- `mvn -pl yudao-module-showroom -Dtest=ShowroomReleaseWebsiteIndexAssemblyTest test` -> PASS，8 tests, 0 failures。
- `build-release-showroom-release-oom-manifest-validation.json` -> PASS，Manifest v1 validation passed。
- `deploy-test-showroom-release-oom-backend-only` -> PASS，测试服 currentReleaseTag=`release-20260708-showroom-release-oom-6c7a613b9d`。
- `mark-tested-showroom-release-oom-backend-only-success.log` -> PASS，NAS 发布包已标记测试通过。
- `release-20260708-showroom-release-oom-6c7a613b9d-prod-preflight-release-dry-run.json` -> PASS，prod dry-run 只读证据通过，writeActions 为空。
- `deploy-prod-showroom-release-oom-backend-only.log` -> PASS，正式服发布完成并释放发布锁。
- `prod-release-publish-after-oom-deploy.json` -> PASS，正式展厅 release publish HTTP 200，耗时 20.89s。
- `prod-backend-log-after-release-publish-after-oom-deploy.txt` -> PASS，release publish 请求完成，未再出现 `OutOfMemoryError` / `Java heap space`。
- `prod-website-verification-after-release-publish.json` -> PASS，正式 Website 页面和静态资源可访问。
- `prod-release-product-detail-attachment-url-probe-after-publish.json` -> PASS，产品详情附件为文件 URL 引用，无本地路径和 release asset 内嵌字段。
