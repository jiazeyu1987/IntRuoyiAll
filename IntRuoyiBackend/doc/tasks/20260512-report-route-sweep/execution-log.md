# Execution Log: Report Management route sweep

BDD: Report backend module is wired for enabled report menu -> Given the Report Management menu exposes report designer, dashboard designer, and screen designer child routes, When the backend starts with those menus enabled, Then the report module must be included in the Maven reactor and server dependency graph so the iframe targets and GoView APIs do not fail with missing routes.

BDD: Report Management real routes load cleanly -> Given an authenticated admin opens the Report Management menu, When each visible child route is opened through the real frontend, Then the page should render without unhandled frontend errors and its initial network requests should not return disabled-module, missing-route, schema-not-imported, or system-exception responses.

## Evidence

- M1: Completed. Previous backend task `20260512-bpm-route-sweep` was found incomplete and marked blocked with impact before starting this task.
- M2: Completed. This backend task document and execution log were created before backend route/module inventory and browser automation.

RED: `mvn -pl yudao-server -am -Dtest=ReportModuleEnablementTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `root pom.xml` did not include `yudao-module-report` and `yudao-server/pom.xml` did not depend on `cn.iocoder.boot:yudao-module-report`.

GREEN: `mvn -pl yudao-server -am -Dtest=ReportModuleEnablementTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, after enabling the report module in the reactor and adding the server dependency.

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, after stopping the old locked backend process and rebuilding `yudao-server/target/yudao-server.jar` with the report module included.

GREEN: `.\restart-ruoyi.bat` -> PASS, restarted the local runtime on frontend `http://localhost:8081` and backend `http://localhost:48081` using the rebuilt report-enabled jar.

GREEN: `docker exec int-ruoyi-mysql mysql -uroot -p123456 -D ruoyi-vue-pro -e "SHOW TABLES LIKE 'jimu_report_category'; SHOW TABLES LIKE 'jimu_dict'; SHOW TABLES LIKE 'onl_drag_page'; SHOW TABLES LIKE 'onl_drag_dataset_head'; SHOW TABLES LIKE 'report_go_view_project';"` -> PASS, after importing `output/JimuReport-upstream/db/jimureport.mysql5.7.create.sql` and creating the MySQL `report_go_view_project` table required by the custom GoView project endpoints.

GREEN: `pnpm install` in `output/yudao-ui-go-view` and background `pnpm dev` on port `3000` -> PASS, the GoView frontend runtime now accepts the iframe access/refresh token login flow used by `report/goview/index`.

GREEN: final real-route sweep dependency support -> PASS, backend logs show successful responses for `/jmreport/list`, `/jmreport/query/report/folder`, `/drag/list`, `/drag/category/list`, `/drag/page/list`, and GoView project-page requests after the local schema and runtime prerequisites were fixed.
