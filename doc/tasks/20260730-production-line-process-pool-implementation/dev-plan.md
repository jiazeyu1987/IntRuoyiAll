# Development Plan

## Task Graph

### task_id: F1

title: 工序池基础模型和事件服务
objective: 新增正式工序池、提交事件、数量片段、PQC 过程检验事件的后端模型、迁移、Mapper、服务和定向测试。
dependency_ids: []
affected_paths: `IntRuoyiBackend/yudao-module-mes`, `IntRuoyiBackend/sql/mysql`, `IntRuoyiBackend/script/tests`
write_scope: 后端 MES 工序池新包、SQL 迁移、MES 测试；不得修改前端报工入口。
acceptance_ids: AC-01, AC-02, AC-03, AC-10, AC-17
validation_steps: T01, T02, T03, T04 对应 Maven 和 SQL 契约测试。
done_definition: 工序池不是余量池，事件保存完整上下文，服务端时间/签名/PQC 入池通过测试。

### task_id: F2

title: 报工和记录本一体提交
objective: 在现有报工入口上实现组合提交契约和事务编排，同时写入报工、记录本和工序池事件。
dependency_ids: [F1]
affected_paths: `IntRuoyiBackend/yudao-module-mes`, `IntRuoyiFronted/src/api/mes`, `IntRuoyiFronted/src/views/mes/pro/feedback`
write_scope: 组合提交接口、服务、VO、前端 API 包装；不得实现时间轴或 FIFO。
acceptance_ids: AC-04, AC-05, AC-10, AC-17
validation_steps: T05, T06, T07, T08。
done_definition: 同一事务返回 `feedbackId`、`recordbookEntryId`、`processPoolEventId`，超限原始值不被裁剪。

### task_id: F3

title: 固定模板录入
objective: 实现生产简化模板和 PQC 简化模板目录、解析、payload 契约和前端渲染。
dependency_ids: [F1]
affected_paths: `IntRuoyiBackend/yudao-module-mes`, `IntRuoyiFronted/src/views/mes/pro/feedback`, `IntRuoyiFronted/src/api/mes`
write_scope: 固定模板服务和报工入口 UI；不得改批记录表单正式绑定来源。
acceptance_ids: AC-06, AC-07, AC-05, AC-17
validation_steps: T09, T10, T11。
done_definition: 生产模板只展示四类字段，PQC 只允许成功/失败，不展示可编辑提交时间。

### task_id: F4

title: 设备账号内切换实际员工
objective: 实现设备账号绑定路线、可切换工序、工序绑定员工、员工切换上下文和提交身份留痕。
dependency_ids: [F1]
affected_paths: `IntRuoyiBackend/yudao-module-mes`, `IntRuoyiFronted/src/views/mes/pro/feedback`, `IntRuoyiFronted/src/api/mes`
write_scope: 设备账号上下文服务、员工切换接口、前端切换控件；不得新增登录/二次认证流程。
acceptance_ids: AC-08, AC-09, AC-02, AC-10
validation_steps: T12, T13, T14。
done_definition: 登录账号不变，实际员工可切换且受工序绑定限制，签名员工必须等于实际员工。

### task_id: F7

title: 生产工单 FIFO 分配基础逻辑
objective: 基于工序池可分配数量片段和生产工单计划开始时间实现 FIFO 分配、明细追溯和已分配锁定。
dependency_ids: [F1]
affected_paths: `IntRuoyiBackend/yudao-module-mes`, `IntRuoyiBackend/sql/mysql`
write_scope: FIFO 服务、分配明细、完成数量计算；不得引入排产目标或计划系统依赖。
acceptance_ids: AC-11, AC-12, AC-13, AC-14
validation_steps: T15, T16, T17。
done_definition: FIFO 只指向生产工单，缺少计划开始时间阻塞，已分配片段不能修改。

### task_id: F8

title: 工序池时间轴 / 甘特图只读查询
objective: 实现工序池提交事件的按天、多条件、只读时间轴查询和详情追溯。
dependency_ids: [F1, F2, F3, F4, F7]
affected_paths: `IntRuoyiBackend/yudao-module-mes`, `IntRuoyiFronted/src/views/mes/pro`, `IntRuoyiFronted/src/api/mes`
write_scope: 时间轴查询接口、前端只读页面/组件、静态/E2E 测试；不得执行修改、审核或 FIFO 写操作。
acceptance_ids: AC-15, AC-16, AC-02, AC-03, AC-13
validation_steps: T18, T19, T20。
done_definition: 时间轴按提交时间展示谁提交了什么，过滤准确，详情只读可追溯。

## Merge Strategy

1. 所有 worktree 从当前 `int_main` 创建独立分支。
2. F1 是基础分支，主线程优先 review 和融合。
3. 其它分支若依赖 F1 编译结果，融合前由主线程将 F1 变更合入对应分支或在 `int_main` 融合后 rebase/merge。
4. 合并顺序为 F1 -> F2/F3/F4/F7 -> F8。
5. 任何分支若修改超出 write scope，主线程拒绝并退回。
