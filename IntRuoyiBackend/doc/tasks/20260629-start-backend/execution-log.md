# Execution Log：启动本机后端

- 用户需求：`启动后端`
- BDD: 本机后端可通过既有脚本启动 -> Given 项目提供标准本机后端重启脚本 / When 执行标准脚本启动 backend 组件 / Then `http://127.0.0.1:48081/actuator/health` 返回健康状态且端口监听存在。
- GREEN: `Get-NetTCPConnection -LocalPort 48081 -State Listen` -> PASS，发现监听进程 PID `74444`。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，返回 `{"status":"UP"}`。
- BLOCKER: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> FAIL，本地 MySQL 前置探针报错 `Unknown column 'ole_id' in 'where clause'`；当前已运行健康实例不受影响，因此本次“启动后端”用户目标仍已满足。
