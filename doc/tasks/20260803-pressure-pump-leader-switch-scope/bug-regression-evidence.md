# Bug Regression Evidence

## Bug

- Bug: pressure-pump production filling switch scope previously drifted toward global menu permission, post, or workstation binding; users could hit `设备账号上下文不完整或不一致：post workstation binding loginUserId=1, postIds=[14]` instead of route-owned production leader authorization.

## Expected

- Expected: batch execution tab visibility remains existing menu permission behavior, while process/employee switching is granted only by route start production leader config matching the login user or login user's role.

## Reproduction

- Reproduction: old behavior was captured by RED backend regression and user report; frontend RED static contract confirmed no production leader route-start field existed.

## Root Cause

- Root Cause: switching authorization source was not modeled as a route start production leader business configuration; old global pressure-pump menu permission/post/workstation paths could not express multi-leader, multi-line ownership.

## RED:

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before route version snapshot dependency and leader config logic existed.
- RED: `node tests/e2e/mes-route-start-production-leaders-static.spec.js` -> FAIL before production leader field/API UI contract existed.

## GREEN:

- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesFrontlineDeviceAccountContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 6 tests.
- GREEN: `node tests\e2e\mes-route-start-production-leaders-static.spec.js` -> PASS.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json --pretty false` -> PASS.

## Verification

- Backend regression verifies old pressure-pump menu permission no longer expands switchable process scope without route start production leader config.
- Backend regression verifies user and role leader config can authorize multiple production lines and routes.
- Frontend static contract verifies the configuration UI and API contract exists.

## Blockers

- Closeout commit/push blocked by unrelated dirty shared workspace; implementation verification is complete.
