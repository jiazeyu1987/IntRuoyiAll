# 执行日志

BDD: NMPA 页面短横线日期与列表点分日期一致 -> Given 产品目录列表有效期为 `2027.9.8` 且注册证页面展示 `有效期：2027-09-08` / When 点击“注册证有效期”触发后端比对 / Then 接口返回 `MATCH`，本地与外站有效期均归一化为 `2027-09-08`，前端显示绿色。

BDD: 真实不同日期仍提示不一致 -> Given 列表有效期与注册证页面有效期年月日不同 / When 执行比对 / Then 接口返回 `MISMATCH`，前端显示红色。

INFO: task-gate -> 已读取 PowerShell 编码门禁、缺陷修复流程、bug evidence contract、项目经验索引和前端样式基线；本轮不执行高风险真实 E2E/服务器/数据库写入。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccProductCatalogRegistrationExpiryCompareServiceTest#compareRegistrationExpiryShouldReturnMatchForNmpaHyphenDateWhenLocalUsesDots" test` -> PASS
INFO: root-cause -> 后端日期归一化测试通过，但真实 NMPA 详情页服务端抓取返回 `HTTP 412`；红色误报来自前端把 `FETCH_FAILED` 也渲染成红色。
