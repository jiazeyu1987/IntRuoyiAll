# 任务：修复 showroom 产品封面字段本机 live schema

## 目标

修复本机 `127.0.0.1:23306/ruoyi-vue-pro` 中 `showroom_product_revision` 缺少 `cover_image` 列的问题，使产品管理页与 showroom 产品查询不再触发 SQLSyntaxErrorException。

## 前置任务检查

- 上一个 showroom 后端任务：`ruoyi-vue-pro/doc/tasks/20260519-showroom-hall-products-no-manual-order/task.md`
- 启动前状态：已完成。
- 影响：可独立开展本次 live schema 修复。

## 缺陷摘要

- 前端真实产品页请求 `/admin-api/showroom/product/page` 时，后端 SQL 报错 `Unknown column 'cover_image' in 'field list'`。
- 源码与测试 schema 已包含 `cover_image`，当前问题只出在本机 live MySQL schema 未同步升级。

## 里程碑

- [x] M1：创建任务文档并确认源码/schema 现状。
- [x] M2：对本机 live MySQL 执行最小 schema 修复。
- [x] M3：验证产品页查询与真实页面恢复。

## 预期验证

- `SHOW COLUMNS FROM showroom_product_revision`
- 认证后 `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20`
- 真实前端页面 `http://127.0.0.1:8081/showroom/product`

## 当前状态

已完成：本机 live MySQL 已补齐 `cover_image` 列，产品页 API 与真实页面查询恢复正常。

## 当前结论

- 已确认根因：源码 baseline、测试 schema 与数据对象都已包含 `cover_image`，只有本机 `127.0.0.1:23306/ruoyi-vue-pro.showroom_product_revision` 缺少该列。
- 已完成修复：对 live MySQL 执行 `ALTER TABLE showroom_product_revision ADD COLUMN cover_image TEXT NULL AFTER model_specification`。
- 已完成验证：认证后 `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20` 返回 `code=0`；前端 `showroom/product` 页面不再出现 `Unknown column 'cover_image'`。

## 最终验证

- PASS：`SHOW COLUMNS FROM showroom_product_revision` 可见 `cover_image`
- PASS：认证后 `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20`
- PASS：真实前端页面 `http://127.0.0.1:8081/showroom/product`
