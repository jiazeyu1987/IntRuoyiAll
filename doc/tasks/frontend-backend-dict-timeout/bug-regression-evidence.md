# Frontend Backend Dict Timeout Regression Evidence

## Bug Summary

Frontend navigation reports `AxiosError: timeout of 30000ms exceeded` while preloading dictionary data through `dictStore.setDictMap()`.

## Expected Behavior

The dictionary preload request should either return data or a normal backend error before the 30000ms Axios timeout, without breaking router startup.

## Reproduction

BDD: frontend dictionary preload should not block router startup -> Given the frontend has a valid local backend configuration, When startup navigation triggers `dictStore.setDictMap()`, Then `/admin-api/system/dict-data/simple-list` should complete before the 30000ms Axios timeout and must not surface an uncaught router error.

RED: pending authenticated frontend reproduction.

## Root Cause

Pending.

## Regression Test

Pending.

## GREEN Evidence

Pending.

## Risk And Scope

Scope is limited to the frontend/backend dictionary preload timeout path. No fallback, silent downgrade, or mock data is allowed.

## Blockers

None yet.
