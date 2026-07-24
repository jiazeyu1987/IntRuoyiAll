# 执行日志：发布当前系统到测试服务器并覆盖测试数据

BDD: full test publish overwrites remote runtime -> Given 用户要求将当前本地系统连同数据一起发布到测试服务器 When 执行 `publish-int-ruoyi-to-test` 默认模式 Then 测试服务器的前后端运行镜像、MySQL `ruoyi-vue-pro` 数据库和 MinIO `yudao` 桶内容都必须被当前本地系统覆盖

BDD: test runtime stays healthy after overwrite publish -> Given 测试服务器数据和镜像会被当前本地系统覆盖 When 发布完成 Then `172.30.30.58:48081/actuator/health` 必须返回健康状态且 `172.30.30.58:8081/` 必须可访问

GREEN: cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-test-status.bat -> PASS, 发布前测试环境运行目录存在，`intruoyi-frontend`、`intruoyi-backend`、`intruoyi-mysql`、`intruoyi-redis` 均在运行，前后端 HTTP 检查为 `200`

BLOCKED: 用户切换到更高优先级的数据核对问题 -> FAIL, 需先确认当前后端数据源以及 `芋道源码` 租户下展厅产品信息是否真实缺失，因此本任务暂停在发布前预检阶段，未执行测试服务器全量覆盖发布

GREEN: cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-test-status.bat -> PASS, 恢复执行前测试环境仍为旧批次 `20260521_184319`，后端健康和管理前端均为 HTTP `200`

RED: cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat default -> FAIL, 管理前端 `pnpm exec vite build --mode test` 因 Node 默认堆内存不足失败，错误为 `JavaScript heap out of memory`

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vite build --mode test -> PASS, 同一管理前端 test 模式构建在明确 8GB Node 堆限制下完成，输出 `Build successful. Please see dist-test directory`

RED: $env:NODE_OPTIONS='--max-old-space-size=8192'; cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat default -> FAIL, 后端 Maven test-compile 被未跟踪且仍为 `In Progress` 的 `20260523-infra-runtime-control-panel` RED 测试残留污染，缺少对应 runtimecontrol 生产类

GREEN: 清理未跟踪 `20260523-infra-runtime-control-panel` 任务产物和 runtimecontrol RED 测试残留 -> PASS, `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro status --short` 重新为空

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\publish-int-ruoyi-to-test.bat default -> PASS, 发布批次 `20260523_142453` 完成；脚本确认后端健康、管理前端、Website 根路径和 `/showroom` 均返回 HTTP `200`，并完成 MySQL 与 MinIO 覆盖同步

GREEN: cmd /c D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\show-int-ruoyi-test-status.bat -> PASS, `intruoyi-frontend` 与 `intruoyi-backend` 当前镜像均为 `20260523_142453`，`intruoyi-mysql` 为 healthy，前后端 HTTP 检查为 `200`

GREEN: ssh root@172.30.30.58 "curl -fsS http://127.0.0.1:48081/actuator/health && curl ... 8081/ 8083/ 8083/showroom" -> PASS, 后端返回 `{"status":"UP"}`，管理前端、Website 根路径和 Website `/showroom` 均返回 HTTP `200`

GREEN: ssh root@172.30.30.58 "docker exec intruoyi-mysql mysql ... COUNT(*) ..." -> PASS, 发布后 `infra_file=2034`，`showroom_product=191`，`system_tenant=4`

GREEN: docker run --rm --add-host host.docker.internal:host-gateway --entrypoint /bin/sh minio/mc -c "mc alias set dst http://172.30.30.58:9000 ... && mc ls dst/yudao/showroom/product/cover/20260523/ | wc -l" -> PASS, 远端 MinIO `yudao/showroom/product/cover/20260523/` 可列出 `98` 个对象
