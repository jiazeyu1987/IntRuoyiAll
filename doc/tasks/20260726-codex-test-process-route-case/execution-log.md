# 执行日志

## 用户意图

- 在测试管理中增加分类 `工艺路线`。
- 根据工艺路线的操作场景新增 4 个测试项。

## 执行范围

- 当前 worktree：`D:\IntRuoyiWorktree\codex-test-process-route`。
- 当前分支：`codex/codex-test-process-route`。
- runtime slot：`int_main/slot=1`，前端 `8082`，后端 `48082`。
- 仅操作本机测试管理和任务自有测试数据，不操作远端、生产数据或共享 `48081`。

## BDD

- BDD: 工艺路线分类可维护 -> Given 测试管理页可访问，When 选择并保存 `工艺路线` 分类，Then 列表和筛选均能识别该分类。
- BDD: 工艺路线基础信息与工序维护 -> Given 任务自有工艺路线数据，When 在 MES 工艺路线页新增并保存基础信息和工序，Then 页面显示完整路线配置。
- BDD: 工艺路线复制与产品绑定 -> Given 任务自有可复制路线，When 复制路线并绑定产品，Then 新路线和产品关系可回读。
- BDD: 工艺路线候选版本编辑发布 -> Given 任务自有 ACTIVE 路线，When 保存候选版本并提交发布，Then 版本生命周期符合页面流程且草稿保存不隐式发布。
- BDD: 工艺路线状态与删除约束 -> Given 任务自有已引用/未引用路线，When 操作启停和删除，Then 状态或业务阻止结果明确可见。

## 前置证据

- 已读取项目规则、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/database-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/branch-runtime-ports.md`、`docs/worktree-restrictions.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` 和命中的 `docs/experience-index.md` 路由。
- `npx` 可用：`D:\Programs\npx.ps1`。
- worktree 绝对路径已确认在 `D:\IntRuoyiWorktree\` 下。
- 运行态槽位预留：`codex-test-process-route -> int_main/slot=1 -> 8082/48082`。

## 主工作区早期记录合并说明

- `int_main` 上曾存在同任务的早期预检记录，范围为新增 1 个工艺路线测试项，且当时 blocker 是本机后端 `48081` 未监听。
- 本 worktree 后续改用隔离运行态 `8082/48082` 完成 4 个工艺路线测试项、前后端契约和真实页面验证；本文件保留完整 worktree 证据，并在此记录早期 blocker 已由隔离运行态路径取代。

## 里程碑记录

- completed: M1 规则与入口前置读取完成。
- completed: M2 确认 `project` 是测试管理分类字段，现有合法值为 `智能排产`、`文控`、`批记录`；工艺路线页面具备新增、复制、版本、保存发布和启停/删除操作入口。
- completed: M3 后端单测和前端静态契约已先 RED 后 GREEN，`工艺路线` 已进入后端项目枚举、前端类型、下拉选项、筛选、列表标签和 OpenAPI 保存 VO 描述。
- completed: M4 已通过真实前端页面新增并回读 4 个 `工艺路线` 测试项。
- in_progress: M5 回归验证、证据校验和经验沉淀完成，状态进入 `ready_for_closeout`，待 cleanup、提交、推送和 worktree 收尾。

## 验证证据

- RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧后端项目枚举拒绝 `工艺路线`，未知项目错误文案仍按旧枚举断言。
- RED: `node IntRuoyiFronted\tests\e2e\system-codex-test-management-static.spec.js` -> FAIL，旧前端项目类型和选项未包含 `工艺路线`。
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，5 tests。
- GREEN: `node IntRuoyiFronted\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- RUNTIME: `http://127.0.0.1:48082/actuator/health` -> `UP`；`http://127.0.0.1:8082/` -> HTTP 200。
- E2E: `node doc\tasks\20260726-codex-test-process-route-case\ensure-process-route-codex-test-items.e2e.cjs` -> PASS，真实页面创建/更新 4 条 `工艺路线` 测试项。
- SUMMARY: `doc/tasks/20260726-codex-test-process-route-case/artifacts/process-route-codex-test-items-summary.json` -> PASS，测试项 ID `18`、`19`、`20`、`21`，每项 4 个目标项。
- REGRESSION: `mvn.cmd -pl yudao-module-system "-Dtest=CodexTestCaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS，5 tests；`-am` 重跑曾在本机 Java 21 fork/surefire 资源下超时并留下 dumpstream，但同模块非 fork 复验通过。
- REGRESSION: `pnpm ts:check` -> PASS。
- SYNTAX: `node --check doc\tasks\20260726-codex-test-process-route-case\ensure-process-route-codex-test-items.e2e.cjs` -> PASS。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260726-codex-test-process-route-case\backend-api-evidence.md` -> PASS。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260726-codex-test-process-route-case\frontend-feature-evidence.md` -> PASS。
- EXPERIENCE: 已按 `project-experience-consolidation` 合并长期经验到 `docs/worktree-memory.md`：新增 “Worktree Java 21 后端低内存启动门禁”。
- STAGE: `doc/tasks/20260726-codex-test-process-route-case/ensure-process-route-codex-test-items.e2e.cjs` 命中任务目录 `.gitignore` 规则；该脚本是本任务真实页面写入证据并已列入 `Cleanup Keep`，提交时使用 `git add -f` 强制纳入。
- COMMIT: 实现提交 `3aa45a95 feat: add process route codex test cases`，包含分类契约、4 条测试项真实 E2E 证据、任务文档和长期经验。
- CLEANUP: `task_closeout.py --task-id 20260726-codex-test-process-route-case --mode apply --worktree-closeout off --extra-delete .runtime/codex-test-process-route/start-backend-limited.ps1` -> PASS；删除本任务临时失败/成功截图和临时后端启动脚本，保留核心任务记录、E2E 脚本和 JSON 摘要。
- RUNTIME-CLEANUP: 已停止本任务运行态 PID `16344`、`58572`、`11564`、`32044`；复查 `8082/48082` 无监听。
- CLOSEOUT-BLOCKER: 自动 worktree merge/remove 未执行；`E:\IntRuoyi` 主工作区存在 unrelated dirty/ahead 状态，且当前分支不能对本地 `int_main` 做 ff-only closeout。任务保持 `ready_for_closeout`，等待主工作区清理或用户授权后再合并/删除 worktree。

## Blockers

- worktree 自动 closeout 阻塞：主工作区 `E:\IntRuoyi` dirty 且 ahead，不能接收 ff-only merge；当前 worktree 暂保留。
