# G67 真实整改通知与多账号权限验收

当前持续目标保持完整HTML，不新开重复主任务。上一goal turn是progress：原生拒绝CAS和名称消息修复、三次真实拒绝+第四A3受控下发、当前30属性/13审核配置/74名称引用/16整改事务证明，未把有限证据外推为全27。

BDD: Given4041 A3正式受控和4042 A3-4旧草稿，When负责人通过正常当前关联窗口增加4028，并发起下一受控申请，Then实际本次审批关联和指派整改名单能够准确匹配所选业务来源；原历史关联不被改写，缺关联要准确复现后修，不SQL补表。

BDD: Given正式dcc_relation_remediation通知包含sourceControlledFileId/relatedMasterId/dueAt和同源受控viewerURL，When接收人从正常消息详情打开，Then显示可理解的业务事实并能进入正式只读文档入口；缺ID、错来源、外域URL/非白名单query明确拒，不能任意信任detailUrl。复用现消息组件；不扩整改完成流程、第二provider或无明确需要的新队列。

BDD: Given两任务自有合法账号与真实签名/项目权限，When指派另一人会签和竞争检出，Then收到独立任务、真正签名、锁只归一人，第二人看到持有人并被业务锁拒；权限缺失不能充锁失败。必要账号/角色/测试签图由Root真实页面配置，标记测试本人，不套用他人签图，不改无关账户。

BDD: GivenB1未来生效2026-10-13且当前上海2026-10-06，When完成受控并查看提醒列表，Then提前7天边界UPCOMING、原A3仍执行、按生效日排序；下发后该提醒移除。维持每分钟原job5625，不改系统时间或人工SQL生效。

所有biz动作真实Playwright前端；Root独占Git、runtime、实库只读和UI；BE/FE代码唯一owner，legacy只读账号前置。原三个非任务资产保原rawSHA。任务in_progress/goal active。

G68执行分工更新：Root明确授权 legacy_occupancy_core 临时独占两个任务自有配置的真实 Playwright 页面操作（项目271既有USER910328 EDIT改OWNER、类别908710查看矩阵追加仅新部门910335一人），Root不并行修改这两个配置。该 agent 不操作数据库或接口、不重置账号/会签/检出。Root继续独占Git、runtime、实库只读及后续主流程页面验收。此有限并行用于完成正常账号前置；旧Root盲目点击隐藏菜单的失败未产生这些配置写入，不作为权限或锁PASS。
