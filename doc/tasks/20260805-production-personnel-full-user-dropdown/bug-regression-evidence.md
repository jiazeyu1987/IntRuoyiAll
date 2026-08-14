# Bug Regression Evidence

## Bug Summary

- 用户在“新增人员 > 正式工姓名”输入 `陈` 时下拉显示 `No data`。
- 期望：正式工下拉按全量系统用户搜索，当前租户内昵称或账号匹配 `陈` 的有效系统用户应返回候选。

## Expected

- 输入中文关键字时，正式工候选接口必须按当前租户全量系统用户搜索并返回匹配候选。
- 空白关键字仍必须返回空列表，不能执行无条件全量扫描。

## Reproduction

- Path: 本机前端 `http://127.0.0.1:8081`，生产组长工作台新增人员弹窗。
- API RED: 登录态只读请求 `GET /mes/pro/process-pool/team-leader/employee-profile/formal-candidates?keyword=陈` -> `code=0,count=0`。
- Control: 同一登录态请求 `/system/user/simple-list` 后本地过滤 `陈` -> 89 条匹配用户。

## Root Cause

- 当前 48081 后端运行包是 `backend-runtime-control-acm04-pqc-source-context-20260805.jar`。
- 该运行包以及既有人员 hotpatch 包的嵌套 MES/System class 均不包含 `getUserListByNickname`，仍包含旧的 `getUserListBySubordinate` 调用。
- 源码和定向测试已经具备全量用户候选实现，但本机运行态没有加载新包。

## Regression Test

- Existing backend regression: `MesTeamLeaderRuntimeConfigServiceTest#shouldSearchFormalCandidatesFromAllSystemUsers`。
- Runtime regression: 重启后同一登录态候选接口 `keyword=陈` 必须返回非空候选，且不要求该用户属于当前组长下属部门。

## RED:

- `GET /mes/pro/process-pool/team-leader/employee-profile/formal-candidates?keyword=陈` -> FAIL, returned 0 candidates while `/system/user/simple-list` contained 89 matching users.

## GREEN:

- Runtime refresh: old PID `60048` running `backend-runtime-control-acm04-pqc-source-context-20260805.jar` was replaced by PID `17936` running `backend-runtime-production-formal-users-20260806.jar`; health returned `UP`.
- Runtime jar SHA256: `2c14fd2d6365c968bc26ed5bb15c0457e2301dbd62ec6aab321387dd6bc84000`; patched nested modules kept `compress_type=0`.
- API GREEN: 登录态 `GET /mes/pro/process-pool/team-leader/employee-profile/formal-candidates?keyword=陈` -> `code=0,count=20`，样例包含 `陈世世`、`陈丹`、`陈丽`、`陈亚辉`。
- Guard GREEN: 空白关键字 `keyword=%20%20` -> `code=0,count=0`；同租户 `/system/user/simple-list` 本地过滤 `陈` 为 89 条。

## Verification

- `validate_bug_regression.py --evidence doc/tasks/20260805-production-personnel-full-user-dropdown/bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- Runtime API verification passed on `http://127.0.0.1:48081` with tenant-id `1`.

## Risk And Regression Scope

- Scope is local runtime loading, not a new frontend interaction change.
- Must not reintroduce subordinate-scope fallback, frontend full-list local filtering, or default empty-success fallback.

## Blockers

- None for runtime behavior. Git closeout remains pending because the shared worktree contains unrelated concurrent changes.
