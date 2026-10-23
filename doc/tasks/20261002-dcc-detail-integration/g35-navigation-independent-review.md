# G35 送审后导航有限独立审查

2026-10-04；只读源码，未执行测试、浏览器、Maven、服务、DB或Git。4项读时hash/bytes见g35-navigation-independent-read-receipt.json。Owner报告7testsGREEN，本Reviewer未重跑，不把它们当真实页面PASS。

结论：本次修复正确连接真实route guard，未识别确认主线范围内新的导航断链。旧from=application-submit被remaining.ts beforeEnter拒绝的问题已修在实际caller，无放宽guard。

- detail/index.vue 2873导入正式helper；handleApplicationSubmitted（5961）先requireSubmittedApplicationId，只接受正decimal string且≤JavaLong最大。相同returnedid与当前controlledFileId字符串相等时reloadAll；新formalcandidateid使用buildSubmittedApplicationRoute后router.push。DetailApplicationPanel成功接口result的emit使用String(result)，符合确切返回协议，不作Number转换。
- helper仅保留实际管理from=browser/project-browser/workbench，且原query management=1或mode=manage；returnTo必须string、本dcc/controlled-file路径且无控制字符/backslash；id精确字符串拼detail路径。输出query只from、management:'1'、returnTo，不携前版taskId/processInstanceId/handling/viewer/traceability/projectIdentity。新候选详情不继承旧BPM/审批办理资格。
- remaining.ts实际guard1196：browser management=1+returnTo接受；project-browser/workbench management=1或mode=manage+同dccreturnTo接受。所以helper所有合法输出均被实际guard承认，旧bare application-submit仍返回DccControlledFileBrowser。原权限并未放宽成任意送审来源。
- 原合法入口证据：browser/index.vue3670起management:'1'/from:'browser'/returnTo真实browserreturnpath；Workbench433 mode:'manage'/from:'workbench'/returnTo route.fullPath。query helper准确保留字符串return路径，不重新推断目录或换旧file身份。
- test源码以TypeScript AST提取实际remaining guard和实际Vue handleApplicationSubmitted运行，核新id/相同id/bigLong/context/清旧query/不合法来源拒绝。并非只测复制一段理想helper。但VM/mocks不是浏览器、后端权限或正式送审结果的运行证明；Root负责必要detail/types/build及之后授权真实UI验收。

本报告仅审送审返回详情接线；不扩大URL策略/文案/菜单平台，不把Root同期发现新项目0accessrules问题掩为此导航修复内容。当前5文件架构/运行审批另由Root统一处理。
