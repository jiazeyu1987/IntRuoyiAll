# Verification Report

## Result

PASS：QA 规程“适用检验类型”已改为正式派生数据。上午巡检、下午巡检固定包含；首检及首检数量来自当前检验项目抽样方案；巡检比例使用抽样方案中的 AQL 百分比原值；末检只受顶部开关控制。

## Automated Verification

- 聚焦派生合同 -> PASS。
- 默认列、产品规则、末检适用性、球囊压力泵 PDF 项目和完整项目相邻合同 -> PASS。
- 页面标题栏和角色矩阵相邻合同 -> PASS。
- `pnpm ts:check` -> PASS。
- 保存链路语义核对 -> PASS；前端 `patrolInspectionRatio=AQL` 与后端 `plannedQuantity × ratio ÷ 100` 公式一致。

## Real Browser Verification

- 运行态：`int_main` 前端 `http://127.0.0.1:8081` HTTP 200，后端 `http://127.0.0.1:48081/actuator/health` 为 `UP`。
- 路径：`/mes/pro/process-pool/qa-regulation`，精确选择 `ID / 球囊扩张压力泵 / 112`，进入“检验项目”。
- 末检开启：无首检抽样方案显示“上午巡检、下午巡检、末检”；含“首件：13 件”的抽样方案显示“首检、上午巡检、下午巡检、末检”。
- 末检关闭：上述两类行均移除“末检”，首检和默认双巡检保持不变。
- 安全与稳定性：MES 写请求 0，目标请求失败 0，console error 0，pageerror 0。

## Scope Confirmation

- 未改变抽样方案原文、后端 API 结构或末检数量规则。
- 未保存、发布或修改正式 QA 规程数据。
- 未修改、清理或提交无关并发任务内容。

## Blockers

- 无。

## Closeout

- 状态：`completed`。
- `task-closeout-cleanup` preview/apply 均 PASS；已删除任务自有临时 E2E 脚本、截图、结果和交付证据，保留生产代码、正式回归测试及三份核心任务文档。
- 长期经验已合并到 `docs/backend-development.md#QA-抽样方案与适用检验类型必须共用项目级正式来源`，并更新经验索引，未新建重复文档。
- 未执行 Git 提交、合并或推送。
