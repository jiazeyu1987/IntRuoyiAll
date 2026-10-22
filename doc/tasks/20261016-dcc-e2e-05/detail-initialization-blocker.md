# C05-PRE-06：已保存草稿重开详情初始化失败

- 前提已GREEN：Root upload-placement-policy-repair.json 通过首次/重复及旧行不变核验；旧失败事务回滚/DB无file由Root确认。旧浏览器/上下文已关闭，ticketID未保存且不猜测/不清理；新run按授权用原fixture重新UI上传。
- 新run U34096mvb8ugne 源件上传和单次保存成功，真实导航至 detail/2054545668044084181?from=browser&management=1...。原result记录fileId，浏览页DOM有精确 DCC241016C05-U34096MVB8UGNE-A / A/1 / 工作版本，源件沿用原任务文件。
- 原运行重开按含小写runId的输入编号匹配失败，实际页面将编号规范为大写。仅本例脚本改为真实大写编号，恢复同一已保存草稿；未创建第二份、未再次保存或正式提交。原result/trace保留，恢复证据独立子目录。
- 从真实浏览行“操作面板”打开同fileId后，详情仅剩页签和空白正文，detail-header-shell不存在。原trace有3条同一 ReferenceError：Cannot access 'currentUserId' before initialization。
- 堆栈落 Vite detail/index.vue:285 computed / setup:327 watch，对应源文件当前 applicationRoundContextKey/delayedConfirmationIdentity 在声明currentUserId之前；watch(delayedConfirmationIdentity,loadDelayedConfirmation,{flush:'post'})在约3106行安装时就读取computed，currentUserId直到约3553行才初始化。仅静态定位，未修改生产文件；具体修复由Root负责。
- 恢复轮证据 evidence/U34096mvb8ugne/resume-1792172142993/result.json、applicant-trace.zip、applicant-failure.png；提取错误 detail-initialization-blocker.json，原trace未改。来源真实浏览→目标行，不拼URL绕过，不API写。
- 当前草稿保留fileId2054545668044084181，尚无本次BPM/正式申请；私有矩阵已保存待重开核验，不能因保存成功写C05-A PASS。B/C/D完整后置未跑，均未PASS。
- Root修复后可对同一run继续 --resume-saved-draft，从真实浏览行重开；禁止重新上传/保存新草稿。若仍读失败继续留证，不自动提交未知对象。

## currentUserId移位后同草稿新context实测
Root移动currentUserId后，resume-1792173677766原U从真实浏览行重开同fileId，旧currentUserId错误未复现，新的3条pageerror是Cannot access 'controlledFileId' before initialization。compiledVite detail/index.vue:269/290，源approvalProcessInstanceId约3062依赖controlledFileId，被早期delayedConfirmationIdentity/watch约3087/3107读取，而controlledFileId约3545才声明。新增精确stack保存在detail-initialization-blocker.json，旧错误嵌套保留。所有自然请求只读，未上传/保存/提交，按Root要求停止同一草稿。
