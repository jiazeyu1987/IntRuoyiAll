# 问题 3 验收记录

2026-11-07 续验通过：已补齐真实审批、电子签名、发布与作废生效；精确签名/时间及失败边界见 [完整流程验收记录](../../../output/word1106-e2e/issue3-fullflow/issue3-fullflow-result.md)。公共上传 4584 已明确确认 V3→V4（新任务部门 910384）并正式发布 ACTIVE A/1，五份节点签名 1701–1705 均 VALID，发布件/盖章件可用。其他类型独立 CUSTOM 升版目标 4591 已正式发布 ACTIVE A/2，签名 1706–1710 均 VALID。普通 Editor 通过 TARGET/F1 固定引用读到公共 A/1 的真实 PDF，且只读详情无编辑/下载扩权。随后公共作废申请 40 复用 V4，真实完成主管、指派、独立会签与批准（1711–1714 均 VALID），23:26:42 生效为 OBSOLETE，master 当前执行指针清空；保留引用的同一正文 URL 打开及刷新均拒绝，引用行仍在且浏览禁用。全部业务动作均通过 Playwright 真实页面完成。

以下为 2026-11-06 原始草稿阶段记录，保留其当时未提交边界。

正式结果及完整边界见 [Issue 3 真实页面验收记录](../../../output/word1106-e2e/issue3-agent3/issue3-result.md)。全部业务写入由 Playwright 真实页面完成，租户为芋道源码。

最终包 `2026-11-06-21-35-19`：任务 Reviewer 的正常上传文件 `2054545668044084584` 已保存草稿，复用类别 908776 的公共矩阵 V3（route 914058），通过真实运行预检到达最终确认，部门 DCC241016C12R3D1 与批准人 admin 均在截图实际视口中。确认已取消，未提交审批。之后保存本次选择，刷新重开读回成功。

- [最终确认截图](../../../output/word1106-e2e/issue3-agent3/18-reviewer-public-v3-confirmation.png)
- [保存重开规则截图](../../../output/word1106-e2e/issue3-agent3/19-reviewer-saved-public-rules.png)
- [Reviewer 上传与规则 trace](../../../output/word1106-e2e/issue3-agent3/issue3-reviewer-public-upload.zip)
- [矩阵限制及任务权限 trace](../../../output/word1106-e2e/issue3-agent3/issue3-admin-matrix-permissions.zip)
- [真实 UI trace 中的公共 V3 保存事实](../../../output/word1106-e2e/issue3-agent3/20-ui-network-matrix-facts.json)

SIGNOFF 非部门主体禁选、“其他”禁止公共矩阵创建、类型新增与矩阵修改审计均已真实页面验收。审计保留操作者、时间和具体修改内容；修复前非法 USER 会签的历史记录与失败证据单独标明，不作为当前有效规则。

仅声明正常上传草稿、公共配置复用及提交前准备通过；不声明审批提交、审批完成、正式发布或生效已通过。Reviewer 无类别维护菜单，未继续扩大权限；管理员的维护与审计结果不等同于 Reviewer 维护权限验收。

最终服务端审查另补齐“其他”类型不得选择 PUBLIC_MATRIX 的反向约束：统一类别新增/修改入口拒绝该组合；类型重命名入口拒绝将绑定 PUBLIC 类别的类型改名为“其他”。均复用准确业务错误，不自动改变模式或历史矩阵。新增类型拒绝后的 H2 事务无孤儿、既有 CUSTOM 不被转换、直接类别新增被拒绝，以及重命名拒绝和两条正常维护路径，共新增 6 例回归。

集中验证：ConfigurationServiceTest 12/12、ConfigurationTransactionTest 10/10、TaxonomyAdminServiceImplTest 19/19，完整 Maven reactor 编译及测试均 BUILD SUCCESS。日志分别为 `C:\Users\D01020\AppData\Local\Temp\word1106-review\agent3-other-source-maven.log` 和 `agent3-other-rename-maven.log`。此处是服务端回归结果，前述 E2E 的实际运行版本仍按记录保留，不虚称在后续构建前验收过新产物。
