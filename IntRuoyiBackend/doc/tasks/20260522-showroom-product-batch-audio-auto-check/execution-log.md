# 执行日志：展厅产品一键语音定时续跑（后端）

BDD: 一键语音只补当前筛选集中缺失的双语音频 -> Given 用户以当前筛选条件触发产品批量语音 / When 后端扫描命中产品 / Then 只处理已发布且当前发布版本中英讲解稿齐全但缺任一语音的产品，并继续保留当前筛选条件语义。

BDD: 已有双语音频和缺讲解稿产品都应跳过 -> Given 命中产品中既有双语音频已齐的产品，也有缺中文或英文讲解稿的产品 / When 后端执行批量语音检查 / Then 双语音频已齐产品计入“跳过已有语音”，缺稿产品计入“跳过缺讲解稿”，两者都不得被误记为成功生成。

BDD: 首轮中断后自动检查继续续跑 -> Given 用户已经启动一键语音自动检查且还有待补语音产品 / When 首轮执行过程中发生失败或服务重启 / Then 后端必须保留 enabled 状态和筛选快照，并在下一个 10 分钟检查周期继续处理剩余产品。

BDD: 无剩余可处理产品时自动关闭 -> Given 当前批次下所有可处理产品均已生成完成，或剩余命中产品都因缺讲解稿被跳过 / When 后端完成一轮自动检查 / Then 自动检查状态必须关闭，后续 10 分钟任务不再重复扫描该批次。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，现有实现仍是单次同步批量执行；没有自动检查状态持久化、缺稿跳过统计、重启续跑能力或停止条件。

GREEN: `mvn -pl yudao-module-showroom "-Dmaven.test.skip=true" compile` -> PASS，showroom 后端生产代码已通过编译，批量语音自动检查状态持久化、共享批次执行器、状态查询接口与定时调度器均已纳入主代码路径。

BLOCKED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`ShowroomProductNarrationRegressionTest` 中已存在的“批量讲解稿定时续跑”测试仍直接引用当前仓库未实现的方法（`startBatchGenerateNarrationScript` / `getProductBatchGenerateNarrationScriptStatus` / `runScheduledProductBatchNarrationScriptAutoCheck`），导致 `testCompile` 阶段失败；本次一键语音后端实现无法在当前仓库状态下完成整套 Maven 测试放行。
