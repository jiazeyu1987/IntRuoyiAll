# Verification Report

## Result

- Status: PASS for requested backend startup.
- Backend PID: `27116`.
- Port: `48081`.
- Health: `{"status":"UP"}` from `http://127.0.0.1:48081/actuator/health`.

## Evidence

- Port listener: `Get-NetTCPConnection -LocalPort 48081 -State Listen` returned `OwningProcess=27116`.
- Runtime Jar: `E:\IntRuoyi\output\runtime\int_main\backend-runtime-process-config-list-autowired-20260806-183405.jar`.
- Runtime command: `java -jar ... --server.port=48081 --spring.profiles.active=local --logging.file.name=E:\IntRuoyi\output\runtime\int_main\logs\yudao-server.log --yudao.runtime-control.storage-guard.log-dir=E:\IntRuoyi\output\runtime\int_main\logs --yudao.codex-test.runner.token=`.
- Runtime immutability: runtime Jar last write time `2026-08-06 18:34:31` is before process start `2026-08-06 19:53:40`.

## Notes

- Initial `target` Jar startup failed because the packaged `MesTeamLeaderProcessConfigServiceImpl` lacked the current explicit `@Autowired` constructor.
- Rebuilding `target` was stopped when concurrent Maven PID `44732` was found writing the same `yudao-module-mes` target tree.
- No port change, data source switch, mock service, or silent fallback was used.
- Cleanup completed with no blocked paths; only task-owned temporary startup scripts were removed.
