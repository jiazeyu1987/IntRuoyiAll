# 测试服发布验证报告

## 结论

本轮测试服发布未通过。`release-20260727-onlyoffice-test-r260727-1445` 已判废，不得复用。

## 已通过

- 发布包本地与 NAS manifest、兼容清单、镜像 tar 哈希一致。
- 发布来源 commit 为 `38e4e9fda369db74a2bf0b0fdaf3fbcb87b67364`，backend/frontend `dirty=false`。
- 目标服务器为 `172.30.30.58`，发布前无并发发布进程和 `RUNNING` 锁。
- 失败收口后测试服恢复为 `release-20260723-dcc-viewer-permission-r260723vp-r1`。
- 恢复后的 backend/frontend/onlyoffice 容器 running，backend health HTTP 200 且为 `UP`，frontend HTTP 200。

## 阻塞

required SQL `20260717_mes_balloon_excel_device_workstation_binding.sql` 在第 420 行失败，原因是脚本期望目标路线工序数量为 49，而测试服只读查询得到 50 条：`ROUTE-XLSX-00001=24`、`ROUTE-XLSX-00002=26`。

该问题属于测试服业务数据基线与迁移契约不一致。未手工修改业务数据，未跳过 required SQL，未执行 `mark-tested`、正式服发布或备用服发布。后续应先确认这 50 条路线工序是否为合法基线，再修复迁移契约并使用新的 releaseTag 重新构建发布。

## 失败收口

- `infra_release_migration`：本轮失败 migration 已收口为 `FAILED`。
- `infra_release_operation_lock`：`test-release-20260727-onlyoffice-test-r260727-1445` 已收口为 `FAILED`。
- 测试服 `.env IMAGE_TAG` 已恢复到失败前实际运行版本。
- 失败发布包保留在远端 release 目录作为排障证据，未删除共享存储内容。
