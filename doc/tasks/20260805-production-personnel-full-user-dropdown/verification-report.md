# Verification Report

## Result

PASS

生产组长新增人员中的正式工远程搜索已从当前组长下属部门范围调整为当前租户内全量系统用户昵称搜索；选中的有效系统用户不再经过下属部门范围校验，可以直接创建当前组长的正式工档案。

功能、定向验证和本机 48081 运行态复验均 PASS；closeout 提交/推送仍待在共享工作区并发改动可安全处理后完成。

## Contract

- `GET /mes/pro/process-pool/team-leader/employee-profile/formal-candidates` 的请求和响应结构不变。
- `POST /mes/pro/process-pool/team-leader/employee-profile/formal/link` 的请求和响应结构不变。
- 两个接口继续要求 `mes:pro-process-pool-team-leader:maintain`。
- 空白关键字仍返回空列表，不执行无条件全量扫描。
- 重复正式工、显示名冲突和系统用户有效性校验保持不变。
- 未修改数据库 schema、前端 API 地址或页面组件。

## Evidence

- RED: system API 缺少 `getUserListByNickname(String)` 时目标测试编译失败。
- GREEN: `AdminUserApiImplPostIdsTest` -> 2 tests PASS。
- GREEN: `MesTeamLeaderRuntimeConfigServiceTest` -> 13 tests PASS。
- REGRESSION: `MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderRuntimeConfigServiceTest` -> 24 tests PASS。
- CONTRACT: task-owned `git diff --check` -> PASS。
- EVIDENCE: `validate_backend_api.py --evidence doc/tasks/20260805-production-personnel-full-user-dropdown/backend-api-evidence.md` -> PASS，`Backend API evidence is valid.`。
- Reactor contract: BPM 手写 `AdminUserApi` 测试实现同步新增显式 unsupported 方法，未添加默认空列表 fallback。
- RUNTIME RED: 旧运行包 `backend-runtime-control-acm04-pqc-source-context-20260805.jar` 下，登录态 `keyword=陈` -> `code=0,count=0`；同租户系统用户简单列表本地过滤 `陈` 为 89 条。
- RUNTIME GREEN: 新运行包 `backend-runtime-production-formal-users-20260806.jar`，SHA256 `2c14fd2d6365c968bc26ed5bb15c0457e2301dbd62ec6aab321387dd6bc84000`，PID `17936`，health `UP`。
- API GREEN: 登录态 `keyword=陈` -> `code=0,count=20`，样例包含 `陈世世`、`陈丹`、`陈丽`、`陈亚辉`；空白关键字 -> `code=0,count=0`。

## Git And Concurrency

- 脏工作区基线提交：`3db8a7030`。
- 并行共享分支提交 `633361dde` 已包含本任务后端实现、测试和初始任务文档，并已进入 `origin/int_main`。
- 未执行 amend、reset、force push 或历史重写。

## Cleanup

- backend API evidence validator 在 cleanup 前通过，关键 RED/GREEN/REGRESSION 结果已归档到本报告和 `execution-log.md`。
- cleanup preview/apply 均通过；仅删除临时 `backend-api-evidence.md`，保留 `task.md`、`execution-log.md` 和本报告，无 blocked/warnings。
- 长期经验已合并到现有 `docs/backend-development.md`、`docs/local-runtime.md` 和 `docs/experience-index.md`，未创建新的经验文档。

## Git Closeout

- 旧 `index.lock` 阻塞已消失。
- 本轮未提交/推送：当前共享工作区仍包含非本任务并发改动，未将其混入本任务 closeout。
- 待处理：按项目 Git 规则在安全窗口完成 closeout 提交与 `git push origin int_main`，或由并发任务先处理其所属改动。

## Residual Risk

- 本次运行态复验为登录态 API 验证，未执行浏览器点击下拉的真实 E2E。
- 下拉仍沿用现有远程输入搜索交互，不会在空关键字时预加载所有用户。
