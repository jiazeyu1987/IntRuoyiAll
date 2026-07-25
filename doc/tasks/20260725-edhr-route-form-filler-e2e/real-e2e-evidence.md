# eDHR 损耗单填写人真实 E2E Evidence

- Task ID: `20260725-edhr-route-form-filler-e2e`
- 状态：PASS
- 前端入口：`http://localhost:8081`
- 目标批次：`EDHRB-1784855561493`
- 登录身份标签：`芋道源码/admin`

## BDD

- BDD: 损耗单卡片显示单据填写人 -> Given 目标批次存在损耗单并配置填写人 `张可莹`, When 通过真实前端打开批次详情, Then 右侧当前工序损耗单卡片显示该填写人，详情接口对应任务 `fillableUsers` 非空。

## Result

- GREEN: `node doc/tasks/20260725-edhr-route-form-filler-e2e/readonly-filler-display.e2e.cjs` -> PASS。
- 批次 ID：`900000000778`。
- 命中任务：`粗洗工序 / 损耗单 / 损耗单 / 粗洗工序`。
- 接口填写人：`张可莹`。
- 页面可见卡片：`EDHRB-1784855561493 动态表单 待打开 损耗单 填写人 张可莹 前一张批记录未填写完成 打开填写`。
- 截图：`doc\tasks\20260725-edhr-route-form-filler-e2e\right-rail-loss-filler.png`。
- MES 写请求检查：详情页验证期间未发现 `/admin-api/mes/**` 非 GET 请求。
