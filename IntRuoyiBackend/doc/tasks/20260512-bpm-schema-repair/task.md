# Task: BPM schema repair

## Goal

Fix `[工作流模块 yudao-module-bpm - 表结构未导入]` by providing and importing the missing MySQL `bpm_*` base tables required by `yudao-module-bpm`.

## Scope

- Reproduce the missing BPM schema from a real frontend workflow path.
- Add a repeatable MySQL BPM base schema script under `sql/mysql/`.
- Validate the schema against BPM DO `@TableName` declarations and required base columns.
- Apply the schema to the live local MySQL used by the current backend process.
- Verify BPM admin UI and APIs return business responses instead of the schema-not-imported message.
- Do not add fallback behavior, mock data, or destructive SQL.

## Milestones

- [x] M1: Previous unfinished task state checked.
- [x] M2: Task documentation created before schema changes.
- [x] M3: RED verification records missing BPM schema.
- [x] M4: BPM base schema script and validation tooling added.
- [x] M5: Local database schema imported and verified.
- [x] M6: Current-task changes committed after verification passes.

## Expected Verification

- `node doc/tasks/20260512-bpm-schema-repair/scripts/validate-bpm-schema.cjs`
- `node doc/tasks/20260512-bpm-schema-repair/scripts/generate-bpm-base-schema.cjs`
- `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -N -e "SHOW TABLES LIKE 'bpm_%';"`
- `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" doc\tasks\20260512-bpm-schema-repair\scripts\MysqlSchemaRunner.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260512_bpm_base_schema.sql`
- `npx --yes --package @playwright/cli playwright-cli -s=bpmgreen requests`

## Current Status

Completed. The BPM base schema script has been generated, imported into the live MySQL database, and verified through real Playwright BPM pages. This task package is ready for its isolated task commit.

## Runtime Evidence

- Current backend process PID `52500` runs with datasource overrides targeting `jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro`.
- Real Playwright navigation to `http://localhost:8081/bpm/manager/model` initially returned `{"code":501,"msg":"[工作流模块 yudao-module-bpm - 表结构未导入][参考 https://cloud.iocoder.cn/bpm/ 开启]","data":null}` from `/admin-api/bpm/category/simple-list`.
- After schema import, the same BPM model page returned `{"code":0,"msg":"","data":[]}` from `/admin-api/bpm/category/simple-list`, and the BPM task page returned `{"code":0,"msg":"","data":{"total":0,"list":[]}}` from `/admin-api/bpm/task/manager-page`.

## Blocker And Impact

- Blocker: none.
- Impact: BPM schema import is fixed in the live local database and BPM pages no longer return the schema-not-imported response on the verified paths.
- Commit status: this task must be committed without mixing unrelated workspace changes.

## Data Safety

The repair must be additive only: create missing BPM tables if they do not exist. It must not drop, truncate, overwrite, or mock existing data.

## Final Verification Result

- `node doc/tasks/20260512-bpm-schema-repair/scripts/generate-bpm-base-schema.cjs` -> PASS.
- `node doc/tasks/20260512-bpm-schema-repair/scripts/validate-bpm-schema.cjs` -> PASS.
- `java -cp "C:\Users\BJB110\.m2\repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" doc\tasks\20260512-bpm-schema-repair\scripts\MysqlSchemaRunner.java "jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true" root 123456 sql\mysql\20260512_bpm_base_schema.sql` -> PASS.
- `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -e "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ruoyi-vue-pro' AND table_name LIKE 'bpm_%' ORDER BY table_name;"` -> PASS.
- Playwright `/bpm/manager/model` and `/bpm/manager/process-tasnk` -> PASS, verified runtime BPM responses return `code=0`.
