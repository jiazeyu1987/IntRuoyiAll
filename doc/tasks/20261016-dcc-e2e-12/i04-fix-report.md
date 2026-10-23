# E2E-I04 最小修复交接

结果：FIXED_VERIFIED。首次打开目录弹框的异步挂载竞态已修复；生产改动仅 directories/index.vue，另新增两个专属回归。未提交、未改数据库/运行配置、未执行全量构建或服务操作。

## 行为变化

原实现设置 directoryFormMounted 后等待动态 import 和一次 nextTick，模块就绪时组件实例未必已挂载，首次点击可能抛“文档目录表单组件加载失败”。

现在保存待打开的类型/行，在 DirectoryForm 的真实 vue:mounted 回调中执行。已挂载的后续打开直接执行，消费后清空待办请求。保留 defineAsyncComponent 和 v-if 按需加载，不增加延时、轮询、fallback或额外产品控件。组件真实缺少 open 时仍明确报错。

生产位置：IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue:179、:254、:412。最小 diff 共一个生产文件。

## BDD / RED / GREEN

- Given：全新 context、真实登录、目录表单尚未挂载；When：首次点击一次“新建目录”；Then：表单出现、编码/名称可填，无该挂载异常。
- Given：表单填入本例前缀但未保存；When：取消后重开；Then：字段清空，打开/取消不产生目录写请求。
- 真实历史 RED：category-preparation.json及其trace记录一次点击后明确pageerror“文档目录表单组件加载失败”、弹框0、目录创建POST0。不是凭连接超时认定产品失败。
- 当轮新真实回归初跑也得到该页面异常；环境恢复后的未修改快速加载基线偶发PASS。artifacts/i04-red/result.json目前保存此快速基线PASS，不把它冒充RED。竞态不能以一次快速通过关闭。
- 稳定组件 RED：node IntRuoyiFronted/tests/e2e/dcc-directory-first-open-lifecycle.spec.cjs，修改前退出1，错误为“文档目录表单组件加载失败”。测试抽取实际页面handler和表单模板节点，使用Vue真实renderer控制独立import完成顺序；它是组件生命周期回归，不模拟业务后端或宣称E2E通过。
- 组件 GREEN：同一命令修改后退出0；覆盖首次加载只打开一次、随后子目录新建的parentId以及编辑对象ID不变。
- 真实 GREEN：node IntRuoyiFronted/tests/e2e/dcc-directory-first-open-real.e2e.cjs green，退出0。全新context首次点击1次；表单加载结束、按钮可用、字段可填写；取消重开字段为空；pageErrors=[]、directoryWrites=[]、traceError=[]、cleanupError=[]。
- GREEN文件：artifacts/i04-green/result.json、first-open.png、reopen.png、trace.zip。截图已复核，填写/取消均为真实页面，未点击保存。

## 回归与限制

- dcc-directory-lazy-loading-static.spec.js：PASS。
- dcc-redbox-first-open-performance-static.spec.js：PASS。
- dcc-directory-expand-actions-toolbar-static.spec.js：修改前后均失败于固定LF字符串锚点；当前文件使用CRLF，规范化LF后该锚点存在。记录为既有静态合同问题，不改无关测试或生产排版迎合它。
- Root确认恢复后的环境：8061/PID36084，48061/PID35528，health HTTP200/UP。此前连接失败不作为本次产品RED。
- 真实GREEN页面仍可见“系统异常”toast，已原样记录visibleNotifications；本窄范围没有定位其来源，也不宣称schema/其它业务全部恢复。Root负责schema；本次证明的是首次挂载与取消重开，不包含目录保存和E2E-12全部会签。
- 凭据仅从运行时环境读取，登录前不开始trace；trace关闭网络/DOM快照。i04-privacy.json对两个I04解压trace扫描未发现授权登录口令原值，网络记录0字节。

## 交接状态

I04实现与定向验证已具备review条件；禁止提交授权继续生效，不标记整个E2E-12 completed。原fixture资产保留，未新增目录或产生业务清理任务。完整E2E-12还需后续fixture/schema准备及会签验收，原五项结果不升级为PASS。
