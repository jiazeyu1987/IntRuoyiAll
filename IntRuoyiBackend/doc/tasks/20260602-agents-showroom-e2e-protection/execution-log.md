# Execution Log

## BDD

BDD: 仓库基线必须声明展厅默认文件配置为受保护资源 -> Given 后续 Agent 或人工执行本机 E2E / When 准备修改文件配置、默认媒体桶或 showroom 直链记录 / Then `AGENTS.md` 必须明确禁止修改 `infra_file_config.id=28`、默认 bucket `yudao` 与默认域 `http://127.0.0.1:9000/yudao`。

BDD: 仓库基线必须要求发现漂移立即失败 -> Given 展厅默认文件配置或 `showroom/%` 媒体 URL 被切到非默认 E2E 桶 / When 后续任务执行 E2E、联调或启动前检查 / Then 规则必须要求 fail fast，不得继续运行或用同步临时修补掩盖。

## TDD Evidence

- STATUS: task-created -> 已建立 AGENTS 展厅 E2E 文件配置保护基线任务，下一步更新仓库规则。
- GREEN: `AGENTS.md` -> PASS，已新增“`infra_file_config.id=28` / bucket `yudao` / domain `http://127.0.0.1:9000/yudao` 为受保护资源”的长期基线。
- GREEN: `AGENTS.md` -> PASS，已新增“`config_id=28` 且 `path LIKE 'showroom/%'` 的默认媒体 URL 漂移时必须 fail fast”的长期基线。
