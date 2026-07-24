# 执行日志：核对发布脚本中的测试与正式服务器地址

BDD: 发布脚本环境核对 -> Given 用户想确认真实测试服务器和正式服务器是否已经写在发布脚本里 When 读取仓库中的发布脚本和统一入口脚本 Then 应明确输出测试/正式服务器地址、远端目录和各自入口文件
GREEN: 只读检查 `运维工具.bat`、`publish-int-ruoyi-to-test.ps1`、`promote-int-ruoyi-test-to-prod.ps1` 以及环境证据文档 -> PASS，已确认测试服务器 `172.30.30.58`、正式服务器 `172.30.30.57`、远端目录 `/opt/intruoyi/runtime`
