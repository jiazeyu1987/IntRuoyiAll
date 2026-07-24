# 执行日志：修复备份服运行控制台数据盘误报

BDD: Backup 状态探测使用备份服数据盘配置 -> Given 备份服运行目录数据盘挂载在 `/mnt/intruoyi-data` 且设备为 `/dev/mapper/cl-home` / When 运行控制台探测 Backup 环境 / Then 不应按默认 `/var/lib/docker` 与 `/dev/vdb` 判定 `invalid-runtime-data-disk`。

BDD: Backup 状态展示真实服务健康 -> Given 备份服发布包 `26-05-30_00-11-31` 已运行 / When 控制台获取前端、后端、整套和 Website 状态 / Then 应展示 HTTP 200 或真实服务状态，不应用数据盘配置错误覆盖服务健康结果。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#backupStatusCommandShouldUseBackupRuntimeDataDiskArguments test` -> FAIL, Backup 状态命令缺少 `-RemoteDataRoot`、`-RemoteDataDiskMount`、`-RemoteDataDiskDevice`，实际仍只传 `-ServerHost 172.30.30.59`、`-ServerUser root`、`-RemoteAppDir /opt/intruoyi/runtime`。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#backupStatusCommandShouldUseBackupRuntimeDataDiskArguments test` -> PASS。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 31 tests passed。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.59 -ServerUser root -RemoteAppDir /opt/intruoyi/runtime -RemoteDataRoot /mnt/intruoyi-data/runtime-data -RemoteDataDiskMount /mnt/intruoyi-data -RemoteDataDiskDevice /dev/mapper/cl-home -Component backend -Json` -> PASS, `status=running`, `httpStatus=HTTP 200`，未出现 `invalid-runtime-data-disk`。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.59 -ServerUser root -RemoteAppDir /opt/intruoyi/runtime -RemoteDataRoot /mnt/intruoyi-data/runtime-data -RemoteDataDiskMount /mnt/intruoyi-data -RemoteDataDiskDevice /dev/mapper/cl-home -Component website -Json` -> PASS, `status=running`, `httpStatus=HTTP 200`，未出现 `invalid-runtime-data-disk`。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.59 -ServerUser root -RemoteAppDir /opt/intruoyi/runtime -RemoteDataRoot /mnt/intruoyi-data/runtime-data -RemoteDataDiskMount /mnt/intruoyi-data -RemoteDataDiskDevice /dev/mapper/cl-home -Component frontend -Json` -> PASS, 未出现 `invalid-runtime-data-disk`；返回真实健康结果 `frontend=HTTP 200; pdfWorker=ERROR: expected application/javascript but got application/octet-stream`。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script/deploy/show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.59 -ServerUser root -RemoteAppDir /opt/intruoyi/runtime -RemoteDataRoot /mnt/intruoyi-data/runtime-data -RemoteDataDiskMount /mnt/intruoyi-data -RemoteDataDiskDevice /dev/mapper/cl-home -Component full -Json` -> PASS, 未出现 `invalid-runtime-data-disk`；返回真实健康结果 `backend=HTTP 200; frontend=HTTP 200; pdfWorker=ERROR: expected application/javascript but got application/octet-stream; OnlyOffice=ERROR: The operation has timed out.`，当前发布包 `26-05-30_00-11-31`。

GREEN: `Invoke-WebRequest -UseBasicParsing -Uri 'http://172.30.30.59:8081/index' -TimeoutSec 10` -> PASS, HTTP 200。
