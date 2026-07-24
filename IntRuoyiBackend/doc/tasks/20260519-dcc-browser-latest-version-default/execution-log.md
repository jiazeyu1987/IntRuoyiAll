# Execution Log

BDD: Browser query returns latest row by default -> Given a directory contains multiple versions of the same controlled file master When the browser page requests the controlled file page with browser latest-only semantics Then the result contains one row for that master and it is the latest visible version

BDD: Browser query keeps version history for dropdown switching -> Given the latest browser row is returned for a file master When the frontend inspects the row payload Then the row still includes visible `versionHistory` entries for current and historical revisions

BDD: Mine query preserves existing record semantics -> Given the requester page asks for controlled files without browser latest-only semantics When multiple revisions exist for the same master Then the page still returns the original per-record list instead of collapsing revisions

RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" test` -> FAIL, `DccControlledFilePageReqVO` 尚无 `setLatestVersionOnly(Boolean)`，证明浏览页专用最新版本语义尚未实现。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" test` -> PASS，浏览视图已支持 `latestVersionOnly`，并在分页前按文件主链聚合为最新可见版本，同时保留历史版本链且不影响请求人视图。
