# G04 最终开发验证报告

## A32 + R09 最终开发验证（2026-10-15）
保留原G04会签证据，不用其旧PASS代替本轮结果。本轮只修改4份获授FE源和所属/受影响测试，旧BE与公共fixture未改。

|验证|实际结果|
|---|---|
|A32正式RED（实际Vue handlers/state/payload/summary）|8个A32场景1PASS/7FAIL；CUSTOM save公共after阻断、配置与部门票据不一致被放行、canonical阶段/上下文错配、缺文控/custom确认、项目切换旧custom保留；修正VNode测试helper后相同7项真实业务失败|
|类型切换关联清理追加RED/GREEN|1/1FAIL：旧关联8保留；最小清理后1/1PASS|
|CUSTOM RESERVED精确定义追加RED/GREEN|1/1FAIL：保存V1被响应V2静默替换；增加原保存执行合同/定义绑定后定向消费组11/11PASS|
|R09原PUBLIC V2 guard RED|2项1PASS/1FAIL，V2被V1-only拒绝；unknown/raw原组拒绝正确|
|R09同V2不同来源回归|追加CUSTOM与PUBLIC来源区分RED后，3/3PASS；未将CUSTOM矩阵伪造成公共基线|
|最终合并回归|260/260PASS，0fail/0skip，4生产源SOURCE_STABILITY PASS；完整命令与输入文件列表/源码hash/loghash见a32-r09-verification.json|

最终合并命令等价于在IntRuoyiFronted运行 `node --test tests/unit/dcc-upload-*.test.cjs tests/unit/dcc-detail-g03-custom-application.test.cjs`（manifest保存实际展开文件列表）。包含真实整页SFC编译、CustomEditor真实模板事件、实际refresh/save/submit/dirty/confirmation处理器，以及正式接口类型的局部typecheck，不是全类型或工程构建。

验收覆盖：唯一实际两账号/会签名单；第一次非空部门复制下发一次、之后独立；显式[]不恢复默认且至少1才submit；项目/类型/来源切换确认及取消保留、确认后清原关联/配置/票据；未就绪CUSTOM用真实partial/null + SAVE token保存无审批提交；源与全部附件ready后列完整确认，取消零写请求且保填；当前输入晚变化拒绝；PREVIEW/RESERVED/保存file/round/context/当前或旧精确prepared身份不混用；PUBLIC_V1/V2原批准组均完整校验，unknown/raw不走fallback。G12可选说明、DELAYED/null及Long身份相邻断言保留。

失败分类：旧VM宿主缺新增sourceMode/isCustom等正式依赖及scopeKey不匹配，只补各自测试注入，45项PUBLIC page回归恢复；没有删除断言或为测试改生产fallback。另有自有VNode读取helper遗漏、跨VM数组比较方式问题，已修测试，不算业务RED。合并首次252项4FAIL最后均为G12宿主缺实际类型依赖，补齐后通过；完整原业务约束仍保留。共享fixture未修改。

依赖边界：测试server端口提供正式形状canonical和测试HMAC用于验证实际载荷传递/相等；客户端不声称认证HMAC或真实人员资格，正式后端仍验证。当前G03 Panel消费者只读2项通过，实际后端freeze/rework/真实服务/数据库事务/E2E未执行。本轮无Maven/sharedtarget/fulltype/build/服务/Git/实库操作、不增依赖。最终4生产+11测试hash在a32-upload-after.json，原始4源hash在a32-upload-before.json。

## 实现与验收映射
|BDD|实际验证|
|---|---|
|公共默认A/B，删原必选A保留B|当前project/Resolver允许只B；当前Workflow.PUBLIC preview只返回B，A原组applied=false且原required/marker保留，不读取A负责人|
|全部删除拒绝|project[]明确SIGNOFF_EMPTY；Resolver[]在目录查询前拒绝；当前PUBLIC preview ready=false且无节点；前端实际refresh不发送空选择并显示精确提示|
|保留B增加C，B/C全部必须通过|节点ALL/100/true；技术HMAC实际名单B/C；同leader不去重；冻结签名门禁拒绝缺C、拒绝、旧版本/旧轮次/无密码校验/缺指派，重复B不代替C|
|公共配置不变|派生/解析/预检前后原节点事实全相同；marker/required与原候选保留；前端批准/文控原必需组不可跳过|
|首次类型含可选会签来源|实际project(null)与当前Workflow.PUBLIC preview返回完整A/B，全◎/▲required=false也默认带出；前端实际refresh两份数组独立，后续会签B/C不改下发A/B|

## 实际RED/GREEN/REGRESSION
- 2026-10-14 node --test tests/unit/dcc-upload-removable-signoff.test.cjs：exit1，5 tests / 2预期失败；run-private-tests.ps1 -Phase red：exit1，31 tests / 5预期失败。均为本轮真实RED。
- 2026-10-15 run-private-tests.ps1 -Phase default-red：exit1，39 tests / 35通过 / 4预期失败 / 0skip；3个project(null)分支与当前Workflow.PUBLIC preview默认optional阻断。
- run-private-tests.ps1 -Phase green：exit0，39/39 PASS，0skip。
- run-private-tests.ps1 -Phase regression：exit0，76/76 PASS，0skip；额外编译/执行当前DccFrozenApprovalSignaturesTest、DccNativeSupervisorSourceTest、DccControlledFileRouteReadinessServiceTest。SOURCE_STABILITY: PASS，全部显式编译源hash前后一致。
- FE在IntRuoyiFronted执行 node --test tests/unit/dcc-upload-removable-signoff.test.cjs tests/unit/dcc-upload-signoff-departments.test.cjs tests/unit/dcc-upload-runtime-approval-groups.test.cjs：exit0，26/26 PASS。局部helper对正式类型声明type check也包含，不是全类型构建。

## 准确失败分类
- 默认RED首次扩编未执行测试：依赖BPM DTO旧class缺当前Workflow所需setProcessDefinitionId，编译失败不算业务RED。只读补编当前DTO源码到任务private目录后执行真实39项RED；未修改DTO/shared target。
- FE新增默认测试首轮26项/1失败：测试对matrixAfter与approvalMatrix同对象重复push来源，fixture失真。只修自有测试引用去重，未改公共fixture或生产；重跑26/26。
- javac警告在当前PowerShell管道显示乱码；JUnit中文失败/提示和统计正常。警告未作为业务证据，未改写原RED日志。

## 编译及依赖边界
私有编译当前G04两份BE生产及当前Workflow/Readiness/Token/FrozenApprovalSignatures、BPM DTO和6份测试，输出只在本任务private-classes。编译/运行前后源码manifest稳定，保存在private-verification-manifest.json；最终改动指纹见final-source-manifest.json。

其余module/framework依赖读现有target/classpath及本地依赖，未声称完整工程源码编译。正式组织目录、项目授权、源矩阵/流程准备选择、签名授权/图片为明确隔离端口替身；实际派生、预检聚合、技术HMAC和最终证据判断执行当前源码。未验证真实组织/签名服务、数据库事务写入或运行页面。

## 未执行及收尾
Maven/全类型/工程大构建、E2E、服务启停、部署、远端、真实业务库写入、Git提交推送均未执行。Root后续集成票与各G接线独立处理；G04提交软件阶段review/文件归还请求，不自动completed。报告/UTF-8/hash与cleanup记录见execution-log.md。

2026-10-15最终归还前8份源码manifest全部SHA256 MATCH。cleanup preview exit0，cleanup apply在Git工程收尾blocked状态下实际拒绝，零删除，不能写为PASS。正式归还Resolver/Derivation及所属测试已记review-ready；完整reactor由Root最终票统一。
