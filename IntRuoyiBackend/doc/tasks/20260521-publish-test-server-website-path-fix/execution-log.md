BDD: 测试发布脚本必须定位到真实 Website 仓库路径 -> Given 线程基线规定展厅前端路径为 `D:\ProjectPackage\Website` / When 发布脚本解析 Website 仓库目录 / Then 脚本必须使用真实存在的绝对路径，不得解析到不存在的 `D:\ProjectPackage\Int\Website`。

BDD: 修复路径后发布必须继续包含数据同步 -> Given 用户要求连带数据一起发布 / When 修复脚本并重新执行测试发布 / Then 发布链路仍必须完成数据库导入、MinIO 同步、远端重启和健康检查，不得跳过数据同步。

RED: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1` -> FAIL, `Resolve-Path` 试图访问不存在的 `D:\ProjectPackage\Int\Website`，导致发布在构建前直接停止。

RED: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1` -> FAIL, 修复 `Website` 路径后，脚本删除远端 `/opt/intruoyi/runtime/website` 目录但未重建，导致 `scp -r` 到该目标时失败。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 发布脚本测试已覆盖 `D:\ProjectPackage\Website` 路径解析与远端 `website` 目录重建约束。

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.ps1` -> PASS, 修复两个路径问题后，完整发布链路成功执行，并完成数据库和 MinIO 同步。

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.58 -RemoteAppDir /opt/intruoyi/runtime` -> PASS, 远端正式环境切换到 `intruoyi-frontend:20260521_184319`、`intruoyi-backend:20260521_184319`，前后端状态正常。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-publish-test-server-website-path-fix --mode preview` -> PASS, 无需清理额外附属产物。
