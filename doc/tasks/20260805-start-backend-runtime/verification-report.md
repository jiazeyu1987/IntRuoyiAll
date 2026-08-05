# Verification Report

## Scope

Validate local `int_main` backend runtime on `48081`.

## Results

- Port ownership: no listener on `48081`.
- Health check: failed, connection refused on `http://127.0.0.1:48081/actuator/health`.
- Startup: retry authorized by user after follow-up check showed no unmerged index entries.
- No fallback used: did not start old Jar, did not start from Maven `target` Jar, did not change port, did not change data source.
- Build/start: standard backend restart script generated independent runtime Jar `output/runtime/int_main/backend-runtime-control-20260805-222248.jar`.
- Process: Java PID `60192` listening on `48081`.
- Health: initial and delayed checks returned `{"status":"UP"}`.
- Log evidence: `Tomcat started on port 48081`, `Started YudaoServerApplication`, `项目启动成功`.

## Final Status

completed
