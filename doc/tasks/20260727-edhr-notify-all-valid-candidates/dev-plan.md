# Development Plan

## Task Graph

### T0

- `task_id`: T0
- `title`: 权威真实 fixture 获取与治理
- `objective`: 取得并确认 Word/Excel 原始 fixture 的来源、版本、完整性和项目内稳定位置，消除个人绝对路径依赖。
- `dependency_ids`: []
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/src/test/resources/**`
  - 依赖两个 fixture 的 MES 测试类
- `write_scope`:
  - 仅经确认的真实 fixture、fixture 清单和对应资源定位代码
- `acceptance_ids`: [AC-01, AC-02, AC-03]
- `validation_steps`:
  - 记录原文件路径、大小、SHA-256、来源和版本。
  - 定向运行所有依赖 Word/Excel fixture 的解析、导入和结构测试。
- `done_definition`: 两个真实 fixture 均经确认并以可移植方式被测试消费，相关测试全部通过。
- `status_note`: Word `.doc` 尚未发现；发现两个内容相同的 Excel 候选副本，SHA-256 为 `A7ACF4ADE2E09A00B68D80701B1FB86BC79B6F3CCDA55504B7C838AB85240354`，仍需确认是否为权威原件。

### T1

- `task_id`: T1
- `title`: 建立完整失败清单与根因分组
- `objective`: 将 41 个失败测试套件映射到稳定失败簇、首个根因、责任路径和定向验证命令。
- `dependency_ids`: []
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/target/surefire-reports/**`
  - 当前任务文档
- `write_scope`:
  - `doc/tasks/20260727-edhr-notify-all-valid-candidates/**`
- `acceptance_ids`: [AC-13, AC-14, AC-16, AC-17]
- `validation_steps`:
  - 从 Surefire XML/TXT 提取失败套件、失败数、错误数和首个异常。
  - 核对 Git 状态，标记并发文件冲突。
- `done_definition`: 每个失败套件都有唯一主失败簇、定向命令和明确所有权，不遗漏或重复。

### T2

- `task_id`: T2
- `title`: 修复静态契约与工程路径漂移
- `objective`: 修复仍引用废弃前端根目录、旧共享组件或旧迁移契约的静态测试和正式工程路径。
- `dependency_ids`: [T1]
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/**ContractTest.java`
  - `IntRuoyiFronted/src/**`
  - `IntRuoyiFronted/tests/**`
- `write_scope`:
  - 仅失败清单中映射到静态契约的文件
- `acceptance_ids`: [AC-04, AC-05, AC-13, AC-14, AC-16, AC-17]
- `validation_steps`:
  - 先逐类重跑形成当前 RED。
  - 按当前项目结构和正式契约修复。
  - 运行静态契约组合测试和相关前端静态测试。
- `done_definition`: 静态契约不再引用废弃路径，且没有通过删除断言或减少扫描范围通过。
- `conflict_note`: 当前前端表单模板文件存在并发修改；若计划触及同文件必须先阻塞协调。

### T3

- `task_id`: T3
- `title`: 修复 schema、Spring 测试上下文和 H2 隔离
- `objective`: 解决运行时 schema 契约、Bean 注入/ApplicationContext、测试数据库初始化和唯一键污染的共享根因。
- `dependency_ids`: [T1]
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/src/test/resources/**`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/**/*DbTest.java`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/**`
  - `IntRuoyiBackend/sql/**`
- `write_scope`:
  - 失败清单中与 schema/DB 测试上下文直接相关的 MES 文件
- `acceptance_ids`: [AC-05, AC-06, AC-07, AC-13, AC-14, AC-17]
- `validation_steps`:
  - 对 schema 契约、每个 BeanCreation 根因和唯一键用例分别保留 RED。
  - 核对真实 migration、DO/Mapper、H2 schema 和测试清理边界。
  - 运行 DB 测试组合并重复运行唯一键用例。
- `done_definition`: schema 契约、全部目标 DB/Spring 测试稳定通过，重复执行不污染。
- `conflict_note`: 如需修改 SQL/migration，必须先按数据库门禁核对真实 schema；不得直接写运行库。

### T4

- `task_id`: T4
- `title`: 修复工艺路线与 eDHR 契约簇
- `objective`: 对齐路线服务新增依赖、eDHR 快照/门禁/演练/legacy 测试与当前正式业务规则。
- `dependency_ids`: [T1, T3]
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/**`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/**`
  - 对应测试类
- `write_scope`:
  - 路线与 eDHR 失败簇的生产代码和测试
- `acceptance_ids`: [AC-08, AC-09, AC-12, AC-13, AC-17]
- `validation_steps`:
  - 修复缺失 mock、tenant、快照和配置前置。
  - 保持工序开始、批记录表单、表单槽位三条来源独立。
  - 运行路线/eDHR 失败组合及通知 66 用例。
- `done_definition`: 路线/eDHR 失败簇全部通过，通知候选人语义无回归。

### T5

- `task_id`: T5
- `title`: 修复批记录解析、布局与路线生成簇
- `objective`: 修复不依赖缺失真实 fixture 的 JSON 类型、布局坐标、形状规则、候选路线治理和旧链字段问题。
- `dependency_ids`: [T1, T3]
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/**`
  - 对应测试类
- `write_scope`:
  - 非 fixture 依赖的批记录解析/布局/路线生成文件
- `acceptance_ids`: [AC-10, AC-13, AC-17]
- `validation_steps`:
  - 逐套件确认正式业务期望。
  - 保留现有强断言，按共享根因修复。
  - 运行 JSON builder、layout、shape、route governance/generation 组合测试。
- `done_definition`: 非 fixture 批记录失败簇全部通过，无模板名特例或宽松断言。
- `conflict_note`: 与 T4 可能共享路线生成边界；T5 不得并行修改 T4 正在写入的文件。

### T6

- `task_id`: T6
- `title`: 修复自动排产与排产订单契约簇
- `objective`: 对齐工单编码、冻结路线版本、准入资源查询、进度风险、无默认配置和容量状态。
- `dependency_ids`: [T1]
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/**`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/**`
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/task/**`
  - 对应测试类
- `write_scope`:
  - 排产、排产订单和 Gantt 失败簇文件
- `acceptance_ids`: [AC-08, AC-11, AC-13, AC-17]
- `validation_steps`:
  - 严格 Mockito 下修复精确参数和新增依赖。
  - 运行 auto-schedule、schedule-order、Gantt 组合测试。
- `done_definition`: 排产失败簇全部通过，不使用 lenient 或宽泛 matcher 隐藏真实调用差异。

### T7

- `task_id`: T7
- `title`: 集成真实 fixture 并完成解析导入回归
- `objective`: 在 T0 完成后接入真实 Word/Excel 资源并修复由真实样本暴露的解析/导入问题。
- `dependency_ids`: [T0, T3, T5]
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/src/test/resources/**`
  - Word/Excel 解析、导入和结构测试及必要生产代码
- `write_scope`:
  - 经确认的 fixture 及直接依赖它们的实现/测试
- `acceptance_ids`: [AC-01, AC-02, AC-03, AC-10, AC-13, AC-17]
- `validation_steps`:
  - 运行全部真实 fixture 套件。
  - 核对资源路径无用户名/盘符依赖。
- `done_definition`: 所有真实 Word/Excel 套件通过并可在项目路径复现。

### T8

- `task_id`: T8
- `title`: 分簇集成回归与通知行为复验
- `objective`: 将 T2-T7 的修复组合验证，并重新证明通知全部有效候选人的行为。
- `dependency_ids`: [T2, T4, T6, T7]
- `affected_paths`:
  - 全部任务拥有的 MES 修复文件
  - 当前任务证据
- `write_scope`:
  - 仅任务证据；产品代码仅在分簇测试暴露集成缺陷时回到对应任务修复
- `acceptance_ids`: [AC-12, AC-13, AC-14, AC-16, AC-17]
- `validation_steps`:
  - 运行原通知目标 3 用例和服务类 66 用例。
  - 运行 41 个原失败套件的组合测试。
- `done_definition`: 原失败套件组合和通知回归全部通过。

### T9

- `task_id`: T9
- `title`: 完整 MES 模块独立验收
- `objective`: 使用未筛选命令完成最终系统级验证。
- `dependency_ids`: [T8]
- `affected_paths`:
  - `IntRuoyiBackend/yudao-module-mes/**`
  - 当前任务测试证据
- `write_scope`:
  - 独立测试报告；不得在测试阶段修产品代码
- `acceptance_ids`: [AC-13, AC-14, AC-15, AC-16, AC-17]
- `validation_steps`:
  - 在 `E:\IntRuoyi\IntRuoyiBackend` 执行 `mvn -pl yudao-module-mes test`。
  - 检查 Surefire 汇总、失败套件和 skipped 变化。
- `done_definition`: 退出码 0、`BUILD SUCCESS`、0 failures、0 errors，且无新增/扩大跳过。

## Execution Order

1. T1 先完成失败清单。
2. T2、T3、T6 在写范围无冲突时可并行；共享工作区冲突优先阻塞而非覆盖。
3. T4、T5 在 T3 后按共享路径串行执行。
4. T0 可与非 fixture 修复并行，但 T7 和最终验收必须等待 T0。
5. T8 完成集成回归，T9 由独立测试角色执行。
