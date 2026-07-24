# Task: 修复 showroom 产品封面字段 live schema 回归

## Goal

修复当前运行中的 showroom 产品管理页再次出现 `Unknown column 'cover_image' in 'field list'` 的 live 故障，确认真实后端所连接的 MySQL schema 与源码基线一致，使 `/admin-api/showroom/product/page` 和前端产品管理页恢复正常。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 当前运行时使用的 live MySQL schema 核对
- showroom 产品修订表 `showroom_product_revision` 的 `cover_image` 列回归修复
- 真实 API 与前端页面的回归验证
- 本任务的 task 文档、执行日志、缺陷证据

## Non-Scope

- 不修改与 `cover_image` 缺列无关的 showroom 业务逻辑
- 不顺带处理讲解审批链路、音频生成或其他模块问题
- 不引入 fallback、兼容分支或静默降级

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-narration-manual-approval-flow\task.md`
- Status before this task: blocked
- Impact: 上一任务已明确记录为被当前更高优先级 live 故障打断，可独立开展本次回归修复。

## Bug Summary

- 当前前端 `showroom-admin/index.vue` 拉取产品列表时，后端查询 `showroom_product_revision` 报 `Unknown column 'cover_image' in 'field list'`。
- 源码中的数据对象、Mapper、测试 schema 与正式 SQL baseline 都已声明 `cover_image`，说明这是 live schema 与运行库漂移问题，而不是当前代码缺字段。

## Milestones

- [x] M1: 核对前序任务状态并创建本次任务文档/执行日志。
- [x] M2: 用真实运行配置复现历史故障并确认当前运行库状态。
- [x] M3: 确认真实运行库已包含 `cover_image` 列，本次无需重复执行 live schema 写操作。
- [x] M4: 回归验证后端 API，并记录前端真实路径存在的无关 router blocker。

## Expected Verification

- 运行态数据源检查（当前 Java 进程命令行 / 真实连接配置）
- `SHOW COLUMNS FROM showroom_product_revision`
- 认证后 `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20`
- 真实前端页面 `http://localhost:8081/showroom/product`

## Current Status

Completed for the reported SQL regression on 2026-05-20. 当前运行中的 `127.0.0.1:23306/ruoyi-vue-pro.showroom_product_revision` 已包含 `cover_image` 列；认证后的 `/admin-api/showroom/product/page?pageNo=1&pageSize=20` 在 2026-05-20 00:43:52 Asia/Shanghai 返回成功，不再复现 `Unknown column 'cover_image' in 'field list'`。本次未执行新的 live schema 写操作，因为真实运行库已处于修复后的正确状态。

## Current Findings

- 运行中的 Java 进程 `backend-20260519-232741.jar` 仍显式连接 `jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro`，不是切到了别的数据库实例。
- 通过 PyMySQL 直连 `127.0.0.1:23306` 执行 `SHOW COLUMNS FROM showroom_product_revision`，确认当前 live 表结构已包含 `cover_image`。
- 通过真实登录 `tenant-id: 1`、`admin/admin123` 调用 `/admin-api/showroom/product/page`，返回 `code=0` 与真实产品数据，说明当前 `cover_image` SQL 报错已恢复。
- 历史运行日志显示该 SQL 错误在 2026-05-19 23:32:00 与 23:39:51 仍存在，但在 2026-05-19 23:46:34 起同一路径开始恢复成功，说明这是已恢复的 live schema 漂移，而不是当前代码回归。

## Additional Blocker

- Playwright 真实浏览器登录 `http://127.0.0.1:8081/login?redirect=%2Fshowroom%2Fproduct` 后，请求链路中的 `POST /admin-api/system/auth/login` 与 `GET /admin-api/system/auth/get-permission-info` 都返回 `200`，但前端当前抛出 `ReferenceError: Cannot access 'remainingRouter' before initialization`，页面停留在登录页。
- 影响：本次无法把“产品管理页 UI 已恢复”完全归因到浏览器快照，因为当前 dev 前端另有 router 初始化错误；但这不是 `cover_image` SQL 问题。

## Final Verification

- PASS: 运行态数据源检查（Java 进程命令行 -> `127.0.0.1:23306/ruoyi-vue-pro`）
- PASS: `SHOW COLUMNS FROM showroom_product_revision`
- PASS: `POST /admin-api/system/auth/login` with `tenant-id: 1`, `admin/admin123`
- PASS: `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20&keyword=`
- BLOCKED: Playwright 浏览器页面闭环，因无关前端错误 `remainingRouter` 初始化失败
