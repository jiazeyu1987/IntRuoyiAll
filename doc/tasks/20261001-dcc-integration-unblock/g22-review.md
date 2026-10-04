# G22 本机审计配置补充范围

Root已只读核对真实缺口：tenant1已有1项dcc publish active，候选26项中的25新增动作无active策略；候选新版本没有批准登记。缺策略在正式GxpAuditServiceImpl.append阻断业务，不能关闭审计、套旧publish或mock成功。

upload Owner只在原public-browser任务目录准备完整25payload/INSERT-only SQL、独立最多1quality-version登记SQL、空实际批准输入模板、只读preflight、生成器/test、coverage字节保护。Root独立运行12项离线test PASS；没有MySQL执行证据、没有正式业务动作被API/SQL代办。

配置scope仅本机ruoyi-vue-pro、tenant1，25缺项，不更新旧publish、不改其它现有策略/批准/其它租户。精确重复0变化，任何字段/版本/原hash/批准事实冲突整批拒绝。新版批准人系统账号/质量职责/实际批准时间/依据及签名证明未提供，保持null；禁止把admin/旧批准引用作为新批准。quality-version登记最多1行是独立许可，不能把配置写授权当质量签名已发生。

源码没有正式GxP audit策略登记/批准页面；signature-governance策略面板是另一链路，不能冒用。项目docs/system/gxp-audit-trail-config-security-deployment.md明确“策略生效需要质量电子签名”，docs/csv-validation/12-qms-training-approval.md明确QA不能开发自批准。Root按实际新写范围和这两条规定向用户单列配置授权与实际批准资料问题；不是因为假想风险新增审批业务节点。

19SQL数据库升级问题仍pending；新25配置+最多1批准问题已另发。当前未DB写入、未服务/真实Playwright/本地Git合入，完整目标仍active。审查入口：integration doc/tasks/20261002-dcc-public-browser/g22-gxp-local-test-configuration-plan.md，固定策略rawSHA776905347c7726983db317eda110908f6762aa1351a8564e1e130da19b0d59df、coverage rawSHA319fc04a677dcb8670b420c9791bf5e4584d6708e0e765e58dbb637d9ab6b558。
