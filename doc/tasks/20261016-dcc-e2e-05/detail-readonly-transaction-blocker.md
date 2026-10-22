# C05-PRE-07：详情附件读取在只读事务执行 FOR UPDATE

- Root 已移动延迟确认watch注册至所有依赖声明之后，当前同一草稿新context原U恢复轮 resume-1792174477005 **没有pageerror**，两TDZ错误未复现。前端组件能渲染，但业务详情读取未成功，不能写详情GREEN。
- 精确fileId2054545668044084181，编号DCC241016C05-U34096MVB8UGNE-A，原工作稿仍保留。从真实浏览行操作面板打开，详情DOM显示受控文件详情、编号-、版本未记录、发布后续加载失败/系统异常；私有值/附件未能核验。所有自然请求GET，未重传/保存/提交。
- 自然GET /admin-api/dcc/controlled-files/2054545668044084181 HTTP200，HTTP状态不证明业务成功；最终DOM对应错误保留在该resume的result.json/applicant-failure.png。未解析接口JSON做验收依据。
- 同期后端日志 output/runtime/int_qms/logs/dcc24-workflowv6/application.log:3145，2026-10-17 02:15:19.707，SQLState25006/error1792：Cannot execute statement in a READ ONLY transaction。SQL为 dcc_controlled_file tenant/id精确读取 LIMIT1 FOR UPDATE。
- 关键调用链：DccControlledFileMapper.selectByIdAndTenantForUpdate → DccControlledFileAttachmentServiceImpl.requireAttachmentVersion:252 → readOrdinaryAttachmentManifest:177。Root需确定只读manifest路径的锁需求与事务合同，不能吞异常或给空附件冒充成功。本线程不改后端/事务/schema，不运行服务或API写。
- 原结果 evidence/U34096mvb8ugne/resume-1792174477005/result.json，旧两TDZ结果独立保留。当前零业务写入；同一草稿待Root修复后从真实浏览行重开继续，禁止新建/重传。四分支均未全PASS。
