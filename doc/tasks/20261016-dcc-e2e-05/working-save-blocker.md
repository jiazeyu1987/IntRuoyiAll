# C05-PRE-05：路线就绪后工作稿保存受审计策略阻塞

- 当前 runtime PID34096 / SHA e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce。原 U/P1/其他类型路线4节点 DOM 已就绪，旧三错误未复现，Root 已接受前置 GREEN。
- 本轮已只给S任务角色补 bpm:process-instance:query；原类别规则为空，给该任务类别加U UPLOAD/VIEW、S VIEW并重读。没有修改项目原负责人、其他账号/类别或公共审阅矩阵。
- U34096mvb5mr4i 真实源件预览成功，DOM显示预览文件，POST upload-preview HTTP200；之后点击“保存申请草稿”，POST /dcc/controlled-files/working HTTP200，仍停上传页且60秒未进入详情。没有把HTTP200记作保存成功，也没有重复保存。
- 原U随后只读从受控浏览“全局文件/全部版本”按 DCC241016C05-U34096mvb5mr4i-A 精确查询，HTTP200，DOM暂无数据/共0条。只证明当前账号可见范围中无匹配，不替代DB不存在证明。证据 evidence/U34096mvb5mr4i/save-readonly-inventory.json，0写请求。
- 同秒运行文件 output/runtime/int_qms/logs/dcc24-workflowv6/application.log 仅记录：2026-10-17 00:01:16.128 serviceExceptionHandler，GxpAuditServiceImpl.append(GxpAuditServiceImpl.java:50)。未记录具体错误message/operationId。源码 GxpAuditServiceImpl.java:48—50 在 selectActive(tenant,operationId)为空时抛 GXP_AUDIT_POLICY_NOT_FOUND；这是高置信根因线索，具体运行operation/policy/事务结果由Root核对，不能臆造。
- 保存后资产/工作稿尝试状态未知，保留2项业务残留，不自动撤回或删除、不重新上传/保存。还保留此前400源件预览失败记录，不能悄然丢弃。
- Root需核对精确working请求对应的审计operation策略与事务/文件/ticket状态；只按正式schema/初始化及项目权限规则修复，禁止跳过GxP审计/吞异常/模拟保存成功。本线程不改共享审计策略、生产代码、DB或服务。
- 四完整子例均未PASS；C05-A停止在保存，B/C/D的完整前后链尚未完成。预检PASS和公共矩阵写前读取成功单独记，不代替完整验收。

## Root暂停后的离线原trace补证
已从原trace保存点击后+2114ms截图取回真实页面错误：GxP 审计策略不存在或未启用：dcc.project-file-placement.upload-bind。精确operation不再只是源码推断；截图evidence/U34096mvb5mr4i/original-save-frames/05-2114ms.jpg，原trace未改，保存点击只有1次。网络载荷原本关闭，trace.network=0，无法取回该次自然响应业务码；源码映射1002032000不标实测码。未重放业务。源件985B预览可见，但ticketID/绑定/存储/DB状态仍未知，原U0行不证明DB无写。Root核对前不上传、不保存、不清理。脚本四分支只读复核见four-branch-script-review.md，明确计划/实跑及最近5待办/精确类别映射等证据边界。
