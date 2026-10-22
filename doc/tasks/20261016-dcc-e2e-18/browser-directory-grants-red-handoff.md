# 浏览目录授权请求内复用 行为RED交接

## 唯一写入范围

Root于2026-10-18分配E2E-18只编写最小行为回归。生产源码未修改，未运行Maven，未修改服务/SQL/全局超时或缓存。本例仍需完成原升版业务，当前不能标记整体PASS。

## 测试

- 文件：IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java
- 方法：getControlledFileBrowserPage_readsDirectoryGrantsOncePerRequestAndRechecksRevocation
- 复用同类setUpDirectoryAccessDefaults、stubReadMaster及既有Mockito/租户/项目fixture。
- 真实调用getControlledFileBrowserPage；4个不同Master候选，2个目录20由QUERY放行、1个目录21仅PREVIEW放行、目录22不可见。断言总数3及精确ID9900/9901/9902，两种集合各读取一次。
- 正文详情授权作为既有独立port明确拒绝，链历史为空；名称投影仍无预览/下载能力。这使计数只覆盖真实browser候选过滤，不改变或绑定其他详情/二进制守卫的调用次数。
- 对同一service再次独立调用前撤销QUERY与PREVIEW集合；断言总数0、空列表及两种集合累计各读取两次，禁止共享字段/跨请求缓存绕过撤权。

## 验证边界

- git diff --check针对该测试文件：PASS（仅LF将转CRLF提示）。
- RED未运行，待Root执行指定方法；当前生产预计因QUERY逐候选读取4次而在times(1)失败，不能把预计当实测。
- Root可在IntRuoyiBackend目录运行：mvn -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest#getControlledFileBrowserPage_readsDirectoryGrantsOncePerRequestAndRechecksRevocation" test。
- GREEN生产边界按Root：仅browser候选过滤请求内按accessType懒加载EnumMap和私有6参重载；旧5参canAccessQuery、旧3参hasAuthorizedDirectoryAccess及正文守卫保持无缓存行为。本例没有写这些生产方法。

## 原业务精确断点

- 最新运行SHA：bf2fd4962d74a923cca3bbf869d87b5407031335f0c410c060c554975c07169f。
- 项目DCC241016C18P1复用；文件2054545668044084232，编号DCC241016C18PARTIAL，流程0a3b05d4-cace-11f1-a035-b082e25ec548，原申请人DCC241016C18U（910503）。
- A/1已由真实页面完成S主管、L指派、C会签、A批准、D文控、U培训上传；只读精确详情回查显示“受控（已生效）/当前受控版本/ACTIVE/A/1”，培训证据为DCC241016C18-PARTIAL-OLD-TRAINING.pdf，自动下发已完成。
- 培训绑定点击时间从Playwright时钟还原为2026-10-18 16:51:44.327495 +08:00；自然POST /admin-api/dcc/controlled-files/2054545668044084232/training-record。ticket已返回且后续UI确认绑定，不记录ticket值；初始30秒超时不是未保存，禁止重传/重绑。
- UI证据：run-2026-10-18T07-43-21-112Z/PARTIAL-exact-detail-loaded-after-training-timeout.png及U-training-binding-checkpoint.zip；新旧runtime证据保留。RootDB只作恢复佐证，UI结论独立来自上述回查。
- 尚未检出/检入或创建正式升版，无新小版本、无B/1。后续先本人新context登录只读回查，再从正式检出入口续跑。已知21自定义矩阵基线丢失由Root处理，本例不重复修复生产文件。
