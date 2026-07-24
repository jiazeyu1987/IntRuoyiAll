# IntRuoyi 服务器访问说明

## 当前系统位置

- 工作区根目录：`E:\IntRuoyi`
- 后端与运维入口：`E:\IntRuoyi\IntRuoyiBackend`
- 前端工程：`E:\IntRuoyi\IntRuoyiFronted`
- 统一运维入口：`E:\IntRuoyi\IntRuoyiBackend\运维工具.bat`
- 部署脚本目录：`E:\IntRuoyi\IntRuoyiBackend\script\deploy`
- 远端运行目录：`/opt/intruoyi/runtime`

## 固定环境

| 环境 | 主机 | 前端 | 后端健康检查 | 展厅站点 |
|---|---|---|---|---|
| 测试服务器 | `172.30.30.58` | `http://172.30.30.58:8081/` | `http://172.30.30.58:48081/actuator/health` | `http://172.30.30.58:8083/` |
| 正式服务器 | `172.30.30.57` | `http://172.30.30.57:8081/` | `http://172.30.30.57:48081/actuator/health` | `http://172.30.30.57:8083/` |
| 备用服务器 | `172.30.30.59` | `http://172.30.30.59:8081/` | `http://172.30.30.59:48081/actuator/health` | `http://172.30.30.59:8083/` |

## 访问门禁

- 默认不访问任何远端服务器；测试、正式、备用环境都必须有当前任务明确授权。
- 远端操作必须先确认目标环境、目标主机、操作类型、数据影响和回滚或清理方式。
- 正式服务器和备用服务器按生产等级处理，发布或重启必须显式确认 `PROD`。
- 缺少主机、SSH、脚本、运行目录、数据盘、MinIO 容器或目标租户时必须 fail fast，不得切换到其他服务器、目录、容器或账号。
- 不在本文档、任务日志或提交中记录 SSH 密码、私钥内容、数据库密码、MinIO 密钥、令牌或 VPN 密钥。

## 测试服务器

- 状态检查：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\show-int-ruoyi-test-status.bat`
- 重启脚本：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-to-test.bat`
- 发布脚本：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\publish-int-ruoyi.ps1 -Environment test -ServerHost 172.30.30.58`
- 统一入口示例：`cmd /c "E:\IntRuoyi\IntRuoyiBackend\运维工具.bat test -ServerHost 172.30.30.58"`
- 直接 SSH 仅限已授权任务：`ssh root@172.30.30.58`

## 正式服务器

- 状态检查：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\show-int-ruoyi-prod-status.bat`
- 重启脚本：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-to-prod.bat`
- 发布脚本：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\publish-int-ruoyi.ps1 -Environment prod -ConfirmText PROD -ServerHost 172.30.30.57`
- 统一入口示例：`cmd /c "E:\IntRuoyi\IntRuoyiBackend\运维工具.bat prod PROD -ServerHost 172.30.30.57"`
- 直接 SSH 仅限已授权任务：`ssh root@172.30.30.57`

## 备用服务器

- 备用服务器主机：`172.30.30.59`
- 备用服务器没有独立 `.bat` 包装入口；必须使用通用 PowerShell 脚本并显式传入主机、数据盘、发布根目录和 MinIO 容器参数。
- 状态检查示例：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.59 -ServerUser root -RemoteAppDir /opt/intruoyi/runtime -RemoteDataRoot /mnt/intruoyi-data/runtime-data -RemoteDataDiskMount /mnt/intruoyi-data -RemoteDataDiskDevice /dev/mapper/cl-home -RemoteMinioContainer intruoyi-minio -Component full -Json`
- 发布示例：`E:\IntRuoyi\IntRuoyiBackend\script\deploy\publish-int-ruoyi.ps1 -Environment backup -ConfirmText PROD -ServerHost 172.30.30.59 -BackupServerHost 172.30.30.59 -RemoteReleaseRoot /mnt/intruoyi-data/intruoyi-releases -RemoteDataRoot /mnt/intruoyi-data/runtime-data -RemoteDataDiskMount /mnt/intruoyi-data -RemoteDataDiskDevice /dev/mapper/cl-home -RemoteMinioContainer intruoyi-minio`
- 备用发布不得回退到根分区、`/var/lib/docker` 默认 release/data 路径或其他 MinIO 容器。
- 直接 SSH 仅限已授权任务：`ssh root@172.30.30.59`

## 发布参数规则

- `publish-int-ruoyi.ps1` 默认不会凭空选择目标主机；直接发布必须传 `-ServerHost`，或由已确认的运行控制台环境变量提供目标主机。
- `-Mode build-release` 和 `-Mode deploy-release` 需要显式配置测试与备用目标主机：`-TestServerHost 172.30.30.58 -BackupServerHost 172.30.30.59`；正式发布还需要 `-ProdServerHost 172.30.30.57`。
- 测试与正式默认远端数据参数为：`RemoteDataRoot=/var/lib/docker/intruoyi-data/runtime-data`、`RemoteReleaseRoot=/var/lib/docker/intruoyi-releases`、`RemoteDataDiskMount=/var/lib/docker`、`RemoteDataDiskDevice=/dev/vdb`。
- 备用服务器必须使用：`RemoteDataRoot=/mnt/intruoyi-data/runtime-data`、`RemoteReleaseRoot=/mnt/intruoyi-data/intruoyi-releases`、`RemoteDataDiskMount=/mnt/intruoyi-data`、`RemoteDataDiskDevice=/dev/mapper/cl-home`、`RemoteMinioContainer=intruoyi-minio`。

## 使用顺序

- 读操作优先用状态脚本，确认运行目录、容器状态和 HTTP 健康检查。
- 写操作优先用统一入口或部署脚本，不直接拼接远端命令。
- PowerShell 串联命令不要使用 `&&`；需要串联时使用分行或分号。
- 任何脚本失败都按真实失败处理，不做静默重试、环境切换或数据源降级。

