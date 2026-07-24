# 执行日志：eDHR 第一张模板乱码诊断

## 2026-06-26

- 初始化任务：用户反馈 eDHR 批次模板说明页中第一张模板静态表单文本显示为 `????`，需要先确认是前端渲染问题还是模板源数据损坏。
- BDD: 第一张模板应显示原始中文静态文本 -> Given 用户打开 eDHR 批次模板说明页第一张模板 When 页面渲染原始模板布局 Then 表头和静态单元格应显示可读中文，不应显示 ?????。
- BDD: 乱码归因必须区分渲染层与源数据层 -> Given 第一张模板的用途提示仍能正常显示中文 When 对比模板静态文本与接口/数据库原始布局 Then 能明确判断乱码是否已存在于源数据。
- CHANGE：读取用户截图，确认蓝色用途提示 `文字/数字` 正常，只有模板原始静态文本异常，初步排除新加提示层的中文编码问题。
- CHANGE：复查 `BatchExecutionTemplatePage.vue` 与 `EdhrExecutionTemplateGuide.vue`，确认模板页只是直接渲染 `getCellRules/getSignatureCellMarkers` 返回的 `sheetLayoutJson.rows[*].cells[*].text`，没有把中文替换成问号的前端逻辑。
- CHANGE：从 `docs/request-command-log.md` 命中历史 live 演练记录，锁定第一张模板 `reportId=81ed39c646b64251827a3eb8860676b7`，并确认 2026-06-22 曾对该单模板执行过一次启发式规则确认与定向 SQL 写回。
- CHANGE：只读查询本机 MySQL `jimu_report`，确认 `id=81ed39c646b64251827a3eb8860676b7` 当前仍存在，`code=EBR_TN1_A_T01`，`name=电子批记录[A]-表1-产品信息`。
- CHANGE：执行 `SELECT LOCATE('????', json_str), LOCATE('产品信息', json_str), LOCATE('吹球囊成型', json_str), LOCATE('RE-PP-ID-01', json_str)`，结果为 `1364, 0, 0, 27034`，说明 live `json_str` 内部确实已经存在问号文本，而中文 `产品信息`、`吹球囊成型` 已经不存在。
- CHANGE：截取 `json_str` 中 `RE-PP-ID-01` 附近原文，确认数据库当前内容包含 `"text":"???????????"`、`"text":"????"`、`"label":"????"` 等问号文本，证明乱码早于前端渲染发生。
- CHANGE：读取 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260622-edhr-director-live-rehearsal\apply-heuristic-reviewed-jimu-report-81ed39c646b64251827a3eb8860676b7.sql`，确认同一模板的离线恢复 SQL 文件中仍是正确中文，例如 `球囊扩张压力泵生产记录`、`产品信息`、`生产指令`。
- CHANGE：读取当前 `jimu_report.update_time/update_by`，结果为 `2026-06-23 09:42:02 / 瑛泰管理员`，与 2026-06-22~2026-06-23 的单模板 live 写回时间线一致。
- 结论：第一张模板显示乱码不是这次“模板说明页”前端改造导致，而是模板源数据 `jimu_report.json_str` 已经被持久化成问号；页面只是把坏数据原样展示出来。
- 用户追加授权：`直接修`，允许继续修复这张本机 live 模板数据。
- GREEN: experience-preflight -> PASS，本轮仅对本机 Docker MySQL 中单张 `jimu_report` 执行定向恢复；已确认恢复源文件、目标 `reportId`、风险边界和回滚基础，不涉及服务器、测试服或其他租户数据。
- RED: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-first-template-garbled-diagnosis\scripts\verify-edhr-first-template-copy.py` -> FAIL，当前 live `jimu_report` 仍包含 `????`，且缺少 `球囊扩张压力泵生产记录 / 产品信息 / 生产指令 / 记录人/日期`。
- CHANGE：确认恢复源采用 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260622-edhr-director-live-rehearsal\heuristic-reviewed-jimu-report-81ed39c646b64251827a3eb8860676b7.json`，该文件为 UTF-8 正常中文，且保留 `123` 个 reviewed 规则，不会把模板恢复到“未确认规则”旧状态。
- CHANGE：新增定向校验脚本 `scripts\verify-edhr-first-template-copy.py`，固定校验 live `reportId=81ed39c646b64251827a3eb8860676b7` 不含 `????` 且包含关键中文标题。
- CHANGE：生成恢复文件 `restore-jimu-report-81ed39c646b64251827a3eb8860676b7.sql`，仅更新目标 `jimu_report.id=81ed39c646b64251827a3eb8860676b7` 的 `json_str/update_by/update_time/update_count`。
- CHANGE：通过 `docker cp` + `docker exec ... mysql < /tmp/restore-jimu-report-81ed39c646b64251827a3eb8860676b7.sql` 导入恢复 SQL，结果 `affected_rows = 1`。
- GREEN: `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-first-template-garbled-diagnosis\scripts\verify-edhr-first-template-copy.py` -> PASS，live 模板已恢复关键中文且不再包含 `????`。
- GREEN: `SELECT LOCATE('????', json_str), LOCATE('产品信息', json_str), LOCATE('球囊扩张压力泵生产记录', json_str), LOCATE('生产指令', json_str), LOCATE('记录人/日期', json_str) FROM jimu_report WHERE id='81ed39c646b64251827a3eb8860676b7';` -> PASS，返回 `0, 1353, 1089, 3289, 71349`。
- GREEN: `SELECT update_time, update_by, update_count FROM jimu_report WHERE id='81ed39c646b64251827a3eb8860676b7';` -> PASS，返回 `2026-06-26 16:11:28 / codex / 3`。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path "/mes/pro/feedback/edhr-batch-execution/template?id=900000000397" --target-text "球囊扩张压力泵生产记录" --timeout 120000` -> PASS，真实登录已进入模板说明页并命中修复后的中文标题。
