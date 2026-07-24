# Task: Monitoring config repair

## Goal

Repair the local Infrastructure monitoring entry points so `Java 监控` and `链路追踪` no longer route to broken targets.

## Scope

- Diagnose why the local backend cannot serve Spring Boot Admin under `/admin/**`.
- Add the minimal backend change required for the monitor server dependency to be present in the running `yudao-server`.
- Configure `url.spring-boot-admin` to a real local monitor address after the backend monitor server is available.
- Determine whether a real SkyWalking UI exists locally; if not, provision one or record the exact external prerequisite.
- Verify the frontend monitoring tabs against the real running services.

## Previous Task Check

- Previous backend task: `doc/tasks/20260512-bpm-route-sweep`.
- Status before this task: completed.
- Impact: no unfinished backend task blocks this monitoring repair task.

## Milestones

- [x] M1: Task documentation created before backend changes begin.
- [x] M2: Root cause for Spring Boot Admin and SkyWalking availability recorded.
- [x] M3: RED verification captured for current local monitoring failures.
- [x] M4: Minimal backend/config/runtime changes applied.
- [x] M5: GREEN verification captured for fixed monitor routes.
- [x] M6: Task evidence finalized.
- [x] M7: Task changes committed separately after verification passes.

## Expected Verification

- Backend classpath contains the Spring Boot Admin server classes needed by `AdminServerConfiguration`.
- Spring Boot Admin login page is reachable locally and no longer falls back to the generic `401 账号未登录` JSON response inside the frontend monitor route.
- `url.spring-boot-admin` resolves to a real reachable local monitor address.
- `url.skywalking` resolves to a real reachable SkyWalking UI address.
- Frontend `Java 监控` and `链路追踪` tabs are rechecked against the running environment.

## Current Status

Completed on 2026-05-12. Spring Boot Admin is present in the `yudao-server` classpath, local `url.spring-boot-admin` and `url.skywalking` values are populated, and the Infrastructure monitor routes pass.

## Blocker and Impact

- Blocker: none.
- Impact: none.
