# Execution Log：生产订单与生产用料清单双向关联展示（后端）

- `2026-06-30 任务创建`：建立后端任务文档，准备按后端接口合同与严格 TDD 实现双向关联字段。
- `BDD: 生产工单接口返回生产用料清单摘要 -> Given 某生产工单对应一张或多张生产用料清单 / When 查询生产工单列表 / Then 返回可用于前端展示的生产用料清单单据号摘要与数量。`
- `BDD: 生产工单无关联时返回空结果 -> Given 某生产工单没有生产用料清单 / When 查询生产工单列表 / Then 接口返回空摘要而不是伪造默认值。`
- `BDD: 生产用料清单分组接口返回对应生产工单 -> Given 某生产用料清单已映射本地生产工单 / When 查询生产用料清单分组或明细 / Then 返回 workOrderId/workOrderCode 等对应字段。`
- `BDD: 生产用料清单未映射时保留空关联 -> Given 某生产用料清单未映射本地工单 / When 查询接口 / Then workOrderId/workOrderCode 为空，保持真实状态。`
- `RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListQueryServiceImplTest,MesProWorkOrderControllerTest" -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL`，缺少生产工单摘要字段与生产用料清单关联字段。
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListQueryServiceImplTest,MesProWorkOrderControllerTest" -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`
- `GREEN: 本地真实数据扫描 -> PASS，tenant_id=122 下分页扫描 987 条生产工单，当前 productionMaterialListCount>0 样本数为 0；扫描生产用料清单前 10 页明细，当前 workOrderId 非空样本数为 0。后端字段行为正确，但本地测试租户当前暂无已同步出的真实双向关联样本。`
