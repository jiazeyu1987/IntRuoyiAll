# Execution Log：调整 NAS 共享根为质量体系文件

BDD: switch NAS share root -> Given 当前 NAS 管理页共享根为 `\\172.30.30.4\it共享` When 将共享名切换为 `质量体系文件` Then 页面测试结果与目录刷新应显示新的共享根 `\\172.30.30.4\质量体系文件`

RED: `POST /admin-api/infra/file/nas-config/test` before update -> FAIL against target root，返回 `rootPath=\\\\172.30.30.4\\it共享`，未满足目标共享根 `\\\\172.30.30.4\\质量体系文件`。

GREEN: `PUT /admin-api/infra/file/nas-config` with `share=质量体系文件` -> PASS，接口返回 `{"code":0,"msg":"","data":true}`。

GREEN: `GET /admin-api/infra/file/nas-config` after update -> PASS，返回 `share=质量体系文件`。

GREEN: `POST /admin-api/infra/file/nas-config/test` after update -> PASS，返回 `rootPath=\\\\172.30.30.4\\质量体系文件`、`itemCount=9`、`message=NAS 连接成功`。

GREEN: `GET /admin-api/infra/file/nas-files?path=` after update -> PASS，返回 `rootPath=\\\\172.30.30.4\\质量体系文件`，根目录包含 `1. QMS documents`、`2.DHF`、`3.DMR` 等 9 个目录。

GREEN: Playwright 真实页面验证 -> PASS，以 `芋道源码 / admin / admin123` 登录 `http://127.0.0.1:8081/system/nas` 后，页面显示：
- `根路径：\\\\172.30.30.4\\质量体系文件；根目录条目数：9`
- `共享根：\\\\172.30.30.4\\质量体系文件`
- 根层目录含 `1. QMS documents`、`2.DHF`、`3.DMR`
