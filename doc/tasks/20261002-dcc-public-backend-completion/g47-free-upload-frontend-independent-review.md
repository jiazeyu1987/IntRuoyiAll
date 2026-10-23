# G47 / LD02 前端自由类型上传独立复核

结论：在本轮限定的源码接线范围内，未发现新增的主流程 P1/P2 阻断。正常上传不再依赖项目模板阶段或预设文件名单；真实文件名、票据和准确 Long 身份没有被该修改替换。第二个提交目录仍存在，是已明确留给 LD03 的独立差异，不应据此把 LD02 报为未修。

本复核只读 `int_qms` 当前源码、交付清单和既有日志，只新建本文件；没有编辑生产/测试、执行测试或构建、访问 DB/API/浏览器、启停服务或执行 Git 写操作。没有用离线测试替身声称真实上传或 E2E 已完成。

## 冻结来源与日志核验

来源：`doc/tasks/20261002-dcc-public-browser/g47-free-file-upload-fingerprints.json`，原始 SHA-256 `5885677ce12e67e58c987f11f2daba838cd608e1daad3f86acdd1b67ba63c1e0`。

已重新计算清单八项资产、未变的 LD01 产品投影 helper 以及七项原日志的 bytes/SHA-256；全部匹配。最终日志 `g47-free-file-upload-final.log` 的原始 SHA-256 为 `4521fcf66655fe15d7305468c0db580b86ea88d1fd22ea514ee10e9d7198e67e`，实际内容记录 tests/pass 55、fail/cancelled/skipped/todo 0。该数字包含原 LD01/上传回归，不重复累计为新的 55 个业务场景。原 RED 日志保留四项失败；初始 GREEN 夹具问题在原交付记录中已与有效 RED 区分。

| 本批正式源码 | 原始 SHA-256 |
| --- | --- |
| upload/index.vue | 61d1890af9f1433c6557f6bc239589a63b7ae5e66c10f3b5dd89e048f5fea15c |
| upload/file-type-options.ts | ca3f915d98aa7838d7d125ecc85992a4aa94cdbc7546ba66ae3cff61b2ca7122 |
| upload/submitter.ts | 9ac17c2bb5170b86f05b264a2d4b951bec313614971ca7f27ef3cdaa5713575f |
| api/dcc/controlledFile/workflow.ts | 8bc74794b165af100685a226bc8525afe80e73d9396edf3e548c220ff6f13eaf |
| api/dcc/controlledFile/fileTypeTaxonomies.ts | 9304ce47281aff0ad4727d63bdc5ebba7de151262c84bb322c2f66e9571350e5 |

## 主线接线核对

以下 FE 路径相对 `IntRuoyiFronted/src/`，行号对应上述冻结源，不是实际 DOM 验收。

1. **类型不依赖模板**：`views/dcc/controlled-file/upload/index.vue:92–98` 只有一个实际类型选择；候选计算 `:900–901` 来自正式类型树的启用叶子完整路径。`loadUploadFileTypes:1340` 独立调用既有 `/dcc/file-type-taxonomies/upload-options`，非法/重复/失效/缺父/循环路径在 helper 中拒绝。`loadProjectFileTemplate:1355` 只保存可选有效建议；空模板不触发核心错误，读取失败只显示辅助警告，不改变正式类型树。没有第一条模板或第一条类别 fallback。
2. **准确分类与矩阵来源**：`syncAutoCategoryFromSelectedFileTypeTaxonomy:1182` 使用用户准确选中的类型调用正式 active-category API；回执序号和当前类型均一致才应用。只接受当前候选集合中唯一且与服务端返回 ID 相同的类别；缺失/歧义明确置空并展示错误。后续矩阵仍按该真实 category 加载。BE G47 已另行闭合正式唯一类别守卫，前端匹配不是唯一授权依据。
3. **真实文件名与正文票据**：正常分支文件名输入 `:214–216` 为 readonly；`handleFileChange:2001` 在 SOURCE 上传前用 `file.raw.name` 建立本次名称上下文，发送的是实际 File 而不是生成对象。`:2050` 对 returned preview.fileName 与 raw.name 做精确比较，错误或迟到回执清理实际返回票据，不接纳为当前预览。`buildUploadPreviewContext:1269` 保留 project/type/category/filename/session，`api/.../workflow.ts:1745` 原 multipart、显式租户及请求 ID 校验保持。`submitter.ts.buildSubmitPayload:417` 的 sourceFileName 来源是成功 preview.fileName，source/original ticket 来源同一个实际 preview，显示名不替代 source claim。
4. **Long 精度**：`file-type-options.ts.uploadFileTypeIdentity` 对 number 使用 safeInteger，字符串按正整数原样保存并校验 signed Long 上限，路径和选择值使用字符串；`submitter.ts` 不把 taxonomy 字符串 Number 化。公共 `workflow.ts.assertControlledFileRequestIdentity:1504` 在送审前校验项目、文件夹、类型、关联与部门身份；完整 `9007199254740993` 和 Long 最大值保持准确，unsafe number/溢出/前导零明确拒绝。本批不是全仓所有旧 category/directory DTO 的精度审计。
5. **取消与上下文失效**：`handleProjectCodeChange:1471` 在真实属性选择被取消、关闭、异常或旧异步返回时恢复/保留准确原项目并提前 return，不进入后面的文件夹/关联/type/preview reset。`handleFileTypeTaxonomyChange:1601` 先真实清理已知票据；清理失败恢复 accepted type 并 return，未重置原 preview/name/category。清理成功才使旧 SOURCE 请求序号、预览、附件和类别失效，并重取实际类型类别。确认弹窗 cancel/close 不提交或导航；已有 55 项日志包含原申请页的取消和迟到项目选择行为回归。

## 范围与剩余实际验证

没有提出新增源修复或扩展审计门禁；无需为本次只读复核重跑这些测试。清单保持未变的 LD01 产品 identity helper，仍使用服务端正式产品来源投影，不恢复 projectCode 产品 fallback，也不允许客户端注入 MDM master 身份。

该批日志是实际离线 SFC handler/validator、Vue render 和 wrapper 合同测试记录，运行时 transport/Element 宿主由夹具提供；它不证明真实文件二进制存储、真实页面中 Element 事件次序、实际租户类型目录、审批签名或送审已成功。Root 正在统一类型检查/构建，随后在所需 schema 已升级的任务运行环境走真实页面核验。这是证据边界，不另报为代码未实现。

LD03 单项目文件夹正式目录映射建议另见 `g48-single-project-folder-plan.md`。其实现前不要用隐藏目录控件加自动第一叶子、逻辑 folderId 冒充 directoryId 或旧票据换名绕过本批保留边界。
