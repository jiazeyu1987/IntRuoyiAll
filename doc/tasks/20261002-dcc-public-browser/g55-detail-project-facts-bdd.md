# G55 r3 — 详情项目文字读取正式项目事实

Status: ready_for_closeout_for_Root_review。Root实际详情“DCC项目”用了产品文字，已授权BE在详情toRespVO投影正式file.dccProjectCodeId同tenant项目projectName和projectCode（null历史未绑定不拒）。本Agent只当前detail computed及ControlledFileVO TS声明，独立BDD/TDD；不改变productName/Code字段含义、真实projectID导航/未绑定提示、G54两源。

Given文件绑定正式项目且实际项目名/码不同于产品名/码，When详情computed和原“DCC项目”描述子树显示，Then只formatDetailPath([projectName,projectCode])；产品自身栏继续原productCode，不冒项目。Given历史未绑定或项目名/码为空，Then不借产品猜项目、保原`-`及未绑定DCC项目代码提示，仍能查看文件。

新apiControlledFileVO补projectName?:string|null和projectCode?:string|null（后端name既有，当前该TS VO原未声明；其他VO的同名字段不可代），不将catalogID/产品ID当项目ID。实际computed/原ASTVue渲染RED→GREEN+copy/route/round/dialog有限回归及2源lint，最终r3统一freeze交Roottypes/build/真实详情验收。无运行/API/DB/browser/Git。

实际RED3项全FAIL/exit1，旧product字段被错误当项目；GREEN最终与前copy4/route4及既有round/dialog共5files31执行全部PASS/0fail/0skip/exit0，两生产文件lint0warning/exit0。当前computed一行换正式name/code、TS仅补这两个字段；原产品栏/ID导航/未绑定提示保持。真实投影由BE Owner/Root另验，离线computed+原子树Vue渲染不冒真实页面PASS。G55r3封本三有限修复当前同源，旧copyseal/G54seal保留；API workflow旧pin只由本两字段声明新阶段替代，不变上传提交逻辑。
