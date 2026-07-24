# Execution Log: BPM schema repair

BDD: BPM model page has required schema -> Given `yudao-module-bpm` is enabled and an authenticated admin opens the BPM model page, When `/admin-api/bpm/model/list` and `/admin-api/bpm/category/simple-list` query MySQL, Then the required `bpm_*` tables must exist and the UI must not receive the schema-not-imported response.

BDD: BPM task pages have required schema -> Given BPM management pages are enabled and an authenticated admin opens BPM task pages, When `/admin-api/bpm/task/manager-page` and related BPM APIs query MySQL, Then the required `bpm_*` tables must exist and the APIs must return business responses instead of missing-table failures.

- M1: Completed. Checked the previous unfinished-task requirement before starting this repair; the earlier MES schema task is already marked completed.
- M2: Completed. BPM schema repair task package created before schema or script changes.
- RED: `node doc/tasks/20260512-bpm-schema-repair/scripts/validate-bpm-schema.cjs` -> FAIL, missing `sql/mysql/20260512_bpm_base_schema.sql`.
- RED: `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e "SHOW TABLES LIKE 'bpm_%';"` -> FAIL, live MySQL on `127.0.0.1:23306` returned no `bpm_*` tables.
- RED: Real Playwright navigation to `/bpm/manager/model` triggered `GET /admin-api/bpm/category/simple-list` -> FAIL, response body `{"code":501,"msg":"[工作流模块 yudao-module-bpm - 表结构未导入][参考 https://cloud.iocoder.cn/bpm/ 开启]","data":null}`.
- GREEN: `node doc/tasks/20260512-bpm-schema-repair/scripts/generate-bpm-base-schema.cjs` -> PASS, generated `sql/mysql/20260512_bpm_base_schema.sql` with 8 BPM tables.
- GREEN: `node doc/tasks/20260512-bpm-schema-repair/scripts/validate-bpm-schema.cjs` -> PASS, schema file covers all BPM DO tables and required base columns.
- GREEN: `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" doc\tasks\20260512-bpm-schema-repair\scripts\MysqlSchemaRunner.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260512_bpm_base_schema.sql` -> PASS, applied and verified 8 BPM tables in the live database.
- GREEN: `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -e "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ruoyi-vue-pro' AND table_name LIKE 'bpm_%' ORDER BY table_name;"` -> PASS, all 8 expected `bpm_*` tables exist.
- GREEN: Real Playwright navigation to `/bpm/manager/model` -> PASS, `/admin-api/bpm/category/simple-list` returned `{"code":0,"msg":"","data":[]}`.
- GREEN: Real Playwright navigation to `/bpm/manager/process-tasnk` -> PASS, `/admin-api/bpm/task/manager-page?pageNo=1&pageSize=10&name=` returned `{"code":0,"msg":"","data":{"total":0,"list":[]}}`.
