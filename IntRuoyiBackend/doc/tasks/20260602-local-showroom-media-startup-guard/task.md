# 本机展厅媒体启动守卫

## Task Goal

在本机 IntRuoyi 后端启动脚本中加入展厅媒体读回守卫，防止后续 E2E 或本机配置变更导致图片、语音桶与运行时读取路径不一致后仍然启动成功。

## Previous Task Check

- 上一个本机展厅媒体任务 `doc/tasks/20260602-local-showroom-file-config-media-read/task.md` 已标记 `completed`。
- 当前仓库存在 DCC/infra 相关未提交改动和未跟踪任务目录，和本任务无关，本任务不接管、不回滚、不提交。

## BDD

BDD: 本机后端启动必须验证展厅图片读回 -> Given 本机后端启动脚本完成 Java 进程启动；When 脚本读取 `infra_file` 中 config 28 的展厅图片样本直链；Then 响应必须是 `image/*`，否则启动任务失败。

BDD: 本机后端启动必须验证展厅语音读回 -> Given 本机后端启动脚本完成 Java 进程启动；When 脚本读取 `infra_file` 中 config 28 的展厅语音样本直链；Then 响应必须是 `audio/*`，否则启动任务失败。

BDD: 缺少展厅媒体样本必须失败 -> Given 本机数据库缺少 config 28 的展厅图片或语音样本；When 启动脚本执行展厅媒体守卫；Then 脚本必须 fail fast，说明缺少的样本类型和影响。

## Milestones

- [x] M1: 建立任务记录，确认上一任务完成状态，并记录设计约束。
- [ ] M2: 添加失败的脚本回归测试，证明当前启动脚本缺少展厅媒体读回守卫。
- [ ] M3: 在本机后端启动脚本中加入健康检查和展厅媒体读回守卫。
- [ ] M4: 运行目标测试、证据校验和收尾预览。

## Expected Verification

- RED: `python -m pytest script/tests/test_runtime_control_scripts.py -q` 在新增守卫测试上失败。
- GREEN: 同一测试通过，脚本中存在后端健康等待和展厅图片/语音读回守卫。
- REGRESSION: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-local-showroom-media-startup-guard\execution-log.md`
- REGRESSION: `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc\tasks\20260602-local-showroom-media-startup-guard\execution-log.md`
- CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-local-showroom-media-startup-guard --mode preview`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。守卫失败直接阻塞本机后端启动任务，不返回假成功。
- `是否从根因和长期维护角度解决`：是。通过真实后端直链读回校验运行时桶、数据库路径、对象内容三者是否一致。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

## Completed Work

- 已建立任务文档。

## Verification Evidence

- 待记录。

## Remaining Blockers

- 无。
