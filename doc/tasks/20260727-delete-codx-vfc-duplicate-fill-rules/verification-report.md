# Verification Report

## Scope

- 本地环境：`http://127.0.0.1:8081` / `http://127.0.0.1:48081`。
- 本地数据库：Docker `int-ruoyi-mysql`，业务库 `ruoyi-vue-pro`。
- 租户：`1`。
- 产品：`CODX-VFC-20260726-批记录`。
- 版本：`V1.0`，批记录版本 ID `134`。
- 表单：`粗洗工序生产记录`。
- 报表 ID：`249d8d8d9b3f4041a3e71951bf603a19`。

## Before State

- 启用 `FILL` 规则总数：`87`。
- 贾泽宇（用户 `795`）：`44` 条。
- 王歆（用户 `810`）：`43` 条。
- 全部规则均为 `CODX_VFC_ASSIST_1..87` 辅助行数据，没有正式
  `scope_key=ALL` 规则。
- 接口因 Mapper `selectOne` 命中 87 条而抛出
  `TooManyResultsException`，页面显示“填写人加载失败”。

## Backup And Repair

- 删除前快照：
  `doc/tasks/20260727-delete-codx-vfc-duplicate-fill-rules/before-87-rules.sql`。
- 快照 SHA-256：
  `D8EC21C8CAF756BD6D73CC738D3A0359594702F16D069976DE0D98F792414C05`。
- 保留载体：最早的王歆规则 `id=1033`。
- 事务结果：更新 `1` 条、删除 `86` 条、剩余 `1` 条。
- 保留规则规范为：
  `scope_key=ALL`、`candidate_source_type=USERS`、
  `candidate_source_ids=810`、`completion_policy=ANY_ONE`、
  `due_minutes=2147483647`。

## Final Verification

- 数据库目标范围最终数量为 `1`，唯一记录 ID 为 `1033`。
- 用户 `810` 在租户 `1` 下昵称为“王歆”，状态启用、未删除。
- 临时存储过程
  `codex_repair_codx_vfc_fill_rules_20260727` 已删除，数量为 `0`。
- 登录态接口返回 HTTP `200`、业务码 `0`，填写人数组唯一值为
  `{userId:810, displayName:"王歆"}`。
- 真实 Playwright 页面目标行显示：
  `CODX-VFC-20260726-批记录 / 粗洗工序生产记录 / 已配置 王歆 / V1.0`。
- 本次未访问远端环境，未修改生产代码或并行任务文件。

## Result

PASS。用户指定的 86 条多余规则已删除，只保留 1 条王歆正式填写人规则。
