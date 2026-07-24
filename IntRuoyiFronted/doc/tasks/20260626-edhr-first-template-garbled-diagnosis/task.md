# 任务：eDHR 第一张模板乱码诊断

## 任务目标

- 查明 eDHR 批次模板说明页中“第一张模板”静态表单文本显示为 `????` 的真实原因。
- 区分问题属于前端模板渲染链路，还是模板源数据 / 数据库中的原始 `sheetLayoutJson` 已经损坏。
- 在不做未授权 live 数据写入的前提下，给出后续可执行修复路径。

## 当前状态

已完成

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-batch-template-preview\task.md`
- 状态：`已完成`
- 处理：模板入口与模板说明页实现任务已完成，不阻塞本次乱码诊断。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本次仅做只读诊断，不做真实登录写入、不做服务器操作、不修改测试或正式租户业务数据。
  - 不允许通过前端 fallback、占位文案或静默降级掩盖模板原始文本损坏；若源数据已损坏，必须直接明确暴露根因。
  - 若后续修复涉及 tenant 1 的 live `jimu_report` 数据，必须先获得用户明确授权，再执行定向恢复。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。仅定位根因，不通过前端兜底文案掩盖数据问题。
- `是否从根因和长期维护角度解决`：是。优先核查模板源数据、历史写回链路和数据库原始 JSON，而不是只在前端渲染层打补丁。
- `是否存在临时补丁或绕过`：否。本次不修改 live 模板数据，不做临时替换。

## BDD 场景

- `BDD: 第一张模板应显示原始中文静态文本 -> Given 用户打开 eDHR 批次模板说明页第一张模板 When 页面渲染原始模板布局 Then 表头和静态单元格应显示可读中文，不应显示 ?????。`
- `BDD: 乱码归因必须区分渲染层与源数据层 -> Given 第一张模板的用途提示仍能正常显示中文 When 对比模板静态文本与接口/数据库原始布局 Then 能明确判断乱码是否已存在于源数据。`

## 里程碑

1. M1：收集用户截图、定位第一张模板对应的 `reportId` 与页面入口。
2. M2：核查 `/cell-rules`、历史任务证据和 `jimu_report.json_str`，确认乱码落点。
3. M3：如获用户明确授权，再执行受保护 live 模板数据恢复与回归验证。

## 预期验证

- `docker exec -e MYSQL_PWD=123456 int-ruoyi-mysql mysql --default-character-set=utf8mb4 -N -B -uroot -D ruoyi-vue-pro -e "SELECT LOCATE('????', json_str), LOCATE('产品信息', json_str), LOCATE('RE-PP-ID-01', json_str) FROM jimu_report WHERE id='81ed39c646b64251827a3eb8860676b7';"`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260622-edhr-director-live-rehearsal\apply-heuristic-reviewed-jimu-report-81ed39c646b64251827a3eb8860676b7.sql`
- `docker exec -e MYSQL_PWD=123456 int-ruoyi-mysql mysql --default-character-set=utf8mb4 -N -B -uroot -D ruoyi-vue-pro -e "SELECT update_time, update_by FROM jimu_report WHERE id='81ed39c646b64251827a3eb8860676b7';"`

## 最终验证结果

- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-edhr-first-template-garbled-diagnosis\scripts\verify-edhr-first-template-copy.py` -> PASS
- `docker exec -e MYSQL_PWD=123456 int-ruoyi-mysql mysql --default-character-set=utf8mb4 -N -B -uroot -D ruoyi-vue-pro -e "SELECT LOCATE('????', json_str), LOCATE('产品信息', json_str), LOCATE('球囊扩张压力泵生产记录', json_str), LOCATE('生产指令', json_str), LOCATE('记录人/日期', json_str) FROM jimu_report WHERE id='81ed39c646b64251827a3eb8860676b7';"` -> PASS，返回 `0, 1353, 1089, 3289, 71349`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password admin123 --target-path "/mes/pro/feedback/edhr-batch-execution/template?id=900000000397" --target-text "球囊扩张压力泵生产记录" --timeout 120000` -> PASS

## 当前阻塞

- 无。
