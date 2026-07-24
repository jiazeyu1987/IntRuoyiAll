# Execution Log: 20260523-showroom-version-center-design-docs

BDD: 版本中心应让用户在一个独立工作台内查看公司或产品的已发布历史版本 -> Given 公司或产品存在多次已发布 revision 与对应已发布媒体 / When 用户进入版本中心 / Then 页面必须一次性展示历史列表、当前版本、历史预览、当前对比与发布入口，而不是要求用户在详情页和列表页之间反复切换。

BDD: 历史版本应能完整回看文字、图片与语音 -> Given 某个已发布 revision 已绑定完整 bundle / When 用户查看该历史版本 / Then 系统必须返回该 revision 对应的字段文本、图片引用、中英文讲解稿与音频引用，而不是只返回当前 live 快照。

BDD: 历史版本重新发布应复制成新版本并立即触发前台 release 切换 -> Given 用户在版本中心选择一个完整历史 bundle / When 用户执行“发布为当前线上版本” / Then 系统必须新建更高 revision、新建对应媒体版本、生成新 showroom release 并切换 current pointer，不得直接覆盖旧 revision。

BDD: 缺少 bundle 或媒体不完整时必须显式阻断版本中心 -> Given 某个已发布 revision 没有完成 bundle 回填，或缺少语音/图片 / When 用户请求历史列表、查看详情或执行重发 / Then 系统必须返回明确 blocker 错误与缺失 revision 信息，不得静默跳过该版本或用当前 live 数据兜底。

INFO: 用户本轮要求“先根据已确认方案设计文档”，因此本任务只输出设计文档，不进入生产代码实现。
INFO: 已核对现有前端入口、后端 release 主链路、历史 revision / narration / preview asset 存储形态，并将与成熟版本中心冲突的现状差异写入设计文档。

