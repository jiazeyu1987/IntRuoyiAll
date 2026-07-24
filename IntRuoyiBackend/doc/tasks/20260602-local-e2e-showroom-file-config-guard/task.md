# 本机 E2E 展厅文件配置保护闸门

## Task Goal

为本机 IntRuoyi 的启动/E2E 运行链路增加 fail-fast 保护，禁止 E2E 或本机脚本修改展厅默认文件配置和默认媒体桶，避免再次污染展厅图片/语音读取链路。

## Previous Task Check

- 上一个后端展厅任务 `doc/tasks/20260602-local-showroom-file-config-media-read/task.md` 已标记 `completed`。
- 当前仓库存在 DCC/infra 相关未提交改动和未跟踪目录，和本任务无关；本任务只修改本地启动脚本、对应测试和本任务文档。

## BDD

BDD: 本机后端启动前必须拦截被篡改的展厅默认文件配置 -> Given 本机启动脚本准备启动后端 / When `infra_file_config.id=28` 的 bucket、endpoint 或 domain 偏离本机展厅受保护默认值 / Then 脚本必须直接失败，禁止继续启动。

BDD: 本机后端启动前必须拦截被篡改的展厅媒体 URL -> Given 本机展厅媒体记录依赖默认文件配置 28 / When `infra_file` 中任一 `showroom/%` 记录 URL 指向非默认受保护桶 / Then 脚本必须直接失败，禁止继续启动。

BDD: 本机 E2E 保护规则必须可回归验证 -> Given 启动脚本承担本机 fail-fast 保护 / When 未来有人修改脚本 / Then 自动化测试必须校验受保护默认 bucket、domain、endpoint 和错误码文案仍然存在。

## Milestones

- [x] M1: 建立任务文档并确认上一任务已完成。
- [x] M2: 为本机启动脚本设计展厅默认文件配置保护闸门。
- [x] M3: 为脚本补充回归测试并先跑 RED。
- [x] M4: 完成脚本实现并跑 GREEN。
- [x] M5: 更新执行证据并完成收尾预览。

## Expected Verification

- RED: 保护规则测试在实现前失败，证明当前脚本未显式保护展厅默认文件配置。
- GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -k showroom_protection`
- REGRESSION: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-local-e2e-showroom-file-config-guard --mode preview`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。异常配置直接阻塞启动，不做兜底恢复。
- `是否从根因和长期维护角度解决`：是。通过启动闸门禁止默认展厅文件配置和媒体 URL 被 E2E 污染。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Completed Work

- 已建立任务文档。
- 已在 `script/deploy/restart-int-ruoyi-local.ps1` 增加 `Assert-LocalShowroomFileConfigProtected` 闸门。
- 已将本机展厅受保护默认值固化为 `infra_file_config.id=28`、`bucket=yudao`、`endpoint=http://127.0.0.1:9000`、`domain=http://127.0.0.1:9000/yudao`。
- 已增加对 `infra_file` 中 `config_id=28` 且 `path LIKE 'showroom/%'` 的 URL 域校验，防止 E2E 或脚本把默认展厅媒体记录写到非默认桶域。
- 已在 `script/tests/test_runtime_control_scripts.py` 补充保护闸门回归测试。
- 已执行一次真实本机后端重启，确认当前环境能通过新闸门并成功启动。

## Verification Evidence

- RED: `python -m pytest script/tests/test_runtime_control_scripts.py -k showroom_default_file_config_from_e2e_mutation` -> FAIL，缺少 `Assert-LocalShowroomFileConfigProtected` 保护函数。
- GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -k showroom_default_file_config_from_e2e_mutation` -> PASS，`1 passed, 9 deselected`。
- REGRESSION: `powershell -ExecutionPolicy Bypass -File script/deploy/restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS，新闸门未误拦当前环境。
- REGRESSION: `curl.exe -sS http://127.0.0.1:48081/actuator/health` -> PASS，响应 `{"status":"UP"}`。
- REGRESSION: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-local-e2e-showroom-file-config-guard --mode preview` -> PASS，预览仅保留 `task.md` 与 `execution-log.md`。

## Remaining Blockers

- 无。
