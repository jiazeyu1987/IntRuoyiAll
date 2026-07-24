# Execution Log：修复 ERP 测试单重复校验字段兼容

RED: git show HEAD:yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/sync/ErpKingdeeProductionOrderClientImpl.java | rg -n "FIssueType|F_PAEZ_Remark1|throwIfQueryReturnedEmbeddedError|BILL_LOOKUP_FIELD_KEYS" -> FAIL，HEAD 基线仍携带 FIssueType/F_PAEZ_Remark1 等不兼容字段，且缺少按 billNo 场景专用 FieldKeys 与嵌套 ExecuteBillQuery 错误暴露。
GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-erp "-Dtest=ErpKingdeeProductionOrderClientImplTest" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，10 个 ERP 生产订单客户端用例通过，覆盖未完成订单查询、按单号查询、嵌套元数据错误暴露、创建前重复校验等场景。

- `2026-06-30 任务创建`：建立后端任务文档，收口当前未提交的 ERP 客户端兼容修复并准备按任务边界提交。
- `BDD: ERP 测试单重复校验只请求稳定字段 -> Given 创建 ERP 测试单前需要按单号检查 PRD_MO 是否已存在 / When 后端执行重复校验查询 / Then 请求字段只包含 FBillNo，不再携带 FIssueType 等环境不兼容扩展字段。`
- `BDD: 单号查单只请求当前场景必需字段 -> Given 后端需要按 billNo 查询生产订单基础信息 / When 调用 ExecuteBillQuery / Then 请求字段只包含查单必需字段，不依赖冲领料、助记码、业务状态、图号、排产状态等扩展字段。`
- `BDD: 金蝶返回嵌套元数据错误时必须暴露真实原因 -> Given 金蝶 ExecuteBillQuery 返回数组包裹的 ResponseStatus 错误对象 / When 后端解析查询结果 / Then 直接抛出包含真实错误消息的异常，而不是继续按成功数组解析。`
- `RED: git show HEAD:yudao-module-erp/src/main/java/cn/iocoder/yudao/module/erp/service/purchase/sync/ErpKingdeeProductionOrderClientImpl.java | rg -n "FIssueType|F_PAEZ_Remark1|throwIfQueryReturnedEmbeddedError|BILL_LOOKUP_FIELD_KEYS" -> FAIL，HEAD 基线仍携带 FIssueType/F_PAEZ_Remark1 等不兼容字段，且缺少按 billNo 场景专用 FieldKeys 与嵌套 ExecuteBillQuery 错误暴露。`
- `READONLY: 当前工作树 diff 与分支 paichan_yanzheng_mubiao 上提交 9a94ea1d07 对照 -> PASS，未提交 ERP 客户端改动与既有正式修复范围一致，聚焦 PRD_MO 查单字段兼容与嵌套错误暴露。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-erp "-Dtest=ErpKingdeeProductionOrderClientImplTest" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，10 个 ERP 生产订单客户端用例通过，覆盖未完成订单查询、按单号查询、嵌套元数据错误暴露、创建前重复校验等场景。`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260630-erp-test-order-duplicate-check-field-compat --mode preview -> PASS，仅保留 task.md 与 execution-log.md，无需删除额外任务产物。`
