# Execution Log: 工艺路线关联产品行复制

BDD: route_product_copy_clones_product_and_bom -> Given 用户在工艺路线编辑页打开关联产品 Tab / When 点击某行复制并选择新产品 / Then 新产品关联被创建，生产参数继承源行，源产品 BOM 配置同步复制到目标产品。

BDD: route_product_copy_rejects_invalid_target -> Given 用户复制关联产品 / When 未选择目标产品或目标产品已被其它路线关联 / Then 后端直接返回校验错误，前端不关闭弹窗且不伪造成功。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest,MesProRouteVersionAndCopyTest" test` -> FAIL, `MesProRouteProductCopyReqVO` 和 `copyRouteProduct` 服务能力不存在，新增测试无法编译。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest" test` -> PASS, 4 tests, 0 failures, 0 errors；覆盖成功复制产品+BOM、源关联不存在、目标产品重复、启用路线禁止复制。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProductServiceImplTest" test` -> PASS, 6 tests, 0 failures, 0 errors；补充覆盖关联产品新增、修改与复制三条路径。

GREEN: `mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.MesProRouteVersionAndCopyTest" test` -> PASS, 6 tests, 0 failures, 0 errors；原整条路线复制和版本复制回归未破坏。

GREEN: experience-preflight -> PASS, `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/route --target-text 路线编码 --timeout 90000` 已通过，真实登录进入工艺路线页面。

GREEN: `node tests/e2e/mes-pro-route-product-copy-real.e2e.js` -> PASS, 真实页面路径使用测试租户 `tenant_id=122`，在工艺路线 `ROUTE-XLSX-00002` 的“关联产品”Tab 点击行级“复制”，源产品 `YXN.069.001.1011` 复制到目标产品 `A002.09.002.230396`，新关联 `id=922103` 创建成功，生产参数继承源行，目标产品 BOM 数量为 26 条且与源产品 BOM 完全一致；验证后通过页面删除复制产生的关联产品行。

GREEN: cleanup-restore -> PASS, 通过真实页面确认并恢复验证候选路线 `ROUTE-XLSX-00002`，状态从 `1` 恢复为 `0`；本次 E2E 未遗留复制关联行或临时截图。

CHANGE: 新增 `POST /mes/pro/route-product/copy`，请求体包含 `sourceRouteProductId`、`targetItemId` 与可选生产参数；服务层复制源产品关联并同步复制源产品在当前路线下的 BOM 配置到目标产品。

GREEN: task-closeout-cleanup preview -> PASS, 仅保留任务核心文档、正式测试和生产代码；无本任务临时产物需要清理。
