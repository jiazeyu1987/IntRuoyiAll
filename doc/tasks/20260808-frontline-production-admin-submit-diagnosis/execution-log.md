# Execution Log

BDD: admin 一线生产模拟提交失败归因 -> Given 本机 `芋道源码/admin` 可通过真实前端进入一线生产页面 / When 操作页面发起一次正式提交 / Then 必须采集提交载荷、后端响应和运行态候选，说明失败是否由设备/工作站上下文不一致导致。

- 已读取 `docs/task-closeout-rules.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/worktree-restrictions.md`、`docs/powershell-encoding.md`。
- 已读取 Playwright 技能 `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`，本轮使用真实浏览器页面路径。
- Git 状态只读检查：当前 `int_main` 工作区已有大量非本任务脏改动，本任务不提交、不清理、不回滚无关变更。
- 创建任务目录：`doc/tasks/20260808-frontline-production-admin-submit-diagnosis/`。
- 命中经验门禁：`docs/backend-development.md#一线运行态 route-start 生产组长来源必须独立于班组设备绑定`、`docs/backend-development.md#一线生产正式提交必须单事务落链并按唯一组长归属可见`、`docs/e2e-rules.md#Playwright 目标链路与外部资源异常归因门禁`。
- GREEN: `$env:PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH='C:\Program Files\Google\Chrome\Application\chrome.exe'; node doc\tasks\20260808-frontline-production-admin-submit-diagnosis\frontline-admin-submit-diagnosis.mjs` -> PASS，使用真实前端页面和 `芋道源码/admin` 发出一次 `/admin-api/mes/pro/feedback/frontline/submit`，HTTP 200，业务码 `1040760111`，复现设备/工作站上下文不一致。
- 提交页面证据：工序 `1. 粗洗工序`，员工 `112`，设备卡片 `超声波清洗机`，产出数量 `1`，确认弹窗出现并执行正式提交。
- 提交载荷证据：`processPoolContext.deviceId=980009`，`processPoolContext.workstationId=980010`，`feedbackPayload.selectedDevice.deviceId=980009`。
- 授权候选证据：`authorizedCandidate.deviceId=41`，`authorizedCandidate.workstationId=980010`，`authorizedCandidate.deviceCode=A03190`，`authorizedCandidate.deviceName=球囊成型机`。
- 运行态证据：`runtimeConfig.devices[0].deviceId=980009`，`deviceCode=B09393`，`deviceName=超声波清洗机`。
- 结论：失败不是工位不一致，而是提交侧班组设备 ID `980009` 与授权侧工作站正式机台 ID `41` 属于不同设备 ID 域，被 `MesFrontlineSubmitAuthorizationServiceImpl.authorize(...)` 直接比较后触发 `PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH`。
- 经验归档：已将“正式提交授权、运行态设备卡片和设备参数校验必须比较同一设备 ID 域”的规则合并到 `docs/backend-development.md` 既有一线运行态 route-start 门禁。
- Closeout preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-production-admin-submit-diagnosis --mode preview` -> PASS，keep 5 个证据文件，delete/blocked/warnings 均为 `<none>`。
- Closeout apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-production-admin-submit-diagnosis --mode apply` -> PASS，deleted_paths 为 `<none>`，当前主工作区 `linked=False`，无需 worktree 合并或删除。
