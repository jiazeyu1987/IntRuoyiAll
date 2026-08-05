# Verification Report

## Scope

Validate local `int_main` backend runtime on `48081`.

## Results

- Port ownership: no listener on `48081`.
- Health check: failed, connection refused on `http://127.0.0.1:48081/actuator/health`.
- Startup: retry authorized by user after follow-up check showed no unmerged index entries.
- No fallback used: did not start old Jar, did not start from Maven `target` Jar, did not change port, did not change data source.

## Final Status

in_progress
