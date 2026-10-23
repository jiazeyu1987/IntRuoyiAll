# Implementation Contract IC-1

2026-09-30建立，2026-10-03同步持续目标。需求v1.4及用户最新确认优先；历史任务状态不代表当前整套业务已完成。

## 当前持续目标授权

用户明确授权子 Agent 并行、任务专属且已登记的附加 worktree 服务、真实前端 Playwright E2E，以及必要本地提交/冲突解决/最终合入 int_qms。Root 负责运行与最终 Git 收口，不要求推送 origin，不以未推送阻塞目标。真实业务库只读；迁移和配置先交具体包与影响方案，未经另外明确授权不直接执行。原四个 worktree 保留交付依据，继续修复在现有整合树，未验证中间状态不带入 int_qms。

这些授权覆盖后文历史“禁止子 Agent/服务/E2E/Git”约束；各文件当前写入归属以主管理最新分派为准。隔离 H2/内存 Flowable 仍是开发测试，不冒充真实页面验收。

## 固定共同字段与方法语义

- Master.latestControlledFileId：最新完成受控的版本，包括未来生效版本。
- Master.currentActiveControlledFileId：当前可执行且已生效版本，未来版受控时不修改；生效日切换并自动作废旧版。
- File.controlledTime：受控完成服务器时间；File.activatedTime：实际生效时间；effectiveDate保留编制预设日期。
- File.revisionChangeType：INITIAL / PARTIAL / REPLACEMENT；必须来自本次实际提交意图，非从编号猜测。Checkin只生成横杠小版本；正式局部/换版动作单独提交。
- 三组属性的请求结构和JSON快照由B命名并尽早通知主管理/A；默认来源和实际值两份快照不得合并或互盖。
- 项目负责人单一账号字段由B实现projectLeaderUserId并提供assertProjectLeader(userId, projectId)正式检查；D引用仅调用此合同，不能用任意OWNER或admin代替。
- D的当前关联解析优先使用正式latestControlledFileId，不回退到执行版；历史关联快照单独读取。latest字段未赋值/目标未受控明确报错，不fallback。

## 临时工作tree字段归属（非主目录并写）

- A允许修改FileDO/MasterDO/StatusEnum/公共生命周期响应及H2 schema以实现上述生命周期字段和签名指派DTO；主线程负责整合B/C额外字段，A只能写自己的字段。
- B修改项目相关模型/组件，公共FileDO等只交integration-notes；提供共享属性值对象服务可先独立测试。
- C允许在自身worker的FileDO/VersionHistory/Checkin/Submit/Iteration VO增加revisionChangeType及源文件名身份字段，以便独立编译；最终与A模型在主管理处按字段合并，不能修改A生命周期字段。
- D先用独立关系/引用对象与接口，不修改公共FileDO；需要latestControlledFileId时以根合同字段调用，主管理同步A基础字段到D worker后编译。
- H2公共schema由各worker添加自己模块列/表，主线程逐段合并。SQL新迁移编号使用20260930_dcc_a_*, b_*, c_*, d_*，只增本模块迁移，不改历史SQL。

## 技术依赖与未定业务

2026-10-01用户已确认作废文件保存20年、培训由文控上传线下文件即可；本轮保留期自实际作废时间起算，不再以保留长度/培训标准未知阻塞或引入逐人确认。提醒提前量仍未定，可以新增正式配置项但缺值明确报错，测试必须显式提供值。2026-10-02产品创建审核人已确认由后台配置、提交时带出，不更改批准节点人员以猜测。引用版本策略未确认时保持选定版本身份，并明确其与关联自动最新不同，不引入隐式自动跟版。

2026-10-02用户确认失败升版重提沿用原目标号（A/2失败后仍A/2），独立保留每次申请/BPM/正文/属性/签名历史。准许在当前整合阶段为明确返工协议调整正式候选唯一性及前向迁移、相关A/C服务和验证；不得删除旧行/改名旧版/跳下一号。写前核对现有schema、锁/幂等/状态，先业务RED再GREEN，未授权执行实库迁移。

前端market checkbox按已有需求建议采用多选、NA互斥；其他说明/转移目标条件必填；保留MADSAP原拼写。作为可核对的实现选择写入任务约束，不冒充后续业务确认。

隔离H2/内存Flowable测试为开发TDD，可以运行；不允许测试连接真实业务数据库，不启动Spring Boot正式服务。不执行Git提交推送。按用户最新安排，由用户手动启动四个worktree线程，各线程不启用子Agent或外部app-server。

## 交付

早期先在自身任务目录的integration-notes.md写明实际字段/方法合同，并继续可独立实现部分；不要只交计划结束。不要求跨线程消息工具，用户将报告路径交主管理Review后统一同步依赖。模块目标是服务、接口、迁移、独立UI组件和真实单测。公共上传/详情/浏览页由主管理接入。完成后给具体修改文件列表、RED/GREEN和实际回归结果、待接入点；主管理Review再整合。
