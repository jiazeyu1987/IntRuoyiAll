# E2E-13 R6 原O/4222定向页面复验准备

状态：BLOCKED_TEST_PRECONDITION_PROJECT_READ_MEMBERSHIP_MISSING。关联PASS保留；正文缺原O对项目302阅读资格，按测试前置缺口交接，不改权限或Guard。日常可见存储目录/全域/原编号查询实际exit0，仅证实UI入口不可用，未执行DAILY正文端点。最终证据与边界见r6-viewer-findings.md；下述为历史执行前计划，由最新只读诊断授权和结果取代。

## 范围与运行门禁

原账号O910463，租户芋道源码，文件2054545668044084222，编号DCC241016C13R18ADJUST，A/1，BPM fb0c1653-ca6f-11f1-9342-b082e25ec548，负责人签名860。既有受保护O凭据仅用于真实登录，不输出/重置/猜测，不新增账号/授权。

Root新包health与真实页面登录核验通过，并明确通知本例可执行后才启动。接收Root实际新proof路径、Jar/PID/SHA256、前端身份与8061/48061，确认嵌套DCC Query字节包含两处已批准修改。已接受Query源码SHA256为1a051841ea33583cdaec2c142e2db2ccaaa368fffc6eed212a35df59d0e118cb；前轮新增单类14例GREEN/exit0及有效RED保留。Root若给出不同源码/字节身份，应先只读核对，禁止借旧包执行。

## Given / When / Then 与真实页面步骤

### 1. 关联分区

Given 原O当前培训职责、原文件/版本/BPM、原负责人签名860不变；When O登录并从真实工作台原行打开详情，等待所选版本关联结束加载；Then 分区显示同一原文件、A/1和精确申请轮次的关联来源及正式集合或正式空态，不出现权限拒绝或未核验轮次提示。仅将原申请/负责人身份作为入口护栏，不重放批准流程或重做整套节点验收。

从真实菜单/工作台进入，唯一业务行必须匹配DCC241016C13R18ADJUST/A1；点击该行dcc-offline-training-open，不手拼详情URL。分别记录关联DOM、真实空态/行数、加载完成、错误提示与截图。等待关联分区自身加载完成后再取DOM，避免R5早期DOM遗漏稍后出现的权限错误。

### 2. 实际培训正文

Given 后端独立PREVIEW动作已由新Query正式投影且O是本轮签名负责人；When 点击真实“预览受控文件”；Then 自然打开的viewer上下文必须为TRAINING_APPLICATION，controlledFileId=2054545668044084222、expectedVersionNo=A/1、applicationType=UPLOAD、bpmRound=上述原BPM，不能落入DAILY或APPROVAL_HISTORY。

核验viewer可见版本身份、PDF实际页图/文本及截图；非零canvas尺寸仅是技术前置，须人工视觉确认原PDF正文，空画布、iframe存在、HTTP200或弹窗成功均不足以通过。自然读取路由中的bodyReadContext用于页面身份断言，禁止自行构造/替换上下文、API/fetch/APIRequest、响应JSON oracle或SQL。缺按钮/弹窗/正文时分别准确FAIL，不用辅助信息通过掩盖正文失败。关闭viewer后确认原详情身份、负责人、签名保持，O仍无培训上传按钮。

### 3. 应拒绝的日常入口

Given O具有当前培训职责但该原版本尚处待培训状态，未正式日常启用；When 同一O从页面正常受控文件浏览菜单进入，选页面提供的全局搜索范围并以原文件编号查询；Then 不得取得该待培训版本的日常正文。记录真实菜单、搜索条件/范围、加载完成、目标文件/版本行与可点击入口状态。

若目标版本无行或没有日常预览入口，只记录“日常入口不可用”，保留页面DOM/空态截图和对应自然请求；不能声称已调用并验证DAILY正文端点拒绝。若真实页面确有该精确版本的日常预览入口，实际点击，断言自然上下文DAILY及明确权限拒绝，正文不得渲染；如能读取则FAIL并交Root，禁止绕过。无菜单/全局范围/精确查询前置时具体BLOCKED，不能把未查到其它目录当全局拒绝。不得手输viewer地址、改URL/query、注入路由或用接口补齐负向动作。既有真实Guard单测中的DAILY拒绝是独立证据，不能替代本次UI结论。

## 证据、结束与禁止项

登录不录trace，登录后保存trace、分区DOM、实际正文与日常入口截图、自然请求method/path/status及页面异常；不解析响应JSON。分别记录预览/阅读审计请求与流程/培训上传等业务写请求，不能把实际发生的阅读审计写请求报为全部写请求0。结束关闭viewer/context/browser，脱敏trace并验证ZIP与证据引用，保留原业务样本；如有任务临时授权/锁/后台任务须恢复，本计划不创建这些资产。

沿用本任务既有Playwright/credential-store/sanitize-traces能力，收到Root精确proof和放行后才绑定本次运行身份并执行；旧r5脚本的固定PID/SHA不能直接用于R6。未执行不得写PASS。

Maven窗口持续RELEASED；旧323/37/14及新增单类14GREEN保留不重跑。冻结生产、Guard、前端和测试源码；不重放审批、不上传、不改权限/数据、不API/SQL、不构建/重启/部署、不Git提交推送、不启用子Agent或跨线程消息。新问题只保存直接事实交Root协调。
