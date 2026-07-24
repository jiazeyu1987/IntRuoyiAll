# 执行日志：运行控制台真实带数据提升正式服验证

BDD: 带数据提升正式服成功 -> Given 运维人员在本机运行控制台打开“提升正式服”弹窗, When 选择“带数据发布”、填写原因并输入 `PROD` 后确认执行, Then 系统应提交 `promote-prod` 动作，参数包含 `publishScope=with-data`，发布脚本应执行数据库同步和 MinIO 同步，并最终成功。

BDD: 带数据发布后 Website 正常打开 -> Given 带数据提升正式服完成, When 访问正式服 Website 根路径和 `/showroom`, Then 页面应返回成功响应且浏览器可以加载展厅页面。

BDD: 操作日志可追溯 -> Given 带数据提升正式服动作完成, When 查看运行控制台操作日志, Then 最近操作应显示“提升正式服”“带数据发布”和成功状态，日志包含数据同步证据。

RED: `Select-String tests\e2e\runtime-control-promote-prod-real-flow.e2e.js -Pattern "RUNTIME_CONTROL_REAL_PROMOTE_SCOPE|with-data|带数据发布"` -> FAIL, existing real promote E2E only supports default `code-only`.

GREEN: preflight health -> PASS, local runtime-control frontend/backend, test backend/frontend/Website, and production backend/frontend/Website all returned HTTP 200.

GREEN: preflight data stores -> PASS, test and production MinIO containers were healthy; test and production MySQL responded to `mysqladmin ping`.

GREEN: preflight table count -> PASS, test database had 421 tables and production database had 420 tables before with-data promotion.

RED: real with-data promote operation `4af0097f-399b-4538-8e8c-129730c98a1c` -> FAIL, scp failed while writing `/opt/intruoyi/releases/20260525_221321/intruoyi-images_20260525_221321.tar` because production root filesystem was 100% full.

RED: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected promote script to use `/var/lib/docker/intruoyi-releases` for remote release bundles instead of `/opt/intruoyi/releases`.

GREEN: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS after adding `SourceReleaseRoot` and `TargetReleaseRoot` defaults of `/var/lib/docker/intruoyi-releases`.

GREEN: production root cleanup -> PASS, moved old `/opt/intruoyi/releases/20260518_*` intermediate bundles to `/var/lib/docker/intruoyi-releases-legacy`, freeing production root from 20K to about 1.4G available without deleting the old bundles.

RED: real with-data promote operation `e0f4242a-1f83-46a3-9c93-815d627a006e` -> FAIL, MySQL import and MinIO mirror completed, but Website `/showroom` returned 500 because the running Website container still referenced the deleted bind-mounted dist directory.

RED: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected promote script to force-recreate the Website container after replacing `/opt/intruoyi/runtime/website/dist`.

GREEN: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS after changing promote script to run `docker compose up -d --force-recreate website`.

GREEN: emergency Website recovery on production -> PASS, `docker compose up -d --force-recreate website` restored `http://127.0.0.1:8083/showroom` to HTTP 200.

GREEN: `RUNTIME_CONTROL_REAL_PROMOTE_SCOPE=with-data RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD=1 RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_WITH_DATA=1 node tests\e2e\runtime-control-promote-prod-real-flow.e2e.js` -> PASS, operation `5806c1d8-ebd9-405e-85cf-f37b322397c2` succeeded.

GREEN: operation audit `5806c1d8-ebd9-405e-85cf-f37b322397c2` -> PASS, `action=promote-prod`, `parameters.publishScope=with-data`, status `succeeded`.

GREEN: promote log `5806c1d8-ebd9-405e-85cf-f37b322397c2.log` -> PASS, includes `Dumping tested MySQL database`, `Mirroring MinIO bucket yudao`, `mc mirror --overwrite`, `Importing tested database`, `docker compose up -d --force-recreate website`, and `Promotion completed.`

GREEN: production health after with-data promotion -> PASS, `http://172.30.30.57:48081/actuator/health`, `http://172.30.30.57:8081/`, `http://172.30.30.57:8083/`, and `http://172.30.30.57:8083/showroom` returned HTTP 200.

GREEN: production Website browser verification -> PASS, Playwright opened `http://172.30.30.57:8083/` and `http://172.30.30.57:8083/showroom`; both rendered non-empty content with no page errors or critical request failures.

GREEN: production login closure -> PASS, login requests were sent to `http://172.30.30.57:48081/admin-api/system/auth/login` and did not call the test backend.

GREEN: local promotion temp cleanup -> PASS, removed `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\tmp\promote-int-ruoyi-test-to-prod` after verifying it resolved inside the expected workspace temp directory; reclaimed 25,769,452,281 bytes.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-real-promote-prod-with-data --mode preview` -> PASS, kept only `task.md` and `execution-log.md`; no delete, blocked, or warning entries.
