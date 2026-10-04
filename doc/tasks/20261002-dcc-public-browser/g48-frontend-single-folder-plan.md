# LD03/G48 正常新上传只选择项目文件夹

状态：FRONTEND_IMPLEMENTED_AWAITING_ROOT_INTEGRATION。仅主仓int_qms。原只读准备已被Root本轮LD03实现授权推进；有限FE实现/TDD在g48-frontend-single-folder-bdd和fingerprints记录。下面保留原实施清单，后端mapping/schema和Root实际类型/构建/页面验证仍单列，本文不代表完整链已运行。

目标前端操作：正常受控文件NEW上传只选择当前项目逻辑文件夹；类别/正式类型用于审批和服务端映射，上传人不再选择额外NAS提交目录。后端通过已审设计的准确项目folder＋类别映射解析/创建真实存储叶子，位置与File同事务。**不把folderId、配置根ID或0塞进directoryId；前端NEW不携directoryId。**

## 精确前端改动点

| 当前源码位置/方法 | LD03最小处理 |
|---|---|
| upload/index.vue `el-form-item label="提交目录" prop="directoryId"` | 对正常NEW完全移除该字段/叶子路径cascader；外部评审按现有独立合同保留。不是仅CSS隐藏，正常NEW不计算/发送值 |
| `formRules.directoryId`、`submitForm`中的 uploadDirectoryTree存在/必须选directoryId两组拒绝 | 仅正常NEW不再要求；外部评审/明确旧入口保留原正式校验，不能全局删除directoryId规则 |
| `syncAutoCategoryFromSelectedFileTypeTaxonomy`末尾`loadUploadDirectoryTree` | NEW类型映射成功不读NAS目录树；保正式唯一activecategory、部门/批准与readiness。`handleCategoryChange`加载只用于外部评审 |
| `categoryPreflightMessage`当前boundCategory.directoryId检查 | NEW不借客户端category投影目录字段判定映射可用；其正式目录映射在服务端提交事务验证，实际缺配置错误准确显示。类型/category歧义守卫保留 |
| `resetUploadDirectoryContext`及`uploadDirectoryTree`/pathMap/selectedUploadDirectoryPath/directoryCascaderProps | 若继续外部评审使用则明确scope保留；不因NEW项目/type切换加载或推导存储目录，不残留旧外部值到NEW payload |
| `controlledBrowserPermissionScopeText` | NEW显示项目/所选逻辑folder/分类以及“存储位置由系统按正式配置确定”，不要写“已落位/已配置ACL”。外部评审可继续显示实际NAS路径 |
| `uploadPreflightChecks`的controlled-browser-directory及view-permission-range对hasDirectoryLanding依赖 | NEW以真实selectedProjectFolder归属/启用/加载状态作为用户前置，说明系统提交时确定实际位置；不显示第二套目录必填错误。正文VIEW权限仍正式服务端校验，不把folder选择当授内容权限 |
| `freezeUploadApplicationDraft`、`captureUploadSubmissionContext`、确认summary | 保精确project/folder、正式type、productprovenance/source、attrs/源票据/部门等；确认展示选定项目folder，NEW不把未解析storage路径当已确定事实 |
| `buildSubmitPayload`当前无条件directoryId:draft.directoryId | 正常NEW条件分支**省略**directoryId（undefined不会序列化也可，建议明确不构造）；外部评审保真实目录字段，不能传null/root/folderID作为代理 |

正常NEW判定必须使用实际processType受控＋changeType NEW，不能仅“不是external”就扩到已存在版本/升版。公开版本小稿、检入/换版/返工位置信息依赖源File的正式已保存位置，由后端inherit合同保留，前端不触发新folder映射去重定位旧版本。历史未登记位置依旧准确未记录，不读当前映射补齐。

## 最小API协调

1. 沿用正常上传POST `/dcc/controlled-files/working`或`/submit`（当前submitter正式路径）及SOURCE preview，不新增任意mapping写API。NEW请求仍传projectCodeId、projectFolderId、categoryId、typeId、登记原因和真实文件票据；预览票据本来不含directoryId，正常NEW预览不需要构造storage事实。
2. 现FE `ControlledFileSubmitReqVO.directoryId:number` 改为可选Long/string字段或按processType/action明确union，**后端先确认NEW没有客户端directoryId时允许serverresolve**。`UploadFormDraft`仍可含外部字段；submitter正常NEW不给，而external按真实值检查/保留。旧response中的File.directoryId、历史位置、浏览目录模型不改语义。
3. 后端NEW返回的真实File/位置ID仍由正式读投影供后续详情/浏览。前端成功后自然进入项目folder行，不能通过把folderId转directoryId选择存储列表。后端映射/目录错误走现有准确错误反馈，不能在界面default成功或自动挑另一根。
4. 可选的存储预检只读API不是本步必需；若后端没有独立正式readiness，不造“系统目录已就绪”假检查。实际首次映射创建发生于提交事务，用户确认前只知道逻辑folder和类型。

保LD01正式产品3source/provenance、LD02实际启用type/唯一category及完整实际filename、原SOURCEactor/category/session/purpose/bytes与全球名称claim、版本号/保留期、真实审批签名/培训/attrs和二次确认。根目录唯一/实际叶子/映射锁序/删除保护由后端正式设计负责；FE不以隐藏字段绕过它。

## 实施时最少BDD与验证

- Given正常NEW、合法项目folder/type/product/route且没有客户端directoryId，When实际SFC选择原件并确认提交，Then公开payload没有该字段、没有uploadDirectoryTree请求；真实projectFolderId保持精确字符串，正式服务器返回真实File/storage位置。
- Given同页面切external→NEW或切项目/type，When准备送审，Then旧外部NAS值不进入NEW payload；原source票据/变更上下文守卫不失效，逻辑folder仍必填。
- Given外部评审入口，When选择类别/目录并提交，Then原正式NAS目录加载/必选/提交字段保持，不从NEW逻辑folder推导。
- Given已保存小版本或升版/检入候选，When后续办理，Then页面只用该源File的实际位置事实；不调用NEW创建映射、改变历史目录或伪造缺失位置。
- Given服务端根配置不唯一/映射冲突/存储叶子失效或事务晚失败，When提交返回错误，Then现有页面准确显示已发生阶段/失败，不补directoryId、换根、自动重提或显示受控成功。

FE有效RED已证明旧NEW必须多选NAS目录/发送directoryId，GREEN执行实际SFC和submitter/wrapper payload及scope渲染；外部/旧入口有限受影响回归。冻结源码最终9文件62PASS，3生产文件lint退出0；原始日志与精确指纹见同目录G48交付。Root统一types/build、后端schema/映射迁移授权和真实UI；本Agent未运行页面，不扩无关负向平台。
