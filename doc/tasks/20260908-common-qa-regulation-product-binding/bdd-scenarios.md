# BDD 场景

BDD: 通用规程版本维护 -> Given QA 管理员创建通用包装规程 When 保存版本内容 Then 版本只包含该版本的工序、项目、标准、工具和抽样方案。

BDD: 已发布版本不可变 -> Given 版本 A 已发布 When 用户修改 A 的检验项目 Then 系统拒绝修改并要求复制新版本。

BDD: 多产品复用版本 -> Given 版本 A 已发布 When 产品 1 和产品 2 分别绑定 A Then 两条产品绑定成功且 A 只有一份内容。

BDD: 单产品唯一有效绑定 -> Given 产品 6 已启用绑定 B When 用户再次新增启用绑定 C Then 服务返回冲突且不产生第二条启用关系。

BDD: 绑定版本候选约束 -> Given 系统存在草稿 A、已发布 B、退休 C When 用户选择产品绑定版本 Then 只有 B 可选。

BDD: 绑定版本变更 -> Given 产品 6 当前启用 B When 用户确认变更为 C Then B 失效、C 启用，且任一写入失败时两者保持原状态。

BDD: 跨租户绑定拒绝 -> Given 当前用户属于租户 T1 When 请求 T2 的产品或规程版本 Then 后端拒绝并不写入关系。

BDD: 退休联动与幂等 -> Given 版本 A 被启用绑定 When 退休 A 或重复提交同一幂等键 Then 绑定停用和版本退休原子完成，重复请求不重复写入。

BDD: 服务端派生字段 -> Given 客户端提交错误 scope/status/DCC When 创建绑定 Then 后端固定并校验正式值，拒绝篡改。

BDD: A 多来源版本 -> Given QA 管理员上传初包装和大中包装两份 Word 来源 When 发布 A 通用包装版本 Then A 版本冻结两份来源文件和完整项目，产品只需要绑定一次 A。

BDD: B 单来源版本 -> Given QA 管理员上传美联初包装 Word 来源 When 发布 B 通用包装版本 Then B 版本只包含该来源文件的项目，不继承 A 的大中包装项目。

BDD: 来源文件解析失败 -> Given 多文件导入中一份 Word 缺少必填表头 When 保存草稿 Then 后端拒绝整个导入并不保留半成品版本。

BDD: 条件标准显式适用性 -> Given A-01 来源中存在“百瑞吉产品要求” When QA 管理员发布 A Then 系统要求选择适用产品 ID 或删除该条件标准，不允许自动套用到全部产品。

BDD: B 版本适用范围 -> Given B-01 来源包含“美联”线索且 B 设置为 PRODUCT_SET When 非适用产品绑定 B Then 后端拒绝绑定且不按名称自动匹配。

BDD: 来源逐项追溯 -> Given A 版本由两份 Word 来源组成 When 查看发布版本证据 Then 每个项目都有来源文件、包装阶段、Word 表号、行号和原始摘录。

BDD: Warning 分级门禁 -> Given 来源 Word 存在 schema warning When warning 不影响检验规则表解析 Then 允许发布并记录 warning；当 warning 或解析结果影响必填内容时发布失败。
