# Analyze And Start Services

## Task Goal

Analyze the frontend and backend project code enough to identify the correct local startup commands, then start the frontend and backend services separately.

## Milestones

- [x] Identify frontend and backend technology stacks, manifests, and startup entry points.
- [x] Confirm required local prerequisites and environment expectations.
- [x] Start backend service and capture evidence.
- [x] Start frontend service and capture evidence.
- [x] Record final verification evidence.

## Expected Verification

- Backend startup command is executed from the correct project directory.
- Frontend startup command is executed from the correct project directory.
- Running processes, ports, or logs confirm service startup, or blockers clearly identify the missing precondition and impact.

## Completed Work

- Frontend: identified Vue 3, Vite 5, TypeScript, and pnpm. Installed lockfile-pinned dependencies and started Vite on port `8081`.
- Backend: identified Java 17 target, Spring Boot 3.5.9, Maven multi-module structure, and the `yudao-server` executable jar.
- Fixed a duplicate pair of fields in `MesProBatchRecordParsedCell` that prevented MES compilation and caused cascading Lombok accessor errors.
- Recreated the stopped `int-ruoyi-mysql` container with the existing MySQL data volume, current workspace init SQL bind mount, and the project-required MySQL runtime options.
- Started backend on port `48081` using the local runtime MySQL/Redis ports.

## Verification Evidence

- Frontend: `GET http://127.0.0.1:8081/` returned HTTP 200.
- Backend package: `mvn -pl yudao-server -am -DskipTests package` passed and created `yudao-server-exec.jar`.
- Backend regression: `mvn -pl yudao-module-mes -Dtest=MesReleaseCompanionContractTest test` passed with 5 tests.
- Backend startup: `GET http://127.0.0.1:48081/actuator/health` returned `{"status":"UP"}`.
- Runtime dependencies: `int-ruoyi-mysql` is listening on `23306`; `int-ruoyi-redis` is listening on `26379`.
- Closeout cleanup: `task-closeout-cleanup --mode preview` and `--mode apply` completed with no blocked paths.

## Blockers

- None remaining.

## Current Status

completed
