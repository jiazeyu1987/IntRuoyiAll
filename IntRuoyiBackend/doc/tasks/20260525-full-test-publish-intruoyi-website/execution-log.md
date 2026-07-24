# 执行日志：全量发布 IntRuoyi 与 Website 到测试服务器

BDD: 全量发布测试服 -> Given 本机后端、管理前端、Website、MySQL 和 MinIO 均可访问, When 执行默认测试服发布脚本, Then 测试服后端健康检查、管理前端首页、Website 根路径与展厅路径均返回成功。

BDD: 数据同步不降级 -> Given 用户要求同步所有展厅数据和 MySQL, When 执行发布, Then 不传入 `skip-db`、`skip-minio` 或 `skip-data` 参数，发布脚本执行数据库 dump/import 与 MinIO mirror。

INFO: 已采用 `ci-cd-environment-delivery` 工作流。
INFO: 已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md` 与 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`。
INFO: 已确认默认脚本 `publish-int-ruoyi-to-test.bat default` 会执行后端、管理前端、Website 构建发布，并同步 MySQL 与 MinIO。

BDD: 测试服登录依赖后端端口可用 -> Given 用户打开 `http://172.30.30.58:8081/login?redirect=/index`, When 登录页请求 `http://172.30.30.58:48081/admin-api/system/tenant/*`, Then 后端 `48081` 必须已监听并返回租户接口结果，不得出现 `ERR_CONNECTION_REFUSED`。

EVIDENCE: 用户反馈浏览器控制台在测试服登录页出现 `GET http://172.30.30.58:48081/admin-api/system/tenant/get-by-website?... net::ERR_CONNECTION_REFUSED` 与 `get-id-by-name?... net::ERR_CONNECTION_REFUSED`。
GREEN: `Test-NetConnection 172.30.30.58 -Port 48081` -> PASS, `TcpTestSucceeded=True`。
GREEN: `Invoke-WebRequest http://172.30.30.58:48081/actuator/health` -> PASS, HTTP 200。
GREEN: `script\deploy\show-int-ruoyi-test-status.bat` -> PASS, `intruoyi-backend` 显示 `0.0.0.0:48081->48080/tcp`，后端健康检查 HTTP 200；`intruoyi-frontend` 显示 `0.0.0.0:8081->80/tcp`，前端 HTTP 200。
GREEN: `Invoke-RestMethod http://172.30.30.58:48081/admin-api/system/tenant/get-id-by-name?name=测试租户` -> PASS, 返回租户 ID `122`。
GREEN: `Invoke-RestMethod http://172.30.30.58:48081/admin-api/system/auth/login` with `tenant-id=122`, `aoteman/admin123` -> PASS, 返回 access token。
INFO: 远端 `docker inspect intruoyi-backend` 显示后端容器启动时间约为 2026-05-25 12:40:43 +08:00；用户看到的 `ERR_CONNECTION_REFUSED` 与发布/重启窗口内后端尚未监听相符。
INFO: 前端当前产物 `http://172.30.30.58:8081/assets/index-DXfBrHsq.js` 包含后端目标 `172.30.30.58:48081`，未发现指向正式服或本机回环地址的构建错误。
NOTE: `get-by-website?website=172.30.30.58:8081` 当前返回 `data:null`，不会造成连接拒绝，但表示该域名未绑定自动租户；测试登录仍应按 `docs/login-access.md` 使用租户名 `测试租户`。

BDD: 手动发布展厅不得因测试服默认 JVM 堆过小失败 -> Given 测试服展厅发布需要读取并物化图片/音频资产, When 发布脚本写入远端 compose `.env`, Then 后端 JVM 堆上限必须满足测试服展厅发布负载，不能保持 `-Xmx512m`。
EVIDENCE: 用户反馈手动发布展厅报错 `Handler dispatch failed: java.lang.OutOfMemoryError: Java heap space`。
EVIDENCE: 测试服磁盘检查 `df -h` -> Docker 数据盘 `/var/lib/docker` 可用约 `1.8T`，系统根分区可用约 `45G`，NAS 可用约 `23T`；该故障不是磁盘空间不足。
EVIDENCE: 测试服内存检查 `free -h` -> 物理内存 31Gi，总 available 约 14-15Gi；主机内存不是耗尽状态。
EVIDENCE: 测试服后端 `JAVA_OPTS=-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom`；OOM 栈位于 `S3FileClient.getContent -> FileServiceImpl.getFileContent -> ShowroomReleaseSourceFileReader.readFileById -> ShowroomReleaseAssembler.resolveNarrationPair -> ShowroomReleasePublisherService.publishRelease`。
EVIDENCE: 测试库 `infra_file` 最大文件约 `44MB`，讲解音频最大约 `11.8MB`；风险来自发布过程一次性物化多份资产到 JVM 堆，而不是单个超大文件或磁盘不足。
REMEDIATION: 已将测试服 `/opt/intruoyi/runtime/.env` 的 `JAVA_OPTS` 调整为 `-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom` 并重建 `intruoyi-backend`。
GREEN: `docker exec intruoyi-backend printenv JAVA_OPTS` -> PASS, 输出 `-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom`。
GREEN: `script\deploy\show-int-ruoyi-test-status.bat` -> PASS, 后端健康检查 HTTP 200。
RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, 1 failed；预期原因：发布脚本仍写入 `JAVA_OPTS=-Xms512m -Xmx512m`，下次发布会覆盖测试服运行时修复。
GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 22 passed；发布脚本默认写入 `JAVA_OPTS=-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom`，并禁止回退到 `-Xmx512m`。
GREEN: `script\deploy\show-int-ruoyi-test-status.bat` -> PASS, `intruoyi-backend` Up，Backend health HTTP 200，Frontend status HTTP 200。
GREEN: `docker exec intruoyi-backend printenv JAVA_OPTS` -> PASS, 输出 `-Xms1g -Xmx2g -Djava.security.egd=file:/dev/./urandom`；`docker stats` 显示后端约 `2.08GiB / 31.25GiB`。

BDD: 管理前端相对文件资源必须代理到后端 -> Given 产品管理和公司信息接口返回 `/admin-api/infra/file/...` 相对资源路径, When 浏览器在 `http://172.30.30.58:8081` 页面加载图片或音频, Then 管理前端 Nginx 必须把 `/admin-api/infra/file/` 代理到后端，而不是返回 SPA `index.html`。
EVIDENCE: `http://172.30.30.58:48081/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` -> PASS, HTTP 200, `image/png`。
EVIDENCE: `http://172.30.30.58:48081/admin-api/infra/file/28/get/showroom/narration/20260525/product-1-en-ruoxi.wav` -> PASS, HTTP 200, audio content。
EVIDENCE: `http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` -> FAIL, HTTP 200 but `Content-Type=text/html` and body is frontend `index.html`；浏览器会把 HTML 当图片/音频资源加载，因此显示失败。
RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, 1 failed；预期原因：管理前端 `script/deploy/int-ruoyi-test/nginx.conf` 缺少 `/admin-api/infra/file/` 代理配置。
GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 23 passed；管理前端 Nginx 模板已覆盖 `/admin-api/infra/file/` 代理到 `backend:48080`。
REMEDIATION: 已将修复后的 `script/deploy/int-ruoyi-test/nginx.conf` 复制到测试服 `intruoyi-frontend:/etc/nginx/conf.d/default.conf`，执行 `nginx -t` 通过并 reload。
GREEN: `http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png` -> PASS, HTTP 200, `image/png`。
GREEN: `http://172.30.30.58:8081/admin-api/infra/file/28/get/showroom/narration/20260525/product-1-en-ruoxi.wav` -> PASS, HTTP 200, audio content。
GREEN: `script\deploy\show-int-ruoyi-test-status.bat` -> PASS, Backend health HTTP 200, Frontend status HTTP 200。

BDD: Website release API 必须代理到后端 -> Given Website 运行在 `http://172.30.30.58:8083`, When 浏览器请求 `/showroom/release/*` 或 `/showroom/assets/*`, Then Website Nginx 必须代理到后端 release/runtime API，不能返回 SPA `index.html` 或隐藏真实 4xx。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 24 passed；测试覆盖 Website Nginx `/showroom/release/` 与 `/showroom/assets/` 代理、测试服发布脚本前端构建调用、后端 JVM 堆配置、默认发布脚本与 compose/runtime 约束。

RED: `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default` -> FAIL, expected reason: 管理前端 Vite 构建通过 `Start-Process` 捕获 `pnpm` 输出时以 exit code `-1` 失败；单独在同样 `NODE_OPTIONS=--max-old-space-size=8192` 与 `VITE_*` 环境下直接运行 `pnpm exec vite build --mode test` 可成功。

RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected reason: 新增 `test_publish_script_streams_local_frontend_build` 要求测试服发布脚本直接执行本地 Vite 构建，当前仍通过 `Invoke-CheckedCommand` 捕获 `pnpm`。

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 24 passed；`publish-int-ruoyi-to-test.ps1` 在保留 `NODE_OPTIONS=--max-old-space-size=8192`、`VITE_BASE_URL=http://172.30.30.58:48081`、`VITE_BASE_PATH=/`、`VITE_OUT_DIR=dist-intruoyi-test` 的前提下，改用 `Invoke-CheckedShell -Command 'pnpm exec vite build --mode test'`。

GREEN: `D:\ProjectPackage\Int\IntRuoyi\publish-int-ruoyi-to-test.bat default` -> PASS, tag `20260525_135729`；默认发布完整执行后端 jar 构建、管理前端构建、Website 构建、Docker 镜像构建/上传、MySQL 全量同步、MinIO mirror、后端/前端/Website 启动与 HTTP readiness。

GREEN: 发布后 URL 检查 -> `http://172.30.30.58:48081/actuator/health` HTTP 200；`http://172.30.30.58:8081/` HTTP 200；`http://172.30.30.58:8083/` HTTP 200；`http://172.30.30.58:8083/showroom` HTTP 200；`http://172.30.30.58:8083/showroom/release/current` HTTP 200。

GREEN: 展厅 release 验证 -> Playwright 测试租户真实前端路径点击“手动发布展厅”，current release `20260525T061337Z-e03a7b68bf1a`；Website manifest `506` 个资产、`166` 个文档，全部资产 HEAD 检查 failures `[]`；原 `410` 资产返回 HTTP `200`。

GREEN: `script\deploy\show-int-ruoyi-test-status.bat` -> PASS, `intruoyi-backend`/`intruoyi-frontend` tag `20260525_135729`，`intruoyi-website` Up，Backend health HTTP 200，Frontend status HTTP 200。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc/tasks/20260525-full-test-publish-intruoyi-website/ci-cd-evidence.md` -> PASS, CI/CD environment evidence is valid.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-full-test-publish-intruoyi-website --mode apply` -> PASS, deleted temporary scripts `verify-test-publish-live.mjs` and `verify-website-nginx-release-proxy.mjs`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-full-test-publish-intruoyi-website --mode preview` -> PASS, keep `task.md`、`execution-log.md`、`ci-cd-evidence.md`，delete `<none>`，blocked `<none>`，warnings `<none>`。
