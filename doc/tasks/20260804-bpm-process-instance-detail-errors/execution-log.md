# Execution Log

## User Intent

用户报告使用本机 `芋道源码/admin` 登录访问 `http://localhost:8081/bpm/process-instance/detail?id=c1cd2ae6-8fbf-11f1-a00f-00155d2984a0` 时出现 3 个错误，要求修复报错。

## Preflight

- 已读取 `bug-regression-fix-loop` 技能与证据契约。
- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取 `docs/experience-index.md` 并命中 `前端 BPMN marker 高亮完整性门禁`。
- 发现工作区已有大量既有未提交改动，当前任务不得覆盖或回滚无关改动。

## BDD

- BDD: BPM 详情页缺失高亮节点不触发页面异常 -> Given 已登录本机 `芋道源码/admin` 且流程实例详情接口返回包含当前 BPMN XML 不存在的高亮节点 ID, When 打开目标 BPM 流程实例详情页, Then 页面应显示流程图高亮不完整警告且不触发 `Cannot read properties of undefined (reading 'markers')`。

## TDD Evidence

- RED: `mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest#getProcessInstanceBpmnModelView_shouldFilterMarkerIdsMissingFromCurrentBpmnModel" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增断言证明后端仍返回当前 BPMN XML 中不存在的任务或连线 marker ID。
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest#getProcessInstanceBpmnModelView_shouldFilterMarkerIdsMissingFromCurrentBpmnModel" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 个测试通过。
- GREEN: `mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 个测试通过。

## Milestone Updates

- completed: 建立任务记录并装载适用规则。
- completed: Playwright 真实路径初始复现定位到 BPMN 高亮 ID 与当前 BPMN XML 不一致；目标接口业务码为 0，无 pageerror，流程图 warning 中有 3 个缺失 marker ID：`flow_start_doc_control_review:success`、`startEvent:success`、`DOC_CONTROL_REVIEW:primary`。
- completed: 在 `BpmProcessInstanceServiceImplTest` 增加缺失 BPMN marker ID 过滤回归测试，RED 先失败。
- completed: 在 `BpmProcessInstanceServiceImpl` 对 unfinished、finished、sequenceFlow、reject marker ID 集合统一保留当前 BPMN XML 中真实存在的元素 ID。
- completed: 聚焦前端静态断言通过，确认 `ProcessViewer.vue` 仍有 `safeAddProcessMarker`、`safeRemoveProcessMarker`、`elementRegistry.get(activityId)` 校验和 `data-testid="bpm-process-viewer-warning"` 可见 warning。
- completed: 已将本次后端 BPMN 模型视图过滤职责沉淀到现有 `docs/frontend-development.md#前端-bpmn-marker-高亮完整性门禁`，未新建长期经验文档；`rg` 已验证 `20260804-bpm-process-instance-detail-errors` 可定位到该门禁。
- completed: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260804-bpm-process-instance-detail-errors\bug-regression-evidence.md` -> PASS。
- completed: `git diff --check` 针对本任务后端源码、测试、任务文档与经验文档 -> PASS，仅提示 LF/CRLF 工作区换行警告。
- blocked: 真实 8081 页面复验当前无法完成；最新 `node doc\tasks\20260804-bpm-process-instance-detail-errors\reproduce-bpm-detail-errors.cjs` 因 `http://127.0.0.1:8081` 连接拒绝失败，端口检查显示 8081/48081 未监听。由于主工作区混有大量非本任务脏改动，本地运行态规则禁止从脏主工作区重打运行 Jar 冒充本任务 E2E 结果。
- blocked: `node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs` 被无关 DCC 断言 `version history reason cells must render from the formal row remark helper` 阻塞，不作为本 BPM 修复失败判定。
- blocked: 当前分支 `int_main...origin/int_main [ahead 9]` 且工作区存在大量非本任务改动；为避免混入无关提交或推送，未执行提交/推送收尾。
