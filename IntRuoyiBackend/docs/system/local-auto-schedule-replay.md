# Local Replay: Auto Schedule Demo

## Purpose

This runbook reproduces the local MES auto-schedule demo flow in an IntRuoyi environment with:

- schema patch
- repeatable demo MES data
- local backend/frontend startup
- browser verification
- database verification

The demo data uses the `900xxx` id range under tenant `1`.

## Prerequisites

- Local MySQL reachable at `127.0.0.1:23306`, or Docker container `int-ruoyi-mysql`
- Local Redis reachable at `127.0.0.1:26379`, or Docker container `int-ruoyi-redis`
- Backend branch checked out at `D:\wt\intsched-be`
- Frontend branch checked out at `D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3`

## Database Setup

Helper script, minimal scenario:

```powershell
powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Scenario Minimal -Action Replay
```

Helper script, complete scenario:

```powershell
powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Scenario Complete -Action Replay
```

The helper supports `ApplySchema`, `Clean`, `Seed`, `Verify`, `Replay`, `ExerciseApiFlow`, `ReplayAndExercise`, and `SyncSimulationDate`, and accepts `-Scenario Minimal` or `-Scenario Complete`.
When you need to normalize the simulation row to a specific day in the active temporary database, add `-SimulationDate yyyy-MM-dd`.

Apply schema patch directly:

```powershell
Get-Content D:\wt\intsched-be\sql\mysql\mes-auto-schedule-first-loop.sql -Raw |
  docker exec -i int-ruoyi-mysql mysql --force -uroot -p123456 ruoyi-vue-pro
```

Minimal scenario seed:

```powershell
Get-Content D:\wt\intsched-be\sql\mysql\mes-auto-schedule-first-loop-demo-clean.sql -Raw |
  docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro

Get-Content D:\wt\intsched-be\sql\mysql\mes-auto-schedule-first-loop-demo-data.sql -Raw |
  docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro
```

Complete scenario seed:

```powershell
Get-Content D:\wt\intsched-be\sql\mysql\mes-auto-schedule-complete-demo-clean.sql -Raw |
  docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro

Get-Content D:\wt\intsched-be\sql\mysql\mes-auto-schedule-complete-demo-data.sql -Raw |
  docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro
```

Standalone simulation-date sync:

```powershell
powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Action SyncSimulationDate -SimulationDate 2026-05-17
```

One-command replay and API exercise:

```powershell
powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Scenario Minimal -Action ReplayAndExercise

powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Scenario Complete -Action ReplayAndExercise

powershell -ExecutionPolicy Bypass -File D:\wt\intsched-be\script\shell\mes-auto-schedule-first-loop-demo.ps1 -Scenario Complete -SimulationDate 2026-05-17 -Action ReplayAndExercise
```

Expected minimal seed:

- work order `AUTO-WO-001`
- one route, one process, one line, one workstation
- one capacity row
- one task auto-code rule
- no formal `mes_pro_task` row before publish

Expected complete seed:

- work orders `AUTO-WO-001` and `AUTO-WO-002`
- one route with two ordered processes
- two production lines and two workstations
- current-day plus next-day capacity windows
- cross-line dependency links after publish
- no formal `mes_pro_task` rows before publish

## Start Services

Package backend:

```powershell
mvn -pl yudao-server -am "-Dmaven.test.skip=true" package
```

Start backend on `48081` using a runtime copy, not the mutable target jar:

```powershell
$java = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\java.exe'
$builtJar = 'D:\wt\intsched-be\yudao-server\target\yudao-server.jar'
$runtimeJar = 'D:\wt\intsched-be\output\runtime\backend-auto-schedule-replay.jar'

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $runtimeJar) | Out-Null
Copy-Item -LiteralPath $builtJar -Destination $runtimeJar -Force

Start-Process -FilePath $java -ArgumentList @(
  '-jar', $runtimeJar,
  '--server.port=48081',
  '--spring.profiles.active=local',
  '--spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true',
  '--spring.datasource.dynamic.datasource.master.username=root',
  '--spring.datasource.dynamic.datasource.master.password=123456',
  '--spring.datasource.dynamic.datasource.slave.url=jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true&nullCatalogMeansCurrent=true',
  '--spring.datasource.dynamic.datasource.slave.username=root',
  '--spring.datasource.dynamic.datasource.slave.password=123456',
  '--spring.data.redis.host=127.0.0.1',
  '--spring.data.redis.port=26379'
) -WindowStyle Hidden
```

Start frontend on `3100`:

```powershell
cmd /c mklink /J D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3\node_modules D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules

Start-Process -FilePath 'C:\Users\BJB110\AppData\Roaming\npm\pnpm.cmd' -ArgumentList @(
  'exec', 'vite', '--mode', 'env.local', '--host', '0.0.0.0', '--port', '3100'
) -WorkingDirectory 'D:\ProjectPackage\Int\IntRuoyi-worktrees\auto-schedule-first-loop\yudao-ui-admin-vue3' -WindowStyle Hidden
```

## Browser Verification

1. Open [http://localhost:3100/](http://localhost:3100/)
2. Login with:
   - tenant: `芋道源码`
   - username: `admin`
   - password: `admin123`
3. Open `/mes/pro/task`
4. Confirm the table contains work order `AUTO-WO-001`
5. When using `-Scenario Complete`, also confirm work order `AUTO-WO-002`
6. Click `自动排产`
7. Click `生成预览`
8. For the minimal scenario, confirm:
   - work order count = `1`
   - generated task count = `1`
   - blocking issue count = `0`
   - preview Gantt contains one generated production task
9. For the complete scenario, confirm:
   - work order count = `2`
   - generated task count = `4`
   - blocking issue count = `0`
   - preview Gantt contains cross-line dependency links
10. Click `确认发布`
11. Confirm the publish dialog and finish publish

## Database Verification

Minimal scenario:

```powershell
docker exec int-ruoyi-mysql mysql -t -uroot -p123456 ruoyi-vue-pro -e "
SELECT id, code, work_order_id, workstation_id, route_id, process_id, item_id, quantity, start_time, end_time, duration, status, remark
FROM mes_pro_task
WHERE work_order_id = 900080
ORDER BY id;
"
```

Expected minimal result:

- one `mes_pro_task` row
- `schedule_source = AUTO`
- `quantity_scheduled = 1.00`

Complete scenario:

```powershell
docker exec int-ruoyi-mysql mysql -t -uroot -p123456 ruoyi-vue-pro -e "
SELECT id, code, work_order_id, workstation_id, route_id, process_id, item_id, quantity, start_time, end_time, duration, status, remark
FROM mes_pro_task
WHERE work_order_id IN (900080, 900082)
ORDER BY id;

SELECT id, source_task_id, target_task_id, source_process_id, target_process_id, dependency_type
FROM mes_pro_task_dependency
WHERE source_task_id IN (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082))
   OR target_task_id IN (SELECT id FROM mes_pro_task WHERE work_order_id IN (900080, 900082))
ORDER BY id;
"
```

Expected complete result:

- four `mes_pro_task` rows
- two dependency rows
- cross-day scheduling on the second line
- `quantity_scheduled = 1.00` for `900080`
- `quantity_scheduled = 2.00` for `900082`

## Cleanup

Minimal scenario:

```powershell
Get-Content D:\wt\intsched-be\sql\mysql\mes-auto-schedule-first-loop-demo-clean.sql -Raw |
  docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro
```

Complete scenario:

```powershell
Get-Content D:\wt\intsched-be\sql\mysql\mes-auto-schedule-complete-demo-clean.sql -Raw |
  docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro
```
