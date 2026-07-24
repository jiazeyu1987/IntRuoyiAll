# 执行日志：执行一次展厅手动发布并核对结果

BDD: 测试租户可通过真实前端执行手动发布 -> Given 本机 `8081` 与 `48081` 已启动最新前后端 / When 测试租户在 `/showroom/company` 点击“手动发布展厅”并确认 / Then 前端应发出真实发布请求并返回成功或明确错误

BDD: 发布成功时 current release 应切换 -> Given 发布接口返回成功 / When 查询 `/showroom/release/current` / Then `releaseId` 应更新到新的值

BDD: 发布失败时必须暴露真实错误 -> Given 发布接口返回失败 / When 前端完成请求 / Then 页面 toast 和记录中必须保留真实后端错误信息

GREEN: 发布前 `GET http://127.0.0.1:48081/showroom/release/current` -> PASS，当前 `releaseId=20260524T100623Z-316b86ad1758`
RED: Playwright 真实前端发布 `http://127.0.0.1:8081/showroom/company` -> FAIL，接口返回 `showroom_release_asset.uk_showroom_release_asset` 唯一键冲突
GREEN: 后端修复并重启后再次 Playwright 真实前端发布 -> PASS，请求成功完成
GREEN: 发布后 `GET http://127.0.0.1:48081/showroom/release/current` -> PASS，`releaseId` 已切换为 `20260524T163916Z-e03a7b68bf1a`
