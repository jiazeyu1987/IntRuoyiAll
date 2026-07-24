# 任务：修复备份服运行控制台数据盘误报

## 任务目标

- 修复运行控制台 Backup 列错误显示 `invalid-runtime-data-disk` 的问题。
- 保持备份服务器 `172.30.30.59` 使用既定数据盘 `/mnt/intruoyi-data` 与设备 `/dev/mapper/cl-home`。
- 修复后验证 Backup 列对应前端、后端、整套、Website 状态不再因数据盘口径错误显示错误。

## 前序任务检查

- 已确认上一任务 `doc/tasks/20260601-backup-server-publish-260530-001131/task.md` 状态为 `completed`。
- 当前仓库存在无关未跟踪 `runtime/`，本任务不触碰、不提交。

## BDD 场景

- BDD: Backup 状态探测使用备份服数据盘配置 -> Given 备份服运行目录数据盘挂载在 `/mnt/intruoyi-data` 且设备为 `/dev/mapper/cl-home` / When 运行控制台探测 Backup 环境 / Then 不应按默认 `/var/lib/docker` 与 `/dev/vdb` 判定 `invalid-runtime-data-disk`。
- BDD: Backup 状态展示真实服务健康 -> Given 备份服发布包 `26-05-30_00-11-31` 已运行 / When 控制台获取前端、后端、整套和 Website 状态 / Then 应展示 HTTP 200 或真实服务状态，不应用数据盘配置错误覆盖服务健康结果。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复现 `invalid-runtime-data-disk` 并定位判定来源。
- [x] M3：增加失败回归测试并最小修复。
- [x] M4：验证脚本/API/页面状态恢复，记录证据。
- [ ] M5：收尾清理预览并提交本任务改动。

## 预期验证

- 复现命令能在修复前得到 `invalid-runtime-data-disk`。
- 回归测试先 RED 后 GREEN。
- 备份服状态脚本或运行控制台 API 返回 Backup 服务健康，不再因数据盘配置误报。
- `http://172.30.30.59:8081/index` 保持 HTTP 200。

## 已完成工作

- 定位到运行控制台默认环境配置把 Backup 也按 Test/Production 的 `/var/lib/docker` 与 `/dev/vdb` 探测，导致备份服真实数据盘 `/mnt/intruoyi-data`、`/dev/mapper/cl-home` 被误报为 `invalid-runtime-data-disk`。
- 为 `RuntimeControlServiceImplTest` 增加回归测试，验证 Backup 状态命令必须携带 `-RemoteDataRoot /mnt/intruoyi-data/runtime-data`、`-RemoteDataDiskMount /mnt/intruoyi-data`、`-RemoteDataDiskDevice /dev/mapper/cl-home`。
- 为远程运行控制环境增加数据盘参数，保持 Test/Production 默认值不变，Backup 单独覆盖真实挂载与设备；状态探测与重启命令共用该配置。

## 验证结果

- RED 已复现：新增用例在修复前失败，Backup 命令未传数据盘参数。
- GREEN 已通过：新增用例通过。
- REGRESSION 已通过：`RuntimeControlServiceImplTest` 31 个用例通过。
- 备份服脚本探测已验证：backend 与 website 为 `running`，frontend/full 不再报 `invalid-runtime-data-disk`，而是暴露真实健康项 `pdfWorker` MIME 与 OnlyOffice 探测结果。
- `http://172.30.30.59:8081/index` 返回 HTTP 200。

## 当前状态

status: completed
