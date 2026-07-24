BDD: 本机读回 origin 归属 local profile -> Given 使用本机 `local` profile 启动后端 / When 执行展厅发布读回校验 / Then `showroom.release.public-website-origin` 默认解析为 `http://127.0.0.1:${server.port}`，不依赖 `restart-ruoyi.bat` 硬编码。

BDD: 服务器读回 origin 由部署环境决定 -> Given 测试或正式服务器使用自身发布脚本和 compose 环境 / When 后端启动 / Then `showroom.release.public-website-origin` 使用对应服务器公开站点入口，不继承本机脚本值。

RED: `python -m pytest script/tests/test_restart_ruoyi_script.py -q` -> FAIL，expected reason: `restart-ruoyi.bat` 仍包含 `--showroom.release.public-website-origin`，且 `application-local.yaml` 尚未声明本机默认读回 origin。

RED: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> FAIL，expected reason: `script/deploy/restart-int-ruoyi-local.ps1` 仍包含 `$ShowroomPublicReleaseOrigin` 本机硬编码和 `--showroom.release.public-website-origin` 启动参数。

GREEN: `python -m pytest script/tests/test_restart_ruoyi_script.py script/tests/test_runtime_control_scripts.py -q` -> PASS，12 个脚本回归测试通过。

RED: `git commit -m "任务: 按环境配置展厅发布读回origin"` -> FAIL，expected reason: 提交钩子要求后端运行配置变更必须包含 `src/test` 下的 Java 测试。

GREEN: `mvn -pl yudao-server "-Dtest=RuntimeControlLocalConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 个 Java 配置回归测试通过。

GREEN: `rg -n "showroom.release.public-website-origin|ShowroomPublicReleaseOrigin" restart-ruoyi.bat` -> PASS，无匹配，根脚本不再携带该业务配置。

GREEN: `rg -n "showroom.release.public-website-origin|ShowroomPublicReleaseOrigin" script/deploy/restart-int-ruoyi-local.ps1 script/deploy/int-ruoyi-test/docker-compose.yml yudao-server/src/main/resources/application-local.yaml` -> PASS，本机运行脚本无硬编码；服务器 compose 保留按 `SERVER_HOST` / `WEBSITE_HOST_PORT` 生成的环境化配置。

GREEN: `task_closeout.py --task-id 20260601-showroom-release-origin-env-config --mode preview` -> PASS，无待删除项、无阻塞、无警告。
