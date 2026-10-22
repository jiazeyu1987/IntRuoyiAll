# G55 授权详情正式项目事实投影

状态：ready_for_closeout_for_Root_review。Root限定BE owner只Query详情/RespVO/必要test；FE另owner。未绑定历史projectId=null不新增拒绝，项目字段null，不catalog/product fallback。已绑定则实际项目ID/当前tenant一致，缺失/foreign明确PROJECT_CODE_NOT_EXISTS；不要求ENABLE，不改产品/列表/name-only/默认权限或历史数据库。源/测试冻结，Maven已释放Root。

- Given文件保存产品事实与正式项目名称/编码不同，When已授权getControlledFile详情，Then项目显示字段只取actualfile.projectId→同tenantproject，产品值不变。先旧实际响应projectName为空有效RED。
- GivenprojectId=null历史，When详情读取，Then原详情可读、projectName/code=null、不借产品；Given已绑定但project缺失/foreign，Then明确数据错误。
- Given用户只有name-only范围，When请求详情，Then现拒绝保持，新增项目投影不可绕访问守卫。列表/native selector合同不改。

本树独占Maven定向旧Queryclass相关回归，使用existingfixture正式service真实方法；Mockito投影不是实际DB/UI证明。Rootpackage之后自然页面读取最终验收。生产schema/源码事实Owner此批只2文件。

## 实际定向记录

旧source真实授权getControlledFile响应projectName=null而有准确正式项目：effectiveRED1fail/0error，原XML g55-project-projection-effective-red.xml。新projectCode及includeRouteSnapshots详情helper实现后，test注解导入/DO继承tenant的builder编译问题单列日志保留，不计业务RED；生产未降guard。

最后2026-10-05 12:33:20 currentreactor限定8method groups共13执行、0fail/error/skip、exit0。新6项正式项目/停用可读、null未绑定、绑定missing/wrongtenant/wrongid/deleted；原7项name-only拒绝/版本历史/检出actor/作废历史/独立日期投影回归。不声明整个Query大class全跑。finalXML byte封 g55-project-projection-final-junit.xml，2production/1test及receipt rawhash由 g55-project-projection-fingerprints.json保存。

无actualDB/UI/runtime/package/Git；RootFE/types/build/同源package与真实展示验收单列。原productsFrozen事实与所有历史行/签名未改，列表不新增project查询或权限放宽。
