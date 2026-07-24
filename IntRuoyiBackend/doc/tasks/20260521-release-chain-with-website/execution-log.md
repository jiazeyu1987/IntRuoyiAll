# 执行日志：三条发布链同时发布 Website 并回显访问路径

BDD: test publish also deploys Website -> Given 操作员执行“发布到测试服务器” When 发布脚本完成本地构建和远端部署 Then 除了 IntRuoyi 后后端与后台前端，还必须把 `D:\ProjectPackage\Website` 的静态前端一起部署到测试服务器

BDD: prod promotion also deploys Website -> Given 操作员执行“从测试服务器发布到正式服务器” When 测试服版本提升到正式服 Then 与该测试版本配套的 Website 运行时静态资源也必须一起提升到正式服务器

BDD: direct prod publish also deploys Website -> Given 操作员执行“直接发布到正式服务器” When 当前本地工作区发布到正式服 Then IntRuoyi 与 Website 必须在同一轮发布中一起完成

BDD: publish success prints both access paths -> Given 任一发布链执行成功 When 脚本输出成功结果 Then 输出里必须同时包含 IntRuoyi 的访问路径和 Website 前端的访问路径

RED: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q -> FAIL, 当前 compose 尚未声明 `intruoyi-website` 与 `WEBSITE_HOST_PORT`，缺少 `website.nginx.conf`，测试发布链未构建/部署 `D:\ProjectPackage\Website`，测试提升正式链也未复制 Website 运行时且未回显 Website 访问路径

GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q -> PASS, 18 tests passed

GREEN: PowerShell parser check for `script\deploy\publish-int-ruoyi-to-test.ps1` and `script\deploy\promote-int-ruoyi-test-to-prod.ps1` -> PASS

GREEN: cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-direct-to-prod.bat cancel -> PASS, 直发正式包装器安全取消路径保持可用

GREEN: cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-prod.bat cancel -> PASS, 测试提升正式包装器安全取消路径保持可用

GREEN: cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\运维工具.bat help -> PASS, 统一入口帮助页仍保留三条发布链直达命令

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-release-chain-with-website --mode preview -> PASS, 仅保留 `task.md` 与 `execution-log.md`，无额外清理项或阻塞
