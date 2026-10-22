# LD03/G48 正常NEW单项目文件夹

Status: ready_for_closeout_for_Root_review。FE限定upload/index.vue、submitter.ts、workflow.ts上传请求DTO/guard及自有有限测试；后端Owner唯一mapping/Java/Maven，Root唯一运行/实库/Git/fulltypes/build。沿用G48设计，不把逻辑folderId、根ID或null塞入storage directoryId。共享task仍in_progress。

- Given正常CONTROLLED_FILE+NEW，When实际SFC选择类型/folder/原件并确认，Then不加载/显示/必填第二NAS目录，提交payload无directoryId属性，真实projectFolderId/type/product/source/attributes保持。
- Given同表已有external旧目录值，When进入NEW，Thenpayload省略而非复制该值；公有working/submit wrapper拒NEW caller任意directoryId，防额外参数绕server映射。
- Given外部评审或显式既有版本操作，When办理，Then原实际storage位置/目录字段按正式合同保留；不能全局删除校验或改成source根。
- Givenserver首次映射尚未执行，When前端提示，Then只说逻辑folder已选、系统提交时按配置确定存储位置，不假显示映射/目录/ACL已就绪。

RED/GREEN验证实际SFC handler及目录子树Vue渲染、实际submitter payload、两个真实API wrapper transport前guard。source/user/type/ticket/二次确认保留，network/Element宿主是明确离线边界，不真实E2E。Root最终统一types/构建和页面。

实际RED5项1PASS/4FAIL：旧NEW带directoryId、加载NAS、目录控件渲染和公有wrapper未拒extra目录；external/REVISION保留行为已PASS。GREEN5全PASS，最终9文件62执行PASS0fail/skip，3相关prodESLint exit0/0warning。新增publicSFC正常NEW无NAS tree/null draft字段仍确认送审、external缺目录仍拒；实际submitter NEW payload没有directoryId ownproperty、working/submit拒任意extra字段，external和REVISION transport保真实dir。旧application/free-type测试fixture补正式isNormalNewUpload ref而不改其行为断言；Vue宿主补原el-cascader以免测试告警。前端API guard按真实BE normalizeProcessType的空/省略默认受控语义同样拒NEW任意clientdir，不扩unknown process或删除external/REVISION目录合同。

目录readiness只显示真实projectfolder已选择及“系统提交时按配置确定”，不宣称派生storage/ACL已就绪。原产品typedprojection、blocker3span、LD02fulltype/实际File名和票据、attrs/confirmation保持；没有DB/API/runtime/Maven/Git/fulltypes/build动作。旧G47seal不动，新G48只替代声明的3生产及测试增量，Root综合后端/mapping迁移验证。

最终冻结验证：2026-10-05，g48-frontend-single-folder-final-frozen.log 为9文件62PASS/0fail/0skip、命令exit0；g48-frontend-single-folder-lint-frozen.log 为当前3生产文件ESLint --max-warnings 0，exit0且日志为空。此前26项source检查及62项r2属于重叠回归，不另累计。自此生产/测试冻结，完整类型、构建、后端映射及真实页面由Root继续。
