-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=data; riskLevel=medium
-- Auto-confirmable draft for remaining showroom legacy product-code mappings.
-- REVIEW ONLY: every SQL statement below is commented out and must not be executed directly.
-- Convert to executable SQL only after business confirms the target INT code and final equal product name.
-- 20260705 showroom legacy product code manual confirmed mapping
-- Generated from reviewed manual_decision_* rows only.
-- This SQL keeps the final INT current product name equal to the confirmed product name.
-- REVIEW ONLY: START TRANSACTION;

-- tenant_id=1 product_001 -> INT-1
-- confirmed_name_cn=一次性使用三通旋塞
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '一次性使用三通旋塞', r.name_en = 'Manifold' WHERE p.tenant_id = 1 AND p.product_code = 'INT-1' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_001' WHERE tenant_id = 1 AND product_code = 'INT-1' AND deleted = 0;

-- tenant_id=1 product_015 -> INT-15
-- confirmed_name_cn=按压式球囊扩充压力泵
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '按压式球囊扩充压力泵', r.name_en = 'Inflation Device II' WHERE p.tenant_id = 1 AND p.product_code = 'INT-15' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_015' WHERE tenant_id = 1 AND product_code = 'INT-15' AND deleted = 0;

-- tenant_id=1 product_049 -> INT-49
-- confirmed_name_cn=经导管主动脉瓣膜输送系统
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '经导管主动脉瓣膜输送系统', r.name_en = 'Transcatheter Aortic Valve Delivery System' WHERE p.tenant_id = 1 AND p.product_code = 'INT-49' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_049' WHERE tenant_id = 1 AND product_code = 'INT-49' AND deleted = 0;

-- tenant_id=1 product_066 -> INT-67
-- confirmed_name_cn=血栓抽吸导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '血栓抽吸导管', r.name_en = 'Neural Aspiration Catheter' WHERE p.tenant_id = 1 AND p.product_code = 'INT-67' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_066' WHERE tenant_id = 1 AND product_code = 'INT-67' AND deleted = 0;

-- tenant_id=1 product_080 -> INT-80
-- confirmed_name_cn=可降解耳鼻止血绵
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '可降解耳鼻止血绵', r.name_en = 'Biodegradable  Nasal & Ear Hemostatic Sponge' WHERE p.tenant_id = 1 AND p.product_code = 'INT-80' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_080' WHERE tenant_id = 1 AND product_code = 'INT-80' AND deleted = 0;

-- tenant_id=1 product_085 -> INT-85
-- confirmed_name_cn=球囊导引导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '球囊导引导管', r.name_en = 'Balloon Guide Catheter' WHERE p.tenant_id = 1 AND p.product_code = 'INT-85' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_085' WHERE tenant_id = 1 AND product_code = 'INT-85' AND deleted = 0;

-- tenant_id=1 product_086 -> INT-86
-- confirmed_name_cn=一次性使用支撑导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '一次性使用支撑导管', r.name_en = 'Support Catheter' WHERE p.tenant_id = 1 AND p.product_code = 'INT-86' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_086' WHERE tenant_id = 1 AND product_code = 'INT-86' AND deleted = 0;

-- tenant_id=1 product_095 -> INT-95
-- confirmed_name_cn=可吸收止血流体明胶
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '可吸收止血流体明胶', r.name_en = 'Absorbable Haemostatic Matrix' WHERE p.tenant_id = 1 AND p.product_code = 'INT-95' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_095' WHERE tenant_id = 1 AND product_code = 'INT-95' AND deleted = 0;

-- tenant_id=1 product_096 -> INT-96
-- confirmed_name_cn=颅内血栓抽吸导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '颅内血栓抽吸导管', r.name_en = 'Aspiration Catheter' WHERE p.tenant_id = 1 AND p.product_code = 'INT-96' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_096' WHERE tenant_id = 1 AND product_code = 'INT-96' AND deleted = 0;

-- tenant_id=1 product_128 -> INT-128
-- confirmed_name_cn=魔芋水凝胶微球
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '魔芋水凝胶微球', r.name_en = 'Konjac Hydrogel Microspheres' WHERE p.tenant_id = 1 AND p.product_code = 'INT-128' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_128' WHERE tenant_id = 1 AND product_code = 'INT-128' AND deleted = 0;

-- tenant_id=1 product_149 -> INT-83
-- confirmed_name_cn=可调弯导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '可调弯导管', r.name_en = 'Steerable Catheter' WHERE p.tenant_id = 1 AND p.product_code = 'INT-83' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_149' WHERE tenant_id = 1 AND product_code = 'INT-83' AND deleted = 0;

-- tenant_id=122 product_001 -> INT-1
-- confirmed_name_cn=一次性使用三通旋塞
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '一次性使用三通旋塞', r.name_en = 'Manifold' WHERE p.tenant_id = 122 AND p.product_code = 'INT-1' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_001' WHERE tenant_id = 122 AND product_code = 'INT-1' AND deleted = 0;

-- tenant_id=122 product_015 -> INT-15
-- confirmed_name_cn=按压式球囊扩充压力泵
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '按压式球囊扩充压力泵', r.name_en = 'Inflation Device II' WHERE p.tenant_id = 122 AND p.product_code = 'INT-15' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_015' WHERE tenant_id = 122 AND product_code = 'INT-15' AND deleted = 0;

-- tenant_id=122 product_049 -> INT-49
-- confirmed_name_cn=经导管主动脉瓣膜输送系统
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '经导管主动脉瓣膜输送系统', r.name_en = 'Transcatheter Aortic Valve Delivery System' WHERE p.tenant_id = 122 AND p.product_code = 'INT-49' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_049' WHERE tenant_id = 122 AND product_code = 'INT-49' AND deleted = 0;

-- tenant_id=122 product_066 -> INT-67
-- confirmed_name_cn=血栓抽吸导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '血栓抽吸导管', r.name_en = 'Neural Aspiration Catheter' WHERE p.tenant_id = 122 AND p.product_code = 'INT-67' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_066' WHERE tenant_id = 122 AND product_code = 'INT-67' AND deleted = 0;

-- tenant_id=122 product_080 -> INT-80
-- confirmed_name_cn=可降解耳鼻止血绵
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '可降解耳鼻止血绵', r.name_en = 'Biodegradable  Nasal & Ear Hemostatic Sponge' WHERE p.tenant_id = 122 AND p.product_code = 'INT-80' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_080' WHERE tenant_id = 122 AND product_code = 'INT-80' AND deleted = 0;

-- tenant_id=122 product_085 -> INT-85
-- confirmed_name_cn=球囊导引导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '球囊导引导管', r.name_en = 'Balloon Guide Catheter' WHERE p.tenant_id = 122 AND p.product_code = 'INT-85' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_085' WHERE tenant_id = 122 AND product_code = 'INT-85' AND deleted = 0;

-- tenant_id=122 product_086 -> INT-86
-- confirmed_name_cn=一次性使用支撑导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '一次性使用支撑导管', r.name_en = 'Support Catheter' WHERE p.tenant_id = 122 AND p.product_code = 'INT-86' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_086' WHERE tenant_id = 122 AND product_code = 'INT-86' AND deleted = 0;

-- tenant_id=122 product_095 -> INT-95
-- confirmed_name_cn=可吸收止血流体明胶
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '可吸收止血流体明胶', r.name_en = 'Absorbable Haemostatic Matrix' WHERE p.tenant_id = 122 AND p.product_code = 'INT-95' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_095' WHERE tenant_id = 122 AND product_code = 'INT-95' AND deleted = 0;

-- tenant_id=122 product_096 -> INT-96
-- confirmed_name_cn=颅内血栓抽吸导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '颅内血栓抽吸导管', r.name_en = 'Aspiration Catheter' WHERE p.tenant_id = 122 AND p.product_code = 'INT-96' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_096' WHERE tenant_id = 122 AND product_code = 'INT-96' AND deleted = 0;

-- tenant_id=122 product_128 -> INT-128
-- confirmed_name_cn=魔芋水凝胶微球
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '魔芋水凝胶微球', r.name_en = 'Konjac Hydrogel Microspheres' WHERE p.tenant_id = 122 AND p.product_code = 'INT-128' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_128' WHERE tenant_id = 122 AND product_code = 'INT-128' AND deleted = 0;

-- tenant_id=122 product_149 -> INT-83
-- confirmed_name_cn=可调弯导管
-- REVIEW ONLY: UPDATE showroom_product_revision r JOIN showroom_product p ON p.current_revision_id = r.id SET r.name_cn = '可调弯导管', r.name_en = 'Steerable Catheter' WHERE p.tenant_id = 122 AND p.product_code = 'INT-83' AND p.deleted = 0 AND r.deleted = 0;
-- REVIEW ONLY: UPDATE showroom_product SET legacy_product_code = 'product_149' WHERE tenant_id = 122 AND product_code = 'INT-83' AND deleted = 0;
-- REVIEW ONLY: COMMIT;
