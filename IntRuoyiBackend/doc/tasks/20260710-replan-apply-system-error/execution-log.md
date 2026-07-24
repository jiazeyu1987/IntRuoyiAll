# Execution Log

## BDD

BDD: 失效用户刷新登录态 -> Given 刷新令牌仍存在但其后台用户已删除 / When 应用重排请求触发访问令牌刷新 / Then 后端清理该刷新令牌及关联访问令牌，并返回明确的未授权错误，不抛出空指针或“系统异常”。

BDD: 有效用户刷新登录态 -> Given 刷新令牌、租户和用户均有效 / When 应用重排请求触发访问令牌刷新 / Then 正常签发新访问令牌，原有刷新成功行为不变。

BDD: 重排请求边界 -> Given 认证刷新失败 / When 用户点击应用重排 / Then 请求不得进入排产应用事务，不得错误归因为排产快照保存失败。

## Evidence

- 用户报告：前端 `confirmApplyReplanStartChoice` 调用 `replanApply` 时收到 `Error: 系统异常`。
- 运行日志显示失败请求停在 `/admin-api/system/auth/refresh-token`，没有进入重排应用接口。
- 刷新令牌指向租户 `122` 中已不存在的后台用户；`OAuth2TokenServiceImpl.buildUserInfo` 直接解引用空用户，触发 `NullPointerException`。
- 当前事务因空指针回滚令牌清理，浏览器会反复使用同一失效刷新令牌并持续收到“系统异常”。

RED: `mvn -pl yudao-module-system -Dtest=OAuth2TokenServiceImplTest#testRefreshAccessToken_userNotExists test` -> FAIL, 期望 `ServiceException`，实际在 `buildUserInfo` 抛出 `NullPointerException`。

GREEN: `mvn -pl yudao-module-system -Dtest=OAuth2TokenServiceImplTest#testRefreshAccessToken_userNotExists test` -> PASS，失效用户返回 401，关联访问令牌与刷新令牌均被清理。

GREEN: `mvn -pl yudao-module-system -Dtest=OAuth2TokenServiceImplTest test` -> PASS，14 个令牌服务测试全部通过。

GREEN: `mvn -pl yudao-module-system test` -> PASS，524 个测试通过，9 个既有条件测试跳过。

INFO: experience-index -> matched `docs/powershell-memory.md`、`docs/worktree-memory.md`、`docs/login-access.md`、`docs/experience/project-error-prevention.md`。

GREEN: experience-preflight -> PASS，已确认仅操作本机测试租户；隔离前端端口 `8096`、后端端口 `48096` 均未占用；主后端工作区无脏改且与本任务文件无重叠；主前端工作区存在其他任务脏改，因此只从提交态创建独立前端运行 worktree，不修改或清理主前端工作区。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，隔离后端可执行包构建成功。

GREEN: login-preflight -> PASS，真实浏览器使用 `http://127.0.0.1:8096`、`测试租户/aoteman` 进入 `/mes/pro/scheduler-workbench`。

BLOCKER: 完整应用重排 E2E -> 测试租户现有 25 个候选均存在业务阻断；尝试通过真实“同步工单”页面补充候选时，后端明确返回“工艺路线流转关系图无效，请先修正关系图”。未修改路线数据或绕过业务门禁。

GREEN: Playwright real stale-token verification -> PASS，使用测试租户真实失效刷新令牌记录 `id=16763` 调用隔离后端 `/admin-api/system/auth/refresh-token`，HTTP 200、业务码 401、消息“刷新令牌对应的用户不存在”；随后数据库只读核验该刷新令牌和关联访问令牌有效记录数均为 0。

REGRESSION: 真实链路结论 -> 用户报告的“应用重排系统异常”发生在重排请求前的令牌刷新阶段；修复后失效登录态明确返回未授权并完成清理，不再触发 `NullPointerException` 或泛化“系统异常”。完整重排写入仍受测试租户路线图数据门禁阻断，与本次认证修复无关。

INFO: task-closeout-cleanup preview -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，仅将任务附属证据文件列为删除项；自动 apply 因主后端工作区存在其他任务的脏改而按规则阻断。

GREEN: merge-overlap-check -> PASS，主后端工作区脏改仅涉及 `script/deploy/restart-int-ruoyi-local.ps1` 和 `script/tests/test_restart_ruoyi_frontend_vite_emfile_config.py`，与本任务提交无文件级重叠。

GREEN: merge-int_main -> PASS，后端 `int_main` 从 `aee9eff185` fast-forward 到实现提交 `ec75ee6653`。

GREEN: merged-result -> PASS，在融合后的 `int_main` 运行 `mvn -pl yudao-module-system -Dtest=OAuth2TokenServiceImplTest test`，14 个测试全部通过。

GREEN: worktree-cleanup -> PASS，后端、前端临时 worktree 与任务分支均已删除；`D:\ProjectPackage\Int\IntRuoyiWorktrees\20260710-replan-apply-system-error` 已不存在。

GREEN: unrelated-changes-preserved -> PASS，主后端工作区原有的重启脚本和测试文件改动未暂存、未提交、未覆盖。

## Current Status

completed
