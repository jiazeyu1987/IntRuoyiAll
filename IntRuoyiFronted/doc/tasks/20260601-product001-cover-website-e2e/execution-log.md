# 执行日志：product_001 透明封面导入与 Website 发布 E2E

BDD: Excel 产品图透明源可识别 -> Given 真实 workbook `产品资料正式版.xlsx` / When 按 `展品编码=product_001` 定位 `产品图` 单元格嵌入图片 / Then 能记录图片 hash、尺寸和透明 alpha 统计。

BDD: 真实前端导入后后台封面透明 -> Given 登录本机 `芋道源码/admin` 的产品管理 / When 点击导入并上传真实 workbook / Then `product_001` 当前后台封面文件包含透明 alpha。

BDD: 手动发布后 Website 使用透明封面 -> Given 后台封面已透明且 Website 本机启动 / When 在公司信息页点击“手动发布展厅” / Then Website 中 `product_001` 的图片源文件包含透明 alpha。

GREEN: Preflight local services -> PASS, `8081` 后台前端、`48081` 后端、`8083` Website 均已监听；前端 `.env.local` 指向 `VITE_BASE_URL=http://127.0.0.1:48081`，Website `.env.local` 指向 `VITE_SHOWROOM_SITE_KEY=yingtai-showroom`、`VITE_SHOWROOM_STAGE=TEST`。

INFO: 8083 cleanup -> 发现 8083 原先由旧 worktree 的后台管理 Vite 占用，不是 Website；已停止该旧 Vite 进程并用 `D:\ProjectPackage\Website\run-website.bat` 将真正 Website preview 启动到 `http://127.0.0.1:8083/`。

GREEN: Excel product_001 cover alpha evidence -> PASS, `产品资料正式版.xlsx` 的 `产品列表` sheet 第 2 行、`产品图` 列嵌入图为 `xl/media/image1.png`，导出到 `evidence/product001-excel-cover.png`；`sha256=b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`，`format=PNG`，`mode=RGBA`，`size=120x90`，透明像素 `9679/10800`，透明比例 `0.8962037037`。

GREEN: Playwright admin import real workbook -> PASS, 真实 UI 在 `/showroom/product` 上传 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 后返回 `totalRows=160`、`successCount=160`、`skippedCount=0`、`failureCount=0`；证据见 `evidence/admin-import-response-rerun.json`。

GREEN: Admin product_001 cover alpha after import -> PASS, 后台当前 `product_001` 修订版本为 `revisionNo=47`，封面 URL 指向 `product-product_001-imported-cover-b7a35f69730887ea.png`；下载文件 `sha256=b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`，`size=120x90`，透明像素 `9679/10800`；证据见 `evidence/product001-admin-cover-after-rerun-import-db.json`。

INFO: manual publish attempts through real UI -> `POST /admin-api/showroom/release/publish` payload 均为 `{"siteKey":"yingtai-showroom","stage":"TEST"}`，但多次响应为 `success=false`、`code=500`、`SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED`；证据见 `evidence/admin-manual-publish-response-stable-website.json`、`evidence/admin-manual-publish-response-final-success-attempt.json`、`evidence/admin-manual-publish-response-after-stale-e2e-stopped.json`、`evidence/admin-manual-publish-response-monitored-final.json`、`evidence/admin-manual-publish-response-final-clean-env.json`。

GREEN: Website published product_001 source alpha -> PASS, 在 8083 确认为 `D:\ProjectPackage\Website` 时，`/showroom/sites/yingtai-showroom/stages/TEST/release/current` 对应发布物中的 `product_001.previewImage.contentHash` 为 `b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`，资源文件 `alphaMin=0`、透明像素 `9679/10800`；证据见 `evidence/website-product001-final-source-alpha.json`。

BLOCKED: Website real page screenshot -> FAIL, 本机旧 E2E 残留进程反复启动旧 worktree 后台管理 Vite 到 `8083`，包括 `tests/e2e/showroom-product-import-atomicity.e2e.js`、`--mode showroom03 --port 8083`、`SHOWROOM_E2E_FRONTEND_BASE=http://127.0.0.1:8083` 等进程链。其行为会主动停止当前 8083 listener 并启动旧 admin，导致 Website 页面变为后台登录页或 `/showroom/sites/.../release/current` 返回 `404`。最终页面探测 `evidence/website-final-navigation-probe.json` 进入的是 `http://127.0.0.1:8083/login?redirect=/index`，不是 Website，按 no-fallback 规则不能作为验收截图。

BLOCKED: Acceptance -> FAIL, 已证明 Excel、后台封面和发布物源文件三者透明图 hash 一致，但未满足“手动发布成功返回新 releaseId”和“真实 Website 页面截图显示新图”，阻塞原因是旧 E2E 残留进程污染 8083。

INFO: task-closeout-cleanup preview -> PASS, `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-product001-cover-website-e2e --mode preview` 返回 `status: ready`；因任务状态为 blocked，仅记录预览，不执行 apply。

INFO: stale E2E process tree cleanup -> 旧任务 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\20260531-showroom-issue-batch-fix` 的 PowerShell 编排器仍在运行，会主动停止 8083 并启动旧 worktree admin Vite；已清理该编排器及其 48083/8083 子进程，之后匹配 `worktrees\20260531-showroom-product-import-success-close`、`--server.port=48083`、`SHOWROOM_E2E_FRONTEND_BASE` 的残留进程为 0。

GREEN: Website 8083 scoped release JSON -> PASS, 8083 重新由 `D:\ProjectPackage\Website` 的 Vite 进程监听，`/showroom/sites/yingtai-showroom/stages/TEST/release/current` 返回 `releaseId=20260531T183422Z-be276b74dfa8-428f69663d1f`、`manifestHash=d90939837ba247a1b775971f171c5498b97852a3567055063e36640966256c21`。

GREEN: Playwright admin manual publish real UI -> PASS, 登录本机 `芋道源码/admin`，进入 `/showroom/company` 点击“手动发布展厅”并确认；请求 `POST http://localhost:48081/admin-api/showroom/release/publish`，payload 为 `{"siteKey":"yingtai-showroom","stage":"TEST"}`，响应 `code=0`，返回 `releaseId=20260531T183422Z-be276b74dfa8-428f69663d1f`；证据见 `evidence/admin-manual-publish-response-http11-success-rerun.json` 和对应截图。

GREEN: Website product_001 real page source alpha -> PASS, Playwright 打开 `http://127.0.0.1:8083/`，切换到“心内介植入展柜”，点击 `product_001` 卡片进入详情；卡片和详情页图片源均为 `/showroom/sites/yingtai-showroom/stages/TEST/assets/product-1-preview/b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`，详情页展示 `产品编码: product_001`；证据见 `evidence/website-product001-page-http11-success-rerun.json` 和 `evidence/website-product001-detail-http11-success-rerun.png`。

GREEN: Website product_001 source alpha statistics -> PASS, 卡片源文件与详情源文件 `sha256=b7a35f69730887ead9da9e7866834635161afa8286783e9fa63dff718769d611`，`format=PNG`，`mode=RGBA`，`size=120x90`，`alphaMin=0`，透明像素 `9679/10800`；证据见 `evidence/website-product001-source-alpha-http11-success-rerun.json`。

INFO: evidence cleanup -> 已删除同一任务目录中被最终 GREEN 证据替代的早期失败尝试截图和响应，保留 16 个最终验收证据文件。

INFO: task-closeout-cleanup preview -> PASS, 再次运行 `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260601-product001-cover-website-e2e --mode preview` 返回 `status: ready`；因本任务明确需要保留截图、网络响应和 alpha/hash 证据，仅记录 preview，不执行 apply。
