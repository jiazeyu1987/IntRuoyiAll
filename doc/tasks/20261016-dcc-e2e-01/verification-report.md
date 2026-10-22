# E2E-01 默认值及编制人验证报告

## R3 round1 最新交付

本轮新授权完整重跑已完成：10/10 PASS，首次实际exit0，无重试。唯一正式交付rerun-20261020-r3-round1/handoff.json；本次临时源件清理、trace隐私/CRC和context/browser/runner全关通过，当前hold供Root review。旧结果/UNKNOWN保留，工程因当轮禁止Git提交推送blocked。

## rerun-20261020-r2-round1 正式EXECUTE

新包709e78bd/41508以U2本人fresh登录完成首次完整重跑，10/10 PASS、实际exit0，无重试。独立交付rerun-20261020-r2-round1/handoff.json及branches.json，业务证据artifacts/rerun-20261020-r2-round1/DCC241016C01-20261020050805303。13截图/trace、临时清理、安全扫描及context/browser/runner全关均核验；无正式申请/版本或最终提交。当前hold不启动第二轮；工程blocked仅因本轮禁止Git提交推送，旧失败UNKNOWN保持。下文保留此前轮次历史结果。

## 最新结论

**2026-10-20第一轮新重跑：10/10 PASS，实际exit0，无遗漏分支，无本次业务问题。** 本次结果为artifacts/DCC241016C01-20261020013336716/result.json；独立汇总rerun-20261020-handoff.json，launcher回执rerun-20261020-launcher-1792460016090.json。原U2本人新登录，重新选项目/文件夹/类型并上传本次唯一PDF，非空canvas通过；真实确认框核对编制人U2、主管S2、属性及名单，取消后输入保留，原页面移除源件、清理提示和列表/预览消失通过。未最终提交或创建申请/正式版本，A/1仅为本次输入。

本次runtime来自doc/tasks/20261020-qms-e2e24-runtime-update/runtime-proof.json：8061/PID20976、48061/PID27908，Jar SHA256 ee67ec8f1b46fe881b81d826c122c6bc06dd06fba8deb4afa60141ba141566ab；本轮实测监听、进程命令Jar/端口及磁盘哈希一致。共享dispatch仍为旧后端信息，脚本显式读取本轮证明，无协调目录写入。

本次13张关键截图及trace独立保存，确认截图12及清理截图13已视觉复核；限定本次run的凭据字节/trace安全检查PASS exit0，network资源0字节，pageErrors=[]。browser/context均关闭且disconnected已记录；runner实际exit0，进程核验本例runner剩余0，详见rerun-20261020-process-close.json。未擅自启动下一轮。最终提交/审批/生效/签收属于其他用例，未执行；历史失败上传UNKNOWN保留。本节之后均为历史记录，不代表本次重跑。

2026-10-19 r4仅完成历史失败资产证据核查，未执行新的E2E。原10/10业务PASS已获协调目录e2e01-root-review.json接受。失败运行DCC241016C01-20261016155412944的资产身份及正式清理回执均保持UNKNOWN；原清理未完成记录不能证明资产存在，也不能证明已清理。限定来源未发现新增身份/清理回执，不重复扫描历史trace或业务页面。

恢复核查须新增可追溯到原失败运行的正式上传会话/ticket/附件或存储身份，并提供关联该身份的正式状态/清理证据。原文件名和输入编号不是服务端资产身份；独立成功批次的清理不能用于回填。详细命令、退出码、证据、代码归属和未执行边界见repair-20261019-r4-handoff.json；本轮无生产代码、Maven或XML结果，无新包PASS声明。旧业务结论如下，运行态均为历史来源。

**原10分支完整PASS，实际退出码0。** 原定义错误已解除，岗位/阶段权限/本人签名图片及专用类别上传许可均经真实页面补齐；最终源件上传、二次确认核对、取消及临时件清理全部完成。工程收口待Root review，未标任务completed或提交代码。

命令：`node doc/tasks/20261016-dcc-e2e-01/run-r2.cjs`

- 结果：`artifacts/DCC241016C01-20261016161107874/result.json`
- 关键确认截图：同目录`12-confirmation-actual-values.png`；编制人及主管独立截图为10/11，清理截图为13。
- trace：同目录`trace.zip`；实际退出码：`launcher-receipt.json`。
- Root预检/完整回执：`root-workflow-resume-result.json`、`root-handoff.json`。

## 运行态与复用fixture

int_qms，前端8061/PID36084，后端48061/PID34096，Jar SHA256 `e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce`。Root环境依据为协调目录`workflow-isolation-runtime-proof.json`，未把其回归结果当本例业务PASS。

复用原U2/S2/L2/A2/D2、ConfigR2、P1R2/Folder/T1R2等DCC241016C01前缀对象，没有因新PID重建。完整清单在`fixture-manifest.json`，核验模式PER_CASE_REAL_UI_MANIFEST，共享roleFixtureVerified=false未被篡改。

U2正式岗位15；S2/L2/A2/D2复用本例岗位16。S2/L2/D2仅绑定本例Review，A2绑定本例Approve，分别只含对应review/approve阶段权限。四个审签账号由本人独立登录上传并启用测试签名图片，真实图片及再次菜单回读通过；业务身份均无管理员角色。

U2保持P1R2的正式EDIT，仅在本例专用类别T1CategoryR2增加USER U2的UPLOAD规则。ConfigR2原OWNER、既有admin范围、共享角色定义及公共矩阵未改。

## 10个分支实际结果

| 分支 | 结果 | 实际页面依据 |
|---|---|---|
|01-defaults|PASS|NMPA、注册人是、生产方否、转移否与已保存项目默认值一致|
|02-compiler|PASS|确认框固定U2/Dept，无可编辑控件|
|03-organization|PASS|确认框主管S2/Dept，与用户管理正式关系一致，无可编辑控件|
|04-market-edit|PASS|本次改CE，取消NMPA，选中集合及确认框为CE|
|05-identity-edit|PASS|注册人改否、生产方改是，控件和确认框均一致|
|06-transfer-edit|PASS|转移改是，转移至为DCC241016C01TransferR2|
|07-matrix|PASS|审批预检4节点就绪，批准人A2、原批准组937532及会签Dept正确|
|08-distribution|PASS|初始下发Dept与类型默认会签部门一致，独立读取两份名单|
|09-training|PASS|培训默认未选，确认框“不需要培训”|
|10-confirmation|PASS|真实PDF上传/非空渲染后核对全部目标字段，取消保留输入，页面移除并清理临时件|

本例编制人确认为`DCC241016C01U2（DCC241016C01Dept）`，主管为`DCC241016C01S2（DCC241016C01Dept）`。它们由真实确认框DOM断言，未用接口JSON或管理员身份替代。

## 清理及验收边界

最终运行`cleanupAttempted=true`、`cleanupCompleted=true`、`residual=[]`，自然请求可见upload-preview及upload-temporary/session-cleanup HTTP200。页面清理成功提示、源件列表和预览消失均通过。

没有点击最终“确认提交”、没有工作稿保存、没有审批申请或正式版本产生；finalSubmissionRequests为空。A/1只作为本次确认输入，不能把它写成已创建版本。本例不替代其他E2E的审批、生效或下发签收验证。

专用fixture和签名资格数据保留供Root review；这与本轮临时源件清理分别记录。浏览器/context关闭后保留trace，凭据仅在DPAPI任务临时配置中维护并被.gitignore排除。

## 保留的历史失败与旁路观察

- 首轮定义错误及5 PASS/5 BLOCKED结果保留；定义修复后的明确账号资格缺口已先报Root再通过真实页面补齐。
- 第一次完整运行`DCC241016C01-20261016155412944`为FAIL/exit1：缺本例类别UPLOAD规则，页面返回owner文案。曾仅对本例U2临时授OWNER排查，核对源码后已恢复EDIT；最终EDIT+独立UPLOAD许可通过，未将该文案当作“新上传必须OWNER”的业务规则。
- 该失败尝试未取得清理成功Toast，原cleanupCompleted=false/unknown回执保留，未覆盖为成功；最终独立成功运行的临时件已确认清理。
- “我的签名”一次浏览器刷新进入404，已保留现场；本人图片从正式菜单再次打开真实可见，没有重传或修改路由，该旁路观察未冒充原业务PASS。
- 旧R1及脚本异步选择/截图观察均保留，详见execution-log.md；无生产源码修复GREEN声明。

证据检查结果见`evidence-safety-check.json`。本轮已知凭据字节未出现在trace，network/HAR资源为空；旧R1已丢失的随机凭据未声称可做字节比对。Root review前未执行cleanup apply删除证据，也未执行Git/服务/数据库/迁移或流程发布操作。
