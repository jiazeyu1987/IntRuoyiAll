# Execution Log: Monitoring config repair

BDD: Local monitor pages open real monitor targets -> Given the local backend and frontend are running, When the Infrastructure monitor tabs open their configured targets, Then Java 监控 should load Spring Boot Admin and 链路追踪 should load a real SkyWalking UI instead of broken or placeholder targets.

- M1: Completed. Created backend task documentation before any production code or runtime changes.
- M2: Root cause confirmed. `/actuator/conditions` reported `AdminServerConfiguration` as a negative match because `de.codecentric.boot.admin.server.config.AdminServerProperties` was missing from the `yudao-server` classpath. `url.spring-boot-admin` and `url.skywalking` were both blank.
- M3 RED: `mvn -pl yudao-server -am -Dtest=MonitoringModuleEnablementTest "-Dsurefire.failIfNoSpecifiedTests=false" test` failed with:
- `ClassNotFoundException: de.codecentric.boot.admin.server.config.AdminServerProperties`
- `yudao-server/pom.xml must depend on de.codecentric:spring-boot-admin-starter-server`
- M4 GREEN: Added a direct `de.codecentric:spring-boot-admin-starter-server` dependency to `yudao-server/pom.xml`.
- M4 GREEN: `mvn -pl yudao-server -am -Dtest=MonitoringModuleEnablementTest "-Dsurefire.failIfNoSpecifiedTests=false" test` passed.
- M4 GREEN: Rebuilt and restarted `yudao-server.jar` with the local runtime arguments on port `48081`.
- M4 GREEN: Updated `application-local.yaml` frame-ancestor allowlist to include `localhost:8081` and `127.0.0.1:8081`.
- M4 GREEN: Updated `infra_config` values:
- `url.spring-boot-admin = http://127.0.0.1:48081/admin/login`
- `url.skywalking = http://127.0.0.1:18081`
- M4 GREEN: Pulled and started local official SkyWalking Docker services:
- `apache/skywalking-oap-server:latest`
- `apache/skywalking-ui:10.1.0`
- M4 GREEN: Connected SkyWalking OAP to the existing local Elasticsearch service with credentials from the local Docker environment.
- M5 GREEN: `node doc/tasks/20260512-infra-route-audit/scripts/run-audit-infra-routes.mjs` returned 21 pass / 0 blocked after the backend and runtime changes.
