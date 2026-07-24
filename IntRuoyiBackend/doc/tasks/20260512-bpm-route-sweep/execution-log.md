# Execution Log: BPM route sweep

BDD: Workflow menu child routes load cleanly -> Given an authenticated admin opens the workflow menu, When each visible child route is opened through the real frontend, Then the page should render without unhandled frontend warnings/errors and its initial network requests should not return BPM disabled, schema-not-imported, missing-route, or system-exception responses.

BDD: Route-sweep fixes hold across the workflow menu -> Given a failing workflow child route is repaired, When the route sweep is rerun across the full workflow menu, Then the previously failing route and the already-passing routes should all still return normal business responses.

- M1: Completed. Previous unfinished task `20260512-mes-route-sweep` was explicitly blocked before this BPM sweep continued.
- M2 GREEN: extracted 14 visible BPM child routes from the authenticated admin menu via `localstorage-get user` and `system/auth/get-permission-info`.
- M3 RED: Playwright route sweep found one frontend warning on `/bpm/task/my`: `Invalid prop: type check failed for prop "value". Expected String | Number | Boolean | Array, got Undefined` from `DictTag`.
- M3 GREEN: initial route discovery sweep reached all 14 BPM child routes and found no `/admin-api/**` 4xx/5xx or failed BPM initialization requests.
- M4 GREEN: updated `yudao-ui-admin-vue3/src/views/bpm/processInstance/index.vue` to guard `DictTag` against missing `scope.row.status` and render `-` when the status is absent.
- M5 GREEN: reran the full 14-route Playwright sweep; every route loaded with `warningCount=0`, `errorCount=0`, and `adminIssueCount=0`.
