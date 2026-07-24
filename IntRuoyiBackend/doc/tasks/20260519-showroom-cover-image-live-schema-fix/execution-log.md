# 执行记录：修复 showroom 产品封面字段本机 live schema

BDD: showroom 产品查询依赖 cover_image 列 -> Given showroom 产品修订数据对象和 SQL baseline 已声明 `cover_image` / When 本机 live 后端查询 `showroom_product_revision` / Then 数据库表必须存在 `cover_image` 列，产品管理页不得再因缺列报错。

BDD: 修复 live schema 后产品页恢复可用 -> Given 本机 live MySQL 已补齐 `cover_image` 列 / When 认证查询 `/admin-api/showroom/product/page` 并打开真实产品管理页 / Then 后端返回 `code=0`，前端不再出现 `Unknown column 'cover_image'`。

REPRO: 真实前端产品页与 `/admin-api/showroom/product/page` 都报 `Unknown column 'cover_image' in 'field list'`。
ROOT CAUSE: 源码 schema 已声明 `cover_image`，但本机 live MySQL `showroom_product_revision` 仍停留在缺列状态。
RED: `SHOW COLUMNS FROM showroom_product_revision` -> FAIL，结果不包含 `cover_image`。
GREEN: `ALTER TABLE showroom_product_revision ADD COLUMN cover_image TEXT NULL AFTER model_specification` -> PASS。
GREEN: `SHOW COLUMNS FROM showroom_product_revision` -> PASS，结果已包含 `cover_image`。
GREEN: 认证后 `GET /admin-api/showroom/product/page?pageNo=1&pageSize=20` -> PASS，返回 `code=0`。
GREEN: 真实前端 `showroom/product` 页面验证 -> PASS，不再出现 `Unknown column 'cover_image'`。
RISK: 本次只修复当前本机 live 库；如果其他环境也未执行 showroom schema 升级，同样需要补列。
BLOCKER: 无剩余 blocker。
