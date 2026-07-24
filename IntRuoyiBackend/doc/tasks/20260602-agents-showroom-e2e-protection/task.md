# AGENTS 展厅 E2E 文件配置保护基线

## Task Goal

将“E2E 不得修改展厅默认文件配置和默认媒体桶”的长期约束写入 IntRuoyi 仓库 `AGENTS.md`，让后续人员与 Agent 在执行前即可看到该保护边界。

## Previous Task Check

- 上一个后端任务 `doc/tasks/20260602-local-e2e-showroom-file-config-guard/task.md` 已标记 `completed`。
- 当前任务只更新仓库根 `AGENTS.md` 和本任务文档，不接管其他未提交改动。

## BDD

BDD: 仓库基线必须声明展厅默认文件配置为受保护资源 -> Given 后续 Agent 或人工执行本机 E2E / When 准备修改文件配置、默认媒体桶或 showroom 直链记录 / Then `AGENTS.md` 必须明确禁止修改 `infra_file_config.id=28`、默认 bucket `yudao` 与默认域 `http://127.0.0.1:9000/yudao`。

BDD: 仓库基线必须要求发现漂移立即失败 -> Given 展厅默认文件配置或 `showroom/%` 媒体 URL 被切到非默认 E2E 桶 / When 后续任务执行 E2E、联调或启动前检查 / Then 规则必须要求 fail fast，不得继续运行或用同步临时修补掩盖。

## Milestones

- [x] M1: 建立任务文档并确认上一任务已完成。
- [x] M2: 更新 `AGENTS.md` 的 E2E 基线规则。
- [x] M3: 记录验证与结果。

## Expected Verification

- `AGENTS.md` 包含针对 `infra_file_config.id=28`、默认 bucket `yudao`、默认域 `http://127.0.0.1:9000/yudao` 的受保护约束。
- 本任务文档与执行日志完成更新。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅补充规则，不增加兜底。
- `是否从根因和长期维护角度解决`：是。通过仓库长期基线减少后续 E2E 或 Agent 再次污染默认展厅文件链路。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Completed Work

- 已建立任务文档。
- 已在仓库根 `AGENTS.md` 的 E2E 基线规则附近新增展厅默认文件配置保护约束。
- 已明确将 `infra_file_config.id=28`、默认 bucket `yudao`、默认域 `http://127.0.0.1:9000/yudao` 视为受保护资源。
- 已明确要求发现 `showroom/%` 默认媒体 URL 漂移到非默认 E2E 桶时必须 fail fast，不得继续启动、继续 E2E 或用临时同步掩盖。

## Verification Evidence

- `AGENTS.md` 已新增两条 `Thread baseline`：
  - 禁止未经授权修改本机展厅默认文件配置 `infra_file_config.id=28`、默认 bucket `yudao`、默认域 `http://127.0.0.1:9000/yudao`
  - 禁止将 `config_id=28` 且 `path LIKE 'showroom/%'` 的默认展厅媒体 URL 改写到非默认受保护域，并要求发现漂移时 fail fast
- 本任务文档与执行日志已完成更新。

## Remaining Blockers

- 无。
