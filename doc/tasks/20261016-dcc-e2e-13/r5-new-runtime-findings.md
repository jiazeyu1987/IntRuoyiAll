# E2E-13 R5 新包问题与待确认 RED

状态：APPROVED_TWO_SITE_FIX_GREEN_14_MAVEN_RELEASED。Root已批准本设计；有效业务RED后严格完成两处Query修复，同14例GREEN/exit0，freshXML与源码哈希已归档。Maven已释放；当前等待Root出包及原O真实页面复验。下文为已执行前的根因/BDD与历史计划，最新实绩见文末。

## 真实页面事实

- Root 23:13 新包放行已生效，覆盖此前旧包 BLOCKED。分支 int_qms，8061/48061，后端 PID19844，Jar backend-workflow-v6-20261019-230954.jar，SHA256 `3e0486e57d2b2eb4e9e0e011db03c5283d843956f08b454d29e2e70670a09a1c`。
- 原 O910463 从真实工作台唯一原行进入 4222/A1/BPM `fb0c1653-ca6f-11f1-9342-b082e25ec548`；负责人 O 和签名860保持，无培训上传按钮。
- 本次申请属性、5个真实节点与签名856/857/859/860/861已在 DOM 可读。独立的关联分区显示 `Current user cannot access this controlled file`。
- 页面“预览受控文件”按钮数量0；未打开viewer，未观察到正文上下文或正文内容，不能声明正文端点已拒绝或已通过。
- 结果与trace：`runs/20261019-repair-r5/O-1792423338598/result.json`、`evidence/original-O-r5.trace.zip`；截图4张。trace脱敏与ZIP验证PASS。登录后写请求0、培训上传写请求0、页面JS异常0、browserClosed=true。登录过程不计入登录后写请求。
- 实际命令 `node --check doc/tasks/20261016-dcc-e2e-13/verify-original-O-r5.cjs; node doc/tasks/20261016-dcc-e2e-13/verify-original-O-r5.cjs`，工具返回exit1；result.json中processExitCode=2是脚本设置值，不能替代实际进程收据。

## R5-13-01：关联来源元数据入口未应用当前培训职责

真实触发：原O当前签名负责人且存在实际等待培训职责，无通用目录QUERY/PREVIEW。申请证据已可读，关联分区仍拒绝。

直接调用链：`IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailRelationsPanel.vue` 的load先调用getControlledFileRelationPermissions(selectedId)，通过后才读精确申请证据；后端 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java#getRelationPermissions` 仍只用canViewFileName/historical准入。已修复的assertRelationNameVisible当前native waiting + hard file scope + canOrganizeTraining准入没有用于这里。因此前端尚未读到关联来源/正式空态。

最小修复候选：getRelationPermissions只复用既有assertRelationNameVisible作为所选来源名称准入；正式File/Master/Project/placement校验保持，canEdit和canPreview仍各自独立计算，关联目标仍逐项鉴权。不得改canViewFileName通用发现规则或赋予全局QUERY、下载、编辑、培训上传权限。

BDD：Given 原O910463/4222/A1/原BPM、当前真实培训职责、硬文件范围有效且无通用QUERY/PREVIEW；When 读取所选来源relation-permissions；Then 返回同一文件/版本身份，canEdit=false，canPreview由独立正文权限与真实artifact决定。Given 同范围普通无权人或O已无当前职责；When 同一读取；Then CONTROLLED_FILE_ACCESS_DENIED，不能借元数据升级授权。

## R5-13-02：预览artifact状态投影遗漏原生培训等待状态

直接调用链：同一Query的toRespVO先调用resolvePreviewArtifactProjection；expectsPreviewArtifact只列受控/WORKING/REJECTED/FINALIZATION_FAILED/READY_TO_PUBLISH/延迟确认/审批状态，不含 `PENDING_APPLICANT_TRAINING_RECORD`。此时直接返回(null,null)，不会进入已有resolvePreviewReferenceId的native waiting分支，也不会读取本轮sourceFileId。故toRespVO.canPreview=false，buildActionProjection无PREVIEW，前端presentation.getDetailActionState=false，index.vue隐藏真实按钮。

已有的canReadBinary native waiting -> canReadTrainingBody以及resolveDetailBodyReadContext精确TRAINING_APPLICATION均位于这条断点之后；不能通过改前端绕过后端动作投影。当前静态断点能解释页面，但修正状态后仍须实际验证artifact存在、严格正文守卫和viewer渲染，不能预先宣称完整根因已关闭。

最小修复候选：只将isNativeWaitingFile(file)纳入expectsPreviewArtifact，沿既有本轮sourceFileId正式选择和文件记录查询；不改变普通状态源选择、不增加fallback、不改正文Guard准入。若artifact缺失，应准确失败，不造文件、不模拟正文。

BDD：Given 原O/4222/A1/原BPM、真实当前培训职责和该轮批准源文件；When Query生成预览artifact与独立正文权限并构建动作投影；Then 选择该轮source、允许PREVIEW、拒绝DOWNLOAD/PRINT/编辑/培训上传。Given 精确TRAINING_APPLICATION上下文；When 真实DetailGuard处理；Then O的当前职责可读，普通无权人、旧轮次、错误文件/版本、失效职责及DAILY均拒绝。

## 待确认的可执行 RED 与后续边界

新增独立测试类 `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingOwnerProjectionRegressionTest.java`；只选此类，不重跑既有323/37/14。调用实际Query入口、实际artifact/动作投影与实际DetailGuard，隔离外部持久化/身份/当前职责端口；不声明真实数据库/Flowable集成覆盖，也不以mock替代E2E。

Root确认后从仓库根执行（现在未执行）：

```powershell
python doc/tasks/20261016-dcc-e2e-13/run-repair-maven.py r5-owner-projection-red DccTrainingOwnerProjectionRegressionTest 20261019-repair-r5
```

预期RED仅两条正向投影用例：relation-permissions因旧入口权限拒绝；artifact期待源文件却得null。负向权限与精确TRAINING_APPLICATION守卫应保持通过。若出现夹具/编译错误，先修测试使其触达预期断点，不把它当业务RED。有效RED后再按Root确认执行两处最小生产修复，同选择器GREEN；由Root构建/发布后才复验原O页面、真实正文及TRAINING_APPLICATION上下文。普通无权人只走新增隔离单测，不新增真实账号或权限数据。

保留4218/4222、签名853/860、既有批准和上传职责证据。无API/SQL业务动作、服务/构建、Git提交推送、子Agent或跨线程消息。本轮没有临时授权、锁或后台任务需恢复；原任务资产继续保留供Root复核。

分析基线HEAD：f06833f06c868dc3a451eb63d01381a467ecfe92。源文件UTF-8/LF规范化SHA256见本run/source-analysis-fingerprints.json；接收代码不同或指纹变化时重新复核。结构与证据校验PASS不等于Java编译或业务RED已执行。


## R5 两处窄修复完成定向RED/GREEN，Maven窗口释放

- Root当轮明确批准生产两处设计与本例唯一Maven窗口。先保留编译尝试r5-owner-projection-red：exit1/no fresh XML，只有新fixture DccProjectCodeDO builder不支持继承tenantId，改为显式setTenantId；不计业务RED。
- VALID RED：r5-owner-projection-red-valid，实际14tests/12PASS/1FAIL/1ERROR/0skip，exit1，freshXML已归档。两处均业务断点：getRelationPermissions原O权限拒绝（ServiceException），预览源artifact断言期望源文件实际null。其余12例通过。
- 生产仅改DccControlledFileQueryServiceImpl#getRelationPermissions复用既有assertRelationNameVisible；expectsPreviewArtifact纳入isNativeWaitingFile。原正式身份/placement、独立正文和编辑授权保持原计算。普通发现/Guard/前端无改动；无fallback或新授权。对本轮前镜像做精确两处源码比较PASS，补丁与前后SHA256已保存。
- GREEN：r5-owner-projection-green，同一14例全部PASS，0failures/0errors/0skip，exit0，freshXML及同案例身份校验PASS。覆盖原O/4222/A1/BPM真实Query与DetailGuard、普通无权人、失效职责、硬范围、tenant、错误轮次/文件/版本、DAILY拒绝及缺源文件准确失败；依赖端口隔离，不声称DB/Flowable集成或页面正文通过。
- 命令为python doc/tasks/20261016-dcc-e2e-13/run-repair-maven.py <上述label> DccTrainingOwnerProjectionRegressionTest 20261019-repair-r5；wrapper保存实际mvn -q -pl yudao-module-dcc -am -Dtest=DccTrainingOwnerProjectionRegressionTest -Dsurefire.failIfNoSpecifiedTests=false -Dfile.encoding=UTF-8 test、cwd、时间、退出码与mtime检查后的XML。详细命令/源码/编译class/XML哈希见runs/20261019-repair-r5/approved-fix-result.json。
- Maven窗口RELEASED，本例Maven已退出；不追加构建或全量回归。旧323/37/14与批准主链证据保留不重跑。本轮同14例已覆盖两个改动决策及权限边界，无新失败或未解决疑点需要扩大回归。
- Root统一后续包和原O真实页面复验；本轮未再跑浏览器、未重启服务、未操作运行库/账号/角色、未提交推送、未启用子Agent/跨线程消息。当前代码GREEN不替代真实正文验收。任务保持in_progress，等待Root出包/页面闭环。
