# 20260601 测试服发布文件配置绑定失败

## Task Goal

修复 NAS 发布包 `26-06-01 22:49:44` 部署到测试服时失败的问题：发布脚本在导入数据库后应将 `infra_file_config.id=28` 重新绑定到测试服 MinIO 访问地址，不能保留 `127.0.0.1:9000`，并继续保持 fail-fast 校验。

## Milestones

- [x] M1: 复现并定位 `SHOWROOM_FILE_CONFIG_UNBOUND` 发布失败根因。
- [x] M2: 先补充失败回归测试，覆盖发布包恢复后的 MinIO 配置重写要求。
- [x] M3: 最小修复发布脚本，确保目标环境重绑定 SQL 实际执行。
- [x] M4: 运行目标测试与必要发布脚本静态/工具校验。
- [x] M5: 记录验证证据，按本任务范围提交变更。

## Expected Verification

- `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "deploy_release_applies_showroom_file_config_rebind_for_code_only_packages" -q`
- 相关发布脚本测试回归命令通过。
- 若重新执行测试服部署，脚本在 `SHOWROOM_FILE_CONFIG_UNBOUND` 闸门不再因 `infra_file_config.id=28` 保留 `127.0.0.1:9000` 失败。

## Current Status

completed

最终结论：

- `26-06-01 22:49:44` 测试服恢复失败的直接原因是发布脚本在 code-only / 缺少数据库 dump 的发布包路径上设置了 `SkipDatabaseSync`，旧逻辑只在数据库同步分支生成、上传和执行目标环境 `post-import.sql`，导致 `infra_file_config.id=28` 没有被重绑定，闸门输出 `SHOWROOM_FILE_CONFIG_UNBOUND` 并终止发布。
- 当前修复把目标环境重绑定 SQL 抽成 `Write-TargetBoundPostImportSql`，并要求所有 `deploy-release` / 部署路径都生成、上传、执行 `post-import.sql`，再执行 required SQL 和 `Assert-RemoteFileStorageConfigRebound`。
- 2026-06-01 测试服只读核验：`infra_file_config.id=28` 已是 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`；`product_001`、`product_003` 等封面 URL 经 `http://172.30.30.58:8081/admin-api/infra/file/28/get/...` 返回 `200 image/png`。
- 2026-06-01 Playwright 真实浏览器核验：登录测试服 `http://172.30.30.58:8081` 的 `芋道源码/admin` 后打开 `/showroom/product`，页面 `加载失败` 数量为 0，前 20 个封面图片均加载成功且无失败请求。

本轮补充验证：

- 修复 `DELIMITER $$` 在 PowerShell double-quoted here-string 中被展开的问题后，重新执行 `26-06-01 22:49:44` 测试服发布，发布脚本完整返回 `Publish completed for test`。
- 发布脚本验证通过：backend health 200、frontend 200、OnlyOffice health 200、Website root/showroom 200、PDF worker `application/javascript`、展厅真实图片代理读取 `image/png`、Website scoped current release verified。
- Playwright CLI 匿名访问 `http://172.30.30.58:8083/showroom` 通过：页面标题 `瑛泰展厅`，公司入口 `瑛泰 / 点击进入公司详情` 可见，console warnings/errors 为 0。

## Notes

- 上一后端任务 `doc/tasks/20260601-test-server-nas-principal-auto-mapping/task.md` 已标记 `completed`。
- 当前仓库已有未跟踪 `runtime/` 与其它任务产物；本任务提交时只纳入本任务直接产生的脚本、测试和任务文档。

## Cleanup Keep

- `doc/tasks/20260601-test-deploy-showroom-file-config-unbound/verification-report.md`
