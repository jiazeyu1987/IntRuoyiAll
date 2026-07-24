# Execution Log：修复本地生产订单与生产用料清单缺少真实关联样本（后端）

- `2026-06-30 任务创建`：建立后端任务文档，准备按严格 TDD 定位并修复真实样本缺失。
- `BDD: productionOrderNo 能映射本地工单时写入关联字段 -> Given 本地已存在 code 与生产用料清单 productionOrderNo 匹配的生产工单 / When 执行生产用料清单同步 / Then 记录写入 workOrderId/workOrderCode，前端查询可见。`
- `BDD: 已写入的关联字段能统计到生产工单页 -> Given 生产用料清单记录已写入 workOrderId / When 查询生产工单分页 / Then 返回 productionMaterialListCount 和 productionMaterialListSummary 非空。`
- `BDD: 无匹配时保留空关联并暴露原因 -> Given 生产用料清单 productionOrderNo 在本地不存在匹配工单 / When 执行同步 / Then 关联字段保持为空且回归测试能说明失败原因。`
- `GREEN: 真实库复现 -> PASS，测试租户 tenant_id=122 下已有 1147 条 workOrderId 非空的生产用料清单记录，涉及 75 个不同工单；SMART-SCHED-20260630-RERUN9-MO 对应 PPBOM00308992。`
- `GREEN: 根因定位 -> PASS，代码与数据链路未丢失，缺陷根因是本地后端运行态损坏：yudao-server-exec.jar 曾为 corrupt zip，restart 脚本无法启动最新 backend，导致 48081 对外不可用或仍暴露旧结果。`
- `RED: 本地运行态恢复 -> FAIL，powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\script\deploy\restart-ruoyi-local-component.ps1 -Component backend -SkipBuild 首次失败，报 Invalid or corrupt jarfile / End Of Central Directory does not correspond to number of entries。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package -> PASS`
- `GREEN: 本地运行态恢复 -> PASS，重新打包后执行 restart-ruoyi-local-component.ps1 -Component backend -SkipBuild 成功，48081 actuator/health 返回 UP。`
- `GREEN: 真实接口回归 -> PASS，测试租户 122 下 /mes/pro/work-order/page、/erp/production-material-list/group-page、/detail-list 均返回真实双向关联样本。`
