# C05-PRE-04：自定义矩阵身份传输合同不一致

## 最新授权下的自然响应实测（仅技术诊断）

- Root 明确允许读取页面自然预检响应后，原 U 于北京时间 23:32 从真实页面重走同一本例配置；未主动调用接口，未上传/保存/提交。
- preview-transport-diagnostic.json 记录 4 次自然预检响应的最小技术字段。最后一次 HTTP200/code0，approvalSourceMode=APPLICATION_CUSTOM，matrixId 字段不存在，routeId=null，approvalMatrix=null，ready=true。
- 最后一次响应 applicationCustomMatrix.approverUserId/documentControlUserId 均为 number；签名 canonical 内 requesterId、approverUserId、documentControlUserId 均为 string。没有落盘票据、canonical 全文、完整响应或 headers。
- 同次 DOM 仍显示“审批矩阵身份缺失或精度损失”，businessReadinessPassedByDom=false。响应 ready=true 不能替代页面通过，四个业务子例仍 BLOCKED。
- 这组运行字段与下方静态定位一致，将两演员传输类型冲突从静态线索推进为自然响应技术证据；未确认前端具体抛出调用栈。Root 独占 helper/index.vue 的正式修复和 RED/GREEN，本线程未动生产代码。
- 下方“未读取响应 JSON”的描述仅指 23:23 业务预检轮；本次 23:32 读取来自 Root 新增明确授权，技术证据与业务 oracle 保持独立。

- 实测时间：2026-10-16 23:23（Asia/Shanghai）；runtime PID34096，Jar SHA e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce。
- Root 已核验两精确 v6 定义 V2、两 editorSourceIsolationVerified=true、metadata 不变、10 旧资源/97 历史绑定保留。原 U910373 岗位已真实补齐，当前主管精确 S910374，双方启用与岗位已回读。
- 真实 UI：原 U 登录，选择本例 P1、项目文件夹、自定义其他类型、会签部门、A、D 后，dcc-upload-route-readiness 显示“审批矩阵身份缺失或精度损失”。4 次自然 POST /admin-api/dcc/controlled-files/route-preview 均 HTTP200，不能当业务成功。未上传源文件、未保存草稿、未提交正式申请。
- 证据：root-workflow-resume-result.json、fixture-manifest.json、fixture-evidence/1792164218487-final-preflight-failure.png 及 resume result 所列 trace。原定义错误和账号错误本轮页面没有复现；没有运行后端日志来宣称具体栈已通过。

## 只读源码依据与推断边界

1. IntRuoyiFronted/src/views/dcc/controlled-file/upload/approval-matrix.ts:31 的 identity 仅接受正整数格式 string；其他类型抛出本次精确可见错误。customValue:97/103 对 approverUserId/documentControlUserId 均调用该严格 identity；accept:246 首先用 sameCustom 比较请求和响应的 applicationCustomMatrix。其他 identity 调用点还包括 context.requesterId，故不凭提示单独断言具体运行字段。
2. CustomApplicationMatrixEditor.vue 把选择目录 ID 规范为 string；workflow.ts:16 的前端 ApplicationCustomMatrix 也声明 string|null。
3. IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrix.java:4 是 record(Long approverUserId, Long documentControlUserId)，字段没有字符串格式标注。
4. DccControlledFileRouteReadinessRespVO.java:12 直接暴露该 record。相邻 routeId 已有 JsonFormat(STRING)，两演员字段没有等价声明。
5. YudaoJacksonAutoConfiguration.java:35 为 Long 注册 NumberSerializer；NumberSerializer.java:31 对安全整数走 number 输出，超范围才写 string。当前本例 A/D 为 910375/910376，处于安全整数范围。
6. DccApplicationCustomMatrixService.java:238/245 对 canonical 内 context/requester/actors 显式 toString，因此 canonical 与响应 record 可能采用不同传输类型。

高置信静态线索是响应 record 的两演员 ID 以 number 传出而前端要求 string，符合本次 UI 错误；本线程未读取/解析响应 JSON，也未注入页面状态，故不把推断写成实际 payload 捕获。Root 可用定向序列化测试核验，不应修改正常 U/S/岗位或绕过前端校验。

## 边界与后续

本线程只维护 C05 测试资产，未改生产代码/全局序列化器、未重启服务/跑 Maven。由 Root 按正式合同修复响应 ID 表达并完成定向测试/运行态核对后，原 U 再走真实预检；就绪后按已有授权继续四子例。当前全部业务子例 BLOCKED，静态分析不是 E2E PASS。
