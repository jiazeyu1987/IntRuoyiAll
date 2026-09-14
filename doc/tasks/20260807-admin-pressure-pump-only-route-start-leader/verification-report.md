# 验证报告

## 结果

PASS。tenant `1` 的 `admin` 当前仅在以下两条 active 工艺路线配置为“工序开始生产组长”：

- `627 / 922119 / RT000028 / 球囊扩张压力泵`
- `622 / 980091 / RT000028-IDI / 按压式球囊扩充压力泵`

其它四条当前 active 工艺路线均未配置 admin；直接 `USER/USERS` 与 admin 当前角色继承的 `ROLE` 两种有效命中均已覆盖。

## RED / GREEN

- RED：写入前 `directTargetRoutes=1`、`effectiveNonTargetRoutes=0`，按预期失败，证明 `922119` 缺少 admin 配置。
- BACKUP：version `627` 单行 `mysqldump --replace` 备份，`82860` bytes，SHA-256 `555F44E051E7196A613DCF74701BC50D587404CD19B5E0895BCA87CCBD26FC04`。
- GREEN：事务只更新 version `627` 一行，结果 `updated_rows=1`、`direct_target_routes=2`、`effective_non_target_routes=0`。
- 保持性：version `627` 除目标字段外的 JSON hash 不变；version `622` 完整 JSON hash 不变；其它 route version 未写入。

## 登录态与页面

- 官方登录前置：本机 `芋道源码/admin` 访问 `/mes/pro/route` PASS。
- 正式配置 API：6 条 active 路线逐路读取，目标两条各返回 1 条 admin，非目标四条各返回 0 条。
- 真实 Playwright：两条目标路线页面均显示 `瑛泰管理员（admin）`；非目标 `900025` 显示“暂无生产组长配置”。
- 浏览器副作用：MES 写请求 `0`，目标网络失败 `0`，page error `0`。
- 视觉检查：三张 `1440x900` 页面截图已人工复核，无配置错位、遮挡或错误状态。

## 范围边界

- admin 的 `mes:pro-process-pool-team-leader:maintain` 维护权限仍可能让维护列表展示全部 active 路线；这不是生产组长职责配置。本报告只按正式 `routeStartProductionLeaders` 证明职责范围。
- 未修改角色权限、非 active version、其它租户、`formBindings`、批记录表单、工序开始附件负责人、前后端生产代码或远端环境。
- 两条目标路线当前没有 DRAFT version；后续从当前 active 新建草稿时应继承配置。若已有旧草稿缺少该字段，发布前必须先补正式草稿配置。

## 恢复

- 备份：`db-backup/route-version-627-before.sql`。
- 仅在 version `627` 仍是 route `922119` 当前 active version 且没有后续合法发布/配置时，才可恢复完整行；否则必须停止并改用字段级恢复方案。

## 收尾门禁

- Database schema evidence validator：PASS，`Database schema evidence is valid.`。
- 最终数据库 GREEN：PASS；目标 active 两条、非目标 active 零命中，备份 SHA-256 不变。
- Cleanup preview/apply：PASS；保留核心任务文档、恢复备份和正式 SQL，删除临时 evidence、一次性 Playwright 脚本及截图目录，blocked/warnings 均为空。
