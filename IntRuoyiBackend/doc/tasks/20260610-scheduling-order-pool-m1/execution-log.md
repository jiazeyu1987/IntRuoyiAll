# 排产工单池 M1 执行日志

- BDD: 从生产工单生成唯一排产工单 -> Given 测试租户存在 ERP 同步生产工单且未生成有效排产工单 / When 排产员填写承诺交期并生成排产工单 / Then 系统创建排产工单，排产数量等于生产工单数量，并记录生产工单快照。
- BDD: 重复生成排产工单失败 -> Given 某生产工单已有未取消排产工单 / When 排产员再次从该生产工单生成排产工单 / Then 系统拒绝生成并提示同一生产工单不可拆分。
- BDD: 生成排产工单工序明细 -> Given 产品存在有效工艺路线和组成工序 / When 排产工单创建成功 / Then 系统按路线工序创建排产工单工序明细，保存工序、工位和产能快照。
- BDD: 缺少承诺交期失败 -> Given 生产工单存在 / When 排产员未填写承诺交期生成排产工单 / Then 系统拒绝创建排产工单。
- RED: `python -m pytest script\tests\test_mes_schedule_order_schema_sql.py` -> FAIL，预期原因：`sql/mysql/20260610_mes_schedule_order_schema.sql` 不存在。
- GREEN: `python -m pytest script\tests\test_mes_schedule_order_schema_sql.py` -> PASS，排产工单三张表、唯一约束、菜单权限和非破坏性约束契约通过。
- RED: `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderServiceImplTest test` -> FAIL，预期原因：排产工单 VO/DO/Mapper/Service 尚不存在。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderServiceImplTest test` -> PASS，覆盖从生产工单生成排产工单、生成工序明细、重复生成失败。
- RED: `node tests\e2e\mes-schedule-order-pool-static.spec.js` -> FAIL，预期原因：前端排产工单 API 和页面不存在。
- GREEN: `node tests\e2e\mes-schedule-order-pool-static.spec.js` -> PASS，前端 API、排产工单池页面、承诺交期、生产工单ID和不可拆分提示契约通过。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。首次不加内存参数运行因 Node heap out of memory 退出，未输出类型错误。
- GREEN: 本机 Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro` 应用 `sql/mysql/20260610_mes_schedule_order_schema.sql` -> PASS。旧版草稿表已平滑升级，未删表；三张表存在，新字段缺失数 `0`，旧排产工单回填缺失数 `0`，菜单 `mes:pro-schedule-order:query` 指向 `mes/pro/schedule-order/index`。
