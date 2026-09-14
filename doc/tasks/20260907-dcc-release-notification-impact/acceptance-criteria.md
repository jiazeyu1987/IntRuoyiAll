# DCC 发布通知与影响评估验收标准

## Purpose and Scope

定义二阶段第一版可测试验收标准，覆盖发布账本、可见范围、通知、正反向关联影响、升版跟踪、权限、幂等和真实页面。

## Evidence Reviewed

- 二阶段 PRD 和用户流程。
- 一阶段版本生命周期、发布状态与快照衔接要求。
- 当前关联文件 schema、消息任务和工作台能力。

## Acceptance Criteria

- AC-01：Given B/1 已批准，When 文控发布成功，Then B/1 ACTIVE、旧版 SUPERSEDED，且同一事务存在唯一发布后续批次和冻结快照。
- AC-02：Given 后续账本无法保存，When 文控发布，Then 发布失败且旧版继续有效；Given 账本已提交但消息失败，Then B/1 保持 ACTIVE。
- AC-03：Given 发布后责任人、CURRENT_VIEW_MATRIX、PUBLIC_FOLDER 收件人或项目分配硬范围发生变化，When 审计人员查看该发布，Then 发布时三类业务授权来源、硬范围过滤结果和最终业务用户清单不变；项目/分类/目录上下文与技术治理访问不被误记成关联方授权，当前访问仍按新规则判断。
- AC-04：Given 一名用户命中文件责任人和分发对象，When 生成通知，Then 只有一条发送记录并展示全部原因。
- AC-05：Given 收件人后来失去 VIEW，When 从通知打开文件，Then 系统拒绝查看且不泄露正文。
- AC-06：Given X 关联 Y 且 Z 当前正式版本反向引用 X，When X 发布，Then Y、Z 各一项任务；双向重复关系按 Master 合并。
- AC-07：Given 任务已经生成，When Y 后续升版或关系被修改，Then任务仍展示发布时冻结的 Y 版本和关系方向。
- AC-08：Given 相关文件责任人停用，When生成任务，Then状态为 UNASSIGNED 并出现在文控列表，不发送给停用用户。
- AC-09：Given 非负责人且非文控用户，When提交决定或转派，Then请求拒绝且任务不变。
- AC-10：Given 两个会话同时处理同一任务，When都提交决定，Then只有第一个成功，第二个返回版本冲突。
- AC-11：Given 负责人选择无需升版，When填写原因并提交，Then任务完成且相关文件版本、状态和正式指针不变。
- AC-12：Given 负责人选择需要升版，When提交决定，Then不自动创建版本，只显示待开始升版。
- AC-13：Given 相关文件已有开放大版本，When负责人开始升版，Then只能关联已有版本；该版本发布后任务变为 RESOLVED。
- AC-14：Given 同一发布事件重复触发物化和发送，When系统处理，Then批次、用户通知和相关 Master 任务数量不增加。
- AC-15：Given 三个收件人中一个发送失败，When批次完成，Then另两人 SENT、失败者 FAILED、文件 ACTIVE；重试只处理失败者。
- AC-16：Given A/1 检入 A/2，When检入成功，Then不产生发布后续批次、通知或影响任务。
- AC-17：Given 用户打开详情、工作台和文控管理页，When查看同一发布，Then页面批次、收件原因、任务状态和结论与只读核验一致。
- AC-18：Given 测试租户有两份任务自有新文件和有效关系，When Playwright 完成发布、收件查看、影响决定及升版入口，Then所有业务动作由真实前端完成且无 API/SQL 写入替代。

## Rejection Criteria

- 把所有 VIEW 用户无差别群发通知。
- 消息发送失败导致已生效文件回退，或后续账本缺失仍宣称发布完成。
- 只处理正向关联而漏掉反向引用。
- 按具体版本 ID 重复生成同一 Master 的多个任务。
- 通知授予或旁路文件查看权限。
- 影响决定自动创建、审批或发布关联文件新版本。
- 负责人缺失时静默跳过任务。
- 直接覆盖历史决定、转派或发送结果而无审计。
- 小版本检入产生发布通知。
- E2E 使用 API/数据库承担被验收业务动作。

## Product Blockers

- 发布事务必须能够原子保存后续账本和快照。
- 测试租户必须提供新文件、关联关系、文控与负责人真实账号。
- 当前责任人正式来源若不再使用版本 requester，必须先形成新的 Master 责任人规则。
