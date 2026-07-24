# Execution Log：MES 缺料判定固定使用生产用料清单

- `2026-06-30 任务创建`：建立后端任务文档，目标是把自动排产缺料判定固定改为 ERP 生产用料清单口径。
- `BDD: 缺料判定固定使用生产用料清单 -> Given 自动排产计算某批生产工单缺料 / When 生成物料需求与短缺问题 / Then 需求量必须来自 mes_kingdee_production_material_list.requiredQuantity，而不是本地工单 BOM 展开。`
- `BDD: 生产用料清单缺失时 fail-fast -> Given 某生产工单存在有效排产范围但没有已映射的生产用料清单 / When 执行自动排产预览或应用 / Then 系统必须生成明确问题，不允许静默回退到本地 BOM 口径。`
- `BDD: 生产用料清单重复物料按物料汇总 -> Given 同一生产工单的生产用料清单存在多个相同 childMaterialId 分录 / When 计算缺料 / Then 系统按 childMaterialId 汇总 requiredQuantity 后与库存台账比较，并输出 requiredQty/availableQty/shortageQty。`
- `RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProAutoScheduleServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL，当前分支存在与本任务无关的审批中心编译错误（ApprovalTaskQueryContext / ApprovalTaskTimelineQueryContext 缺少 isGlobalView），测试尚未进入本次自动排产用例。`
- `GREEN: 局部实现自检 -> PASS，已把 MesProAutoScheduleServiceImpl 缺料需求来源切换为 MesKingdeeProductionMaterialListMapper.selectListByWorkOrderIds，并新增生产用料清单缺失/未映射/应发数量缺失阻塞逻辑。`
- `GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.includes=**/MesProAutoScheduleServiceImpl.java,**/MesKingdeeProductionMaterialListMapper.java,**/ErrorCodeConstants.java -DskipTests compile -> PASS`
- `GREEN: 代码链路复核 -> PASS，MesProAutoScheduleServiceImpl 的 preview/replanPreview/apply/replanApply 均走生产用料清单口径；但 MesProScheduleCalendarServiceImpl:642 仍调用 workOrderBomService.getWorkOrderMaterialDemandMapByWorkOrderIds(...)，因此当前只能确认“自动排产缺料判定”已全部切换，不能确认“排程相关全部缺料分析”已全部切换。`
- `BDD: 排程日历缺料汇总也固定使用生产用料清单 -> Given 排程日历按天展示物料占用与缺料汇总 / When 构建每日物料需求行与剩余库存 / Then 需求量必须来自 mes_kingdee_production_material_list.requiredQuantity，而不是本地工单 BOM 展开。`
- `GREEN: schedule 链路残留检索 -> PASS，当前 schedule 相关源码与测试已无 workOrderBomService / getWorkOrderMaterialDemandMapByWorkOrderIds 缺料口径残留。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.includes=**/MesProScheduleCalendarServiceImpl.java,**/MesProAutoScheduleServiceImpl.java,**/ErrorCodeConstants.java -DskipTests compile -> PASS`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.useIncrementalCompilation=false -Dmaven.compiler.includes=**/MesProScheduleCalendarServiceImpl.java,**/MesProAutoScheduleServiceImpl.java,**/ErrorCodeConstants.java -Dmaven.compiler.testIncludes=**/MesProScheduleCalendarServiceImplTest.java,**/MesProAutoScheduleContractTest.java,**/MesProAutoScheduleAlgorithmContractTest.java -DskipTests test-compile -> PASS`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProScheduleCalendarServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS`
- `BLOCKER: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dtest=MesProAutoScheduleContractTest#preview_shouldIgnoreMissingRouteScheduleOrderAndKeepAutoSchedulingReadyOrders -Dsurefire.failIfNoSpecifiedTests=false test -> 失败，当前服务既有范围校验会对请求中不可自动排产/不存在的排产工单直接 fail-fast，而该旧 contract test 仍期望忽略此类请求；该冲突为既有行为与测试预期不一致，不属于本次生产用料清单口径切换新增问题。`
- `BLOCKER: task-priority-switch -> 用户当前线程已切换到“排程日历正式排程为空空态回归修复”，本任务保留现有编译/测试阻塞证据并显式暂停，待后续恢复优先级后继续。`
