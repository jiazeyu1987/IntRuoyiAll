# eDHR 偏差管理开发计划

## Purpose and Scope

按P1～P6实现PRD，以28项验收逐项验证。文档结构校验只验证需求包；不得把它当成业务行为证据。

## Evidence Reviewed

PRD、frontend-interaction、后端/API、数据模型、权限设计和验收标准；当前MES批记录、NCR、PQC生产放行和统一签名实现。旧NCR任务只作为历史线索，不作为本功能已完成证明。

## Milestones

### 里程碑 1：正式身份、编号、数据与权限

目标：建立批记录偏差的数据、编号、权限和正式批记录身份基础，覆盖未推送 PQC 的场景。

建议落点：

- IntRuoyiBackend/sql/mysql
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes
- IntRuoyiBackend/yudao-module-mes/src/test

交付物：

- 偏差、唯一处理记录及月序列迁移
- 偏差数据访问与权限服务（含GxP操作登记、电子签名及同事务审计接入）
- 正式批记录来源解析和并发/幂等测试

覆盖 P1-AC1、P1-AC2、P1-AC3、P1-AC4、P1-AC5。

- 复核当前目标diff归属、schema/migration、权限菜单和正式来源映射。
- 证明尚未推送PQC时正式批记录候选可用于偏差发起；QA关键偏差转审及同场景NCR路径归P4验收，不能在P1宣称完整转审已完成。若当前生命周期晚于此阶段才建批记录，先补正式身份形成设计与测试，不能新增订单主关联。
- 提供正式批记录选项/详情上下文，不以PQC放行申请为唯一候选来源。
- 数据库唯一约束、一份处理记录、序列首月并发、幂等和上市放行竞态；使用隔离MySQL验证迁移重放和真实Mapper SQL并发。
- 偏差发起创建必须完成真实电子签名，并在同一业务事务调用获批的GxP审计操作；获批policy/owner/signature/retention输入缺失时保持P1阻塞，不允许在P2事后修补这条写路径。
- 先写实测RED，再做最小实现；并发用服务事务/数据库测试证明，不以字符串检查代替。

### 里程碑 2：唯一处理、签名及常规关闭

目标：实现唯一处理记录、完整电子签名矩阵、内容版本及常规关闭状态机。

建议落点：

- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord
- IntRuoyiBackend/yudao-module-mes/src/test

交付物：

- 偏差处理与签名服务
- 常规关闭状态机
- 签名失效和多角色测试

覆盖 P2-AC1、P2-AC2、P2-AC3、P2-AC4、P2-AC5、P2-AC6。

- 按Word实现完整字段，等级只普通/重大（关键），无原“主要”迁移推断。
- 首次保存建立处理行，编辑、验证不合格后重验均复用；常规关闭全部签名包含部门负责人。
- 注册发起/处理编制、验证、部门负责人、QA、质量负责人、管理者代表的正式签名动作。
- 分离正文contentVersion与状态version：修改使本处理版签名失效，新增同版签名互不失效。
- QA和质量无先后；最后必要签名完成原子关闭；同账号多角色逐节点签。

### 里程碑 3：标准列表、详情和前端交互

目标：实现标准偏差列表、详情、正式批记录主 Tab 和两个只读追溯入口。

建议落点：

- IntRuoyiFronted/src/api/mes/pro/edhr
- IntRuoyiFronted/src/views/mes/pro
- IntRuoyiFronted/tests/e2e

交付物：

- 偏差管理列表和详情页面
- 正式批记录偏差 Tab 与只读追溯组件
- 前端静态/组件合同测试

覆盖 P3-AC1、P3-AC2、P3-AC3、P3-AC4、P3-AC5、P3-AC6。

- 实现服务端状态分页、白名单排序和租户全部查询范围，复用UnifiedListTemplate。
- 按frontend-interaction实现入口、表单、字段分区、按钮条件、回读和返回恢复。
- 正式顶层批记录显示主Tab，两种追溯宿主各只读；内嵌逐工序表单不重复，不从recordScope直接授权。
- activeOrderId-only入口由服务端正式关系投影回批记录身份，正常路径不能以报错替代。
- 空/错/无权/缺身份分开；未保存、取消、版本冲突和结果不明重试有清晰交互。

### 里程碑 4：关键偏差转 NCR 及四项控制

目标：复用统一不合格评审，实现关键偏差直接转审、原子关闭及四项业务动作阻断。

建议落点：

- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline
- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease
- IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance

交付物：

- 批记录偏差来源的不合格评审入口
- 四项动作服务端门禁
- 三类处置、原子性及不自锁测试

覆盖 P4-AC1、P4-AC2、P4-AC3、P4-AC4、P4-AC5、P4-AC6、P4-AC7。

- 扩展统一NCR显式批记录偏差来源，保留原PQC不合格来源和内部正式订单事实。
- QA签署后原子创建NCR、冻结、连接同批已选关键偏差并标TRANSFERRED_TO_NCR关闭；允许处理未完成直接转审。
- 后端四个动作分别覆盖生产提交、一线PQC、当前PQC生产放行服务、批次上市放行，不能仅改老放行接口。
- 门禁置于最终写事务，串行处理创建/关闭/转审/放行竞态；处理和NCR处置继续可用。
- 沿用一批一个待审NCR，创建时可多选；已有待审明确拒绝，不静默追加。
- QA/PQC授权处置三分支；保留其他偏差/评审/外部冻结，返工依正式合同，作废终止。
- QA在PQC推送前仍可按正式批记录身份发起关键偏差NCR；此项验收由T20/P4-AC3覆盖，不归P1候选API阶段宣称完成。

### 里程碑 5：在线追溯与历史证据

目标：在管理、批记录详情和两个追溯入口统一呈现偏差、签名和不合格评审结果。

建议落点：

- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord
- IntRuoyiFronted/src/views/mes/pro/edhr/form-trace
- IntRuoyiFronted/src/views/mes/pro/edhr-batch

交付物：

- 批记录级偏差读模型
- 只读历史和签名证据展示
- 同源、只读及跨租户测试

覆盖 P5-AC1、P5-AC2、P5-AC3。

- 偏差和NCR链接查询同批真实事实，历史及现场入口一致。
- 已关闭/已上市放行只读；显示真实签名、修订、失效记录、关闭方式及NCR最终结果。
- 不把转审关闭写成验证合格，不因NCR后续处置而改写偏差签名正文。
- PDF/归档导出为范围外候选，不写强制开发/测试门禁。

### 里程碑 6：定向验证与交接

目标：完成全部定向回归、迁移策略和任务证据，形成可独立复验的交接结果。

建议落点：

- doc/tasks/20260923-edhr-deviation-management
- IntRuoyiBackend/yudao-module-mes/src/test
- IntRuoyiFronted/tests/e2e

交付物：

- 执行日志和测试报告
- 28 项验收映射证据
- 发布前门禁和剩余风险清单

覆盖 P6-AC1。

- 按test-plan执行每项目标用例、相关既有回归、前端ESLint/类型检查和迁移策略合同。
- 仅当轮明确授权后执行真实E2E；分别记录已通过、未运行和具体前置缺口。
- 不自动启动子Agent、提交推送、写数据库、重启服务或发布；这些需遵守仓库当轮授权要求。

## Implementation Steps

先P1正式身份和数据，再P2状态/签名、P3界面、P4联动、P5历史，最终P6。每阶段记录BDD、实际RED、GREEN及受影响回归；接口或UI依赖未齐可先完成对应测试设计，不能伪报最终阶段完成。

各层遵循同一PRD及frontend-interaction；改动身份、状态、权限或字段时同步API、VO、组件和验收用例。当前存在并行改动只在目标文件重叠时需协调，不处理其他任务资产。

## Verification Gates

| 阶段 | 退出证据 |
| --- | --- |
| P1 | 同租户合法发起/越权拒绝、未推送PQC正式批记录候选、月切/扩位/并发、创建与放行竞态、迁移合同、GxP登记/电子签名及同事务审计。 |
| P2 | 常规全签名、QA/质量任意顺序、普通/关键差异、同版多签、修订失效、唯一处理。 |
| P3 | 标准列表与查询、页面宿主/只读/身份流、取消/冲突/返回、空错无权、ESLint及类型。 |
| P4 | 四项动作逐一运行证据、直接转审/多选/回滚/幂等、三类处置、残余阻断、处理流程不自锁。 |
| P5 | 管理/详情/追溯同源，历史签名证据正确、关闭原因可辨、只读可靠。 |
| P6 | 28项AC均有明确测试归属；当前任务权限允许的检查真实PASS，E2E状态如实记录。 |

## Rollback or Stop Conditions

真实身份缺失、schema/权限/签名动作未配置、事务部分提交或四项门禁缺任一不得放行；先修复正式来源/实现，不新增默认值或模拟成功。未通过验证不提交为完成。回滚部署不能删除审计、偏差或评审历史；需单独批准并基于实际迁移制定方案。
