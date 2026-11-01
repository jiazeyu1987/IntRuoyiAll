# 简易通用解析交付与验证

日期：2026-10-31。代码已同步至 `E:/IntRuoyi`，未提交或推送。

## 功能结果

- 入口：eDHR批记录 → 检验表单 → 简易通用解析。
- 使用现有通用规程 Word 解析与字段映射，先返回只读解析结果。支持修改规程信息、工序、检验项目、标准、方法、抽样、结果类型和设备，支持本地草稿恢复、内容预览及试填。
- 仅“确认发布”调用正式发布事务，归属限定为 `MES_QA_COMMON`。沿用既有版本校验及发布规则；旧当前版本停用，历史快照保留。
- 发布后通过“管理通用规程套”进入原页面组装文档，再由 QA 页面关联套版本。解析器不自动改变规程套组成或产品绑定。
- 同名项目结合原始标准和原始方法唯一匹配，保留各自编码与设备；不能唯一匹配时明确报错。
- 修复 QA“查看通用规程”进入缓存页面后未按地址参数切换规程套的问题。

## 真实前端 E2E

环境：预留槽位69，前端8364、后端48364；租户“芋道源码”。所有解析、编辑、草稿保存恢复、预览、试填、发布、组套和产品绑定动作均通过 Playwright 操作真实页面，数据库仅只读核验业务结果。

输入目录：`E:/IntRuoyi/resource/通用检验规程`。

| 输入文件 | 正式发布版本 | 版本ID | 解析结果 |
| --- | --- | --- | --- |
| PQC-CR-003（A 7）初包装过程检验规程.docx(1).docx | A/9 | 82 | 1工序、4项 |
| PQC-CR-004（A 1）大中包装过程检验规程 (2)(1).docx | A/3 | 81 | 1工序、3项 |
| PQC-MECR-001（B 1）美联初包装过程检验规程--2026.08.10生效(1).docx | B/2 | 83 | 1工序、4项 |

1. 大中包装修改接受标准、不合格处理要求；保存本地草稿、刷新、恢复，编辑内容一致；内容预览展示修改，返回修改后完成3个项目的巡检试填。
2. 只读对比确认：解析、编辑、草稿恢复、预览期间，三份通用规程及其版本没有正式写入。确认发布后新增版本快照保留人工修改。
3. 初包装两个“密封强度”项目分别保留1.2N、1.5N标准、独立编码与设备；美联文档的同名项目也可正常升级解析发布。
4. 黄框组 `SCP-YELLOW-20261031`，套ID8、套版本ID4：CR003 A/9＋CR004 A/3，2份文档7项。
5. 红框组 `SCP-RED-20261031`，套ID9、套版本ID5：MECR001 B/2，1份文档4项。
6. QA选择IDI项目，产品ID14依次绑定黄框、红框套版本，点击“查看通用规程”分别显示正确组成。回归覆盖先缓存红框，再从QA查看黄框。
7. 验收结束通过页面恢复产品14原套版本ID1，刷新后仍正确。数据库只读确认该产品仅有一条启用绑定且指向原版本。
8. 对比原版本的 `snapshot_json`，历史内容未修改；原当前版本按既有发布规则变为 `RETIRED`。

证据目录：`E:/IntRuoyi/output/playwright/simple-common-parser-20261031/`，包含发布前预览、试填完成、两组组成、QA两次绑定及恢复原绑定截图。

## 检查结果与限制

- 标准 `backend-package.py build`：成功，来源与产物校验通过；使用该包启动任务自有后端。
- `MesQaInspectionRegulationWordImportServiceTest`：16项通过。
- `MesQaInspectionRegulationWordParserTest`：8项通过。修正一处既有断言，使其检查实际“生效/实施日期”错误文案，未改变解析器行为。
- 本次前端文件与 QA 缓存切换修改：ESLint通过；新文件Prettier检查通过。
- 菜单迁移检查通过；迁移包含既有检验解析角色与租户套餐授权，不新增权限标识。
- 完整前端类型检查存在33项既有错误，涉及Echart、DCC、系统管理等文件；本次新增文件未报错。不能将全项目类型检查报告为通过。
- Windows长路径导致包来源指纹读取失败，已在文件摘要读取处使用Windows扩展路径，完整包校验实测通过。
- 隔离验证环境同步了主工作区已有的两份批记录测试编译修正，以及共用试填组件修正；这些文件未作为本任务改动覆盖回主工作区。
- 本次没有构建生产发布包、部署远程环境、重启主分支后端或执行Git提交推送。

## 业务链路代码依据

| 节点 | 代码依据 |
| --- | --- |
| 菜单及授权 | `IntRuoyiBackend/sql/mysql/20261031_simple_common_parser_menu.sql`、`config/menu-migrations.json` |
| 选择项目、上传、编辑及本地草稿 | `IntRuoyiFronted/src/views/form-center/simple-common-parser/index.vue` |
| 发布前检查及问题定位 | `IntRuoyiFronted/src/views/form-center/simple-common-parser/draft.ts` |
| 只读解析接口、权限与正式发布入口 | `MesSimpleCommonParserController.java` 的 `parse`、`publish` |
| 既有解析算法、通用版本基线、同名项目映射 | `MesQaInspectionRegulationWordImportService.java` 的 `previewCommonWord`、`buildSaveRequest` |
| 发布事务、归属校验、版本及快照落库 | 既有 `MesQaInspectionRegulationServiceImpl.publish`、`resolveRegulation` |
| 组套、QA关联及查看正确套版本 | `QaRegulationPage.vue` 的套版本维护、`handleBindCommonRegulationVersion`、`viewCurrentCommonRegulation`及路由监听 |
