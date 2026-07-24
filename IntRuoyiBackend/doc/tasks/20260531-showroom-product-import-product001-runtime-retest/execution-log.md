# 执行日志：重启本地后端复验 product_001 导入封面变化

BDD: 新后端导入 product_001 图片差异算变化 -> Given 本地 48081 运行包含封面 hash 修复的新后端 / When 通过前端或接口导入 `产品资料正式版.xlsx` / Then `product_001` 不再因为旧固定导入封面 URL 被列入跳过产品。

BDD: 旧后端跳过结果可解释 -> Given 48081 仍运行 2026-05-31 17:35 的旧 jar / When 导入同一 workbook / Then 弹窗仍可能显示 `product_001` 在跳过产品列表。

REPRO: 用户截图中导入结果已分行显示，但 `跳过产品` 仍包含 `product_001`。

ROOT CAUSE: 本地 8081 前端 `.env.local` 指向 `http://127.0.0.1:48081/admin-api`；48081 PID 54816 启动于 2026-05-31 17:35，运行 `backend-companytype-20260531-173100.jar`，早于后端封面 hash 修复提交 `20ab7c1f4a`。

REPRO: Playwright 登录诊断发现当前 8081 实际请求 `http://localhost:48082/admin-api`；48082 PID 51936 运行 `backend-showroom-import-20260531-173410.jar`，同样早于后端封面 hash 修复提交 `20ab7c1f4a`。

GREEN: mvn -pl yudao-server -am -DskipTests package -> PASS，生成 `yudao-server/target/yudao-server.jar`。

BLOCKER: 首次启动新 jar 到 48081 时 fail fast：缺少 `yudao.dcc.download.encryption.*` 必填配置，错误为 `base64-key must be valid Base64`。影响：后端无法启动，不能复验导入。处理：使用本地开发专用显式启动参数补齐 policy-version、key-id、base64-key 和 artifact-directory，不改源码、不提交密钥。

GREEN: 启动 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-showroom-import-20260531-193000.jar` 到 48081 -> PASS，`/actuator/health` 返回 `UP`。

GREEN: 启动同一新 jar 到 48082 -> PASS，Tomcat 日志显示 `Tomcat started on port 48082`，PID 63264，命令行指向 `backend-showroom-import-20260531-193000.jar`。

GREEN: Playwright real frontend path at `http://127.0.0.1:8081/showroom/product` with test tenant `测试租户/aoteman` -> PASS，登录后用户显示 `芋道1`，上传 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx`，接口请求打到 `http://localhost:48082/admin-api/showroom/product/import-excel`。

VERIFY: 测试租户导入返回 `totalRows=160, successCount=0, skippedCount=160, failureCount=0`，`skippedProductCodes` 包含 `product_001`。进一步核查 workbook 第 2 行第 13 列 `产品图` 的 SHA-256 前 16 位为 `b7a35f69730887ea`；测试租户当前 `product_001` 封面 URL 为 `/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-product_001-imported-cover-b7a35f69730887ea.png`，与 workbook 图片内容一致，因此新后端跳过是正确结果。

VERIFY: 默认租户当前 `product_001` 封面 URL 仍为旧固定路径 `/admin-api/infra/file/28/get/showroom/product/cover/20260531/product-product_001-imported-cover.png`。该租户刷新页面后使用新 48082 导入时，会上传带 hash 的导入封面并被判定为变化；本次自动化未在默认租户执行导入，避免修改非测试租户数据。

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-showroom-product-import-product001-runtime-retest --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json -> PASS，保留 `task.md` 与 `execution-log.md`，`delete=[]`，`blocked=[]`。
