# G68 原生升版撤回后提示修正文稿重提

Status: ready_for_closeout_for_Root_review。共享任务保持in_progress。Root实际r15/112原生REVISION 4043 WITHDRAWN仍提示可删除/legacy重提；BE已禁止该类旧动作并接通本人检出→新正文检入B/1-1→同B/1 attempt2。

Given原文件statusWITHDRAWN、准确原生revision key与实际BPM、尚无后继，WhenParent实际detailHandlingSummary和原组件显示，Then提示通过现“检出 / 检入”修正正文后重提，目标由正式服务保持、撤回记录和历史保留。Givenlegacy或缺正式native身份、已有后继，Then保原summary；不新按钮/路由/API/节点，不改审批或业务mutations。

最小生产Parent computed，在其它阶段提示前识别该具体文件状态，不修改共用legacyhelper或新增DTO。先actualcomputed/原template RED→GREEN，近期native/round/readonly回归和Parentlint；无类型变化不全types、不build（Root明确此batch），G67通知两个source/test全pin保。所有actualUI/DB/服务/Maven/Git仍Root。

Root随后明确原withdraw两个dropdown也在授权显示范围：canHandleWithdrawnFlow保原status/requester/无successor，再要求有正式DELETE或RESUBMIT允许动作；各项分别消费canDeleteWithdrawnFlow/canResubmitWithdrawnFlow，缺projection和native无动作均不显示，legacy单项授权不显示另一项。原handlers/routes/后端资格不改，不新UIaction或权限。

有效旧actualcomputed/原template RED3=1PASS/2FAIL（effective-red.log），菜单追加场景RED4=1PASS/3FAIL（menu-red.log）。首次RED夹具legacy传undefined仍触默认scope、renderer缺approvalTodoTask导致不相关失败/警告，red.log原保，不计业务RED；纠正宿主后旧source nativehint/template失败、legacy保留PASS，再执行有效菜单RED。最终测试去掉缺新computed时defaultfalse分支，并使用真实shared/lifecycle与formaction helpers串实际Parent gate及原dropdown renderer，不镜像allowedActions。

最后5文件16项PASS、0fail/skip、exit0（final-frozen.log），新4项含当前native已撤回、残余阶段优先级、legacy/缺原身份/有后继原summary、原说明template、两独立菜单原renderer；旧12 native阶段/completion/审批中心/readonly回归不累加历史。Parent ESLint --max-warnings0实际exit0、无输出（39380终态），此后生产不改。没有新类型/interface/import，按Root不重复全types/build，G67旧build不冒新Parent检查。

Source/test冻结，g68-withdrawn-revision-hint-fingerprints.json仅Parent及newtest，原G67通知2生产/1test精确raw均保持，旧seals不覆。Root实际新hint/菜单及本人新正文同B1重提仍待复验，本Agent未UI/API/DB/Git/服务/Maven操作。
