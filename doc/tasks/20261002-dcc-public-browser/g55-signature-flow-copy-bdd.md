# G55 — 签名弹框流转说明

Status: ready_for_closeout_for_Root_review。Root真实完成会签、批准和文控审核受控时看到“末级文控批准后…直接生效”旧提示，违反HTML v1.6的受控日期/预设生效日期及独立文控下发合同。范围只detail/index.vue该computed返回文案与定向文案测试，不改流程、权限、提交或G54两源。

Given上传/升版或独立作废均使用同签名弹框，When通过模式显示提交后流转，Then分列准确“上传、升版：会签→批准→培训（如需）→文控审核→受控→下发；按预设生效日期生效，新版生效时旧版自动作废”和“作废：会签→批准，批准通过即完成作废并结束流程”。不能猜File旧key/父mode或新增读接口，不描述作废还受控/下发，不使生效与下发互为额外前置。

Given驳回模式，When显示流转，Then原驳回说明原样。文案有效RED执行生产computed与真实descriptions-item AST Vue渲染，GREEN同源及有限邻近回归/lint。Root类型/构建及实际页面独立执行；本Agent不DB/API/运行/browser/Git。

实际RED4项1PASS/3FAIL/exit1，旧reject文本已PASS，旧throughcomputed及实际Vue描述子树不符确认合同；GREEN含相邻round-context共2files14执行PASS/0fail/0skip/exit0，唯一生产源lint exit0日志空(session78234终态)。文本onlyliteral替换，不加mode/type读取或修改审批逻辑；测试文件位于e2e目录但只Node执行，非真实页面E2E。Root另发现离开详情空ID请求将独立G55后续BDD/RED处理，不混本文案seal。
