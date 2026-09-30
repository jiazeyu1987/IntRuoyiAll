# 线程D：关联、引用与统一文件选择器

任务id：20260930-dcc-d-relations。Owner：D。优先级P1。G0共同基线、G1共享契约通过后实施；独立组件/服务可并行，依赖A双版本定位及B正式负责人/目录。

## 目标与边界

1. 统一文件选择器和关联窗口：顶部初始文件，左侧项目目录，右侧全局搜索；分页、已选项保留、去重、自关联禁止。
2. 跨项目关联稳定文件身份，当前解析最新受控版，未来生效显示待生效；历史审批证据固定当时版。
3. 上传选择和既有文件关联增删共用组件，依据权限区分候选可见与正文查看；接口再核验不能只靠UI。
4. 会签期间按关联文件选择整改负责人和期限，未选不生成任务；实际受控成功才通知所选人，中断不通知。同一关联文件的冲突需协调，不后写覆盖。
5. 项目文件夹引用/取消独立业务关系；仅目标项目负责人操作。橙色＋文字标识、按项目去重计数、取消最后一个引用减数与颜色恢复。
6. 引用不复制正文，不给新编辑权限，不取消文件关联。源文件独立作废显示不可执行，保留追溯。

允许：D归属关系/整改服务、对象、controller、新独立引用API、文件选择器/关系/引用组件及测试。公共QueryService由C接入投影；ProjectCodeTabPanel由B接入组件；upload/detail/browser由主管理接入。

## 当前依据

- 上传当前仅同项目多选下拉；RelatedFileService.validateAndBindRelatedFiles限制同项目且绑定具体版本。
- listRelatedFiles按原relatedControlledFileId查询，存在另一个resolveCurrentActiveRelatedFileIds用于再送审解析，但不足以证明浏览关系始终最新。
- PublicationFollowup在发布后生成影响评估，责任人主要来自关联文件requester；不等于会签期间明确指派和受控后通知。
- 未发现本需求中的项目文件夹引用/取消完整链。既有project assignment、关联文档、签名引用计数不能冒充项目引用。

## 里程碑

- D1：两种关系模型、权限、最新/历史查询、事件和选择器props/emit方案G1Review。
- D2：选择器、跨项目关联、分页与历史证据RED/GREEN。
- D3：整改安排与受控事件去重、项目引用/取消/计数/权限RED/GREEN。
- D4：交独立组件、Query/B/公共页面接入说明、迁移及Review证据。

## 必须记录的BDD

| 编号 | Given | When | Then |
|---|---|---|---|
| D-01 | 本文件位于项目A | 从关联窗口搜索项目B | 授权候选可找到，顶部本文件身份不变 |
| D-02 | 已选文件跨多页 | 切项目/搜索/翻页 | 已选保留，去重，不能自关联 |
| D-03 | A关联B，B新版受控未生效 | 打开当前关联 | 指向B最新受控版并显示待生效 |
| D-04 | 历史审批曾看B旧版 | B后来升版 | 历史仍显示当时版及证据，不被最新替换 |
| D-05 | 名称权限有、内容权限无 | 搜索/点击预览 | 可按权限发现名称但正文拒绝，不自动提权 |
| D-06 | 会签选择部分整改责任人 | 受控成功/驳回/撤回/失败 | 仅成功受控通知所选人；其他情况不通知 |
| D-07 | 同一受控事件重试/并发 | 消费事件 | 不重复创建整改任务或通知；期限按冻结安排 |
| D-08 | 本项目负责人/其他项目负责人/成员/admin | 引用或取消 | 只有本项目正式负责人成功，无菜单旁路 |
| D-09 | 两项目各多文件夹引用同文件 | 查看计数 | 按项目数2，不按文件夹条数 |
| D-10 | 本项目仍有引用/最后一个/全局最后一个 | 二次确认取消 | 正确入口、计数、颜色；源文件与关联不删 |
| D-11 | 源文件作废且无可执行版 | 浏览引用/关联 | 明确作废事实，不用旧文件假装当前受控执行 |
| D-12 | 改关系失败或搜索旧响应晚到 | 取消/重新搜索 | 不伪报保存、不覆盖新上下文，已选不串文件 |

引用是否跟随最新版仍待确认；先提供稳定关系与策略方案，不能擅自自动切换。整改期限单位与冲突裁定人也不得默认猜测。

## 验证计划

IntRuoyiBackend已有回归：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRelatedFileServiceTest,DccRelatedFileImpactAssessmentServiceTest,DccImpactAssessmentTransactionIntegrationTest,DccImpactAssessmentArchitectureTest,DccProjectAccessServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

新增项目引用、计数并发、取消权限及最新/历史分离业务测试必须加入目标列表；现有影响评估测试通过不能代替整改指派需求。涉及持久关系/计数事务需对应测试，不仅字符串断言。

IntRuoyiFronted现有候选：

```powershell
node scripts/dcc-related-file-pagination.test.mjs
```

新增选择器交互、分页保留、错租户权限、待生效标签、取消确认和请求失败可见测试；主页面接入与类型构建由主管理统一验证。真实引用/关联E2E只在另获当轮授权后进行，API不能替页面承担验收动作。

## 交付与Review

交选择器组件、关系服务接口、Query投影方案、稳定文件身份与历史快照、受控事件消费者、负责人权限与计数事务、迁移、真实RED/GREEN及integration-notes。

主管理重点Review关系不混用、最新受控与当前执行不混用、历史不变、事件不重复、项目负责人权限无旁路、取消引用不误删源文件、隐私权限在后端重复检查。

## 可复制给线程D的指令

你负责线程D。先读AGENTS.md、对应规则及docs/dcc-parallel-delivery/README、shared-contract、ownership和本任务书。共同基线/G1未通过先交设计和BDD，不能在遗漏当前实现的HEAD上修复。独立worktree严格RED→GREEN，只改D归属文件；C/B/主管理分别接入Query/项目页/公共页。未确认引用版本策略不得自动套关联规则。未经授权不提交推送、写数据库、运行服务或真实E2E。交实现与真实验证证据给主管理Review。
