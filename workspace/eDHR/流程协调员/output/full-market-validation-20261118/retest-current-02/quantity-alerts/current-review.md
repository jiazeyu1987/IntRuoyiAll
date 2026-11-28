# 问题四当前复测：源码、单测与 SSR 辅助证据

1. 本 Agent 只读检查当前源码并执行辅助渲染测试；未写业务数据、未调用 API/DB、未创建周期或办理放行。真实前端新增对象与数量三场景由 batch_identity Agent 串行执行，其结果需另行合并，不能用本文 SSR 冒充 E2E。

2. 与上一轮审查记录比对，以下关键实现未见变化：TeamLeaderWorkbenchPage.vue:1784/1799 为数量、参数两个独立 v-if；:5880 背景类数量优先；MesTeamLeaderActiveOrderServiceImpl.java:3508-3576 按当前分配事实计算告警；:3667-3681 最大分配量减冻结上限；MesProFrontlineFeedbackSubmitServiceImpl.java:181-188,222-230 保存原始提交 quantityWarning。当前组件 SHA256=b3fdc0f9a72984e0a19bd47bea8b84a776ff9bfe290a9370360a119e892a8942。

3. 现有 tests/unit/active-order-parameter-warning.test.cjs 仍仅3项，与上一轮所读内容一致。没有新增实际双标签一起渲染的断言。当前重新执行 3/3 PASS，日志 current-unit-tests.log。其 coexist 名称实际只检验背景类优先级，不能证明两个标签同时可见。

4. 本次新增独立辅助脚本 verify-current-flags.cjs，从未改动真实 Vue 页面提取完整订单号单元格模板，使用 Vue SSR 和真实 Element Plus ElTag，在内存传入四组 flag。数量单独、参数单独、两者同时、两者均无，4/4 PASS；both.html 中两种标签同时存在。证据 ssr-results.json 及四份 HTML。脚本没有调用系统接口，flag 是内存测试输入，不是业务测试数据。SSR不证明浏览器布局、标签未被裁切、后端实际告警字段为真或真实数据已保存。

5. 数量口径仍分离：提交时 productionQuantityWarning 基于前量+本次原始产出；活跃池 quantityConflict 基于当前 CURRENT 分配量/物料比例数量。未分配量不另计入列表。MesOutputMaterialProgressCalculator.java:55-58 无输出物料时直接加 allocatedQuantity，:155-167 有输出物料时按 outputQuantity * allocatedQuantity / reportOutputQuantity，:200-230 仅汇总订单工序匹配分配。故报5501但只分5000、余501时不能期待列表必有数量告警。是否持续展示原始超报属设计确认。

6. 参考上限由 MesProductionQuantityRange.java:11-19 冻结ERP数量乘冻结损耗系数；MesRouteLossCoefficient.java:9-20 定义1.1业务默认。新对象必须前端确认实际冻结范围，不能假定每单上限5500。不要更改全局规则或工艺标准来制造异常。

7. 当前正式创建入口：生产组长活跃订单行“更多→复制测试单”，仅测试模式开启时显示（TeamLeaderWorkbenchPage.vue:1970-1981），点击真实确认弹框后 sourceActiveOrderId、自动唯一 simulationRunId 两项由页面提供（:10185-10216）。后端 maintain 权限（MesProcessPoolTeamLeaderController.java:460-476）；source须同一组长且ACTIVE（MesTeamLeaderActiveOrderServiceImpl.java:1305-1318）。上市关闭0626不符合该入口源条件，不能重新打开或重置它。现入口不能凭空创建无来源周期。

8. 复制影响范围：新增SELF工单、复制数量/产品/客户等，batchCode为新copyCode，确认工单，addActiveOrder绑定最新工艺及QA版本，写模拟元数据及审计（service:1326-1373）。不复制原报工/进度/PQC/放行事实，但会新增真实持久化测试对象与后续待办。源须由主Agent确认适用且授权；代码支持不等于本轮已获可用对象。

9. 本轮已收到主Agent转达授权创建任务专有模拟对象及必要提交，禁止重置0626及放行。已将操作方案发给唯一页面执行者 batch_identity；本Agent不并行操作页面或写数据。

10. 最小真实三场景：3个独立新周期 Q/P/QP，各5000，页面核对U及精洗次数范围后，优先同工序精洗输入 Q=U+1且次数1，P=5000且次数0，QP=U+1且次数0。前提真实参数显示次数>=1，1正常且0异常。如U=5500则超量5501。必要前置按真实页面办理，不用P1自动生成全链数据。不得同周期先制造异常后删除充当正常。

11. Q和QP正式分配须保持U+1给本单，使列表当前分配也超限；若只分配5000余501，则测试的是“原报工超限+当前分配未超限”，应另行记录，不能用来验列表双标签。三组分别保存真实记录与分配、刷新列表、截图两种标签与背景，记录浏览器宽度/列宽，查看历史保存原值。仅数量、仅参数、组合都应可辨识且不拦截必要保存。不得办理放行以证明不拦截；本轮授权不含放行。

12. 未测项余501跨单分配仍需指定适用接收对象；只创建数量三组合周期并不自动授权将其他任务订单作为接收者。源和接收归属、同产品规格项目版本、总分配不超报工量及保存后两边读回须各自完成才可通过。
