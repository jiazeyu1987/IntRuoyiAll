# Execution Log: NAS 目录树跳过无权限目录（后端）

BDD: 跳过无权限目录继续同步 -> Given NAS 根目录下存在不可读取的子目录 / When 调用目录树接口 / Then 后端跳过该目录，继续返回其他可访问目录树，并附带 skipped 列表

BDD: skipped 列表显式返回 -> Given 目录树构建过程中存在被跳过目录 / When 接口成功返回 / Then 响应中包含被跳过路径和原因

RED: live `GET /admin-api/infra/file/nas-tree` -> FAIL, 当前真实返回 `STATUS_ACCESS_DENIED ... \\172.30.30.4\\it共享\\#recycle`，整次目录树同步失败

GREEN: `mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests green，目录树接口已改为跳过无权限目录并返回 skipped 列表

GREEN: live probe after fix -> PARTIAL, 真实接口不再立即报 `#recycle` 的 `STATUS_ACCESS_DENIED`，但整棵共享递归超过 `180s` 仍未完成
