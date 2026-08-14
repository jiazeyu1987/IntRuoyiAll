# Feature

DF03 adds a dedicated DCC project code binding section to the route edit page. The section reads, saves, and removes the route-DCC relation through dedicated APIs rather than through route header save or flow graph save.

## Acceptance

- AC-03 / AC-04 / AC-05 / AC-07: the route-DCC relation is explicit, versioned, and independent from product, QA regulation, and formBindings inference.
- AC-11 / AC-12 / AC-13: the frontend contract keeps route save and DCC binding save separate and surfaces API failure instead of claiming success.

## BDD: independent DCC save

Given a user edits route basic data and DCC project binding, When DCC binding save fails, Then the page must not report the DCC binding as saved by the route header save.

## BDD: dedicated unbind

Given a route has a DCC binding, When the user unbinds it, Then the page calls the dedicated DELETE route-DCC API with routeId and expectedVersion.

## RED:

node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs was introduced to lock the frontend/API contract before the final implementation. The backend RED failure blocked full contract pass until the missing route-DCC backend error contract was implemented.

## GREEN:

node tests/e2e/mes-route-dcc-project-binding-static.spec.cjs -> PASS. The contract found route-DCC read/save/delete APIs, DCC state on RouteFormContent, separate save/delete calls, expectedVersion, and DELETE route update permission without DCC/QA permissions.

## Verification

- RouteFormContent keeps dccProjectBinding and dccProjectBindingForm as independent state.
- DCC save uses saveRouteDccProjectBinding with expectedVersion from the latest relation version.
- DCC unbind uses deleteRouteDccProjectBinding and does not depend on route header submitForm success.
- Loading/error behavior surfaces API failures with route operation error messages; no mock data or fallback success was added.

## Blockers

No DF03 frontend static-contract blocker. Real Playwright write-path verification remains for later INT/VAL stages where confirmed login and task-owned data are available.
