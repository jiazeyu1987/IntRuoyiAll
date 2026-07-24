# Execution Log

BDD: 本机后端启动 -> Given 本机存在 IntRuoyi 后端仓库和 Maven/Java, When 使用项目既有启动方式启动后端, Then 后端进程应保持运行并输出启动成功日志或可被本机探活访问。
GREEN: preflight -> PASS, 已确认 `ruoyi-vue-pro` 存在，`mvn` 可用，Java 21 可用；`mvnw.cmd` 不存在，因此不使用 Maven Wrapper。
GREEN: experience-preflight -> PASS, 本次仅启动本机后端，已读取 PowerShell 经验并确认不操作服务器/远程环境。

RED: script/deploy/restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main -> FAIL, Maven package failed during yudao-module-mes testCompile because test sources reference missing MesProRouteUse* classes.

BLOCKER: backend-start -> Maven package failed before producing yudao-server-exec.jar; root cause is testCompile errors in yudao-module-mes test sources referencing missing MesProRouteUse* classes. Impact: local backend was not started.

GREEN: jar-preflight -> PASS, yudao-server-exec.jar exists and manifest was readable before direct local startup.

GREEN: direct-jar-start -> PASS, PID=40712, port=48081, startedLog=False, health=HTTP_CHECK_FAILED: 无法连接到远程服务器, outLog=D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260709-153538.out.log, errLog=D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260709-153538.err.log.

BLOCKER: backend-verify -> port/process verification failed, portListen=False, processFound=True, probe=FAILED: 无法连接到远程服务器.

GREEN: backend-verify -> PASS, port=48081 is listening, startedLog=True, probe=HTTP_200: 123 34 115 116 97 116 117 115 34 58 34 85 80 34 125.
GREEN: task-closeout-status -> PASS, task.md Current Status updated to completed for closeout validation.

VERIFY: post-commit-interference-check -> portListen=True, health=HTTP_200: 123 34 115 116 97 116 117 115 34 58 34 85 80 34 125. Current 48081 java processes: 58668,66416,20308.

GREEN: final-backend-health -> PASS, port=48081, owner=PID=66416, CommandLine="C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe" -Xms256m -Xmx1024m -XX:MaxMetaspaceSize=512m -XX:ReservedCodeCacheSize=96m -XX:CICompilerCount=2 -XX:TieredStopAtLevel=1 -jar "E:\Int\CacheData\IntRuoyi\runtime\backend-loss-report-open-20260709-151016.jar" --server.port=48081 --spring.profiles.active=local --spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true --spring.datasource.dynamic.datasource.master.username=root --spring.datasource.dynamic.datasource.master.password=123456 --spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true --spring.datasource.dynamic.datasource.slave.username=root --spring.datasource.dynamic.datasource.slave.password=123456 --spring.data.redis.host=127.0.0.1 --spring.data.redis.port=26379 --yudao.dcc.preview.onlyoffice.base-url=http://127.0.0.1:8080 --yudao.dcc.preview.onlyoffice.public-file-base-url=http://host.docker.internal:48081 --yudao.runtime-control.repo-root=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --yudao.runtime-control.state-dir=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\runtime\runtime-control , health=HTTP_200: 123 34 115 116 97 116 117 115 34 58 34 85 80 34 125.

GREEN: final-commit-precheck -> PASS, port=48081, owner=PID=66416, CommandLine="C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe" -Xms256m -Xmx1024m -XX:MaxMetaspaceSize=512m -XX:ReservedCodeCacheSize=96m -XX:CICompilerCount=2 -XX:TieredStopAtLevel=1 -jar "E:\Int\CacheData\IntRuoyi\runtime\backend-loss-report-open-20260709-151016.jar" --server.port=48081 --spring.profiles.active=local --spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true --spring.datasource.dynamic.datasource.master.username=root --spring.datasource.dynamic.datasource.master.password=123456 --spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true --spring.datasource.dynamic.datasource.slave.username=root --spring.datasource.dynamic.datasource.slave.password=123456 --spring.data.redis.host=127.0.0.1 --spring.data.redis.port=26379 --yudao.dcc.preview.onlyoffice.base-url=http://127.0.0.1:8080 --yudao.dcc.preview.onlyoffice.public-file-base-url=http://host.docker.internal:48081 --yudao.runtime-control.repo-root=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --yudao.runtime-control.state-dir=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\runtime\runtime-control , health=HTTP_200: 123 34 115 116 97 116 117 115 34 58 34 85 80 34 125.

GREEN: backend-already-running -> PASS, port=48081, pid=66416, health=HTTP_200: 123 34 115 116 97 116 117 115 34 58 34 85 80 34 125.
