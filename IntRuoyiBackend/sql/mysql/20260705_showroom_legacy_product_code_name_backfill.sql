-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260705_showroom_legacy_product_code_mapping; type=data; riskLevel=medium
-- 20260705 showroom legacy product code name backfill
-- Rule: write product_* only when product and current INT product names are exactly matched; duplicate same-name groups require equal cardinality and are paired by numeric code order.
-- Generated from: D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-cardinality-mapping.csv
START TRANSACTION;
UPDATE showroom_product SET legacy_product_code = NULL WHERE legacy_product_code LIKE 'product\_%';
UPDATE showroom_product SET legacy_product_code = 'product_003' WHERE tenant_id = 1 AND product_code = 'INT-3' AND deleted = 0;
-- verified_name_cn: Y型连接器
UPDATE showroom_product SET legacy_product_code = 'product_004' WHERE tenant_id = 1 AND product_code = 'INT-4' AND deleted = 0;
-- verified_name_cn: 按压式Y型连接器
UPDATE showroom_product SET legacy_product_code = 'product_005' WHERE tenant_id = 1 AND product_code = 'INT-5' AND deleted = 0;
-- verified_name_cn: Y型连接阀套件
UPDATE showroom_product SET legacy_product_code = 'product_006' WHERE tenant_id = 1 AND product_code = 'INT-6' AND deleted = 0;
-- verified_name_cn: 穿刺针
UPDATE showroom_product SET legacy_product_code = 'product_007' WHERE tenant_id = 1 AND product_code = 'INT-7' AND deleted = 0;
-- verified_name_cn: 一次性使用血管鞘
UPDATE showroom_product SET legacy_product_code = 'product_008' WHERE tenant_id = 1 AND product_code = 'INT-8' AND deleted = 0;
-- verified_name_cn: 一次性使用导管鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_009' WHERE tenant_id = 1 AND product_code = 'INT-9' AND deleted = 0;
-- verified_name_cn: 一次性使用亲水涂层导管鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_010' WHERE tenant_id = 1 AND product_code = 'INT-10' AND deleted = 0;
-- verified_name_cn: 股动脉鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_011' WHERE tenant_id = 1 AND product_code = 'INT-11' AND deleted = 0;
-- verified_name_cn: 桡动脉鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_012' WHERE tenant_id = 1 AND product_code = 'INT-12' AND deleted = 0;
-- verified_name_cn: 球囊扩张压力泵
UPDATE showroom_product SET legacy_product_code = 'product_013' WHERE tenant_id = 1 AND product_code = 'INT-13' AND deleted = 0;
-- verified_name_cn: 数显球囊扩张压力泵
UPDATE showroom_product SET legacy_product_code = 'product_014' WHERE tenant_id = 1 AND product_code = 'INT-14' AND deleted = 0;
-- verified_name_cn: 按压式球囊扩张压力泵
UPDATE showroom_product SET legacy_product_code = 'product_016' WHERE tenant_id = 1 AND product_code = 'INT-16' AND deleted = 0;
-- verified_name_cn: 造影剂推入器
UPDATE showroom_product SET legacy_product_code = 'product_017' WHERE tenant_id = 1 AND product_code = 'INT-17' AND deleted = 0;
-- verified_name_cn: 造影剂推入器-C
UPDATE showroom_product SET legacy_product_code = 'product_018' WHERE tenant_id = 1 AND product_code = 'INT-18' AND deleted = 0;
-- verified_name_cn: 股动脉止血带
UPDATE showroom_product SET legacy_product_code = 'product_019' WHERE tenant_id = 1 AND product_code = 'INT-19' AND deleted = 0;
-- verified_name_cn: 股动脉止血带
UPDATE showroom_product SET legacy_product_code = 'product_020' WHERE tenant_id = 1 AND product_code = 'INT-20' AND deleted = 0;
-- verified_name_cn: 气囊式止血带II
UPDATE showroom_product SET legacy_product_code = 'product_021' WHERE tenant_id = 1 AND product_code = 'INT-21' AND deleted = 0;
-- verified_name_cn: 桡动脉止血带I
UPDATE showroom_product SET legacy_product_code = 'product_022' WHERE tenant_id = 1 AND product_code = 'INT-22' AND deleted = 0;
-- verified_name_cn: 压力传感器
UPDATE showroom_product SET legacy_product_code = 'product_023' WHERE tenant_id = 1 AND product_code = 'INT-23' AND deleted = 0;
-- verified_name_cn: 有创压力传感器
UPDATE showroom_product SET legacy_product_code = 'product_024' WHERE tenant_id = 1 AND product_code = 'INT-24' AND deleted = 0;
-- verified_name_cn: 高压延长管
UPDATE showroom_product SET legacy_product_code = 'product_025' WHERE tenant_id = 1 AND product_code = 'INT-25' AND deleted = 0;
-- verified_name_cn: 介入手术器械包
UPDATE showroom_product SET legacy_product_code = 'product_026' WHERE tenant_id = 1 AND product_code = 'INT-27' AND deleted = 0;
-- verified_name_cn: 造影导管
UPDATE showroom_product SET legacy_product_code = 'product_027' WHERE tenant_id = 1 AND product_code = 'INT-28' AND deleted = 0;
-- verified_name_cn: 亲水涂层造影导管
UPDATE showroom_product SET legacy_product_code = 'product_028' WHERE tenant_id = 1 AND product_code = 'INT-29' AND deleted = 0;
-- verified_name_cn: 亲水涂层血管造影导管
UPDATE showroom_product SET legacy_product_code = 'product_029' WHERE tenant_id = 1 AND product_code = 'INT-30' AND deleted = 0;
-- verified_name_cn: 指引导管
UPDATE showroom_product SET legacy_product_code = 'product_030' WHERE tenant_id = 1 AND product_code = 'INT-31' AND deleted = 0;
-- verified_name_cn: 导引导管
UPDATE showroom_product SET legacy_product_code = 'product_031' WHERE tenant_id = 1 AND product_code = 'INT-32' AND deleted = 0;
-- verified_name_cn: 亲水涂层导引导管
UPDATE showroom_product SET legacy_product_code = 'product_032' WHERE tenant_id = 1 AND product_code = 'INT-33' AND deleted = 0;
-- verified_name_cn: PTFE导丝
UPDATE showroom_product SET legacy_product_code = 'product_033' WHERE tenant_id = 1 AND product_code = 'INT-34' AND deleted = 0;
-- verified_name_cn: PTCA球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_034' WHERE tenant_id = 1 AND product_code = 'INT-35' AND deleted = 0;
-- verified_name_cn: 指引导丝
UPDATE showroom_product SET legacy_product_code = 'product_035' WHERE tenant_id = 1 AND product_code = 'INT-36' AND deleted = 0;
-- verified_name_cn: 多环测量灌注导管
UPDATE showroom_product SET legacy_product_code = 'product_036' WHERE tenant_id = 1 AND product_code = 'INT-37' AND deleted = 0;
-- verified_name_cn: 冠脉微导管
UPDATE showroom_product SET legacy_product_code = 'product_037' WHERE tenant_id = 1 AND product_code = 'INT-38' AND deleted = 0;
-- verified_name_cn: 超滑导丝
UPDATE showroom_product SET legacy_product_code = 'product_038' WHERE tenant_id = 1 AND product_code = 'INT-39' AND deleted = 0;
-- verified_name_cn: 依维莫司冠脉乳突球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_039' WHERE tenant_id = 1 AND product_code = 'INT-40' AND deleted = 0;
-- verified_name_cn: 指引延长导管
UPDATE showroom_product SET legacy_product_code = 'product_040' WHERE tenant_id = 1 AND product_code = 'INT-41' AND deleted = 0;
-- verified_name_cn: 棘突球囊
UPDATE showroom_product SET legacy_product_code = 'product_041' WHERE tenant_id = 1 AND product_code = 'INT-42' AND deleted = 0;
-- verified_name_cn: 左心耳封堵器系统
UPDATE showroom_product SET legacy_product_code = 'product_042' WHERE tenant_id = 1 AND product_code = 'INT-43' AND deleted = 0;
-- verified_name_cn: 一次性使用心脏脉冲电场消融导管
UPDATE showroom_product SET legacy_product_code = 'product_043' WHERE tenant_id = 1 AND product_code = 'INT-44' AND deleted = 0;
-- verified_name_cn: 可调弯导管鞘
UPDATE showroom_product SET legacy_product_code = 'product_044' WHERE tenant_id = 1 AND product_code = 'INT-45' AND deleted = 0;
-- verified_name_cn: 血管鞘
UPDATE showroom_product SET legacy_product_code = 'product_045' WHERE tenant_id = 1 AND product_code = 'INT-46' AND deleted = 0;
-- verified_name_cn: 心脏瓣膜球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_046' WHERE tenant_id = 1 AND product_code = 'INT-47' AND deleted = 0;
-- verified_name_cn: 可扩张血管鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_047' WHERE tenant_id = 1 AND product_code = 'INT-48' AND deleted = 0;
-- verified_name_cn: 心脏瓣膜支架Hanchor Valve
UPDATE showroom_product SET legacy_product_code = 'product_048' WHERE tenant_id = 1 AND product_code = 'INT-49' AND deleted = 0;
-- verified_name_cn: 新型聚合物瓣膜
UPDATE showroom_product SET legacy_product_code = 'product_050' WHERE tenant_id = 1 AND product_code = 'INT-51' AND deleted = 0;
-- verified_name_cn: 可降解镁合金支架
UPDATE showroom_product SET legacy_product_code = 'product_051' WHERE tenant_id = 1 AND product_code = 'INT-52' AND deleted = 0;
-- verified_name_cn: 自主冶炼镁合金管材
UPDATE showroom_product SET legacy_product_code = 'product_052' WHERE tenant_id = 1 AND product_code = 'INT-53' AND deleted = 0;
-- verified_name_cn: 自主冶炼镁合金管棒材
UPDATE showroom_product SET legacy_product_code = 'product_053' WHERE tenant_id = 1 AND product_code = 'INT-54' AND deleted = 0;
-- verified_name_cn: 人工血管
UPDATE showroom_product SET legacy_product_code = 'product_054' WHERE tenant_id = 1 AND product_code = 'INT-55' AND deleted = 0;
-- verified_name_cn: 胸主动脉覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_055' WHERE tenant_id = 1 AND product_code = 'INT-56' AND deleted = 0;
-- verified_name_cn: Grency髂静脉支架系统
UPDATE showroom_product SET legacy_product_code = 'product_056' WHERE tenant_id = 1 AND product_code = 'INT-57' AND deleted = 0;
-- verified_name_cn: 分体式分支型胸主动脉覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_057' WHERE tenant_id = 1 AND product_code = 'INT-58' AND deleted = 0;
-- verified_name_cn: 胸主动脉裸支架
UPDATE showroom_product SET legacy_product_code = 'product_058' WHERE tenant_id = 1 AND product_code = 'INT-59' AND deleted = 0;
-- verified_name_cn: 模块内嵌双分支覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_059' WHERE tenant_id = 1 AND product_code = 'INT-60' AND deleted = 0;
-- verified_name_cn: WeFlow-Tribranch三分支全腔内修复系统
UPDATE showroom_product SET legacy_product_code = 'product_060' WHERE tenant_id = 1 AND product_code = 'INT-61' AND deleted = 0;
-- verified_name_cn: WeFlow-JAAA复杂腹主动脉覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_061' WHERE tenant_id = 1 AND product_code = 'INT-62' AND deleted = 0;
-- verified_name_cn: ZIPPER主动脉弓腹膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_062' WHERE tenant_id = 1 AND product_code = 'INT-63' AND deleted = 0;
-- verified_name_cn: 长鞘
UPDATE showroom_product SET legacy_product_code = 'product_064' WHERE tenant_id = 1 AND product_code = 'INT-65' AND deleted = 0;
-- verified_name_cn: 微导管（外周）
UPDATE showroom_product SET legacy_product_code = 'product_065' WHERE tenant_id = 1 AND product_code = 'INT-66' AND deleted = 0;
-- verified_name_cn: 超滑导丝
UPDATE showroom_product SET legacy_product_code = 'product_067' WHERE tenant_id = 1 AND product_code = 'INT-68' AND deleted = 0;
-- verified_name_cn: 负压抽吸泵
UPDATE showroom_product SET legacy_product_code = 'product_075' WHERE tenant_id = 1 AND product_code = 'INT-76' AND deleted = 0;
-- verified_name_cn: 可降解鼻窦药物支架
UPDATE showroom_product SET legacy_product_code = 'product_076' WHERE tenant_id = 1 AND product_code = 'INT-77' AND deleted = 0;
-- verified_name_cn: 鼻腔冲洗液
UPDATE showroom_product SET legacy_product_code = 'product_077' WHERE tenant_id = 1 AND product_code = 'INT-78' AND deleted = 0;
-- verified_name_cn: 抗鼻腔过敏凝胶
UPDATE showroom_product SET legacy_product_code = 'product_078' WHERE tenant_id = 1 AND product_code = 'INT-79' AND deleted = 0;
-- verified_name_cn: 咽鼓管球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_079' WHERE tenant_id = 1 AND product_code = 'INT-80' AND deleted = 0;
-- verified_name_cn: 筋膜缝合器
UPDATE showroom_product SET legacy_product_code = 'product_083' WHERE tenant_id = 1 AND product_code = 'INT-84' AND deleted = 0;
-- verified_name_cn: 可调弯造影导管
UPDATE showroom_product SET legacy_product_code = 'product_084' WHERE tenant_id = 1 AND product_code = 'INT-85' AND deleted = 0;
-- verified_name_cn: 神经微导管
UPDATE showroom_product SET legacy_product_code = 'product_087' WHERE tenant_id = 1 AND product_code = 'INT-88' AND deleted = 0;
-- verified_name_cn: 颅内支架系统
UPDATE showroom_product SET legacy_product_code = 'product_088' WHERE tenant_id = 1 AND product_code = 'INT-89' AND deleted = 0;
-- verified_name_cn: 神经介入手术器械包(治疗)
UPDATE showroom_product SET legacy_product_code = 'product_089' WHERE tenant_id = 1 AND product_code = 'INT-90' AND deleted = 0;
-- verified_name_cn: 神经介入手术器械包(造影)
UPDATE showroom_product SET legacy_product_code = 'product_090' WHERE tenant_id = 1 AND product_code = 'INT-91' AND deleted = 0;
-- verified_name_cn: 微导丝CG
UPDATE showroom_product SET legacy_product_code = 'product_091' WHERE tenant_id = 1 AND product_code = 'INT-92' AND deleted = 0;
-- verified_name_cn: 微导丝TG
UPDATE showroom_product SET legacy_product_code = 'product_092' WHERE tenant_id = 1 AND product_code = 'INT-93' AND deleted = 0;
-- verified_name_cn: 栓塞保护器
UPDATE showroom_product SET legacy_product_code = 'product_093' WHERE tenant_id = 1 AND product_code = 'INT-94' AND deleted = 0;
-- verified_name_cn: 一次性使用血管内微导丝
UPDATE showroom_product SET legacy_product_code = 'product_094' WHERE tenant_id = 1 AND product_code = 'INT-95' AND deleted = 0;
-- verified_name_cn: 颅内取栓支架
UPDATE showroom_product SET legacy_product_code = 'product_097' WHERE tenant_id = 1 AND product_code = 'INT-98' AND deleted = 0;
-- verified_name_cn: 磷酰胆碱涂层密网支架
UPDATE showroom_product SET legacy_product_code = 'product_098' WHERE tenant_id = 1 AND product_code = 'INT-99' AND deleted = 0;
-- verified_name_cn: 无创透皮系统
UPDATE showroom_product SET legacy_product_code = 'product_099' WHERE tenant_id = 1 AND product_code = 'INT-100' AND deleted = 0;
-- verified_name_cn: 瑛之秘头皮赋活精华液
UPDATE showroom_product SET legacy_product_code = 'product_100' WHERE tenant_id = 1 AND product_code = 'INT-101' AND deleted = 0;
-- verified_name_cn: 瑛之秘多肽修护液
UPDATE showroom_product SET legacy_product_code = 'product_101' WHERE tenant_id = 1 AND product_code = 'INT-102' AND deleted = 0;
-- verified_name_cn: 无针透皮组合
UPDATE showroom_product SET legacy_product_code = 'product_102' WHERE tenant_id = 1 AND product_code = 'INT-103' AND deleted = 0;
-- verified_name_cn: 微针
UPDATE showroom_product SET legacy_product_code = 'product_103' WHERE tenant_id = 1 AND product_code = 'INT-104' AND deleted = 0;
-- verified_name_cn: 瑛之秘 肌活修护精华霜
UPDATE showroom_product SET legacy_product_code = 'product_104' WHERE tenant_id = 1 AND product_code = 'INT-105' AND deleted = 0;
-- verified_name_cn: 瑛之秘淡纹修护精华液
UPDATE showroom_product SET legacy_product_code = 'product_105' WHERE tenant_id = 1 AND product_code = 'INT-106' AND deleted = 0;
-- verified_name_cn: 瑛之秘舒润弹嫩精萃水
UPDATE showroom_product SET legacy_product_code = 'product_106' WHERE tenant_id = 1 AND product_code = 'INT-107' AND deleted = 0;
-- verified_name_cn: 瑛之秘赋活凝萃精华液
UPDATE showroom_product SET legacy_product_code = 'product_107' WHERE tenant_id = 1 AND product_code = 'INT-108' AND deleted = 0;
-- verified_name_cn: 台车
-- verified_name_cn_cont: （聚焦超声无创治疗顽固性高血压系统）
UPDATE showroom_product SET legacy_product_code = 'product_108' WHERE tenant_id = 1 AND product_code = 'INT-109' AND deleted = 0;
-- verified_name_cn: 关节介入手术器械
UPDATE showroom_product SET legacy_product_code = 'product_109' WHERE tenant_id = 1 AND product_code = 'INT-110' AND deleted = 0;
-- verified_name_cn: 可视软组织松解器械及组件
UPDATE showroom_product SET legacy_product_code = 'product_110' WHERE tenant_id = 1 AND product_code = 'INT-111' AND deleted = 0;
-- verified_name_cn: 椎体工具包
UPDATE showroom_product SET legacy_product_code = 'product_111' WHERE tenant_id = 1 AND product_code = 'INT-112' AND deleted = 0;
-- verified_name_cn: 骨水泥成型套装
UPDATE showroom_product SET legacy_product_code = 'product_112' WHERE tenant_id = 1 AND product_code = 'INT-113' AND deleted = 0;
-- verified_name_cn: 人工骨
UPDATE showroom_product SET legacy_product_code = 'product_113' WHERE tenant_id = 1 AND product_code = 'INT-114' AND deleted = 0;
-- verified_name_cn: 可降解镁合金骨钉
UPDATE showroom_product SET legacy_product_code = 'product_114' WHERE tenant_id = 1 AND product_code = 'INT-115' AND deleted = 0;
-- verified_name_cn: 可降解镁合金骨板
UPDATE showroom_product SET legacy_product_code = 'product_115' WHERE tenant_id = 1 AND product_code = 'INT-116' AND deleted = 0;
-- verified_name_cn: 骨髓血穿刺抽吸循环器械
UPDATE showroom_product SET legacy_product_code = 'product_116' WHERE tenant_id = 1 AND product_code = 'INT-117' AND deleted = 0;
-- verified_name_cn: 椎体扩张球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_117' WHERE tenant_id = 1 AND product_code = 'INT-118' AND deleted = 0;
-- verified_name_cn: 骨髓血穿刺抽吸循环动力泵
UPDATE showroom_product SET legacy_product_code = 'product_118' WHERE tenant_id = 1 AND product_code = 'INT-119' AND deleted = 0;
-- verified_name_cn: 一次性使用电子输尿管镜
UPDATE showroom_product SET legacy_product_code = 'product_119' WHERE tenant_id = 1 AND product_code = 'INT-120' AND deleted = 0;
-- verified_name_cn: 带压力监测电子镜导管
UPDATE showroom_product SET legacy_product_code = 'product_120' WHERE tenant_id = 1 AND product_code = 'INT-121' AND deleted = 0;
-- verified_name_cn: 硬管电子输尿管镜
UPDATE showroom_product SET legacy_product_code = 'product_121' WHERE tenant_id = 1 AND product_code = 'INT-122' AND deleted = 0;
-- verified_name_cn: 输尿管球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_122' WHERE tenant_id = 1 AND product_code = 'INT-123' AND deleted = 0;
-- verified_name_cn: 一次性输尿管负压导引鞘
UPDATE showroom_product SET legacy_product_code = 'product_123' WHERE tenant_id = 1 AND product_code = 'INT-124' AND deleted = 0;
-- verified_name_cn: 内窥镜取石网篮
UPDATE showroom_product SET legacy_product_code = 'product_124' WHERE tenant_id = 1 AND product_code = 'INT-125' AND deleted = 0;
-- verified_name_cn: 取石球囊
UPDATE showroom_product SET legacy_product_code = 'product_125' WHERE tenant_id = 1 AND product_code = 'INT-126' AND deleted = 0;
-- verified_name_cn: 无菌抽吸管路
UPDATE showroom_product SET legacy_product_code = 'product_126' WHERE tenant_id = 1 AND product_code = 'INT-127' AND deleted = 0;
-- verified_name_cn: 斑马导丝
UPDATE showroom_product SET legacy_product_code = 'product_127' WHERE tenant_id = 1 AND product_code = 'INT-128' AND deleted = 0;
-- verified_name_cn: 台车
-- verified_name_cn_cont: （控温控压软镜系统）
UPDATE showroom_product SET legacy_product_code = 'product_129' WHERE tenant_id = 1 AND product_code = 'INT-130' AND deleted = 0;
-- verified_name_cn: 液体创口贴
UPDATE showroom_product SET legacy_product_code = 'product_130' WHERE tenant_id = 1 AND product_code = 'INT-131' AND deleted = 0;
-- verified_name_cn: 光控可吸收粘合剂
UPDATE showroom_product SET legacy_product_code = 'product_131' WHERE tenant_id = 1 AND product_code = 'INT-132' AND deleted = 0;
-- verified_name_cn: 水凝胶
UPDATE showroom_product SET legacy_product_code = 'product_132' WHERE tenant_id = 1 AND product_code = 'INT-133' AND deleted = 0;
-- verified_name_cn: 肠道球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_133' WHERE tenant_id = 1 AND product_code = 'INT-134' AND deleted = 0;
-- verified_name_cn: 非重力输注装置
UPDATE showroom_product SET legacy_product_code = 'product_134' WHERE tenant_id = 1 AND product_code = 'INT-135' AND deleted = 0;
-- verified_name_cn: 电子脐带剪
UPDATE showroom_product SET legacy_product_code = 'product_135' WHERE tenant_id = 1 AND product_code = 'INT-136' AND deleted = 0;
-- verified_name_cn: 弹簧管
UPDATE showroom_product SET legacy_product_code = 'product_136' WHERE tenant_id = 1 AND product_code = 'INT-137' AND deleted = 0;
-- verified_name_cn: 膀胱压力监测器
UPDATE showroom_product SET legacy_product_code = 'product_137' WHERE tenant_id = 1 AND product_code = 'INT-138' AND deleted = 0;
-- verified_name_cn: CT造影套件
UPDATE showroom_product SET legacy_product_code = 'product_138' WHERE tenant_id = 1 AND product_code = 'INT-139' AND deleted = 0;
-- verified_name_cn: 输入接头及附件
-- verified_name_cn_cont: (加药延长管、加药接头)
UPDATE showroom_product SET legacy_product_code = 'product_139' WHERE tenant_id = 1 AND product_code = 'INT-140' AND deleted = 0;
-- verified_name_cn: 颅内血栓抽吸导管
UPDATE showroom_product SET legacy_product_code = 'product_140' WHERE tenant_id = 1 AND product_code = 'INT-141' AND deleted = 0;
-- verified_name_cn: 神经输送支架微导管
UPDATE showroom_product SET legacy_product_code = 'product_141' WHERE tenant_id = 1 AND product_code = 'INT-142' AND deleted = 0;
-- verified_name_cn: 可控弯导管 内径6F-20F
UPDATE showroom_product SET legacy_product_code = 'product_142' WHERE tenant_id = 1 AND product_code = 'INT-143' AND deleted = 0;
-- verified_name_cn: 胆道用斑马导丝
UPDATE showroom_product SET legacy_product_code = 'product_143' WHERE tenant_id = 1 AND product_code = 'INT-144' AND deleted = 0;
-- verified_name_cn: 胆道取石球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_144' WHERE tenant_id = 1 AND product_code = 'INT-145' AND deleted = 0;
-- verified_name_cn: 取石网篮
UPDATE showroom_product SET legacy_product_code = 'product_145' WHERE tenant_id = 1 AND product_code = 'INT-146' AND deleted = 0;
-- verified_name_cn: 心内输送导丝微导管
UPDATE showroom_product SET legacy_product_code = 'product_146' WHERE tenant_id = 1 AND product_code = 'INT-147' AND deleted = 0;
-- verified_name_cn: OEM编织导管
UPDATE showroom_product SET legacy_product_code = 'product_147' WHERE tenant_id = 1 AND product_code = 'INT-148' AND deleted = 0;
-- verified_name_cn: PTFE管 壁厚：5μm
UPDATE showroom_product SET legacy_product_code = 'product_148' WHERE tenant_id = 1 AND product_code = 'INT-149' AND deleted = 0;
-- verified_name_cn: 热收缩管收缩比2:1
UPDATE showroom_product SET legacy_product_code = 'product_150' WHERE tenant_id = 1 AND product_code = 'INT-151' AND deleted = 0;
-- verified_name_cn: 亲水涂层溶液
UPDATE showroom_product SET legacy_product_code = 'product_151' WHERE tenant_id = 1 AND product_code = 'INT-152' AND deleted = 0;
-- verified_name_cn: 海波管
UPDATE showroom_product SET legacy_product_code = 'product_152' WHERE tenant_id = 1 AND product_code = 'INT-153' AND deleted = 0;
-- verified_name_cn: 蚀刻PTFE内衬管
UPDATE showroom_product SET legacy_product_code = 'product_153' WHERE tenant_id = 1 AND product_code = 'INT-154' AND deleted = 0;
-- verified_name_cn: 导丝芯轴
UPDATE showroom_product SET legacy_product_code = 'product_154' WHERE tenant_id = 1 AND product_code = 'INT-155' AND deleted = 0;
-- verified_name_cn: 扁丝
UPDATE showroom_product SET legacy_product_code = 'product_155' WHERE tenant_id = 1 AND product_code = 'INT-156' AND deleted = 0;
-- verified_name_cn: 热收缩管
UPDATE showroom_product SET legacy_product_code = 'product_156' WHERE tenant_id = 1 AND product_code = 'INT-157' AND deleted = 0;
-- verified_name_cn: PTFE涂层钢丝
UPDATE showroom_product SET legacy_product_code = 'product_157' WHERE tenant_id = 1 AND product_code = 'INT-158' AND deleted = 0;
-- verified_name_cn: 磨削芯丝
UPDATE showroom_product SET legacy_product_code = 'product_158' WHERE tenant_id = 1 AND product_code = 'INT-159' AND deleted = 0;
-- verified_name_cn: 冲洗阀
UPDATE showroom_product SET legacy_product_code = 'product_159' WHERE tenant_id = 1 AND product_code = 'INT-160' AND deleted = 0;
-- verified_name_cn: 单向阀
UPDATE showroom_product SET legacy_product_code = 'product_160' WHERE tenant_id = 1 AND product_code = 'INT-161' AND deleted = 0;
-- verified_name_cn: 正压接头
UPDATE showroom_product SET legacy_product_code = 'product_161' WHERE tenant_id = 1 AND product_code = 'INT-162' AND deleted = 0;
-- verified_name_cn: 精量调节器
UPDATE showroom_product SET legacy_product_code = 'product_162' WHERE tenant_id = 1 AND product_code = 'INT-163' AND deleted = 0;
-- verified_name_cn: 接头类
UPDATE showroom_product SET legacy_product_code = 'product_163' WHERE tenant_id = 1 AND product_code = 'INT-164' AND deleted = 0;
-- verified_name_cn: 三通旋塞
UPDATE showroom_product SET legacy_product_code = 'product_164' WHERE tenant_id = 1 AND product_code = 'INT-165' AND deleted = 0;
-- verified_name_cn: 无针加药接头
UPDATE showroom_product SET legacy_product_code = 'product_165' WHERE tenant_id = 1 AND product_code = 'INT-166' AND deleted = 0;
-- verified_name_cn: 抗脂三通
UPDATE showroom_product SET legacy_product_code = 'product_003' WHERE tenant_id = 122 AND product_code = 'INT-3' AND deleted = 0;
-- verified_name_cn: Y型连接器
UPDATE showroom_product SET legacy_product_code = 'product_004' WHERE tenant_id = 122 AND product_code = 'INT-4' AND deleted = 0;
-- verified_name_cn: 按压式Y型连接器
UPDATE showroom_product SET legacy_product_code = 'product_005' WHERE tenant_id = 122 AND product_code = 'INT-5' AND deleted = 0;
-- verified_name_cn: Y型连接阀套件
UPDATE showroom_product SET legacy_product_code = 'product_006' WHERE tenant_id = 122 AND product_code = 'INT-6' AND deleted = 0;
-- verified_name_cn: 穿刺针
UPDATE showroom_product SET legacy_product_code = 'product_007' WHERE tenant_id = 122 AND product_code = 'INT-7' AND deleted = 0;
-- verified_name_cn: 一次性使用血管鞘
UPDATE showroom_product SET legacy_product_code = 'product_008' WHERE tenant_id = 122 AND product_code = 'INT-8' AND deleted = 0;
-- verified_name_cn: 一次性使用导管鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_009' WHERE tenant_id = 122 AND product_code = 'INT-9' AND deleted = 0;
-- verified_name_cn: 一次性使用亲水涂层导管鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_010' WHERE tenant_id = 122 AND product_code = 'INT-10' AND deleted = 0;
-- verified_name_cn: 股动脉鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_011' WHERE tenant_id = 122 AND product_code = 'INT-11' AND deleted = 0;
-- verified_name_cn: 桡动脉鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_012' WHERE tenant_id = 122 AND product_code = 'INT-12' AND deleted = 0;
-- verified_name_cn: 球囊扩张压力泵
UPDATE showroom_product SET legacy_product_code = 'product_013' WHERE tenant_id = 122 AND product_code = 'INT-13' AND deleted = 0;
-- verified_name_cn: 数显球囊扩张压力泵
UPDATE showroom_product SET legacy_product_code = 'product_014' WHERE tenant_id = 122 AND product_code = 'INT-14' AND deleted = 0;
-- verified_name_cn: 按压式球囊扩张压力泵
UPDATE showroom_product SET legacy_product_code = 'product_016' WHERE tenant_id = 122 AND product_code = 'INT-16' AND deleted = 0;
-- verified_name_cn: 造影剂推入器
UPDATE showroom_product SET legacy_product_code = 'product_017' WHERE tenant_id = 122 AND product_code = 'INT-17' AND deleted = 0;
-- verified_name_cn: 造影剂推入器-C
UPDATE showroom_product SET legacy_product_code = 'product_018' WHERE tenant_id = 122 AND product_code = 'INT-18' AND deleted = 0;
-- verified_name_cn: 股动脉止血带
UPDATE showroom_product SET legacy_product_code = 'product_019' WHERE tenant_id = 122 AND product_code = 'INT-19' AND deleted = 0;
-- verified_name_cn: 股动脉止血带
UPDATE showroom_product SET legacy_product_code = 'product_020' WHERE tenant_id = 122 AND product_code = 'INT-20' AND deleted = 0;
-- verified_name_cn: 气囊式止血带II
UPDATE showroom_product SET legacy_product_code = 'product_021' WHERE tenant_id = 122 AND product_code = 'INT-21' AND deleted = 0;
-- verified_name_cn: 桡动脉止血带I
UPDATE showroom_product SET legacy_product_code = 'product_022' WHERE tenant_id = 122 AND product_code = 'INT-22' AND deleted = 0;
-- verified_name_cn: 压力传感器
UPDATE showroom_product SET legacy_product_code = 'product_023' WHERE tenant_id = 122 AND product_code = 'INT-23' AND deleted = 0;
-- verified_name_cn: 有创压力传感器
UPDATE showroom_product SET legacy_product_code = 'product_024' WHERE tenant_id = 122 AND product_code = 'INT-24' AND deleted = 0;
-- verified_name_cn: 高压延长管
UPDATE showroom_product SET legacy_product_code = 'product_025' WHERE tenant_id = 122 AND product_code = 'INT-25' AND deleted = 0;
-- verified_name_cn: 介入手术器械包
UPDATE showroom_product SET legacy_product_code = 'product_026' WHERE tenant_id = 122 AND product_code = 'INT-27' AND deleted = 0;
-- verified_name_cn: 造影导管
UPDATE showroom_product SET legacy_product_code = 'product_027' WHERE tenant_id = 122 AND product_code = 'INT-28' AND deleted = 0;
-- verified_name_cn: 亲水涂层造影导管
UPDATE showroom_product SET legacy_product_code = 'product_028' WHERE tenant_id = 122 AND product_code = 'INT-29' AND deleted = 0;
-- verified_name_cn: 亲水涂层血管造影导管
UPDATE showroom_product SET legacy_product_code = 'product_029' WHERE tenant_id = 122 AND product_code = 'INT-30' AND deleted = 0;
-- verified_name_cn: 指引导管
UPDATE showroom_product SET legacy_product_code = 'product_030' WHERE tenant_id = 122 AND product_code = 'INT-31' AND deleted = 0;
-- verified_name_cn: 导引导管
UPDATE showroom_product SET legacy_product_code = 'product_031' WHERE tenant_id = 122 AND product_code = 'INT-32' AND deleted = 0;
-- verified_name_cn: 亲水涂层导引导管
UPDATE showroom_product SET legacy_product_code = 'product_032' WHERE tenant_id = 122 AND product_code = 'INT-33' AND deleted = 0;
-- verified_name_cn: PTFE导丝
UPDATE showroom_product SET legacy_product_code = 'product_033' WHERE tenant_id = 122 AND product_code = 'INT-34' AND deleted = 0;
-- verified_name_cn: PTCA球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_034' WHERE tenant_id = 122 AND product_code = 'INT-35' AND deleted = 0;
-- verified_name_cn: 指引导丝
UPDATE showroom_product SET legacy_product_code = 'product_035' WHERE tenant_id = 122 AND product_code = 'INT-36' AND deleted = 0;
-- verified_name_cn: 多环测量灌注导管
UPDATE showroom_product SET legacy_product_code = 'product_036' WHERE tenant_id = 122 AND product_code = 'INT-37' AND deleted = 0;
-- verified_name_cn: 冠脉微导管
UPDATE showroom_product SET legacy_product_code = 'product_037' WHERE tenant_id = 122 AND product_code = 'INT-38' AND deleted = 0;
-- verified_name_cn: 超滑导丝
UPDATE showroom_product SET legacy_product_code = 'product_038' WHERE tenant_id = 122 AND product_code = 'INT-39' AND deleted = 0;
-- verified_name_cn: 依维莫司冠脉乳突球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_039' WHERE tenant_id = 122 AND product_code = 'INT-40' AND deleted = 0;
-- verified_name_cn: 指引延长导管
UPDATE showroom_product SET legacy_product_code = 'product_040' WHERE tenant_id = 122 AND product_code = 'INT-41' AND deleted = 0;
-- verified_name_cn: 棘突球囊
UPDATE showroom_product SET legacy_product_code = 'product_041' WHERE tenant_id = 122 AND product_code = 'INT-42' AND deleted = 0;
-- verified_name_cn: 左心耳封堵器系统
UPDATE showroom_product SET legacy_product_code = 'product_042' WHERE tenant_id = 122 AND product_code = 'INT-43' AND deleted = 0;
-- verified_name_cn: 一次性使用心脏脉冲电场消融导管
UPDATE showroom_product SET legacy_product_code = 'product_043' WHERE tenant_id = 122 AND product_code = 'INT-44' AND deleted = 0;
-- verified_name_cn: 可调弯导管鞘
UPDATE showroom_product SET legacy_product_code = 'product_044' WHERE tenant_id = 122 AND product_code = 'INT-45' AND deleted = 0;
-- verified_name_cn: 血管鞘
UPDATE showroom_product SET legacy_product_code = 'product_045' WHERE tenant_id = 122 AND product_code = 'INT-46' AND deleted = 0;
-- verified_name_cn: 心脏瓣膜球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_046' WHERE tenant_id = 122 AND product_code = 'INT-47' AND deleted = 0;
-- verified_name_cn: 可扩张血管鞘套装
UPDATE showroom_product SET legacy_product_code = 'product_047' WHERE tenant_id = 122 AND product_code = 'INT-48' AND deleted = 0;
-- verified_name_cn: 心脏瓣膜支架Hanchor Valve
UPDATE showroom_product SET legacy_product_code = 'product_048' WHERE tenant_id = 122 AND product_code = 'INT-49' AND deleted = 0;
-- verified_name_cn: 新型聚合物瓣膜
UPDATE showroom_product SET legacy_product_code = 'product_050' WHERE tenant_id = 122 AND product_code = 'INT-51' AND deleted = 0;
-- verified_name_cn: 可降解镁合金支架
UPDATE showroom_product SET legacy_product_code = 'product_051' WHERE tenant_id = 122 AND product_code = 'INT-52' AND deleted = 0;
-- verified_name_cn: 自主冶炼镁合金管材
UPDATE showroom_product SET legacy_product_code = 'product_052' WHERE tenant_id = 122 AND product_code = 'INT-53' AND deleted = 0;
-- verified_name_cn: 自主冶炼镁合金管棒材
UPDATE showroom_product SET legacy_product_code = 'product_053' WHERE tenant_id = 122 AND product_code = 'INT-54' AND deleted = 0;
-- verified_name_cn: 人工血管
UPDATE showroom_product SET legacy_product_code = 'product_054' WHERE tenant_id = 122 AND product_code = 'INT-55' AND deleted = 0;
-- verified_name_cn: 胸主动脉覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_055' WHERE tenant_id = 122 AND product_code = 'INT-56' AND deleted = 0;
-- verified_name_cn: Grency髂静脉支架系统
UPDATE showroom_product SET legacy_product_code = 'product_056' WHERE tenant_id = 122 AND product_code = 'INT-57' AND deleted = 0;
-- verified_name_cn: 分体式分支型胸主动脉覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_057' WHERE tenant_id = 122 AND product_code = 'INT-58' AND deleted = 0;
-- verified_name_cn: 胸主动脉裸支架
UPDATE showroom_product SET legacy_product_code = 'product_058' WHERE tenant_id = 122 AND product_code = 'INT-59' AND deleted = 0;
-- verified_name_cn: 模块内嵌双分支覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_059' WHERE tenant_id = 122 AND product_code = 'INT-60' AND deleted = 0;
-- verified_name_cn: WeFlow-Tribranch三分支全腔内修复系统
UPDATE showroom_product SET legacy_product_code = 'product_060' WHERE tenant_id = 122 AND product_code = 'INT-61' AND deleted = 0;
-- verified_name_cn: WeFlow-JAAA复杂腹主动脉覆膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_061' WHERE tenant_id = 122 AND product_code = 'INT-62' AND deleted = 0;
-- verified_name_cn: ZIPPER主动脉弓腹膜支架系统
UPDATE showroom_product SET legacy_product_code = 'product_062' WHERE tenant_id = 122 AND product_code = 'INT-63' AND deleted = 0;
-- verified_name_cn: 长鞘
UPDATE showroom_product SET legacy_product_code = 'product_064' WHERE tenant_id = 122 AND product_code = 'INT-65' AND deleted = 0;
-- verified_name_cn: 微导管（外周）
UPDATE showroom_product SET legacy_product_code = 'product_065' WHERE tenant_id = 122 AND product_code = 'INT-66' AND deleted = 0;
-- verified_name_cn: 超滑导丝
UPDATE showroom_product SET legacy_product_code = 'product_067' WHERE tenant_id = 122 AND product_code = 'INT-68' AND deleted = 0;
-- verified_name_cn: 负压抽吸泵
UPDATE showroom_product SET legacy_product_code = 'product_075' WHERE tenant_id = 122 AND product_code = 'INT-76' AND deleted = 0;
-- verified_name_cn: 可降解鼻窦药物支架
UPDATE showroom_product SET legacy_product_code = 'product_076' WHERE tenant_id = 122 AND product_code = 'INT-77' AND deleted = 0;
-- verified_name_cn: 鼻腔冲洗液
UPDATE showroom_product SET legacy_product_code = 'product_077' WHERE tenant_id = 122 AND product_code = 'INT-78' AND deleted = 0;
-- verified_name_cn: 抗鼻腔过敏凝胶
UPDATE showroom_product SET legacy_product_code = 'product_078' WHERE tenant_id = 122 AND product_code = 'INT-79' AND deleted = 0;
-- verified_name_cn: 咽鼓管球囊扩张导管
UPDATE showroom_product SET legacy_product_code = 'product_079' WHERE tenant_id = 122 AND product_code = 'INT-80' AND deleted = 0;
-- verified_name_cn: 筋膜缝合器
UPDATE showroom_product SET legacy_product_code = 'product_083' WHERE tenant_id = 122 AND product_code = 'INT-84' AND deleted = 0;
-- verified_name_cn: 可调弯造影导管
UPDATE showroom_product SET legacy_product_code = 'product_084' WHERE tenant_id = 122 AND product_code = 'INT-85' AND deleted = 0;
-- verified_name_cn: 神经微导管
UPDATE showroom_product SET legacy_product_code = 'product_087' WHERE tenant_id = 122 AND product_code = 'INT-88' AND deleted = 0;
-- verified_name_cn: 颅内支架系统
UPDATE showroom_product SET legacy_product_code = 'product_088' WHERE tenant_id = 122 AND product_code = 'INT-89' AND deleted = 0;
-- verified_name_cn: 神经介入手术器械包(治疗)
UPDATE showroom_product SET legacy_product_code = 'product_089' WHERE tenant_id = 122 AND product_code = 'INT-90' AND deleted = 0;
-- verified_name_cn: 神经介入手术器械包(造影)
UPDATE showroom_product SET legacy_product_code = 'product_090' WHERE tenant_id = 122 AND product_code = 'INT-91' AND deleted = 0;
-- verified_name_cn: 微导丝CG
UPDATE showroom_product SET legacy_product_code = 'product_091' WHERE tenant_id = 122 AND product_code = 'INT-92' AND deleted = 0;
-- verified_name_cn: 微导丝TG
UPDATE showroom_product SET legacy_product_code = 'product_092' WHERE tenant_id = 122 AND product_code = 'INT-93' AND deleted = 0;
-- verified_name_cn: 栓塞保护器
UPDATE showroom_product SET legacy_product_code = 'product_093' WHERE tenant_id = 122 AND product_code = 'INT-94' AND deleted = 0;
-- verified_name_cn: 一次性使用血管内微导丝
UPDATE showroom_product SET legacy_product_code = 'product_094' WHERE tenant_id = 122 AND product_code = 'INT-95' AND deleted = 0;
-- verified_name_cn: 颅内取栓支架
UPDATE showroom_product SET legacy_product_code = 'product_097' WHERE tenant_id = 122 AND product_code = 'INT-98' AND deleted = 0;
-- verified_name_cn: 磷酰胆碱涂层密网支架
UPDATE showroom_product SET legacy_product_code = 'product_098' WHERE tenant_id = 122 AND product_code = 'INT-99' AND deleted = 0;
-- verified_name_cn: 无创透皮系统
UPDATE showroom_product SET legacy_product_code = 'product_099' WHERE tenant_id = 122 AND product_code = 'INT-100' AND deleted = 0;
-- verified_name_cn: 瑛之秘头皮赋活精华液
UPDATE showroom_product SET legacy_product_code = 'product_100' WHERE tenant_id = 122 AND product_code = 'INT-101' AND deleted = 0;
-- verified_name_cn: 瑛之秘多肽修护液
UPDATE showroom_product SET legacy_product_code = 'product_101' WHERE tenant_id = 122 AND product_code = 'INT-102' AND deleted = 0;
-- verified_name_cn: 无针透皮组合
UPDATE showroom_product SET legacy_product_code = 'product_102' WHERE tenant_id = 122 AND product_code = 'INT-103' AND deleted = 0;
-- verified_name_cn: 微针
UPDATE showroom_product SET legacy_product_code = 'product_103' WHERE tenant_id = 122 AND product_code = 'INT-104' AND deleted = 0;
-- verified_name_cn: 瑛之秘 肌活修护精华霜
UPDATE showroom_product SET legacy_product_code = 'product_104' WHERE tenant_id = 122 AND product_code = 'INT-105' AND deleted = 0;
-- verified_name_cn: 瑛之秘淡纹修护精华液
UPDATE showroom_product SET legacy_product_code = 'product_105' WHERE tenant_id = 122 AND product_code = 'INT-106' AND deleted = 0;
-- verified_name_cn: 瑛之秘舒润弹嫩精萃水
UPDATE showroom_product SET legacy_product_code = 'product_106' WHERE tenant_id = 122 AND product_code = 'INT-107' AND deleted = 0;
-- verified_name_cn: 瑛之秘赋活凝萃精华液
UPDATE showroom_product SET legacy_product_code = 'product_107' WHERE tenant_id = 122 AND product_code = 'INT-108' AND deleted = 0;
-- verified_name_cn: 台车
-- verified_name_cn_cont: （聚焦超声无创治疗顽固性高血压系统）
UPDATE showroom_product SET legacy_product_code = 'product_108' WHERE tenant_id = 122 AND product_code = 'INT-109' AND deleted = 0;
-- verified_name_cn: 关节介入手术器械
UPDATE showroom_product SET legacy_product_code = 'product_109' WHERE tenant_id = 122 AND product_code = 'INT-110' AND deleted = 0;
-- verified_name_cn: 可视软组织松解器械及组件
UPDATE showroom_product SET legacy_product_code = 'product_110' WHERE tenant_id = 122 AND product_code = 'INT-111' AND deleted = 0;
-- verified_name_cn: 椎体工具包
UPDATE showroom_product SET legacy_product_code = 'product_111' WHERE tenant_id = 122 AND product_code = 'INT-112' AND deleted = 0;
-- verified_name_cn: 骨水泥成型套装
UPDATE showroom_product SET legacy_product_code = 'product_112' WHERE tenant_id = 122 AND product_code = 'INT-113' AND deleted = 0;
-- verified_name_cn: 人工骨
UPDATE showroom_product SET legacy_product_code = 'product_113' WHERE tenant_id = 122 AND product_code = 'INT-114' AND deleted = 0;
-- verified_name_cn: 可降解镁合金骨钉
UPDATE showroom_product SET legacy_product_code = 'product_114' WHERE tenant_id = 122 AND product_code = 'INT-115' AND deleted = 0;
-- verified_name_cn: 可降解镁合金骨板
UPDATE showroom_product SET legacy_product_code = 'product_115' WHERE tenant_id = 122 AND product_code = 'INT-116' AND deleted = 0;
-- verified_name_cn: 骨髓血穿刺抽吸循环器械
UPDATE showroom_product SET legacy_product_code = 'product_116' WHERE tenant_id = 122 AND product_code = 'INT-117' AND deleted = 0;
-- verified_name_cn: 椎体扩张球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_117' WHERE tenant_id = 122 AND product_code = 'INT-118' AND deleted = 0;
-- verified_name_cn: 骨髓血穿刺抽吸循环动力泵
UPDATE showroom_product SET legacy_product_code = 'product_118' WHERE tenant_id = 122 AND product_code = 'INT-119' AND deleted = 0;
-- verified_name_cn: 一次性使用电子输尿管镜
UPDATE showroom_product SET legacy_product_code = 'product_119' WHERE tenant_id = 122 AND product_code = 'INT-120' AND deleted = 0;
-- verified_name_cn: 带压力监测电子镜导管
UPDATE showroom_product SET legacy_product_code = 'product_120' WHERE tenant_id = 122 AND product_code = 'INT-121' AND deleted = 0;
-- verified_name_cn: 硬管电子输尿管镜
UPDATE showroom_product SET legacy_product_code = 'product_121' WHERE tenant_id = 122 AND product_code = 'INT-122' AND deleted = 0;
-- verified_name_cn: 输尿管球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_122' WHERE tenant_id = 122 AND product_code = 'INT-123' AND deleted = 0;
-- verified_name_cn: 一次性输尿管负压导引鞘
UPDATE showroom_product SET legacy_product_code = 'product_123' WHERE tenant_id = 122 AND product_code = 'INT-124' AND deleted = 0;
-- verified_name_cn: 内窥镜取石网篮
UPDATE showroom_product SET legacy_product_code = 'product_124' WHERE tenant_id = 122 AND product_code = 'INT-125' AND deleted = 0;
-- verified_name_cn: 取石球囊
UPDATE showroom_product SET legacy_product_code = 'product_125' WHERE tenant_id = 122 AND product_code = 'INT-126' AND deleted = 0;
-- verified_name_cn: 无菌抽吸管路
UPDATE showroom_product SET legacy_product_code = 'product_126' WHERE tenant_id = 122 AND product_code = 'INT-127' AND deleted = 0;
-- verified_name_cn: 斑马导丝
UPDATE showroom_product SET legacy_product_code = 'product_127' WHERE tenant_id = 122 AND product_code = 'INT-128' AND deleted = 0;
-- verified_name_cn: 台车
-- verified_name_cn_cont: （控温控压软镜系统）
UPDATE showroom_product SET legacy_product_code = 'product_129' WHERE tenant_id = 122 AND product_code = 'INT-130' AND deleted = 0;
-- verified_name_cn: 液体创口贴
UPDATE showroom_product SET legacy_product_code = 'product_130' WHERE tenant_id = 122 AND product_code = 'INT-131' AND deleted = 0;
-- verified_name_cn: 光控可吸收粘合剂
UPDATE showroom_product SET legacy_product_code = 'product_131' WHERE tenant_id = 122 AND product_code = 'INT-132' AND deleted = 0;
-- verified_name_cn: 水凝胶
UPDATE showroom_product SET legacy_product_code = 'product_132' WHERE tenant_id = 122 AND product_code = 'INT-133' AND deleted = 0;
-- verified_name_cn: 肠道球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_133' WHERE tenant_id = 122 AND product_code = 'INT-134' AND deleted = 0;
-- verified_name_cn: 非重力输注装置
UPDATE showroom_product SET legacy_product_code = 'product_134' WHERE tenant_id = 122 AND product_code = 'INT-135' AND deleted = 0;
-- verified_name_cn: 电子脐带剪
UPDATE showroom_product SET legacy_product_code = 'product_135' WHERE tenant_id = 122 AND product_code = 'INT-136' AND deleted = 0;
-- verified_name_cn: 弹簧管
UPDATE showroom_product SET legacy_product_code = 'product_136' WHERE tenant_id = 122 AND product_code = 'INT-137' AND deleted = 0;
-- verified_name_cn: 膀胱压力监测器
UPDATE showroom_product SET legacy_product_code = 'product_137' WHERE tenant_id = 122 AND product_code = 'INT-138' AND deleted = 0;
-- verified_name_cn: CT造影套件
UPDATE showroom_product SET legacy_product_code = 'product_138' WHERE tenant_id = 122 AND product_code = 'INT-139' AND deleted = 0;
-- verified_name_cn: 输入接头及附件
-- verified_name_cn_cont: (加药延长管、加药接头)
UPDATE showroom_product SET legacy_product_code = 'product_139' WHERE tenant_id = 122 AND product_code = 'INT-140' AND deleted = 0;
-- verified_name_cn: 颅内血栓抽吸导管
UPDATE showroom_product SET legacy_product_code = 'product_140' WHERE tenant_id = 122 AND product_code = 'INT-141' AND deleted = 0;
-- verified_name_cn: 神经输送支架微导管
UPDATE showroom_product SET legacy_product_code = 'product_141' WHERE tenant_id = 122 AND product_code = 'INT-142' AND deleted = 0;
-- verified_name_cn: 可控弯导管 内径6F-20F
UPDATE showroom_product SET legacy_product_code = 'product_142' WHERE tenant_id = 122 AND product_code = 'INT-143' AND deleted = 0;
-- verified_name_cn: 胆道用斑马导丝
UPDATE showroom_product SET legacy_product_code = 'product_143' WHERE tenant_id = 122 AND product_code = 'INT-144' AND deleted = 0;
-- verified_name_cn: 胆道取石球囊导管
UPDATE showroom_product SET legacy_product_code = 'product_144' WHERE tenant_id = 122 AND product_code = 'INT-145' AND deleted = 0;
-- verified_name_cn: 取石网篮
UPDATE showroom_product SET legacy_product_code = 'product_145' WHERE tenant_id = 122 AND product_code = 'INT-146' AND deleted = 0;
-- verified_name_cn: 心内输送导丝微导管
UPDATE showroom_product SET legacy_product_code = 'product_146' WHERE tenant_id = 122 AND product_code = 'INT-147' AND deleted = 0;
-- verified_name_cn: OEM编织导管
UPDATE showroom_product SET legacy_product_code = 'product_147' WHERE tenant_id = 122 AND product_code = 'INT-148' AND deleted = 0;
-- verified_name_cn: PTFE管 壁厚：5μm
UPDATE showroom_product SET legacy_product_code = 'product_148' WHERE tenant_id = 122 AND product_code = 'INT-149' AND deleted = 0;
-- verified_name_cn: 热收缩管收缩比2:1
UPDATE showroom_product SET legacy_product_code = 'product_150' WHERE tenant_id = 122 AND product_code = 'INT-151' AND deleted = 0;
-- verified_name_cn: 亲水涂层溶液
UPDATE showroom_product SET legacy_product_code = 'product_151' WHERE tenant_id = 122 AND product_code = 'INT-152' AND deleted = 0;
-- verified_name_cn: 海波管
UPDATE showroom_product SET legacy_product_code = 'product_152' WHERE tenant_id = 122 AND product_code = 'INT-153' AND deleted = 0;
-- verified_name_cn: 蚀刻PTFE内衬管
UPDATE showroom_product SET legacy_product_code = 'product_153' WHERE tenant_id = 122 AND product_code = 'INT-154' AND deleted = 0;
-- verified_name_cn: 导丝芯轴
UPDATE showroom_product SET legacy_product_code = 'product_154' WHERE tenant_id = 122 AND product_code = 'INT-155' AND deleted = 0;
-- verified_name_cn: 扁丝
UPDATE showroom_product SET legacy_product_code = 'product_155' WHERE tenant_id = 122 AND product_code = 'INT-156' AND deleted = 0;
-- verified_name_cn: 热收缩管
UPDATE showroom_product SET legacy_product_code = 'product_156' WHERE tenant_id = 122 AND product_code = 'INT-157' AND deleted = 0;
-- verified_name_cn: PTFE涂层钢丝
UPDATE showroom_product SET legacy_product_code = 'product_157' WHERE tenant_id = 122 AND product_code = 'INT-158' AND deleted = 0;
-- verified_name_cn: 磨削芯丝
UPDATE showroom_product SET legacy_product_code = 'product_158' WHERE tenant_id = 122 AND product_code = 'INT-159' AND deleted = 0;
-- verified_name_cn: 冲洗阀
UPDATE showroom_product SET legacy_product_code = 'product_159' WHERE tenant_id = 122 AND product_code = 'INT-160' AND deleted = 0;
-- verified_name_cn: 单向阀
UPDATE showroom_product SET legacy_product_code = 'product_160' WHERE tenant_id = 122 AND product_code = 'INT-161' AND deleted = 0;
-- verified_name_cn: 正压接头
UPDATE showroom_product SET legacy_product_code = 'product_161' WHERE tenant_id = 122 AND product_code = 'INT-162' AND deleted = 0;
-- verified_name_cn: 精量调节器
UPDATE showroom_product SET legacy_product_code = 'product_162' WHERE tenant_id = 122 AND product_code = 'INT-163' AND deleted = 0;
-- verified_name_cn: 接头类
UPDATE showroom_product SET legacy_product_code = 'product_163' WHERE tenant_id = 122 AND product_code = 'INT-164' AND deleted = 0;
-- verified_name_cn: 三通旋塞
UPDATE showroom_product SET legacy_product_code = 'product_164' WHERE tenant_id = 122 AND product_code = 'INT-165' AND deleted = 0;
-- verified_name_cn: 无针加药接头
UPDATE showroom_product SET legacy_product_code = 'product_165' WHERE tenant_id = 122 AND product_code = 'INT-166' AND deleted = 0;
-- verified_name_cn: 抗脂三通
COMMIT;
