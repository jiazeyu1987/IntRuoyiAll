# Implementation Contract IC-1

2026-09-30，主管理批准本轮开发的共同代码合同。需求v1.3优先，旧task包G0已由新HEAD的已提交DCC源码满足；本文件的Owner分配覆盖旧桌面线程限制。尚未提交代码、运行真实E2E或部署数据库。

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

不硬编码保留天数/提醒提前量。可以新增正式配置项，但运行时缺值明确阻断；测试必须显式提供值。产品审核人仍待讨论，不更改现有批准流程人员以猜测。引用版本策略未确认时保持选定版本身份，并明确其与关联自动最新不同，不引入隐式自动跟版。

前端market checkbox按已有需求建议采用多选、NA互斥；其他说明/转移目标条件必填；保留MADSAP原拼写。作为可核对的实现选择写入任务约束，不冒充后续业务确认。

隔离H2/内存Flowable测试为开发TDD，可以运行；不允许测试连接真实业务数据库，不启动Spring Boot正式服务。不执行Git提交推送。按用户最新安排，由用户手动启动四个worktree线程，各线程不启用子Agent或外部app-server。

## 交付

早期先在自身任务目录的integration-notes.md写明实际字段/方法合同，并继续可独立实现部分；不要只交计划结束。不要求跨线程消息工具，用户将报告路径交主管理Review后统一同步依赖。模块目标是服务、接口、迁移、独立UI组件和真实单测。公共上传/详情/浏览页由主管理接入。完成后给具体修改文件列表、RED/GREEN和实际回归结果、待接入点；主管理Review再整合。
