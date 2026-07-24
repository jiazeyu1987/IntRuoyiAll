# 执行日志：排产重排支持已报工剩余量

BDD: 已报工工序剩余量参与重排 -> Given 排产工序计划数量 1000、已报工 200、剩余 800 且已有非草稿报工任务 / When 用户发起重排预览 / Then 预览保留已报工任务，并生成数量为 800 的新重排任务。

BDD: 无剩余量的已报工工序不重复排产 -> Given 排产工序无剩余数量且已有非草稿报工任务 / When 用户发起重排预览 / Then 仅显示报工保护任务，不生成额外任务。

GREEN: experience-preflight -> PASS，已读取 PowerShell、经验索引、智能排产防错经验、bug-regression-fix-loop 与 backend-api-delivery 契约；本轮仅做本地后端代码与测试修改，不执行真实 E2E、服务器写入、发布、数据库修改或 worktree 清理等高风险动作。


RED: mvn -pl yudao-framework/yudao-spring-boot-starter-excel,yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityWhenFeedbackTaskProtected" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，新增回归用例进入业务断言后发现测试按错误的甘特列表总数/ID 前缀识别新任务；实现已生成任务数，但测试需要锁定真实 `_preview_` 新任务节点。

GREEN: mvn -pl yudao-module-system,yudao-framework/yudao-spring-boot-starter-excel -am "-DskipTests" install -> PASS，刷新本地依赖产物，避免混合工作区中无关 ExcelUtils 新签名未安装导致 MES 单模块编译失败。

GREEN: mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#replanPreview_shouldScheduleRemainingQuantityWhenFeedbackTaskProtected" test -> PASS，已验证工序计划 1000、已报工 200、剩余 800 且存在 FEEDBACK 保护任务时，重排预览保留原报工任务，并生成 1 个数量为 800 的 `_preview_` 新任务。

BLOCKER: mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" test -> FAIL，`testCompile` 阶段被无关新增文件 `MesProRouteProductServiceImplTest` 阻塞，原因是该测试引用不存在的 `MesProRouteProductCopyReqVO`；该文件不属于本任务修改范围，未回退也未修复。

GREEN: task-closeout-preview -> PASS，`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-replan-feedback-remaining --mode preview --worktree-closeout off` 返回 ready；预览保留 task.md 与 execution-log.md，建议删除 backend-api-evidence.md 与 bug-regression-evidence.md。

GREEN: experience-preflight-e2e -> PASS，用户要求补充真实数据 E2E；已读取 `docs/login-access.md`、`docs/powershell-memory.md`、`docs/experience-index.md`、Playwright 技能和前端入口规则；本次仅在本机 `http://localhost:8081` 使用测试租户 `测试租户/aoteman` 做只读重排预览验证，不点击“应用重排”，不写服务器、不改数据库、不切换租户。

GREEN: login-preflight -> PASS，`node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password ****** --target-path /mes/pro/schedule-order --target-text 排产` 已通过，真实浏览器进入排产工单页面。

BLOCKER: real-data-e2e-before-runtime-refresh -> 当前本机后端运行包 `yudao-server/target/yudao-server-exec.jar` 早于 `yudao-module-mes/target/classes/.../MesProAutoScheduleServiceImpl.class`，真实 E2E 预览仍可能运行旧逻辑；需要本机重建并重启 48081 后复验。

GREEN: runtime-refresh-for-real-e2e -> PASS，已执行 `mvn -pl yudao-server -am "-DskipTests" package`，并重启本机 48081 后端；`/actuator/health` 返回 200，运行包时间晚于本次 MES 服务类编译产物。

BLOCKER: real-data-e2e -> FAIL，真实登录与页面入口已通过，但测试租户当前没有满足验收条件的可预览候选。扫描 72 条真实排产工单后，找到 6 条“已报工且有剩余量”的真实工单：`SCH-20260610-0002` 剩余 420、`SCH-CODexERP20260610D-20260610-0001` 剩余 122、`SCH-CODexERP20260610E-20260610-0001` 剩余 121、`SCH-SMARTSCHED20260629MO2-20260629-0001` 剩余 9、`SCH-SMARTSCHED20260629MO3-20260629-0001` 剩余 9、`SCH-SMART-SCHED-20260630-RERUN11-MO-20260630-0001` 剩余 9；其中前 3 条预检被“缺少可用工艺路线”阻塞，后 3 条预检 PASS 但重排预览返回 `工艺路线已被禁用`，因此真实页面不会生成剩余量重排预览任务。按真实 E2E 门禁，本轮不造数、不改路线、不点击应用重排。

EVIDENCE: real-data-e2e-artifacts -> `yudao-ui-admin-vue3/tests/output/mes-replan-feedback-remaining-candidate-scan/candidate-scan.json` 与 `yudao-ui-admin-vue3/tests/output/mes-replan-feedback-remaining-diagnostic/diagnostic.json` 记录了真实候选扫描、预检与预览响应。

BLOCKER: real-data-e2e-rerun-with-system-chrome -> FAIL，默认 `chromium_headless_shell-1223` 因 ICU data 启动失败后，按前端 E2E 门禁切换到系统 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 复跑 `node tests/e2e/mes-replan-feedback-remaining-readonly.e2e.js`；真实浏览器已启动并扫描排产工单页面，但当前测试租户未找到可用于只读重排预览的真实已报工剩余量排产工单。脚本检查的真实列表中仅 `SCH-CODexERP20260610E-20260610-0001` 存在报工进度，其余检查项 `feedbackProcessCount=0`，因此本轮不造数、不改路线、不点击“应用重排”。
