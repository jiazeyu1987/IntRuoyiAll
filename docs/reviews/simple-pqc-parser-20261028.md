# 简易PQC解析验证（2026-10-28）

## 结论
指定 PQC-IDI-001 B2 Word 已通过真实页面导入、修改、预览和确认发布，正式 QA 规程60当前版本由70/B/5更新为79/B/6。QA页面已确认B/6、22项目及设备绑定。业务内容与原B/5一致。

用户本轮明确：以当前QA为准。气密性高压/低压沿用FM-E-24；整体粘结牢固度、负压检测沿用未绑定状态。仅点击“复用当前QA此项目的设备及补充配置”才确认沿用未绑定，不自动猜选设备。Word识别内容仍保留，不整体替换为QA模板。

## 实际路径
Playwright在任务环境8312登录，选择IDI/DCC129，上传指定Word，识别6道工序22项目；通过页面复用现有检验规则、逐项选择既有设备/未绑定状态，勾选抽样核对，预览业务差异0，点击确认发布。随后进入QA检验规程，选择IDI，查看B/6和两页项目，包括C01017及FM-E-24。业务动作全部经真实前端完成；数据库仅只读核验。

已验证标准文本修改、保存草稿、刷新恢复、还原；未确认设备时发布禁用；发布成功后锁定内容。浏览器pageerror为0。

## 保存结果
只读比对旧70与新79的名称、编号、生效日期、检验规则、末检适用性、6工序顺序、22项目标准/方法/抽样/结果类型/数值配置及设备编号、默认设备、启用状态、实际排列顺序，业务差异0。旧B/5的工序快照与发布前保存的基线完全相同。

不是数据库逐字节相同：版本号、版本ID和项目编码新建；设备编号排序值由0规范为1（每组仅一个编号，实际顺序不变）；来源信息按本次导入保存。生效日期按文档正文及QA为2025-09-30，不按文件名推断。

证据：output/simple-pqc-final-comparison.json、output/simple-pqc-published-snapshots.jsonl、output/simple-pqc-published-devices.jsonl、output/simple-pqc-published.png、output/simple-pqc-qa-published.png。可重跑只读导出后的比较脚本output/verify-simple-pqc-published.py。

## 实现链路与检查
入口simple-pqc-parser/index.vue → parse/import及可视化编辑 → simplePqc.ts校验与差异 → publishSimplePqc → MesQaInspectionRegulationController(simple-parser/publish，既有mes:qc-template:update权限) → MesSimplePqcPublishService事务内调用正式规程publish及设备replaceItemConfigs → QA正式页面读取。新项目编码隔离历史版本设备关联；设备ID回查台账名称；异常抛出并回滚。

前端5项逻辑测试、ESLint通过；后端2项测试通过；生产类编译通过；菜单迁移预检通过。全量类型检查有其他模块错误，不声明全库构建通过。本页已删除旧检查发现的未使用类型导入，复检结果见output/simple-pqc-typecheck-verified.log。

任务环境：C:/Users/BJB110/.codex/worktrees/simple-parser-verification/IntRuoyi，前端8312/后端48312。主8081未切换到新版，未进行主服务部署、提交或推送。解析规则来源的标题截断清理已编译；本轮运行包仍含旧的末尾标题展示，未影响识别业务数据或发布结果。
