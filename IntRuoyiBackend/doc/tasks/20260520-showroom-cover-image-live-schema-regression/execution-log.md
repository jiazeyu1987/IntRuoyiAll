# Execution Log: 修复 showroom 产品封面字段 live schema 回归

BDD: showroom 产品页查询依赖 cover_image 列 -> Given 当前 showroom 产品修订数据对象、Mapper 与 SQL baseline 已声明 `cover_image` / When 真实运行中的 `/admin-api/showroom/product/page` 查询最新产品修订 / Then live MySQL `showroom_product_revision` 必须存在 `cover_image` 列，页面不得因缺列报错。

BDD: 修复 live schema 回归后产品页恢复可用 -> Given 真实运行库的 `showroom_product_revision` 与源码 schema 基线一致 / When 认证请求 `/admin-api/showroom/product/page` 并打开真实前端 `showroom/product` 页面 / Then 后端返回成功结果，前端不再出现 `Unknown column 'cover_image'`。

INFO: 2026-05-20 已将上一条同仓库任务 `20260519-showroom-narration-manual-approval-flow` 标记为 blocked，原因是当前更高优先级 live showroom 故障需要先修复。
REPRO: `output/runtime/backend-20260519-232741.out.log` 历史运行日志在 2026-05-19 23:32:00、23:39:46、23:39:51 记录了 `Unknown column 'cover_image' in 'field list'`，证明该故障确实在真实运行态出现过。
GREEN: Java 进程命令行检查 -> PASS，当前后端实例 `backend-20260519-232741.jar` 连接 `jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro`。
GREEN: `SHOW COLUMNS FROM showroom_product_revision` via PyMySQL -> PASS，当前 live 表结构已包含 `cover_image`。
GREEN: `POST http://127.0.0.1:48081/admin-api/system/auth/login` with `tenant-id: 1`, `admin/admin123` -> PASS，返回 `code=0`。
GREEN: `GET http://127.0.0.1:48081/admin-api/showroom/product/page?pageNo=1&pageSize=20&keyword=` with bearer token -> PASS，返回 `code=0` 与真实产品数据。
GREEN: 运行日志回查 -> PASS，`/admin-api/showroom/product/page` 在 2026-05-20 00:43:52、00:45:20、00:47:26 均已成功完成。
BLOCKED: Playwright 真实浏览器页面闭环 -> FAIL for unrelated frontend issue，`POST /admin-api/system/auth/login` 与 `GET /admin-api/system/auth/get-permission-info` 均成功，但前端抛出 `ReferenceError: Cannot access 'remainingRouter' before initialization` 并停留在登录页。
ROOT CAUSE: 当前调查时点的 live MySQL schema 已经与源码基线一致，`cover_image` 缺列不是活动中的运行故障；浏览器剩余阻塞来自无关的前端 router 初始化错误，而不是 showroom SQL。
