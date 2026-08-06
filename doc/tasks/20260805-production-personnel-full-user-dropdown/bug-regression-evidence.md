# Bug Regression Evidence

## Bug Summary

- 用户在“新增人员 > 正式工姓名”输入 `陈` 时下拉显示 `No data`。
- 期望：正式工下拉按全量系统用户搜索，当前租户内昵称或账号匹配 `陈` 的有效系统用户应返回候选。

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

## RED

- `GET /mes/pro/process-pool/team-leader/employee-profile/formal-candidates?keyword=陈` -> FAIL, returned 0 candidates while `/system/user/simple-list` contained 89 matching users.

## GREEN

- Pending runtime refresh and same API recheck.

## Risk And Regression Scope

- Scope is local runtime loading, not a new frontend interaction change.
- Must not reintroduce subordinate-scope fallback, frontend full-list local filtering, or default empty-success fallback.

## Blockers

- None for runtime refresh after `index.lock` disappeared.
