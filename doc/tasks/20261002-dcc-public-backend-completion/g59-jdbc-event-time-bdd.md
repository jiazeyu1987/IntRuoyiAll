# G59 JDBC事件时间类型精确修复

状态：ready_for_closeout。Root真实MySQL预览在21:51:13被DCC_REPAIR_LIFECYCLE_EVENT_REQUIRED拒，原事件完整身份/保存日期匹配。现sameTime仅接受Timestamp，MySQL ConnectorJ map可返回LocalDateTime；授权仅RepairService/既有专属测试、soleMaven。旧所有源seal保历史，Root运行包健康但未POSTrepair，无真实写入。

- Given正式事件map具有精确Master/version/key/type/file/tenant/BPM及原时间，JDBC返回LocalDateTime，When实际exactEvent判断，Then与相同Timestamp都精确成功；旧仅Timestamp有效RED。
- Given时间纳秒不同、身份错误或第三种字符串/Date类型，Then明确false/原repair拒，不能字符串解析、截断微秒、时区猜测或GET修历史。
- Final必要范围：专属原repair6 + 新真实JDBC map类型行为1，共7；不重跑143无关核心、无SQL/API/浏览器/服务/Git。


## 结果

实际保存H2 event Map的Timestamp原true，同值LocalDateTime旧false，有效RED1failure0error，原XML保留；仅sameTime两个正式typed分支exactequals，无任何parse/truncate/时区/历史变化。GREEN现reactor专属repair7/1全0fail/error/skip CLI0 2026-10-05 22:00:56，原7XMLbyte归档。新增类型case同时验证纳秒差1、字符串和错误Masterfalse，原6完整权限/签名/事件/回滚/重放保持。其余12production/4tests及原corecompiled逐SHA0漂移；旧r2seal不改，r3只supersede两个资产与编译repair类。Maven/source/target已冻结释放Root，Root真实MySQL运行与页面重验，不据H2成功冒实际修复完成。
