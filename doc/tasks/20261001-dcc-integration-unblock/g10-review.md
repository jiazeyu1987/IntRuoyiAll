# G10 文件列表轻量关联入口

2026-10-03。当前实际主目录int_qms，整合分支codex/20261001-dcc-integration、HEAD a801dc8b91579241e221d129ab34343997673f40。前一轮有实际产品核对/后端引用明细/组合验证进展，本轮不作为全目标阻塞。

## 当前实际执行与Owner

正式Agent状态工具核实backend_closure/detail_closure/upload_closure都已completed。本轮已登记后续UI-02/UI-03/UI-05分工，但尚无新followup回执；不会用task文件或计划冒称Agent运行。Root在已有主任务补BDD并暂接UI-02父页/独立弹框，不并写DetailRelationsPanel/shared API/此前申请代码。后续必须正式续派未实施范围。

## UI-02实际修复及验证

公共ProjectBrowserPanel的实际列表行增加“关联”。独立ProjectFileRelationsDialog只用所点文件的relation-permissions轻量正式读取，精确验证文件ID/租户/Master/项目/编号/名称/版本；身份不符明确报错，不改强详情鉴权、不使用项目详情补读，不新增权限API。通过既有DetailRelationFile形状挂接DetailRelationsPanel，当前/历史来源、编辑和正文权限仍由该组件及正式后端守卫控制。

列表context包括目标项目/目录、全局/目录scope、关键字、版本筛选、页码；切列表或开始重新读取关闭旧关系上下文。关闭和卸载使旧元数据请求失效。真正选历史版时保持原版本身份，让已存在关系组件明确只读，不跳latest源来获得编辑资格。

先在真实转译/渲染ProjectBrowserPanel的定向测试RED1，旧源码没有关联按钮。生产接线后GREEN12；补外租户/错Master、关闭/切scope/unmount晚响应回归，最终四文件组合65 PASS，0fail/skip/cancel（g10-relations-entry-regression.log）。错误提示最初断言了stub文字而Element alert存title属性，改为检查真实title后PASS；这是fixture断言修正，不能算业务RED。

两生产文件完整ESLint exit0（g10-relations-entry-lint.log）。Root统一正式project types和env.local build session15302已启动，待正式句柄确认。真正DetailRelationsPanel在父测试中是明确边界组件替身，用来核对所传身份和不读强详情；原详细关系行为测试另独立真实执行，组合65不宣称整页运行或E2E。

## 同一关联窗口步骤追加收口

Root复核HTML flow-07/08：仅外层关系表再让用户点击关联不满足直接打开同一选择窗口的步骤。先修正测试宿主读取Element原生model-value属性，保留最初fixture失败；之后实际业务RED1/25为可编辑来源加载后选择器仍关闭，只读案例已过。在主任务登记写入扩展后，新增DetailRelationsPanel/DccFileRelations可选autoOpenEditor，仅新公共列表入口传true；正式当前源、canEdit、目录和关系全部就绪后自动打开既有DccFileSelector，不产生写入。编辑窗口打开事件由父面板消费一次，后续保存/刷新不会重复自动打开；原详情/上传未传此参数，原步骤保留。

追加定向五文件85PASS，最终11文件组合136 PASS/0fail/skip；四生产文件lint0/0。session15302的此前types/build已核实exit0，但不覆盖这次auto-open追加；最终types/build在正式session28601运行，待回执确认。完整行为/边界日志均在既有browser任务路径，不把测试宿主当真实页面。

最终session28601已正式确认exit0/Build successful，g10-ui-receipt.json绑定16项当前生产/测试资产SHA256和136组合结果。没有修改正式tsconfig/env模式、安装依赖或执行实际页面。旧Browserslist警告保留，不以无关依赖更新扩任务。主任务Keep/状态和验收矩阵同步，不标completed。

## 尚未完成

UI-03选择器左侧跨项目目录、UI-05引用使用明细前端仍未实现；后端usage-page已经审查通过。UI-01、UI-04、UI-06前批证据保留。真实环境MySQL23306/Redis26379仍不可用，启动共享依赖和提醒业务值问题待答；不启动实库迁移，不改变主服务、48081或共享任务。

没有Git提交、合入、真实E2E或发布。完整目标持续active，不能因这一公共按钮通过就宣告完成。
