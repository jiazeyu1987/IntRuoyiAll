# Execution Log

BDD: 详情页展示生产用料清单 -> Given 当前活跃订单对应的生产订单号在 ERP 生产用料清单列表中存在一张或多张正式单据 When 用户打开活跃订单工序提交详情页并切换到“生产用料清单”tab Then 页面按单据逐张展示单据头、明细表和制单审核信息，明细来自生产用料清单正式列表，不得使用领料单、补料单或 BOM 推断。

REGRESSION: worktree setup -> PASS，创建 `D:\IntRuoyiWorktree\production-material-list-tab`，分支 `codex/20260908-production-material-list-tab`，预约 `int_main slot=32`，前端端口 `8207`，后端端口 `48207`。

RED: node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-material-list-tab-static.spec.cjs -> FAIL，详情页尚未按当前显示生产订单号调用 ERP 生产用料清单正式分页接口，也未向展示面板传递生产用料清单数据。

GREEN: node IntRuoyiFronted\tests\e2e\team-leader-active-order-production-material-list-tab-static.spec.cjs -> PASS。确认详情页新增“生产用料清单”主 tab，按生产订单号调用 ERP 生产用料清单正式分页接口，向面板传递数据，面板按 `sourceBillNo` 分组展示，不读取领料单、补料单或 BOM 作为替代来源。

GREEN: node IntRuoyiFronted\node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p IntRuoyiFronted\tsconfig.relaxed.json -> PASS。前端类型检查通过。

GREEN: .\scripts\runtime\start-branch-backend.ps1 -Slot 32 -Build -> PASS。worktree 后端 Maven 构建成功并在 `48207` 启动。

GREEN: PML_E2E_WORK_ORDER_CODE=KDMO-309748-1416202028 node doc\tasks\20260908-production-material-list-tab\production-material-list-tab-real.e2e.cjs -> PASS。Playwright 通过真实前端登录、进入生产组长页签、打开活跃订单详情、切换“生产用料清单”tab；接口 `/admin-api/erp/production-material-list/page?productionOrderNo=KDMO-309748-1416202028` 返回 1 条，页面显示单据 `SIM-PML-150-20260821`。

BLOCKED: PML_E2E_WORK_ORDER_CODE=SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891 node doc\tasks\20260908-production-material-list-tab\production-material-list-tab-real.e2e.cjs -> FAIL，预期原因：正式生产用料清单接口对该生产订单返回 `productionMaterialListRowCount=0`。按无 fallback 规则，页面不能用 BOM、领料单、补料单或默认数据冒充生产用料清单。

GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260908-production-material-list-tab\frontend-feature-evidence.md -> PASS。前端功能证据文件结构有效。

DATA CHANGE: 用户授权复制本地库生产用料清单测试数据。目标工单 `SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891` 产品编码 `IDI`，产品名 `按压式球囊扩充压力泵`。未找到 `product_code=IDI` 的正式生产用料清单；选择同类“球囊扩张压力泵”来源 `881MO101365 / 881PPBOM00001983`。复制时创建新单据号 `SIM-PML-CODX-PQC-WO05-001`，并将 `production_order_no/work_order_code/work_order_id/product_code/product_id` 同步为目标工单。

RED: SELECT COUNT(*) FROM mes_kingdee_production_material_list WHERE production_order_no='SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891' -> 0。目标订单复制前没有正式生产用料清单。

GREEN: INSERT INTO mes_kingdee_production_material_list SELECT ... FROM source `881MO101365 / 881PPBOM00001983` -> PASS。初次插入 44 行，随后按目标工单租户清理非目标租户 33 行，最终保留租户 1 的 11 行。

GREEN: PML_E2E_WORK_ORDER_CODE=SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891 node doc\tasks\20260908-production-material-list-tab\production-material-list-tab-real.e2e.cjs -> PASS。真实页面显示目标订单生产用料清单，接口返回 `productionMaterialListRowCount=11`，单据号 `SIM-PML-CODX-PQC-WO05-001`。

GREEN: project-experience-consolidation -> PASS。已将 worktree 端口误看、已登录重定向和 Element Plus 隐藏 DOM 的 E2E 经验合并到 docs\e2e-rules.md。
