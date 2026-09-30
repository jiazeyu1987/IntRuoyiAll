# Verification Report

## Status

verified; Git closeout intentionally not performed per user instruction.

## Scope

DCC 文控日志/历史查询的 19 位 `controlledFileId` 精度保持和 URL 筛选回放；不包含数据库写入、业务数据写入或 Git 操作。

## Evidence

## Root Cause

`logs/index.vue` converted `route.query.controlledFileId` with `Number()`. The JavaScript safe integer limit rounded `2054545668044084022` to `2054545668044084000` before Axios serialized the request. `keyword` was also absent from the initial query model, so a deep-linked keyword filter was not replayed.

## Implementation

- `DccControlledFileLogPageReqVO.controlledFileId` is now `String`.
- The DCC logs API type uses `controlledFileId?: string`.
- The logs page reads both `keyword` and `controlledFileId` with `getFirstQueryValue`, preserving the URL text exactly.
- The query service parses the string once at its boundary to `Long` for existing database-domain comparisons and throws an explicit `IllegalArgumentException` for invalid values.

## Verification Evidence

- Frontend logs static contract: PASS.
- DCC lifecycle timeline static contract: PASS.
- BPM approval timeline current-node static contract: PASS.
- DCC traceability UX static contract: PASS.
- DCC detail trace-list template static contract: PASS.
- `DccControlledFileLogControllerTest`: 2/2 PASS.
- `DccControlledFileLogQueryServiceTest`: 6/6 PASS, including `2054545668044084022` exact filtering and invalid input rejection.
- `pnpm ts:check`: PASS (`TS_CHECK_EXIT=0`).
- `git diff --check`: PASS with no whitespace errors.

## Scope Guard

No database, business data, service runtime, or Git write operation was performed. The current worktree already contained unrelated changes; they were left untouched.
