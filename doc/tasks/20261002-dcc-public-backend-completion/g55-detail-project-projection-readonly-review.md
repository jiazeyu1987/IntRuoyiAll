# G55 详情当前项目显示的正式投影差异

状态：prepared_readonly_for_Root_scope。2026-10-05主线实际已受控ACTIVE并下发。Root提供r4/066.json页面事实：“当前DCC项目”显示G53文控验证产品 / G53PRODUCT，而所属实际项目为G53文控主流程项目 / G53-P-20261005。本owner只读当前FE/BE，未改生产或测试、执行Maven/DB/API/UI/Git/runtime；FE detail/index.vue由另owner独占。

## 精确来源

- FE `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue:3875–3876` 当前变量实际名 `currentDccProjectCodeText`，调用 `formatDetailPath([fileDetail.productName,fileDetail.productCode])`。这明确把文件冻结的产品字段当成当前项目显示；不是产品目录数据错或已控版本需要迁移。
- 正式 `DccControlledFileRespVO:21` 已有projectName，但没有projectCode字段；dccProjectCodeId为准确Long字符串。FileDO自身没有projectName/projectCode业务列。
- 授权详情 `DccControlledFileQueryServiceImpl.getControlledFile:527–535` 先canAccessDetail，再走 `toRespVO:2850`。该投影复制productSource/productCode/productName及dccProjectCodeId（:2874–2879），**没有读取正式DccProjectCodeDO并填projectName**；源码setProjectName只有selector/list部分，不证明详情已有值。
- FE正式 `api/dcc/controlledFile/workflow.ts.ControlledFileVO:677` 目前也没有projectName/projectCode。该文件另一响应接口中同名字段不属于详情VO，不能因搜索命中就当实际可读。
- 正式项目事实已存在 `dcc_project_code / DccProjectCodeDO.projectName/projectCode`，服务已有projectCodeMapper。`getRelationPermissions:1032` 已按file.dccProjectCodeId查询项目并核tenant/ID，证明稳定身份路径可复用；不用catalog/product名称猜映射。

## 推荐的最小正式修复边界

1. BE仅在正式授权详情投影中，按该File已保存dccProjectCodeId读取准确项目，核project.id==file.dccProjectCodeId且tenant==file/currenttenant、未删除。不要求当前ENABLE作为历史显示前置（历史版本所属项目后来停用仍应按实际记录可读）；不增加编辑权限、不回填File/Master。
2. RespVO新增String projectCode，现projectName由该正式项目填充；当前未绑定项目时保明确缺失，不使用productCode/projectCodeRecognitionText/requester或catalog fallback。已绑定而正式项目缺失/foreigntenant/异常不返回产品冒成功，应明确不可读/数据不一致；Root确定既有响应错误合同后有限实施。
3. 复用当前权限门禁之后读取，不能在name-only响应补完整项目事实或按查询参数任意projectId读取。最小先 `includeRouteSnapshots=true` 的已授权getControlledFile详情，或独立helper仅被该分支调用；不无因扩大所有browser/list投影，避免列表N+1及原权限语义变化。
4. FE owner仅补 `ControlledFileVO.projectName/projectCode` 声明并让当前项目显示用这两个正式字段，项目跳转仍保原准确dccProjectCodeId。产品名称/编码显示区继续保其产品字段；不为了满足显示增加client读取目录猜项目或第二身份输入。
5. 无schema/种子/历史DML/签名改写，只Query详情响应+VO两生产位置及FE既有DTO/computed有限源接线；实际实施由Root明确归属，当前此报告不是已修复交付。

## 最少定向BDD与测试边界

- Given同租户准确file.projectId指真实项目且文件productName/productCode刻意与项目完全不同，When经过实际授权getControlledFile读取，Then响应准确projectName/projectCode而产品字段不变；旧源码projectCode缺失/projectName为空形成有效RED。
- Given绑定project丢失/foreigntenant，When详情投影，Then明确错误、不取product fallback、不改原记录。未绑定历史则准确缺失，name-only用户仍不能借该修复获取详情。
- Given真实项目响应与产品值不同，When当前detail computed/template渲染，Then“当前DCC项目”使用项目字段和原ID导航，产品区不改；缺字段保持明确未记录而非产品值。

优先现 `DccControlledFileQueryServiceTest` 授权详情fixture及其既有name-only拒绝相邻case；避免复原整仓平台或运行无关类别/签名全量。FE有实际computed/SFC子树测试可形成有限RED→GREEN。Root必要组合types/package后再自然刷新真实当前File页面，业务动作仍原UI完成，后端只读核所属项目即可。本owner不跑任何测试或页面。

## 本次读取rawSHA

| 文件 | SHA-256 |
| --- | --- |
| detail/index.vue | 04c5f1584c110672f81262239d8d91f9ffd894665882a728c4231eb91424b367 |
| workflow.ts | 6e3079a4df6f335b90ce12de0ea0aa2b101151e96b5bf4dfaa946f033f88a8da |
| DccControlledFileQueryServiceImpl.java | 02352d43752293817155ccf4bb2cc3467777f0601d825131b3aa8adf7fe5011d |
| DccControlledFileRespVO.java | 018c563bd555b506746db6f2eafc1fff59f306941f746a5da6d555b84fb27d0e |

这些是方案读取快照，不冻结FE owner后续变化；无本owner运行验证声明。
