# Frontend Feature Evidence

## Feature Goal And Non-Goals

- P3 workbench, detail and document-control management surfaces for publication follow-up, notifications and impact tasks.
- Runtime E2E, database migration execution and service restart remain deferred to P4.

## Requirements And Acceptance IDs

- P3-AC1 through P3-AC5; business AC-03, AC-04, AC-05, AC-09, AC-14, AC-15 and AC-17.

## UI Entry Points, Routes, Components, And Owned Files

- DCC detail embeds `PublicationFollowupPanel.vue` and shows structured visibility sources/users, notification reasons/statuses and impact directions/statuses after the normal controlled-file detail authorization succeeds.
- DCC workbench adds a paged “我的影响评估” section with start, decision, source iteration selection, existing-open-revision link and tracking states.
- `dcc/controlled-file/publication-followup/index.vue` is the document-control management page with database-backed filters, expandable delivery/task details and retry action.
- `remaining.ts` provides the hidden route fallback; `20260907_dcc_publication_notification.sql` provides the visible dynamic menu and permission.
- `notifyMessageNavigation.ts` and `MyNotifyMessageDetail.vue` add the same-origin DCC publication target and “查看发布文件” action.

## API Contracts And Data States

- `publicationFollowup.ts` declares every business/Snowflake ID as `string`; API wrappers never use JavaScript `Number` for task, batch, file, user, message or revision IDs.
- Detail: loading, no-follow-up, authorized data and API error states.
- Workbench default: ordinary unfinished tasks plus required-revision `NOT_STARTED/REVISION_LINKED`; terminal no-revision/resolved work is excluded unless explicitly queried.
- Revision dialog: the backend returns selectable current-Master source iterations and the unique open major revision. An open revision disables creation and exposes only explicit linking.
- Management: all filters, paging, PENDING/FAILED retry with reason/CAS, SENT no action, loading/error and preserved rows on action failure.
- Shared presentation maps batch, notification, task, decision, tracking, visibility-source, relation-direction and controlled-file status codes to simplified Chinese; unknown values render explicitly.

## BDD Scenarios

`BDD: publication follow-up is visible and actionable -> Given P1/P2 facts exist, When a permitted user opens the DCC workbench or file detail, Then the page shows authoritative notification and impact states and only exposes permitted commands.`

`BDD: large ids stay exact -> Given API data contains an id above JavaScript's safe integer limit, When the user pages, opens detail, retries, creates or links a revision, Then the exact decimal string is used in route and payload.`

`BDD: required revision remains in the workbench -> Given a decision changes a task to COMPLETED+NOT_STARTED and later REVISION_LINKED, When the workbench reloads, Then it remains visible for the next action/tracking until RESOLVED.`

`BDD: UI failure is explicit -> Given the user cancels a dialog or an API/CAS command fails, When the action finishes, Then cancel is quiet, failure is visible in the current area, the row remains, and only successful commands refresh data.`

## RED And GREEN

- RED: `dcc-release-impact-workbench-static.spec.js` and `dcc-detail-publication-followup-static.spec.js` initially failed because `publicationFollowup.ts` and the three P3 surfaces did not exist.
- RED: `dcc-publication-notify-navigation-static.spec.js` failed because station-message navigation did not recognize a safe DCC detail target or preserve a large string ID.
- Corrective RED covered missing source selection/existing revision linking, unhandled cancel/API errors, internal code labels, silent 20-row truncation and the disappearing COMPLETED+NOT_STARTED task.
- GREEN: all `3` P3 frontend static contracts passed; relaxed project `vue-tsc` and targeted P3 ESLint passed.

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Detail, workbench and management surfaces provide explicit loading, empty and error states; command errors remain visible without clearing successful prior rows.
- Action buttons and dialogs use loading/disabled guards against duplicate submission. Prompt cancellation does not produce a global error.
- Tables have stable minimum widths and narrow-screen horizontal containment; headers and filters collapse to one column on mobile.
- Sections use headings/ARIA labels and error alerts use `role=alert`; notification cards and detail actions retain keyboard-accessible navigation.
- Document-control route/menu requires the new manage permission; backend independently checks manage plus doc-control and approve.

## E2E Or Component Verification Path

- Static frontend contracts, relaxed project TypeScript and targeted ESLint passed in P3.
- P4 must use Playwright through the real UI for publication, inbox navigation, impact decision, source selection/open-revision linking and document-control retry; API/DB may only perform final read-only verification.

## Blockers And Follow-Up Skills

- No P3 frontend/static-delivery blocker remains.
- Runtime migration, real Playwright and service restart remain deferred to P4 authorization and are not claimed here.
