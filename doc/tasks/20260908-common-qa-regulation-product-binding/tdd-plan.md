# TDD 计划

## 后端 RED/GREEN 顺序

1. 现状适配门禁：RED 证明现有 QA 规程通过 DCC 项目代码定位；GREEN 确认 A/B/C 独立主档和产品绑定四元组后继续。
2. 已发布版本不可编辑：RED 验证更新发布版本失败；GREEN 增加版本状态门禁。
3. 多产品复用：RED 验证 A 绑定产品 1 后仍可绑定产品 2；GREEN 完成关系表服务。
4. 单产品唯一绑定：RED 验证同产品第二条启用绑定冲突；GREEN 增加事务锁和唯一约束。
5. 变更版本事务：RED 模拟新绑定写入失败，旧绑定不能被停用；GREEN 完成事务边界。
6. 租户隔离：RED 使用跨租户产品/版本 ID；GREEN 返回明确租户错误且零写入。
7. 退休联动：RED 证明启用绑定指向 `RETIRED` 版本；GREEN 以事务拒绝或先停用绑定再退休。
8. 并发与幂等：RED 两个并发绑定和重复幂等键；GREEN 仅一个成功且不重复写入。
9. A 多来源导入：RED 证明当前单文件导入只能覆盖一个草稿；GREEN 支持 `files[] + sourceStages[]` 同事务导入并保留两条来源记录。
10. B 单来源隔离：RED 证明 B 会错误继承 A 的第二来源；GREEN B 只包含自己的来源文件。
11. 来源表头白名单：RED 未知表头被当作空成功；GREEN 返回 `COMMON_REGULATION_SOURCE_DOCUMENT_INVALID` 且零写入。
12. 条件标准确认：RED A-01 的“百瑞吉产品要求”被默认套用到所有产品；GREEN 生成 `PRODUCT_SET_REQUIRED` 条件标准并要求正式产品 ID。
13. 版本适用性：RED 非适用产品可绑定 B；GREEN `PRODUCT_SET` 版本仅允许显式产品 ID 绑定。
14. 来源逐项追溯：RED 发布快照缺 Word 行号/原始摘录；GREEN 每个项目保存 source 映射并在版本证据返回。
15. Warning 分级：RED schema warning 被当作成功或失败一刀切；GREEN 非内容 warning 可审计放行，内容阻断 warning 失败。

## 前端 RED/GREEN 顺序

1. 版本候选过滤：RED 草稿/停用/产品规程出现在下拉；GREEN 按后端正式候选接口渲染。
2. 绑定摘要：RED 列表缺产品/版本/工序数/项目数；GREEN 增加响应类型和列。
3. 冲突交互：RED 重复点击或已有绑定导致误报成功；GREEN loading、错误保留和变更引导。
4. 已发布版本只读：RED 仍显示编辑；GREEN 显示复制新版本。
5. 多来源预览：RED A 版本只显示单文件摘要；GREEN 显示来源文件清单、包装阶段、工序数、项目数。
6. DCC 身份展示：RED 通用规程创建/绑定预览缺 DCC；GREEN 创建抽屉、列表、版本抽屉和绑定预览都显示 DCC 项目代码/名称。
7. 条件标准交互：RED 未确认条件标准仍可发布；GREEN 发布按钮阻断并定位来源文件/行号。

## 命令模板

- 后端：`mvn -pl yudao-module-mes -am "-Dtest=<target-tests>" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- SQL 合同：`python -X utf8 -m pytest script/tests/test_<migration>.py -q`
- 前端：`node tests/e2e/<common-regulation-static>.spec.js`
- 类型：`pnpm ts:check`
- 文档：对应 skill validator。
- Word 样本验证：`officecli view/get` 读取 `resource/通用检验规程` 下 A/B 三份真实样本，并记录来源摘要。
- Fixture 断言：A-01 期望 3 个主要项目和 1 个条件标准；A-02 期望 3 个主要项目；B-01 期望 3 个主要项目和 `PRODUCT_SET` 适用性验证。
