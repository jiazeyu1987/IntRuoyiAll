# Task: DCC 受控浏览默认显示最新版本

## Goal

为 DCC 受控浏览分页接口补充“浏览视图默认按文件聚合到最新版本”的真实后端语义，使浏览页可以默认展示最新版本，并保留同文件的历史版本列表用于前端下拉切换。

## Non-Scope

- 不修改 `我的受控文件` 请求方视图当前按提交记录返回的语义。
- 不改动 DCC 文件详情接口已有 `versionHistory` 结构。
- 不新增数据库表、兼容兜底或静默降级逻辑。

## Previous Task Check

- Previous same-repo task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-aliyun-nls-shared-tts\task.md`
- Status before this task: blocked on 2026-05-19 because the current thread switched to the higher-priority DCC browser version behavior change.
- Impact: previous task artifacts remain preserved; this DCC query change can proceed independently without reusing the unfinished showroom TTS write scope.

## Scope

- `yudao-module-dcc` controlled file page request/response handling for browser-specific latest-version aggregation
- Targeted DCC query service regression tests
- This task directory evidence

## Milestones

- [x] M1: Create the task record and confirm the current page/query semantics.
- [x] M2: Add RED backend tests proving the browser query currently returns duplicate history rows instead of one latest row per file.
- [x] M3: Implement the minimal browser-only aggregation parameter and preserve existing requester/mine behavior.
- [x] M4: Run GREEN backend verification and capture evidence.
- [x] M5: Execute closeout preview and prepare a task-only commit.

## Expected Verification

- `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260519-dcc-browser-latest-version-default/backend-api-evidence.md`

## Current Status

Completed.

## Write Boundary

- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFilePageReqVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
- `doc/tasks/20260519-dcc-browser-latest-version-default/**`

## Risks And Constraints

- The new behavior must be opt-in for browser view only so `我的受控文件` and other callers keep their current semantics.
- Aggregation must happen before pagination to avoid fake totals or missing latest rows.
- If version ordering data is inconsistent, tests must expose it explicitly instead of silently guessing with fallback branches.

## Final Verification Result

- PASS: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest" test`

## Cleanup Keep

- doc/tasks/20260519-dcc-browser-latest-version-default/backend-api-evidence.md
