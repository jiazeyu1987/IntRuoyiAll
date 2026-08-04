# Execution Log

## Intent

- 用户要求将业务审批策略列表默认筛选改成 `policyMode = BPM_REQUIRED`。
- 用户进一步澄清：只显示一个 BPM 审批也不对，文控、表单、批记录等审批流程都应出现在默认可开关视图里。
- 用户反馈默认仍看到 102 条，要求默认只显示所有可以开关审批的业务，并且页面筛选只在这些可开关业务内继续过滤。

## BDD

- BDD: 默认只看 BPM 审批策略 -> Given 管理员打开业务审批策略页面 / When 页面首次加载策略列表 / Then 请求参数默认带 `policyMode = BPM_REQUIRED`，默认列表只展示 BPM 审批策略。
- BDD: 默认展示可开关审批策略 -> Given 管理员打开业务审批策略页面 / When 页面首次加载策略列表 / Then 请求默认使用可开关审批视图，展示文控、表单、批记录等顶层策略，并排除 eDHR 路线表单明细。
- BDD: 默认可开关业务正向白名单 -> Given 顶层审批开关策略和同对象类型明细策略同时存在 / When 管理员打开业务审批策略页面并在默认范围内筛选 / Then 默认结果只包含 DCC、表单模板、工艺路线、批记录版本、批次执行等明确可开关业务，不包含表单实例、路线附件、路线表单填写等明细策略。

## Milestone Updates

- M1: 已创建任务目录，读取 `docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/experience-index.md`、`frontend-feature-delivery` 技能和 `references/frontend-contract.md`。
- M2: 已在 `bpm-business-approval-policy-static.spec.js` 增加默认 `BPM_REQUIRED` 筛选静态契约。
- M3: 已将业务审批策略页 `queryParams.policyMode` 默认值改为 `BPM_REQUIRED`。
- M4: 定向静态契约、frontend evidence validator、UTF-8 读取和限定 diff check 均已通过；任务状态更新为 `ready_for_closeout`。
- Scope correction: 用户澄清默认 `policyMode=BPM_REQUIRED` 过窄，本任务重新进入 `in_progress`，改为新增后端 `approvalSwitchScope` 查询口径并调整前端默认参数。
- M2: 已补充前端静态契约和后端 Mapper 回归，锁定默认可开关审批视图不能按 `BPM_REQUIRED` 收窄。
- M3: 已新增后端 `approvalSwitchScope` 请求字段；Mapper 在该范围下排除 `EDHR_ROUTE_FORM` / `MES_EDHR_ROUTE_FORM_FILL` 明细策略，前端默认传 `approvalSwitchScope: true` 且 `policyMode` 为空。
- Scope tightening: 用户反馈默认仍有 102 条，已将任务重新置为 `in_progress`，准备把 Mapper 改为顶层执行器正向白名单。

## Verification Evidence

- RED: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> FAIL，旧实现 `queryParams.policyMode` 为 `undefined`，默认列表不限制 BPM 审批策略。
- GREEN: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> PASS，默认筛选契约通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/frontend-feature-evidence.md` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅提示两个前端文件下次 Git 触碰时 LF 会转 CRLF。
- GREEN: `python -X utf8 -c "...read_text(encoding='utf-8')..."` -> PASS，任务文档 UTF-8 可读。
- PROJECT_EXPERIENCE: 已读取 `project-experience-consolidation`；本次只是默认筛选小改动，既有业务审批策略和前端静态契约门禁已覆盖，无需新增长期经验文档。
- CLOSEOUT_PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-bpm-policy-default-bpm-required --mode preview` -> PASS，keep 包含 task.md、execution-log.md、verification-report.md、frontend-feature-evidence.md；delete/blocked/warnings 均为 `<none>`。
- RED: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> FAIL，页面缺少 `approvalSwitchScope: true` 且仍默认 `policyMode=BPM_REQUIRED`。
- RED: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> expected FAIL before implementation，`BusinessApprovalPolicyPageReqVO` 缺少 `setApprovalSwitchScope(true)` 对应字段，Mapper 也未过滤 eDHR 路线表单明细。
- RED: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest" test` -> FAIL，期望默认可开关范围返回 7 条顶层业务，实际返回 10 条，说明当前排除型筛选仍漏出同对象类型下的明细策略。
- GREEN: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> PASS，前端默认可开关视图契约通过。
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> PASS，4 tests, 0 failures, 0 errors。
- GREEN: `node tests/e2e/bpm-business-approval-policy-static.spec.js` -> PASS，前端补充 `DCC_UPLOAD` 中文标签后静态契约通过。
- GREEN: `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest" test` -> PASS，执行器正向白名单实现后 4 tests, 0 failures, 0 errors。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/backend-api-evidence.md` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS，仅提示任务文档 LF 下次 Git 触碰会转 CRLF。
- GREEN: `python -X utf8 -c "...read_text(encoding='utf-8')..."` -> PASS，5 个任务文档 UTF-8 可读。
- PROJECT_EXPERIENCE: 已读取 `project-experience-consolidation`；本次经验已由现有“业务审批策略按配置执行门禁”和前端静态契约隔离门禁覆盖，无需新增长期经验文档。
- CLOSEOUT_PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260804-bpm-policy-default-bpm-required --mode preview` -> PASS，keep 包含 task.md、execution-log.md、verification-report.md、frontend-feature-evidence.md、backend-api-evidence.md；delete/blocked/warnings 均为 `<none>`。
- RUNTIME_RESTART: `E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> FAIL，Maven package 在 `yudao-module-dcc` testCompile 阶段大量报 `NoSuchFileException` / 找不到 DCC class，后续只读核对发现同仓存在并发 Maven 构建写入同一 `target`。
- RUNTIME_STATE: `Get-NetTCPConnection -LocalPort 48081 -State Listen` -> `NO_LISTENER_48081`，标准重启失败后本地后端未恢复监听。
- RUNTIME_BLOCKER: 并发 Maven PID `47680` 命令为 `-pl yudao-server -am -DskipTests package`，线程栈位于 `java.io.WinNTFileSystem.delete0 -> org.apache.maven.shared.incremental.IncrementalBuildHelper.beforeRebuildExecution`；该进程不是本次标准重启命令创建的可确认任务进程，未停止。

## Blockers

- 当前工作区进入本任务前已有大量其他任务的已暂存、未暂存和未跟踪改动，且分支已领先 `origin/int_main`；本任务只触碰本次页面、现有目标静态契约和本任务文档，未执行提交/推送，避免混入无关并行任务改动。
- Commit/push closeout remains blocked until the unrelated dirty workspace is reconciled or the user authorizes the required baseline flow.
- Runtime restart is blocked because another same-workspace Maven package process is stuck in Windows incremental delete while `48081` is already stopped; proceeding requires explicit approval to stop PID `47680` and rerun the standard restart, or explicit approval to use the documented isolated BPM module runtime-jar update path.
