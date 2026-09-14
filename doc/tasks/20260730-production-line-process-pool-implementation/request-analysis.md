# Request Analysis

## User Goal

用户要求以 `docs/acceptance/production-line-process-pool/` 已放行的 6 个功能点为依据，启动 6 个子 agent，分别在 6 个 worktree 中实现并验证，主线程 review，符合 21 条需求门禁后才放行，最终融合进 `int_main`。

## Current System

- 当前仓库根目录为 `E:\IntRuoyi`，主分支为 `int_main`。
- 后端 MES 模块已有生产报工、生产工单、批记录/eDHR、电子签名、工艺路线等相关能力。
- 前端已有 MES 报工、eDHR 批次执行、批记录表单、测试/E2E 脚本等相关入口。
- 已有报工余量池相关脚本和代码，但验收文档明确要求新增正式工序池，不能用现有 `mes_pro_feedback_surplus_pool` 替代。
- 已有 BDD/TDD/E2E/测试数据文档位于 `docs/acceptance/production-line-process-pool/`。

## Constraints

- 6 个功能点分别是 F1 工序池基础模块、F2 报工 + 记录本一体提交、F3 固定模板录入、F4 设备账号内切换员工、F7 生产工单 FIFO、F8 工序池时间轴 / 甘特图。
- 6 个功能点必须共同满足 R01-R21。
- 代码实现必须按 BDD + strict TDD 记录 RED/GREEN/REGRESSION。
- worktree 只能创建在 `D:\IntRuoyiWorktree\`。
- 附加 worktree 如需启动服务，必须按端口槽位登记，不得随机换端口。
- 不考虑排产系统，订单只考虑生产工单。
- 一线入口是“报工”，也是记录本入口；提交后同时形成报工信息和记录本/工序池信息。

## Unknowns

- 组合提交最终采用新增 API 还是扩展现有报工 API。
- 设备账号绑定路线、工序绑定员工、员工模板绑定的现有正式配置来源是否足够。
- 电子签名在一线场景是否可复用 DCC/eDHR 现有签名链路，还是需要 MES 适配层。
- PQC 成功/失败如何影响可分配数量的质量状态。
- 生产工单计划开始时间字段在当前 schema 中的正式字段名和空值基线。
- 真实 E2E 所需测试租户、设备账号、员工、设备、工作站、签名能力是否已存在。

## Risks

- F1 是其它功能的共享基础，6 个 worktree 并行实现会存在 schema/接口依赖和 merge 冲突风险。
- F2、F3、F4 可能同时触碰前端报工入口和后端组合提交契约。
- F7 依赖 F1 的事件片段和生产工单计划开始时间字段，不能独立猜测排序字段。
- F8 依赖 F1/F2/F4/F7 的事件字段，必须保持只读，不能把查询接口写成业务操作入口。
- 若本地数据库、Redis、登录、签名或测试数据缺失，真实 E2E 会阻塞。

## Validation Surface

- 后端 JUnit：MES 模块 schema、服务、控制器、FIFO、时间轴、固定模板、设备账号上下文。
- SQL 契约：新增表、菜单/权限、迁移字段、生产工单计划开始时间字段核对。
- 前端静态/单元测试：报工入口、员工切换、固定模板渲染、时间轴只读。
- Playwright E2E：真实登录、报工入口提交、PQC 提交、时间轴查询。
- Git/worktree：6 个 worktree 分支实现、主线程 review、逐个融合到 `int_main`。

## Blocking Prerequisites

- 若 `D:\IntRuoyiWorktree\` 不存在或不可写，阻塞 worktree 创建。
- 若 worktree 目标路径不是 `D:\IntRuoyiWorktree\` 子路径，阻塞创建。
- 若本地没有可用 `origin` 或推送失败，任务不能完成。
- 若真实 E2E 缺少授权测试租户、设备账号、实际员工、签名能力或数据清理策略，E2E 阶段必须阻塞，不能用 mock 替代。
