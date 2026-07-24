# 任务：运行控制台真实带数据提升正式服验证

## 任务目标

- 使用 Playwright 通过真实前端路径点击运行控制台的“提升正式服”按钮。
- 在弹窗中选择“带数据发布”，把测试服代码、MySQL 数据库和 MinIO `yudao` 桶同步到正式服。
- 发布完成后验证正式服 Website 根路径和 `/showroom` 能正常打开。

## 前序任务检查

- 后端上一任务 `20260525-runtime-control-real-promote-prod-flow` 状态为 `completed`，无阻塞。

## BDD 场景

- BDD: 带数据提升正式服成功 -> Given 运维人员在本机运行控制台打开“提升正式服”弹窗, When 选择“带数据发布”、填写原因并输入 `PROD` 后确认执行, Then 系统应提交 `promote-prod` 动作，参数包含 `publishScope=with-data`，发布脚本应执行数据库同步和 MinIO 同步，并最终成功。
- BDD: 带数据发布后 Website 正常打开 -> Given 带数据提升正式服完成, When 访问正式服 Website 根路径和 `/showroom`, Then 页面应返回成功响应且浏览器可以加载展厅页面。
- BDD: 操作日志可追溯 -> Given 带数据提升正式服动作完成, When 查看运行控制台操作日志, Then 最近操作应显示“提升正式服”“带数据发布”和成功状态，日志包含数据同步证据。

## 里程碑

- [x] M1：建立任务文档并确认前序任务状态。
- [x] M2：扩展真实 E2E 支持带数据提升正式服。
- [x] M3：记录发布前测试服和正式服状态。
- [x] M4：用 Playwright 完整执行带数据提升正式服。
- [x] M5：验证正式服 Website、展厅、健康状态和操作日志。

## 预期验证

- Playwright 从真实前端路径操作“提升正式服”。
- 请求参数为 `publishScope=with-data`。
- 操作日志不包含 `-SkipDatabaseSync` 或 `-SkipMinioSync`。
- 操作日志包含数据库导出/导入和 MinIO mirror 相关证据。
- 正式服 `48081/actuator/health` 返回 HTTP 200。
- 正式服 `8083/` 和 `8083/showroom` 返回 HTTP 200，并通过真实浏览器加载。

## 当前状态

- 状态：completed
- 已完成：
  - 已发现并修复带数据提升正式服时远端中转包写入正式服根分区的问题；发布中转目录改为 `/var/lib/docker/intruoyi-releases`。
  - 已发现并修复 Website bind mount 目录被替换后容器未重建导致 `/showroom` 500 的问题；提升脚本现在会 `docker compose up -d --force-recreate website`。
  - 已通过 Playwright 从真实前端路径选择“提升正式服”与“带数据发布”，填写原因并输入 `PROD`。
  - 已验证成功操作 `5806c1d8-ebd9-405e-85cf-f37b322397c2` 状态为 `succeeded`，审计参数为 `publishScope=with-data`。
  - 已验证日志包含 MySQL dump/import、MinIO `mc mirror --overwrite` 和 Website 强制重建证据。
  - 已验证正式服后端、管理端前端、Website 根路径和 `/showroom` 均返回 HTTP 200。
  - 已验证真实浏览器可打开正式服 `http://172.30.30.57:8083/` 和 `http://172.30.30.57:8083/showroom`。
- 阻塞：暂无。
