# Execution Log: OAuth2 Access Token Redis 运行时回退修复

BDD: protected requests fall back to DB when access-token Redis read fails ->
Given a valid OAuth2 access token exists in MySQL, When the Redis cache lookup
throws a runtime exception, Then the token service must continue with the MySQL
lookup path instead of surfacing a 500 response.

BDD: valid token semantics stay unchanged -> Given Redis is healthy or the DB
contains no matching token, When the token service reads the access token, Then
success and failure semantics remain the same except for the new runtime
fallback behavior.

- M1: Completed. The latest backend task `20260516-electronic-batch-record-import-analysis-tabs` was already completed before this runtime fallback fix started.
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-system -Dtest=OAuth2TokenServiceRuntimeFallbackTest,OAuth2AccessTokenRedisDAOTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `OAuth2TokenServiceRuntimeFallbackTest` reproduced the runtime `ClassCastException` escaping from `oauth2AccessTokenRedisDAO.get(...)`.
- GREEN: same Maven command -> PASS after `OAuth2TokenServiceImpl.getAccessToken()` started catching runtime Redis lookup failures and continued with the DB fallback path.
- GREEN: live follow-up -> PASS, authenticated request to `/admin-api/dcc/file-categories` returned `code=0` after fresh login instead of the previous token-cache 500.
