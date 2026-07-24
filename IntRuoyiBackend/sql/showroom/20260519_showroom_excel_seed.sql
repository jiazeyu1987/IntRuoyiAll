-- Showroom Excel seed generated from 展厅产品与描述清单.xlsx
-- Source workbook: D:/ProjectPackage/Int/IntRuoyi/resource/展厅产品与描述清单.xlsx
-- Hall count: 8
-- Product count: 166
-- Non-empty company count: 15
-- Empty description products: product_166:一次性使用射频房间隔穿刺针, product_049:经导管主动脉瓣膜输送系统

DROP PROCEDURE IF EXISTS init_showroom_excel_seed;
DELIMITER //
CREATE PROCEDURE init_showroom_excel_seed()
BEGIN
  DECLARE v_live_products BIGINT DEFAULT 0;
  DECLARE v_revision_advanced BIGINT DEFAULT 0;
  DECLARE v_company_revision_rows BIGINT DEFAULT 0;
  DECLARE v_relation_rows BIGINT DEFAULT 0;
  DECLARE v_change_request_rows BIGINT DEFAULT 0;
  DECLARE v_change_request_item_rows BIGINT DEFAULT 0;
  DECLARE v_assignment_rows BIGINT DEFAULT 0;
  DECLARE v_comment_rows BIGINT DEFAULT 0;
  DECLARE v_narration_rows BIGINT DEFAULT 0;
  DECLARE v_preview_rows BIGINT DEFAULT 0;
  DECLARE v_company_diff BIGINT DEFAULT 0;
  DECLARE v_company_rows BIGINT DEFAULT 0;
  DECLARE v_product_diff BIGINT DEFAULT 0;
  DECLARE v_product_rows BIGINT DEFAULT 0;
  DECLARE v_product_revision_diff BIGINT DEFAULT 0;
  DECLARE v_product_revision_rows BIGINT DEFAULT 0;
  DECLARE v_hall_diff BIGINT DEFAULT 0;
  DECLARE v_hall_rows BIGINT DEFAULT 0;
  DECLARE v_hall_product_diff BIGINT DEFAULT 0;
  DECLARE v_hall_product_rows BIGINT DEFAULT 0;
  DECLARE v_seed_tenant_id BIGINT DEFAULT 1;

  CREATE TEMPORARY TABLE tmp_showroom_seed_company LIKE showroom_company;
  CREATE TEMPORARY TABLE tmp_showroom_seed_product LIKE showroom_product;
  CREATE TEMPORARY TABLE tmp_showroom_seed_product_revision LIKE showroom_product_revision;
  CREATE TEMPORARY TABLE tmp_showroom_seed_hall LIKE showroom_hall;
  CREATE TEMPORARY TABLE tmp_showroom_seed_hall_product LIKE showroom_hall_product;

INSERT INTO tmp_showroom_seed_company (id, company_code, display_name, company_type, current_revision_id, current_revision_no, incomplete_flag, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (1, 'OWNER_2A5083D8C10E', '瑛泰', 'MAIN', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2, 'OWNER_FE9838DF977E', '德诺电生理', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (3, 'OWNER_B0CBF18CB762', '翰凌', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (4, 'OWNER_129B59977EFB', '唯强', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (5, 'OWNER_32EE7B9F6214', '堃博', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (6, 'OWNER_3B84428F5B72', '七木', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (7, 'OWNER_B940F20BF2E7', '璞慧', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (8, 'OWNER_479445106B59', '瑛泰生物', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (9, 'OWNER_2022FD9C695C', '璞润', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (10, 'OWNER_02F5E0272ACB', '德瑞', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (11, 'OWNER_4EA3FF99654F', '璞跃', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (12, 'OWNER_D725F228A535', '山东瑛泰', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (13, 'OWNER_C27043E2E72E', '吉尔邦', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (14, 'OWNER_3413447A4102', '璞康', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (15, 'OWNER_4C65B7CD7D08', '自动化', 'SUBSIDIARY', NULL, 0, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0);

INSERT INTO tmp_showroom_seed_hall (id, hall_code, name, name_en, description, description_en, display_order, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (1, 'hall_01', '心内介植入展柜', 'Cardiac Intervention Implant Showcase', '集中展示心内介植入相关产品，覆盖冠脉介入、通路建立及术中辅助器械。', 'Presents cardiac interventional implant products, covering coronary intervention, access establishment, and procedure-support devices.', 1, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2, 'hall_02', '心脏植入展柜', 'Cardiac Implant Showcase', '集中展示心脏植入及相关介入产品，覆盖结构性心脏病、心脏通路及配套器械。', 'Presents cardiac implant and related interventional products, covering structural heart disease, cardiac access, and supporting devices.', 2, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (3, 'hall_03', '外周介植入展柜', 'Peripheral Intervention Implant Showcase', '集中展示外周介植入相关产品，覆盖主动脉、外周血管及分支血管治疗器械。', 'Presents peripheral interventional implant products, covering aortic, peripheral vascular, and branch-vessel treatment devices.', 3, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (4, 'hall_04', '神经介植入展柜', 'Neuro Intervention Implant Showcase', '集中展示神经介植入相关产品，覆盖颅内血管通路、取栓、输送及支撑器械。', 'Presents neuro interventional implant products, covering intracranial vascular access, thrombectomy, delivery, and support devices.', 4, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (5, 'hall_05', '外泌体与超声聚焦展柜', 'Exosome and Focused Ultrasound Showcase', '集中展示外泌体应用与聚焦超声相关产品，覆盖无创透皮、能量治疗及配套解决方案。', 'Presents exosome application and focused ultrasound products, covering non-invasive transdermal delivery, energy therapy, and supporting solutions.', 5, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (6, 'hall_06', '骨科与泌尿产品展柜', 'Orthopedics and Urology Products Showcase', '集中展示骨科与泌尿方向产品，覆盖关节介入、骨科手术及泌尿治疗相关器械。', 'Presents orthopedics and urology products, covering joint intervention, orthopedic procedures, and urological treatment devices.', 6, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (7, 'hall_07', '非介入类产品展柜', 'Non-interventional Products Showcase', '集中展示非介入类医疗与健康产品，覆盖材料、消费医疗及配套健康管理方案。', 'Presents non-interventional medical and health products, covering materials, consumer medical products, and supporting health-management solutions.', 7, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (8, 'hall_08', '医疗标准件展柜', 'Medical Standard Components Showcase', '集中展示医疗器械标准件与基础组件，覆盖导管、连接件、耗材组件及制造配套。', 'Presents standard medical device components and foundational parts, covering catheters, connectors, consumable components, and manufacturing support.', 8, 'ACTIVE', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0);

INSERT INTO tmp_showroom_seed_product (id, product_code, current_revision_id, current_revision_no, incomplete_flag, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (1, 'product_001', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2, 'product_002', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (3, 'product_003', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (4, 'product_004', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (5, 'product_005', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (6, 'product_006', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (7, 'product_007', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (8, 'product_008', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (9, 'product_009', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (10, 'product_010', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (11, 'product_011', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (12, 'product_012', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (13, 'product_013', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (14, 'product_014', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (15, 'product_015', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (16, 'product_016', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (17, 'product_017', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (18, 'product_018', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (19, 'product_019', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (20, 'product_020', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (21, 'product_021', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (22, 'product_022', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (23, 'product_023', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (24, 'product_024', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (25, 'product_025', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (26, 'product_166', NULL, 1, b'1', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (27, 'product_026', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (28, 'product_027', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (29, 'product_028', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (30, 'product_029', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (31, 'product_030', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (32, 'product_031', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (33, 'product_032', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (34, 'product_033', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (35, 'product_034', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (36, 'product_035', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (37, 'product_036', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (38, 'product_037', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (39, 'product_038', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (40, 'product_039', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (41, 'product_040', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (42, 'product_041', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (43, 'product_042', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (44, 'product_043', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (45, 'product_044', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (46, 'product_045', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (47, 'product_046', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (48, 'product_047', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (49, 'product_048', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (50, 'product_049', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (51, 'product_050', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (52, 'product_051', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (53, 'product_052', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (54, 'product_053', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (55, 'product_054', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (56, 'product_055', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (57, 'product_056', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (58, 'product_057', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (59, 'product_058', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (60, 'product_059', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (61, 'product_060', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (62, 'product_061', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (63, 'product_062', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (64, 'product_063', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (65, 'product_064', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (66, 'product_065', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (67, 'product_066', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (68, 'product_067', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (69, 'product_068', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (70, 'product_069', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (71, 'product_070', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (72, 'product_071', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (73, 'product_072', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (74, 'product_073', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (75, 'product_074', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (76, 'product_075', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (77, 'product_076', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (78, 'product_077', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (79, 'product_078', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (80, 'product_079', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (81, 'product_080', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (82, 'product_081', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (83, 'product_082', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (84, 'product_083', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (85, 'product_084', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (86, 'product_085', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (87, 'product_086', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (88, 'product_087', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (89, 'product_088', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (90, 'product_089', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (91, 'product_090', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (92, 'product_091', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (93, 'product_092', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (94, 'product_093', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (95, 'product_094', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (96, 'product_095', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (97, 'product_096', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (98, 'product_097', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (99, 'product_098', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (100, 'product_099', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (101, 'product_100', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (102, 'product_101', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (103, 'product_102', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (104, 'product_103', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (105, 'product_104', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (106, 'product_105', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (107, 'product_106', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (108, 'product_107', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (109, 'product_108', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (110, 'product_109', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (111, 'product_110', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (112, 'product_111', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (113, 'product_112', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (114, 'product_113', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (115, 'product_114', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (116, 'product_115', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (117, 'product_116', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (118, 'product_117', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (119, 'product_118', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (120, 'product_119', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (121, 'product_120', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (122, 'product_121', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (123, 'product_122', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (124, 'product_123', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (125, 'product_124', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (126, 'product_125', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (127, 'product_126', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (128, 'product_127', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (129, 'product_128', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (130, 'product_129', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (131, 'product_130', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (132, 'product_131', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (133, 'product_132', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (134, 'product_133', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (135, 'product_134', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (136, 'product_135', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (137, 'product_136', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (138, 'product_137', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (139, 'product_138', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (140, 'product_139', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (141, 'product_140', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (142, 'product_141', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (143, 'product_142', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (144, 'product_143', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (145, 'product_144', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (146, 'product_145', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (147, 'product_146', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (148, 'product_147', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (149, 'product_148', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (150, 'product_149', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (151, 'product_150', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (152, 'product_151', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (153, 'product_152', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (154, 'product_153', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (155, 'product_154', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (156, 'product_155', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (157, 'product_156', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (158, 'product_157', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (159, 'product_158', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (160, 'product_159', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (161, 'product_160', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (162, 'product_161', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (163, 'product_162', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (164, 'product_163', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (165, 'product_164', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (166, 'product_165', NULL, 1, b'0', 'DRAFT_ONLY', 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0);

INSERT INTO tmp_showroom_seed_product_revision (id, product_id, revision_no, status, name_cn, name_en, owner_company_id, product_owner_type, lifecycle_stage, target_market, pipeline_layout, registration_certificate, indication_content, core_selling_points, model_specification, clinical_effect, fim_status, submitted_by, approved_by, published_at, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (1001, 1, 1, 'DRAFT', '三通旋塞-OFF', 'Manifold for Single use-OFF', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用三通旋塞
注册证号：沪械注准20242030122
生效时间：2024.5.30', '用于介入手术中压力监测管路中的连接、输液和通路切换。
ON-阀门开关所指方向开放', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1002, 2, 1, 'DRAFT', '三通旋塞-ON', 'Manifold for Single use-ON', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用三通旋塞
注册证号：沪械注准20242030122
生效时间：2024.5.30', '用于介入手术中压力监测管路中的连接、输液和通路切换。
OFF-阀门开关所指方向关闭', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1003, 3, 1, 'DRAFT', 'Y型连接器', 'Stanch™ Y Connector Pack', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：Y型连接器
注册证号：沪械注准20242030313
生效时间：2025.1.14', '产品用于PTCA手术中，在体外辅助建立导引钢丝进入人体的工作通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1004, 4, 1, 'DRAFT', '按压式Y型连接器', 'Stanch™ Y Connector Pack', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：按压式Y型连接器
注册证号：沪械注准20222030087
生效时间：2022.7.25', '该产品主要用于PTCA手术中，在体外辅助建立导引钢丝进入人体的工作通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1005, 5, 1, 'DRAFT', 'Y型连接阀套件', 'Stanch™ Y Connector Pack', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：Y型连接阀套件
注册证号：沪械注准20232030324
生效时间：2023.11.24', '该产品是经皮血管成形术中的辅助器械，用于连接管路，在体外建立通道辅助器械进入人体，同时可减少血液流出。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1006, 6, 1, 'DRAFT', '穿刺针', 'Seldinger Needle', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：介入手术器械
注册证号：国械注准20193031843
生效时间：2024.5.8', '适用于对人体进行经皮穿刺。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1007, 7, 1, 'DRAFT', '一次性使用血管鞘', 'Introducer', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用血管鞘
注册证号：国械注准20213030647
生效时间：2021.8.18', '用于介入手术中，将导丝、导管等医疗器械插入血管，另外还可通过其侧臂延长管进行输液、测压、给药等。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1008, 8, 1, 'DRAFT', '一次性使用导管鞘套装', 'Introducer Set', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用导管鞘套装
注册证号：沪械注准20242030270
生效时间：2024.8.20', '主要用于介入手术中扩大经皮切口，建立导管导入血管的通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1009, 9, 1, 'DRAFT', '一次性使用亲水涂层导管鞘套装', 'Introducer Set', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用亲水涂层导管鞘
注册证号：国械注准20203031014
生效时间：2020.12.31', '该产品用于介入手术中扩大桡动脉经皮切口，建立导管导入血管的通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1010, 10, 1, 'DRAFT', '股动脉鞘套装', 'Femoral Introducer set', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用导管鞘套装
注册证号：国械注准20213030647
生效时间：2021.8.18', '用于股动脉介入手术，将导丝、导管等医疗器械插入血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1011, 11, 1, 'DRAFT', '桡动脉鞘套装', 'Transradial introducer Set', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用导管鞘套装
注册证号：国械注准20213030647
生效时间：2021.8.18', '用于桡动脉介入手术，将导丝、导管等医疗器械插入血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1012, 12, 1, 'DRAFT', '球囊扩张压力泵', 'ForceFlator™ Inflation Device', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：球囊扩张压力泵
注册证号：沪械注准20212030373
生效时间：2021.6.19', '用于PTCA手术中，向球囊扩张导管加压，从而使球囊扩张，以达到扩张血管或在血管内留置支架的目的。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1013, 13, 1, 'DRAFT', '数显球囊扩张压力泵', 'Digiflator®Digital Inflation Device', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：数显球囊扩张压力泵
注册证号：沪械注准20232030021
生效时间：2023.2.6', '该产品适用于在介入手术中向球囊扩张导管加压、泄压，使球囊扩张和回缩，监测并控制球囊的压力。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1014, 14, 1, 'DRAFT', '按压式球囊扩张压力泵', 'FastFlator™ Inflation Device', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：按压式球囊扩张压力泵
注册证号：沪械注准20212030374
生效时间：2021.6.19', '用于PTCA手术中，向球囊扩张导管加压，从而使球囊扩张，以达到扩张血管或在血管内留置支架的目的。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1015, 15, 1, 'DRAFT', '按压式球囊扩张压力泵', 'Inflation Device II', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：按压式球囊扩充压力泵
注册证号：沪械注准20232030005
生效时间：2023.1.13', '该产品适用于在介入手术中向球囊扩张导管加压、泄压，使球囊扩张和回缩，监测并控制球囊的压力。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1016, 16, 1, 'DRAFT', '造影剂推入器', 'Angiography Syringe', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用造影剂推入器
注册证号：国械注准20153030120
生效时间：2024.5.30', '用于血管造影中承载、推注造影剂，产品通过与三通旋塞连接，推动芯杆可将造影剂推入人体血管或造影导管中进行造影显示。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1017, 17, 1, 'DRAFT', '造影剂推入器-C', 'PC Syringe', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用造影剂推入器
注册证号：国械注准20153030120
生效时间：2024.5.30', '用于需要精准控制造影剂流速、剂量及压力的影像引导手术，确保病变显影清晰且操作安全。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1018, 18, 1, 'DRAFT', '股动脉止血带', 'Femoral Pressure Bandage', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：股动脉压迫止血带
注册证号：沪械注准20212140484
生效时间：2021.8.24', '适用于经股动脉介入手术后动脉穿刺点体外压迫闭合止血。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1019, 19, 1, 'DRAFT', '股动脉止血带', 'Femoral Pressure Bandage', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：气囊式股动脉止血带
注册证号：沪械注准20212140395
生效时间：2021.6.29', '本产品适用于经股动脉介入手术后动脉穿刺点体外压迫闭合止血。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1020, 20, 1, 'DRAFT', '气囊式止血带II', 'Transradial Pressure Bandage II', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：气囊式止血带
注册证号：沪械注准20202140417
生效时间：2020.8.31', '用于桡动脉导管插管术后压迫止血用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1021, 21, 1, 'DRAFT', '桡动脉止血带I', 'Transradial Pressure Bandage I', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用动脉压迫止血带
注册证号：沪械注准20202140509
生效时间：2020.10.27', '在动静脉穿刺手术中拔除穿刺针或留置针后辅助压迫止血用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1022, 22, 1, 'DRAFT', '压力传感器', 'Extension Transducer', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用有创压力传感器
注册证号：国械注准20173073316
生效时间：2022.8.29', '该产品采用有创方式测量患者的动脉压和静脉压，供有资质的专业医护人员在手术室或住院病房中使用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1023, 23, 1, 'DRAFT', '有创压力传感器', 'Pressure Transducer', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用有创压力传感器
注册证号：国械注准20173073316
生效时间：2022.8.29', '该产品采用有创方式测量患者的动脉压和静脉压，供有资质的专业医护人员在手术室或住院病房中使用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1024, 24, 1, 'DRAFT', '高压延长管', 'High Pressure Tubing', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用高压延长管
注册证号：国械注准20223030034
生效时间：2022.1.12', '适用于血管造影手术中作为推注造影剂的通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1025, 25, 1, 'DRAFT', '介入手术器械包', 'Manifold Kit', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：介入手术器械
注册证号：国械注准20193031843
生效时间：2024.5.8', '主要用于心脏造影、经皮冠状动脉成形术等将导管导入血管的介入手术。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1026, 26, 1, 'DRAFT', '一次性使用射频房间隔穿刺针', '', NULL, NULL, 'R_AND_D', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1027, 27, 1, 'DRAFT', '造影导管', 'Angiography Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用造影导管
注册证号：国械注准20163032106
生效时间：2021.6.24', '用于注射或输入对照介质和/或液体，用于血管造影检查，神经血管应用除外。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1028, 28, 1, 'DRAFT', '亲水涂层造影导管', 'Hydrophilic Angiography Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：亲水涂层造影导管
注册证号：国械注准20213030369
生效时间：2021.5.25', '用于注射或输入对照介质和/或液体，用于血管造影检查，神经血管应用除外。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1029, 29, 1, 'DRAFT', '亲水涂层血管造影导管', 'Hydrophilic Angiography Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：亲水涂层血管造影导管
注册证号：国械注准20233030849
生效时间：2023.6.20', '用于注射或输入对照介质和/或液体，用于血管造影检查。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1030, 30, 1, 'DRAFT', '指引导管', 'VAS PASS® Guiding Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用指引导管
注册证号：国械注准20193030955
生效时间：2025.1.20', '用于在冠状动脉介入手术中为介入器械和诊断器械的导入提供通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1031, 31, 1, 'DRAFT', '导引导管', 'VAS PASS® Guiding Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：导引导管
注册证号：国械注准20243030311
生效时间：2024.2.21', '用于以介入治疗方式进入血管系统，为介入治疗建立通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1032, 32, 1, 'DRAFT', '亲水涂层导引导管', 'Hydrophilic  Guiding Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：亲水涂层导引导管
注册证号：国械注准20243030310
生效时间：2024.2.21', '用于以介入治疗方式进入血管系统，为介入治疗建立通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1033, 33, 1, 'DRAFT', 'PTFE导丝', 'PTFE Guide Wire', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用造影导丝
注册证号：国械注准20163032107
生效时间：2021.5.17', '用于血管造影，目的是建立了一个从穿刺部位到病变部位或通过病变部位到达远端的通道，辅助其他器械进行定位操作。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1034, 34, 1, 'DRAFT', 'PTCA球囊扩张导管', 'PTCA Balloon Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：PTCA球囊扩张导管
注册证号：国械注准20193030239
生效时间：2024.4.29', '用于冠状动脉血管狭窄或血管内支架内扩张治疗。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1035, 35, 1, 'DRAFT', '指引导丝', 'Guidewire 0.014’', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用指引导丝
注册证号：沪械注准20242030327
生效时间：2024.9.19', '适用于在经皮冠状动脉成形术（PTCA）和经皮血管成形术（PTA）中，引导球囊导管或支架系统送达病变部位。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1036, 36, 1, 'DRAFT', '多环测量灌注导管', 'Waterfull™ Infusion Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：多环测量灌注导管
注册证号：国械注准20213030436
生效时间：2021.6.15', '本产品用于介入手术中，将诊断介质（造影剂）或治疗溶剂（溶栓溶液）递送至血管内，神经血管应用除外。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1037, 37, 1, 'DRAFT', '冠脉微导管', 'RAD PASS Micro Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用微导管
注册证号：国械注准20193030352
生效时间：2024.5.31', '一次性使用微导管用于注射或输入对照介质和/或液体(如造影剂)和/或栓塞材料，和/或适当的器械(如支架、弹簧圈),神经血管应用除外。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1038, 38, 1, 'DRAFT', '超滑导丝', 'Hydrophilic Guide Wire', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用亲水涂层导丝
注册证号：国械注准20173033094
生效时间：2022.2.24', '用于引导导管插入血管并辅助其他器械进行定位操作，神经血管应用除外；长度为45cm的导丝用于Seldinger术，用于建立有助于血管内器械的经皮进入通路，不具有血管内定位或建立血管内通路作用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1039, 39, 1, 'DRAFT', '依维莫司冠脉乳突球囊扩张导管', 'Everolimus Coronary  Mastoid Balloon', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '适用于冠状动脉支架内再狭窄病变。
适用于血管直径≥2.0mm且≤2.75mm的冠状动脉原发小血管病变治疗。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1040, 40, 1, 'DRAFT', '指引延长导管', 'Prolong™ Guide Extension Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：指引延长导管
注册证号：国械注准20233030388
生效时间：2023.3.23', '与导引导管结合使用，可进入冠状脉管系统和/或外周脉管系统的病变区域，并可辅助放置介入器械。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1041, 41, 1, 'DRAFT', '棘突球囊', 'Coronary Dilatation Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：冠状动脉棘突球囊扩张导管
注册证号：国械注准20243032641
生效时间：2024.12.26', '该类产品在临床上通常定位于血管病变的预处理，具体适用的情况包括：1.支架内再狭窄病变；2.开口病变；3.分叉病变； 4.轻中度钙化病变或重度钙化病变经过旋磨等预处理后；5. 纤维性病变等常规球囊处理效果不佳的，经腔内影像确认后，使用常规球囊扩张试过仍然效果不佳的。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1042, 42, 1, 'DRAFT', '左心耳封堵器系统', 'SeaLA® LAA Closure System', 2, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：左心耳封堵器系统
注册证号：国械注准20223131498
生效时间：2022.11.9', '适用于有卒中风险（CHA2DS2-VASc评分≥2分）且长期口服抗凝治疗禁忌的非瓣膜性房颤患者。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1043, 43, 1, 'DRAFT', '一次性使用心脏脉冲电场消融导管', 'Disposable Cardiac PFA Catheter', 2, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用心脏脉冲电场消融导管
注册证号：国械注准20243010461
生效时间：2024.3.8', '该产品与本公司生产的心脏脉冲电场消融仪（型号：CP-GR，软件发布版本1）、可调弯导管鞘配合使用，用于18岁或以上患者药物难治性、复发性、症状性、阵发性房颤的治疗。与心脏多道电生理记录仪配合使用，用于18岁或以上患者的心脏电生理标测（刺激和记录）。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1044, 44, 1, 'DRAFT', '可调弯导管鞘', 'Steerable Sheath System', 2, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：可调弯导管鞘
注册证号：浙械注准20232031690
生效时间：2023.10.10', '与扩张器配合使用，用于将导丝、导管等医疗器械插入血管。产品不进入冠状动脉与神经血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1045, 45, 1, 'DRAFT', '血管鞘', 'Introducer Sheath', 2, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：血管鞘
注册证号：浙械注准20232031819
生效时间：2024.2.5', '用于建立有助于血管内介入器械的经皮进入通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1046, 46, 1, 'DRAFT', '心脏瓣膜球囊扩张导管', 'Cardiac Valves Balloon Dilatation Catheter', 3, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：心脏瓣膜球囊扩张导管
注册证号：国械注准20223031192
生效时间：2022.9.9', '心脏瓣膜球囊扩张导管适用于经导管主动脉瓣置换术中的主动脉瓣的预扩张和后扩张。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1047, 47, 1, 'DRAFT', '可扩张血管鞘套装', 'Expandble sheath introducer set', 3, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可扩张血管鞘套装用于建立血管介入手术通路。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1048, 48, 1, 'DRAFT', '心脏瓣膜支架Hanchor Valve', 'Cardiac Valve Stent Hanchor Valve', 3, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '经导管主动脉瓣膜系统用于经导管主动脉瓣置入术（TAVR）中，用以治疗主动脉瓣膜病变，产品适用范围为不适合进行外科手术的主动脉瓣关闭不全患者。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1049, 49, 1, 'DRAFT', '新型聚合物瓣膜', 'Novel Poiymeric Heart Valve', 3, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '新型聚合物瓣膜用于心脏瓣膜置换术中植入到人体，代替自体瓣膜工作，恢复病变自体瓣膜处的血流，聚合物瓣膜具有出色的抗钙化性能，瓣膜使用寿命更长，同时可大幅降低生产成本。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1050, 50, 1, 'DRAFT', '经导管主动脉瓣膜输送系统', 'Transcatheter Aortic Valve Delivery System', NULL, NULL, 'R_AND_D', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1051, 51, 1, 'DRAFT', '可降解镁合金支架', 'Bio-degradable Magnesium Alloy Stent', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '该产品用于经皮冠状动脉介入术治疗原发冠状动脉粥样硬化患者的血管内狭窄，改善患者的冠状动脉血流并预防再狭窄的发生。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1052, 52, 1, 'DRAFT', '自主冶炼镁合金管材', 'Magnesium Alloy Tube', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '自主冶炼无稀土高强度镁合金棒材，抗拉强度可达350MPa，拉伸屈服强度达300MPa，断裂延伸率在20%左右。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1053, 53, 1, 'DRAFT', '自主冶炼镁合金管棒材', 'Magnesium Alloy Rod', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '自主冶炼无稀土高强度镁合金棒材，抗拉强度可达350MPa，拉伸屈服强度达300MPa，断裂延伸率在20%左右。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1054, 54, 1, 'DRAFT', '人工血管', 'Artificial blood vessel', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '人工血管用于主动脉及其分支血管的置换或旁路手术。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1055, 55, 1, 'DRAFT', '胸主动脉覆膜支架系统', 'Fabulous Thoracic Aortic Stent System', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：胸主动脉支架系统
注册证号：国械注准20223130685
生效时间：2022.5.23', '胸主动脉支架系统适用于治疗Stanford B型夹层，支架近端锚定区长度≥15mm，且病变符合以下条件之一：1. 存在远端破口，有处理远端病变的必要性；2. 夹层累及范围较广，且存在远端真腔塌陷；3. 夹层合并远端分支血管动态梗阻型灌注不良。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1056, 56, 1, 'DRAFT', 'Grency髂静脉支架系统', 'Iliac Vein Stent System', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：髂静脉支架系统
注册证号：国械注准20243131244
生效时间：2024.7.11', '该产品预期在髂总静脉内使用，用于治疗非血栓性髂静脉压迫综合征。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1057, 57, 1, 'DRAFT', '分体式分支型胸主动脉覆膜支架系统', 'WeFlow-Tbranch', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：分体式分支型胸主动脉覆膜支架系统
注册证号：国械注准20243130650
生效时间：2024.3.29', '该产品适用治疗近左锁骨下动脉的需要重建左锁骨下动脉血运的Stanford B型夹层患者。其近端锚定区长度应≥15mm；左锁骨下动脉距离左颈总动脉≥5mm。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1058, 58, 1, 'DRAFT', '胸主动脉裸支架', 'Thoracic Aortic Stent', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：胸主动脉支架系统
注册证号：国械注准20223130685
生效时间：2022.5.23', '胸主动脉支架系统适用于治疗Stanford B型夹层，支架近端锚定区长度≥15mm，且病变符合以下条件之一：1. 存在远端破口，有处理远端病变的必要性；2. 夹层累及范围较广，且存在远端真腔塌陷；3. 夹层合并远端分支血管动态梗阻型灌注不良。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1059, 59, 1, 'DRAFT', '模块内嵌双分支覆膜支架系统', 'Modular Double-Branched Aortic Stent Graft System', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '该产品预期用于主动脉弓部病变的治疗，包括累及升主动脉的真性主动脉弓动脉瘤、假性主动脉弓动脉瘤及累及主动脉弓部的溃疡，是全球首款设计针对弓上双分支重建的产品，通过升主动脉、分支和弓段三个模块组合，实现腔内重建头臂干和左颈总动脉，且整个手术过程中脑血流不受影响。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1060, 60, 1, 'DRAFT', 'WeFlow-Tribranch三分支全腔内修复系统', 'Modular Trible-Branched Aortic Stent Graft System', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '国际首创的三分支内嵌式主动脉弓覆膜支架系统，可同时重建头臂干、左颈总动脉和左锁骨下动脉三个分支血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1061, 61, 1, 'DRAFT', 'WeFlow-JAAA复杂腹主动脉覆膜支架系统', 'Stent Graft System for Juxtarenal AAA', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '该产品预期用于复杂腹主动脉病变的治疗，国际首创的非定制"开窗-分支"支架系统，集成标准肾动脉内分支、SMA短内嵌分支和腹腔干U形开槽结构设计，显著提升近肾腹主动脉瘤的治疗效率。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1062, 62, 1, 'DRAFT', 'ZIPPER主动脉弓腹膜支架系统', 'ZIPPER AORTIC ARCH STENT GRAFT', 4, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', 'ZIPPER一体式主动脉弓覆膜支架系统适用于多种主动脉弓部血管病变及复杂解剖结构患者，尤其在需要覆盖并重建弓上全部分支血管的情况下，ZIPPER可提供安全且操作简便的治疗方案。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1063, 63, 1, 'DRAFT', '长鞘', 'Guiding introducer Sheath', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用血管鞘
注册证号：国械注准20213030647
生效时间：2021.8.18', '该产品用于介入手术中，将导丝、导管等医疗器械插入血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1064, 64, 1, 'DRAFT', '按压式球囊扩张压力泵40ml/40atm', 'FastFlator Inflation Device', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：按压式球囊扩充压力泵
注册证号：沪械注准20232030005
生效时间：2023.1.13', '该产品适用于在介入手术中向球囊扩张导管加压、泄压，使球囊扩张和回缩，监测并控制球囊的压力。规格型号为：40ml/40atm', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1065, 65, 1, 'DRAFT', '微导管（外周）', 'RAD CROSS® Micro Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用微导管
注册证号：国械注准20193030352
生效时间：2024.5.31', '用于注射或输入对照介质和/或液体(如造影剂)和/或栓塞材料，和/或适当的器械(如支架、弹簧圈),神经血管应用除外。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1066, 66, 1, 'DRAFT', '超滑导丝', 'Hydrophilic Guide wire', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用亲水涂层导丝
注册证号：国械注准20173033094
生效时间：2022.2.24', '用于引导导管插入血管并辅助其他器械进行定位操作，神经血管应用除外；长度为45cm的导丝用于Seldinger术，用于建立有助于血管内器械的经皮进入通路，不具有血管内定位或建立血管内通路作用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1067, 67, 1, 'DRAFT', '血栓抽吸导管', 'Neural Aspiration Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用血栓抽吸系统
注册证号：国械注准20223030596
生效时间：2022.5.9', '用于清除血管系统的新鲜、柔软栓子和血栓块，神经血管应用除外。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1068, 68, 1, 'DRAFT', '负压抽吸泵', 'Aspiration Pump', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：医用电动吸引器
注册证号：沪械注准20232140055
生效时间：2023.3.19', '用于医疗机构利用负压吸出人体中的液体和/或固体。不适用于流产和胸腔负压吸引。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1069, 69, 1, 'DRAFT', '雾泉®一次性使用内窥镜雾化微导管', 'Mist Fountain
Disposable nebulizing micro-catheter for endoscope', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用内窥镜雾化微导管
注册证号：浙械注准20222021162
生效时间：2024.7.9', '与内窥镜配合，用于灌洗、喷洒药液或配合造影剂使用，不用于血液循环系统和中枢神经系统给物。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1070, 70, 1, 'DRAFT', '智衡®肺部射频消融导管', 'BroncAblate® Disposable Pulmonaryrasiofrequency Ablation Catheter', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用肺部射频消融导管
注册证号：国械注准20253010767
生效时间：2025.4.17', '产品与本公司生产的肺部射频消融系统（型号：BRS-PA-50W）配合，经软性支气管内窥镜（器械通道尺寸≥2.0mm）将射频能量作用于肺部肿瘤进行消融，适用于不能耐受手术切除且不能进行SBRT（立体定向体部放疗）的IA期原发性非小细胞肺癌患者。仅限用于单侧肺部肿瘤病灶数量≤3个，肿瘤病灶大小≤3cm，肿瘤与气管、主支气管、食管、主动脉弓支、主肺动脉、左和右肺动脉及心脏最近距离不小于2cm，经皮消融产品无法到达的肺部肿瘤。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1071, 71, 1, 'DRAFT', 'TLD肺部靶向去神经消融导管', 'TLD Targeted Lung Denervation Ablation Catheter', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用肺部射频消融导管
注册证号：国械注准20253010767
生效时间：2025.4.17', '产品与本公司生产的肺部射频消融系统（型号：BRS-PA-50W）配合，经软性支气管内窥镜（器械通道尺寸≥2.0mm）将射频能量作用于肺部肿瘤进行消融，适用于不能耐受手术切除且不能进行SBRT（立体定向体部放疗）的IA期原发性非小细胞肺癌患者。仅限用于单侧肺部肿瘤病灶数量≤3个，肿瘤病灶大小≤3cm，肿瘤与气管、主支气管、食管、主动脉弓支、主肺动脉、左和右肺动脉及心脏最近距离不小于2cm，经皮消融产品无法到达的肺部肿瘤。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1072, 72, 1, 'DRAFT', 'InterVapor®一次性使用经支气管内窥镜热蒸汽治疗导管', 'InterVapor® Disposable Transbronchoscopic Thermal Vapor Ablation Catheter', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用经支气管内窥镜热蒸汽治疗导管
注册证号：国械注准20233091032
生效时间：2023.7.27', '产品在医疗机构使用，与本公司生产的热蒸汽治疗设备（UM-GEN-100)及Uptake Medical公司生产的治疗规划程序包（UM-IP3-100)配合使用，用于Gold评级3-4级、经充分药物及康复治疗不佳的非均质性（双上肺）肺气肿患者的治疗。使用时需配合最小工作通道直径为2.8mm的支气管内窥镜。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1073, 73, 1, 'DRAFT', '途扩™一次性使用经支气管镜穿刺扩张导管', 'BroncTru™ Disposable Transbronchial Puncturedilation Catheter', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用经支气管镜扩张导管
注册证号：浙械注准20232021787
生效时间：2024.7.19', '产品与支气管镜配合使用，进行穿刺、扩张和建立操作通道，导引其他内镜工具到达肺内目标病灶。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1074, 74, 1, 'DRAFT', 'BioStar®一次性内窥镜吸引活检针', 'BioStar® Ebus Aapiration Needle', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性内窥镜吸引活检针
注册证号：浙械注准20202020594
生效时间：2024.12.30', '一次性内窥镜吸引活检针由手柄、针管、鞘管、导丝、负压装置和连接装置（i系列型号不含单独的连接装置）组成,负压装置包含负压器、负压器配件和鲁尔二通阀。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1075, 75, 1, 'DRAFT', '一次性内窥镜吸引活检针', 'ATV®FleXNeedleCN', 5, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性内窥镜吸引活检针
注册证号：浙械注准20192020448
生效时间：2024.2.5', '产品用于获取支气管树的隆突、气管旁以及肺门病变粘膜下层活检样本。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1076, 76, 1, 'DRAFT', '可降解鼻窦药物支架', 'Biodegradable Drug Eluting Sinus Stent', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：可降解鼻窦药物支架系统
注册证号：国械注准20233130121
生效时间：2023.2.2', '用于慢性鼻窦炎实施功能性内窥镜鼻窦手术（FESS）患者，主要用于防止FESS术后粘连，保持鼻腔通畅，减少炎症。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1077, 77, 1, 'DRAFT', '鼻腔冲洗液', 'Nasal Cleaner', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：鼻腔冲洗液
注册证号：沪械注准20252140138
生效时间：2025.4.2', '用于急慢性鼻炎、过敏性鼻炎、鼻息肉、鼻窦炎鼻腔疾病患者的鼻腔清洗，也用于鼻炎手术后及化疗后的鼻腔清洗。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1078, 78, 1, 'DRAFT', '抗鼻腔过敏凝胶', 'Anti Nasal Allerrgy Gel Dressing', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：抗鼻腔过敏凝胶
注册证号：沪械注准20252140248
生效时间：2025.6.7', '用于过敏性鼻炎患者、过敏性哮喘患者，通过阻隔致病性微生物及其他颗粒性过敏物质进入鼻腔，缓解因过敏性鼻炎、过敏性哮喘引发的相关症状。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1079, 79, 1, 'DRAFT', '咽鼓管球囊扩张导管', 'Eustachian Tube Balloon Dilation Catheter', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：咽鼓管球囊扩张导管
注册证号：沪械注准20232140286
生效时间：2023.10.23', '适用于狭窄的咽鼓管的扩张，以辅助治疗阻塞性咽鼓管功能障碍。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1080, 80, 1, 'DRAFT', '筋膜缝合器', 'Fascial Closure System', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：微创筋膜闭合器
注册证号：沪械注准20222020040
生效时间：2022.3.21', '用于在腹腔镜手术中收拢组织、经皮缝合、以便闭合手术切口。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1081, 81, 1, 'DRAFT', '可吸收耳鼻止血海绵', 'Absorbable Hemostatic Sponge', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '本产品主要用于鼻腔、中耳与外耳术后的暂时压迫止血与支撑。选用几种天然的植物多糖作为原材料，生物安全性高且降解产物无毒性。几种材料相互促进、紧密耦合，最终实现了“1+1 > 2”的高效协同止血效果。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1082, 82, 1, 'DRAFT', '输送导管', 'Delivery Catheter', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：输送导管
注册证号：国械注准20253030578
生效时间：2025.3.14', '适用于介入性器械的导入，协助介入性器械进入外周、冠状和颅内血管系统。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1083, 83, 1, 'DRAFT', '可调弯导管', 'Steerable Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可用于心脏血管、颅内血管及外周血管，经皮穿刺进入血管系统，在介入诊断或治疗手术中为导丝输送建立通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1084, 84, 1, 'DRAFT', '可调弯造影导管', 'Steerable Angiography Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可用于心脏血管、颅内血管及外周血管，用于注射或输入对照介质和/或液体。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1085, 85, 1, 'DRAFT', '神经微导管', 'MicroCatheter（017）', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用微导管
注册证号：国械注准20203030984
生效时间：2021.3.16', '适用于神经血管和外周血管，用于注射或输入对照介质和/或液体和/或栓塞材料, 和/或适当的器械（如支架、弹簧圈）等。
该型号主要用以获得最佳的弹簧圈输送、栓塞、解脱。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1086, 86, 1, 'DRAFT', '带球囊指引导管', 'Guiding Cathter with Balloon', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：球囊导引导管
注册证号：国械注准20223030672
生效时间：2022.5.18', '协助血管内导管插入并被引导至外周或神经血管系统的目标血管内。在进行血管造影和诊断治疗时，该产品还可同时临时封堵血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1087, 87, 1, 'DRAFT', '支撑导管(支持导管)', 'Support Catheter', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用支撑导管
注册证号：国械注准20213030233
生效时间：2021.4.8', '适用于神经血管和外周血管，用于术中经桡动脉或股动脉建立血管通路，辅助诊断或治疗器械进入血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1088, 88, 1, 'DRAFT', '颅内支架系统', 'Intraluminal Support Device', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '适用于配合弹簧圈栓塞术治疗血管瘤的微创手术或治疗血管狭窄。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1089, 89, 1, 'DRAFT', '神经介入手术器械包(治疗)', 'Neurovascular surgical instrument set', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：介入手术器械
注册证号：国械注准20193031843
生效时间：2024.5.8', '在介入手术中用于连结管路和设备，提供药液的输注通路或用于有创血压监测。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1090, 90, 1, 'DRAFT', '神经介入手术器械包(造影)', 'Neurovascular surgical instrument set', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：介入手术器械
注册证号：国械注准20193031843
生效时间：2024.5.8', '在介入手术中用于连结管路和设备，提供药液的输注通路或用于有创血压监测。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1091, 91, 1, 'DRAFT', '微导丝CG', 'Cooperative guide wire', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：微导丝
注册证号：国械注准20243031492
生效时间：2024.8.14', '适用于神经血管和外周血管，辅助诊断或治疗器械顺利到达病变部位。
优异导航性，接近1：1扭矩传导', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1092, 92, 1, 'DRAFT', '微导丝TG', 'Tracking guide wire', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：微导丝
注册证号：国械注准20243031492
生效时间：2024.8.14', '适用于神经血管和外周血管，辅助诊断或治疗器械顺利到达病变部位。
头端塑形及远端保持佳，实现迂曲/远端血管超选', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1093, 93, 1, 'DRAFT', '栓塞保护器', 'Embolic Protection Device', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：栓塞保护器
注册证号：国械注准20243030031
生效时间：2024.1.4', '在颈动脉及外周血管狭窄及堵塞治疗手术，捕获并去除栓塞物质（血栓/碎片），为患者提供血管远端栓塞的保护。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1094, 94, 1, 'DRAFT', '一次性使用血管内微导丝', 'Microguide Wire', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用血管内微导丝
注册证号：国械注准20203031016
生效时间：2020.12.31', '适用于神经血管和外周血管，辅助诊断或治疗器械顺利到达病变部位。
安全无创尖端设计，强支撑，顺应抵达病变位置', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1095, 95, 1, 'DRAFT', '颅内取栓支架', 'Intracranial thrombectomy', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '用于在症状发作8小时内移除缺血性脑卒中患者大血管（包括颈内动脉、大脑中动脉M1和M2段）中的血栓，从而恢复血流。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1096, 96, 1, 'DRAFT', '可吸收流体明胶基质', 'Absorbable Haemostatic Matrix', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '本产品适用于在毛细血管、静脉和细小动脉等出血而依靠压迫、结扎或其他传统方法控制无效时或不可行时的手术辅助止血(眼科手术除外)。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1097, 97, 1, 'DRAFT', '血栓抽吸导管', 'Neural Aspiration Catheter', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：颅内血栓抽吸导管
注册证号：国械注准20233030307
生效时间：2023.3.13', '适用于对颅内大血管阻塞（颈内动脉、大脑中动脉—M1段和M2段、基底动脉和椎动脉内）继发急性缺血性脑中风的患者进行血管再通', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1098, 98, 1, 'DRAFT', '磷酰胆碱涂层密网支架', 'Flow diverter with PC coating', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '适用于该产品用于18岁及以上成人颈内动脉岩骨段以上血管及其分支和椎动脉血管内未破裂的大、巨大型以及中、小型宽颈（瘤颈宽≥4 mm或瘤体/瘤颈比< 2）囊性或梭形动脉瘤，载瘤动脉直径需≥2.25 mm且≤6.25 mm。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1099, 99, 1, 'DRAFT', '无创透皮系统', 'NFTD', 8, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '告别传统针刺注射，引入前沿的无针操作技术。利用冲击波空化技术及毛细管的分散效应，通过微米级精密喷头，释放高速喷射流，无需针头穿刺将精华液雾化至直径小于5μm的微液滴，将活性成分逐层渗透，极大地降低穿刺破皮造成的感染风险，焕发肌肤新生。通过压力将物质（如透明质酸钠溶液，胶原蛋白溶液等）穿透皮肤外层输送到皮内，促进皮肤对物质的吸收。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1100, 100, 1, 'DRAFT', '瑛之秘头皮赋活精华液', 'EXOINT Scalp Revitalizing Essence', 8, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：瑛之秘头皮赋活精华液
注册证号：沪G妆网备字2024024096
生效时间：2024.12.27', '瑛之秘头皮赋活精华液以（动物）脐带提取物为核心，结合咖啡因以促进血液循环，延长毛发生长期，与多肽复合物等成分激活毛囊活力，同时补充生物素，可促进角蛋白合成，增强发丝强度，强化发根支撑力；配合保湿因子舒缓头皮敏感，构建环境损伤防护屏障。从毛囊代谢到头皮微生态，重塑健康头皮环境，助力发丝浓密强韧。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1101, 101, 1, 'DRAFT', '瑛之秘多肽修护液', 'EXOINT Youth Peptides Repair Essence （YPRE)', 8, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：瑛之秘多肽修护液
注册证号：沪G妆网备字2024023523
生效时间：2024.11.27', '瑛之秘多肽修护液蕴含多种核心成分，其中（动物）脐带提取物可以有效增强皮肤增殖活力，协同多肽复合物抑制衰老信号，促进胶原生成，提升肌肤弹性。复配透明质酸钠与多元氨基酸构建深层保湿体系，为肌肤注入持续水润力。轻盈质地快速渗透，从基底改善干燥与松弛等问题，重塑肌肤弹润透亮。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1102, 102, 1, 'DRAFT', '无针透皮组合', 'Needle Free Transdermal set', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '无针透皮组合由喷头、负压壳、缓冲垫、移液器组成，通过负压壳与发射枪连接，形成负压环境，在透皮导入过程中缓冲垫与皮肤接触，保证接触的舒适度，同时保证皮肤与缓冲垫紧密接触，负压壳将负压传递给皮肤，使皮肤被负压吸起与喷头接触紧密，实现透皮导入过程中高效导入，减少渗漏和体验者的痛感，提升整体使用效果。通过压力将物质（如透明质酸钠溶液，胶原蛋白溶液等）穿透皮肤外层输送到皮内，促进皮肤对物质的吸收。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1103, 103, 1, 'DRAFT', '微针', 'Micro Needle', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '本产品与电子注射器配套使用，用于面部真皮层注射透明质酸钠等精华液。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1104, 104, 1, 'DRAFT', '瑛之秘 肌活修护精华霜', 'REJUVENATE RECOVERY ESSENCE CREAM', 8, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：沪G妆网备字2025011577
生效时间：2025-04-26 00:00:00', '即时抗老、持续抗老、抵御衰老——分阶式修护抗老，焕新肌底原生。
即时抗老:芋螺肽+类蛇毒肽+乙酰基六肽-8，多重珍稀小肽，能够阻断神经信号传导路径的不同节点，抑制肌肉收缩达到松弛表情肌、减少皱纹的效果；
持续抗老：30%玻色因+胶原+腺苷，玻色因刺激胶原新生，外源胶原直接补给，腺苷加速细胞代谢协同抗老；
抵御衰老：多重植物精粹+植物愈伤组织+神经酰胺NP+角鲨烷强化屏障，高山火绒草可以修复细胞DNA损伤并强效抗氧化，海茴香提取物可以激活皮肤干细胞促进组织再生，两者协同提升肌肤修护力与年轻态。
适用人群：敏感肌/全肤质/有抗皱需求、有深度静态纹，皮肤松弛的人群可用
肤感质地：厚实绵密的绷带质地推开后化为丝绒哑光，吸收快、不油不黏，肤感如柔焦滤镜般清爽顺滑。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1105, 105, 1, 'DRAFT', '瑛之秘淡纹修护精华液', 'CONCENTRATED ANTI-AGING SKIN SERUM', 8, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：沪G妆网备字2025011449
生效时间：2025-04-24 00:00:00', '专研淡纹修护，塑修肌肤嘭弾纤维。高端植物细胞科技，两大核心植物愈伤组织
高山火绒草+海茴香，唤醒肌肤新生；玻色因+抗皱肽+麦角硫因，抗老三角配方体系，精准淡纹嘭弹；细胞级多效修护，多重植物精粹舒缓保湿，神经酰胺NP+β-葡聚糖，增强肌肤抵御力。
核心功效：修护肌肤屏障，增加肌肤抵御力，同时有效抑制皱纹产生，基底紧致充盈。
适用人群：敏感肌可用/全肤质可用
肤感质地：清透凝露质地', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1106, 106, 1, 'DRAFT', '瑛之秘舒润弹嫩精萃水', 'HYDRASILK SOOTHING NOURISH ESSENCE', 8, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：沪G妆网备字2025011448
生效时间：2025-04-26 00:00:00', '细胞级修护抗老，唤醒肌肤弹嫩新生。醇+极大螺旋藻提取物+ β-葡聚糖，层层保湿，润泽舒缓；高山火绒草+海茴香，两大核心植物愈伤组织，细胞级焕活，强韧屏障；玻色因+腺苷+水解丝胶蛋白，深层抗老，嘭弹紧致。
适用人群：敏感肌可用/全肤质可用
肤感质地：清透精萃水质地', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1107, 107, 1, 'DRAFT', '瑛之秘赋活凝萃精华液', 'REJUVENATE RECOVERY CONCENTRATE SERUM', 8, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：沪G妆网备字2025011717
生效时间：2025-04-29 00:00:00', '急救型修护，快速缓解敏感泛红：通过双重植物细胞精华+九重植物精萃，快速舒缓敏感，协同肌源保湿水盾深层保湿，修复屏障；
主动型抗老，自我修护式抗皱紧致：多链路立体抗垮，精选黄金比重组胶原蛋白，搭配抗皱信号网络构建，协同玻色因促生胶原，多效协同，开启抗垮全模式；
鲜活冻干保鲜，天天享鲜：通过超低温真空冷冻干燥技术，极速锁住成分活性，实现无防腐剂添加下的长效保鲜与稳定功效。
核心成分：脐带精华（外泌体，肌底修护）；玻色因+黄金配比胶原（I+III+XVII型撑起垮脸）；六重靶向肽网（类蛇毒肽闪电抚纹+芋螺肽长效抗衰）。
适用人群：全肤质适用、突发泛红敏肌、抗老需求、熬夜党、医美术后修复人群
肤感质地：水感清透秒吸收，不粘不腻，即刻舒缓', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1108, 108, 1, 'DRAFT', '台车
（聚焦超声无创治疗顽固性高血压系统）', 'RDN', 9, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '聚焦超声无创治疗顽固性高血压系统是利用体外聚焦超声精准消融肾动脉周围的交感神经。全球大多数RDN治疗都是经血管的射频或超声消融，我们的体外无创方案在国际上属于领先技术，既避免介入的风险，又保留疗效；对于药物效果不佳的顽固性高血压患者，这是一种安全、舒适、恢复快的新选择。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1109, 109, 1, 'DRAFT', '关节介入手术器械', 'Joint  Intervention Kit', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：关节镜用腕管松解手术器械
注册证号：沪械注准20202040468
生效时间：2020.9.25', '用于在关节镜下作腕管松解手术。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1110, 110, 1, 'DRAFT', '可视软组织松解器械及组件', 'Visualization of soft tissue laxity instruments and components', 10, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '用于建立、扩张手术通道，对软组织进行切割、剥离。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1111, 111, 1, 'DRAFT', '椎体工具包', 'Spinal Tool Kit', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用椎体工具包
注册证号：沪械注准20212040182
生效时间：2021.3.25', '用于经皮椎体成形术的配套使用工具，在椎体成形术后凸成型术中建立手术通道，并通过该通道进行骨水泥注入或作为取活检的通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1112, 112, 1, 'DRAFT', '骨水泥成型套装', 'Bone Cement Shaping Kit', 10, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：骨水泥颗粒成型套装
注册证号：粤械注准20242040147
生效时间：2024.1.24', '用于膝关节用骨水泥定型，不包含植入体内的组件。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1113, 113, 1, 'DRAFT', '人工骨', 'Artificial Bone', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：β-磷酸三钙人工骨
注册证号：国械注准20243130405
生效时间：2024.3.4', '用于成人骨科手术时四肢50mm以下非承重性骨缺损的填充，必要时与内、外固定产品配合使用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1114, 114, 1, 'DRAFT', '可降解镁合金骨钉', 'Bio-degradable Magnesium Alloy Screw', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '适用于四肢的骨折内固定，3-6个月仍能维持80%以上力学强度。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1115, 115, 1, 'DRAFT', '可降解镁合金骨板', 'Degradable Magnesium Alloy Bone Plate', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '适用于四肢的骨折内固定，配合骨钉使用，主要用于手足部位的皮质骨固定', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1116, 116, 1, 'DRAFT', '骨髓血穿刺抽吸循环器械', 'MSCS Collection Kit', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：骨髓血穿刺抽吸循环器械
注册证号：国械注准20213101079
生效时间：2021.12.21', '利用骨穿针对髂骨穿刺，通过真空抽吸注射器抽取骨髓血后，为骨髓血的处理提供循环通路。开展该产品配合β-磷酸三钙生物陶瓷临床使用的临床研究应符合卫生健康主管部门相关要求，产生的制品应在相关指南规范下使用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1117, 117, 1, 'DRAFT', '椎体扩张球囊导管', 'PKP Balloon', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '该产品适用于椎体成形术及脊柱后凸成形术微创手术中形成通道，恢复椎体高度，形成骨水泥灌注腔的配套使用器械。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1118, 118, 1, 'DRAFT', '骨髓血穿刺抽吸循环动力泵', 'MSCS Collection Power Pump', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：骨髓血穿刺抽吸循环动力泵
注册证号：沪械注准20232100249
生效时间：2023.9.6', '该产品与本公司生产的骨髓血穿刺抽吸循环器械配合使用，为骨髓血在器械中循环提供动力。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1119, 119, 1, 'DRAFT', '一次性使用电子输尿管镜', 'Single-Use Digital Flexible Ureteroscope', 11, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用电子输尿管内窥镜导管
注册证号：沪械注准20232060320
生效时间：2023.11.21', '镜头前端集成温度压力传感器，直接接触肾盂内环境，实时精准测量并显示肾盂内温度与压力数据，确保温度和压力维持在预设的安全区间内，根源上避免了因高温高压引发的术后并发症发生，保障手术过程的安全性和有效性，与本公司生产的电子内窥镜图像处理器（型号：PY-IPE）配套，经尿道进入人体，通过视频显示器提供影像，配合内窥镜附件，对患者输尿管及肾盂进行内镜检査或内镜手术。不能与高频附件配用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1120, 120, 1, 'DRAFT', '带压力监测电子镜导管', 'Digital Flexible Ureteroscope with Pressure Monitoring', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用测压输尿管内窥镜导管
注册证号：沪械注准20242060002
生效时间：2024.1.3', '在医疗机构中使用，与本公司生产的电子内窥镜图像处理器（型号：PY-IPE）、医用控压冲吸系统（型号：PY-IPS-SF、PY-IPS-F）配套，经尿道进入人体，通过视频显示器提供影像，配合内窥镜附件，对患者输尿管及肾盂进行内镜检査或内镜手术。不能与高频附件配用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1121, 121, 1, 'DRAFT', '硬管电子输尿管镜', 'Rigid Digital ureteroscope', 11, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：硬管电子输尿管镜
注册证号：沪械注准20242060102
生效时间：2024.4.5', '与本公司生产的电子内窥镜图像处理器配套使用，经尿道进入人体，通过视频显示器提供影像，配合内窥镜附件，对患者输尿管及肾盂进行内镜检査或内镜手术。在医疗机构中使用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1122, 122, 1, 'DRAFT', '输尿管球囊导管', 'Ureteral Balloon Catheter', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：输尿管球囊导管
注册证号：沪械注准20242140036
生效时间：2024.1.24', '用于输尿管狭窄的经腔扩张，或在输尿管镜检查或结石操作之前进行输尿管扩张。
头端柔韧且呈锥型，利于穿过输尿管或病变区，表面带有亲水涂层，减少输尿管损伤，三翼/六翼球囊折叠，profile尺寸小，尼龙材料，耐高压，可精准扩张，外径小，韧性高，无需借助导丝使用', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1123, 123, 1, 'DRAFT', '一次性输尿管负压导引鞘', 'Ureteral access sheath', 11, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性输尿管负压导引鞘
注册证号：沪械注准20232020149
生效时间：2023.6.2', '供泌尿外科手术中，建立内窥镜等器械进入泌尿道的通道用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1124, 124, 1, 'DRAFT', '内窥镜取石网篮', 'Ureteroscopic Stone Retrieval Basket', 11, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：内窥镜取石网篮
注册证号：沪械注准20242020014
生效时间：2024.1.12', '泌尿系统手术中，在内窥镜下操作，用于取出组织、异物、粉碎的结石。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1125, 125, 1, 'DRAFT', '取石球囊', 'Ureteal Balloon Cathter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用取石球囊
注册证号：沪械注准20222020035
生效时间：2022.3.9', '用于通过内窥镜，经胆道从胆管系统中取出结石，或在利用球囊阻塞胆管时注入造影剂。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1126, 126, 1, 'DRAFT', '无菌抽吸管路', 'Sterile Aspiration Tubing', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：无菌抽吸管路
注册证号：沪械注准20222140208
生效时间：2022.11.23', '用于体外连接负压吸引装置和抽吸导管或引流导管，与适宜器械配套后，用于向外引出体内废液（积液或血栓），不直接接触人体，经环氧乙烷灭菌后，一次性使用。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1127, 127, 1, 'DRAFT', '斑马导丝', 'Zebra Guide Wire', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：胆道用斑马导丝
注册证号：沪械注准20212020385
生效时间：2021.6.22', '手术中在内窥镜下操作，用于引导器械，进入胆道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1128, 128, 1, 'DRAFT', '台车
（控温控压软镜系统）', 'PT-SCOPE', 11, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '璞跃智能控温控压输尿管软镜系统（简称：PT-SCOPE）由电子内窥镜图像处理器 、医用控压冲吸系统与测温测压输尿管软镜、输尿管负压柔性鞘组成，集成灌注 、吸引 、异常报警等功能于一体，除镜头端精准测温测压外，同时智能调控术区压力和温度至安全范围内（控温控压），降低术后并发症的发生，保障手术安全性、有效性，减轻患者身心负担，且有助于促进临床医生专业技能快速提升。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1129, 129, 1, 'DRAFT', '水凝胶饱腹微球', 'Hydrogel nicrospheres', 12, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：魔芋水凝胶微球（改前）
魔芋膳食微球（改后）
注册证号：生产许可证：SC10737110203268
生效时间：2025.3.2', '体重管理食品，以魔芋作为原料，采用国家双专利技术、帮助微球体积膨胀80倍、饱腹长达6小时；自研配方、配料极简，主原料魔芋被称为”天然的保健食品“；可作为晚间代餐、配合16+8轻断食帮助管理体重。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1130, 130, 1, 'DRAFT', '液体创口贴', 'Bioadhesive Hydrogel Bandage', 13, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '本产品适用于创面黏合修复，具有一定的免缝合效果，完全自主研发，拥有核心国家发明专利。产品为液态水凝胶，具备易涂抹、生物安全性高、可降解等优势；同时通过可调控光固化作用,切缘对合最佳后即刻闭合，在阻菌和抑菌双重作用下，降低伤口感染风险，具备优异的组织黏合促修复性能。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1131, 131, 1, 'DRAFT', '光控可吸收粘合剂', 'Light-initiated Absorbable Dural & Spine Sealant', 13, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '本产品适用于开颅及脊柱手术中，硬脑（脊）膜缝合部位的辅助封合，防止脑脊液渗漏。使用我司完全自主研发的双修饰新型医用生物材料，拥有核心国家发明专利。产品为单一组分的液态水凝胶，打开包装后不需要配制，即开即用，具备易涂抹、生物安全性高、可体内降解吸收等优势。产品同时具有组织黏附和光控固化的性能，可与硬脑（脊）膜紧密黏合，有效防止脑脊液渗漏；按需涂抹，可控固化，极大提高了临床使用的便利性。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1132, 132, 1, 'DRAFT', '水凝胶', 'Hydrogal', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '温敏型变温水凝胶，可以填加变色粉，温度变化时，水凝胶会随着温度的变化颜色变深或者变浅，用于体表降温，方便体表温度变化的观察', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1133, 133, 1, 'DRAFT', '肠道球囊导管', 'Balloon Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：肠道球囊导管
注册证号：沪械注准20182140050
生效时间：2023.2.11', '适用于扩张直肠肠腔，辅助直肠、肛周部位的影像学检查，提高病变形态、大小和部位的显示效果。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1134, 134, 1, 'DRAFT', '非重力输注装置', 'Infusion Pump', 10, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用输液装置
注册证号：国械注准20243142420
生效时间：2024.11.29', '临床静脉输注药液。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1135, 135, 1, 'DRAFT', '电子脐带剪', 'Umbilical Cord Clamps', 10, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：脐带剪夹器
注册证号：沪械注准20232180026
生效时间：2023.2.11', '适用于孕妇生产后母婴间脐带的分离，供切断并封闭新生儿脐带残端用，其中QDJ-Ⅱ可辅助记录时间。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1136, 136, 1, 'DRAFT', '弹簧管', 'Coiled Tube', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可以将压力延长管等管路通过热塑性做成类似于电话线的弹簧管，便于收纳', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1137, 137, 1, 'DRAFT', '膀胱压力监测器', 'Pressure Monitoring System for Bladder', 10, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：膀胱压力监测器
注册证号：粤械注准20242071199
生效时间：2024.9.10', '临床用于膀胱压力的监测。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1138, 138, 1, 'DRAFT', 'CT造影套件', 'CT Multi-Dosing kit', 10, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', 'CT造影用，可同时注射造影剂与生理盐水；控制双侧心室的造影剂浓度；可大幅度降低图像伪影。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1139, 139, 1, 'DRAFT', '输入接头及附件
(加药延长管、加药接头)', 'Infusion Conneclor and Accessories', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：输液接头及附件
注册证号：国械注准20183141510
生效时间：2023.1.4', '主要用于注射液体、重力输液、液体抽取，可与静脉输液器，静脉导管等连接，避免由于针头穿刺可能带来的微粒、微屑的污染，实现单个方向的液体流动。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1140, 140, 1, 'DRAFT', '颅内血栓抽吸导管', 'Intracranial Thrombus Aspiration Catheter', 7, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：颅内血栓抽吸导管
注册证号：国械注准20233030307
生效时间：2023.3.13', '适用于对颅内大血管阻塞（颈内动脉、大脑中动脉—M1段和M2段、基底动脉和椎动脉内）继发急性缺血性脑中风的患者进行血管再通，而且必须在症状发作的8小时内。不能使用静脉组织型纤溶酶原激活物（IV t-PA）或IV t-PA治疗失败的患者是该治疗的人选。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1141, 141, 1, 'DRAFT', '神经输送支架微导管', 'Neurovascular Stent Delivery Micro Catheter', 14, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '适用于神经血管和外周血管，用于注射或输入对照介质和/或液体和/或栓塞材料, 和/或适当的器械（如支架、弹簧圈）等。
该型号主要用以获得最佳的弹簧圈输送、栓塞、解脱。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1142, 142, 1, 'DRAFT', '可控弯导管 内径6F-20F', 'Steerable Catheter Tube ID：6F-20F', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：可控弯导管鞘
注册证号：沪械注准20242030241
生效时间：2024.7.19', '与扩张器配合使用，用于将导丝、导管等医疗器械插入血管。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1143, 143, 1, 'DRAFT', '胆道用斑马导丝', 'Zebra Guide Wire', 6, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：胆道用斑马导丝
注册证号：沪械注准20212020385
生效时间：2021.6.22', '手术中在内窥镜下操作，用于引导器械，进入胆道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1144, 144, 1, 'DRAFT', '胆道取石球囊导管', 'Biliary stone extraction balloon catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用取石球囊
注册证号：沪械注准20222020035
生效时间：2022.3.9', '用于通过内窥镜，经胆道从胆管系统中取出结石，或在利用球囊阻塞胆管时注入造影剂。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1145, 145, 1, 'DRAFT', '取石网篮', 'Stone extractor', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用取石网篮
注册证号：沪械注准20212020492
生效时间：2021.8.25', '适用于在内窥镜下捕获和取出胆管内的结石或上下消化道中的异物。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1146, 146, 1, 'DRAFT', '心内输送导丝微导管', 'Cardiology Micro Catheter', 14, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '一次性使用微导管用于注射或输入对照介质和/或液体(如造影剂)和/或栓塞材料，和/或适当的器械(如支架、弹簧圈),神经血管应用除外。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1147, 147, 1, 'DRAFT', 'OEM编织导管', 'OEM Breidinp Tube', 14, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可以OEM代加工像造影导管、微导管、指引导管等带有编织网结构的管体。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1148, 148, 1, 'DRAFT', 'PTFE管 壁厚：5μm', 'PTFE Tube', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '采用挤出工艺或者浸涂工艺制备的超薄PTFE内衬管，壁厚可以达到0.005mm。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1149, 149, 1, 'DRAFT', '热收缩管收缩比2:1', 'Heat shrink tubeShrink ratio 2:1', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '采用FEP等材料制备的热收缩管，收缩比可达2:1，常用于多层管体的包覆。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1150, 150, 1, 'DRAFT', '可调弯导管', 'Steerable Catheter', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可用于心脏血管、颅内血管及外周血管，经皮穿刺进入血管系统，在介入诊断或治疗手术中为导丝输送建立通道。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1151, 151, 1, 'DRAFT', '亲水涂层溶液', 'Hydrophilic Coating Solution', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '自研配方，溶液通过在医疗器械表面形成一层光滑的薄膜、遇水后凝胶化，使器械表面超级顺滑，可显著减少器械通过人体组织时的阻力，提高了介入治疗的效果，同事减轻了患者痛苦。⁠⁣', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1152, 152, 1, 'DRAFT', '海波管', 'Hypotube', 15, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可定制尺寸，不锈钢管浸涂工艺，涂层厚度5-12μm。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1153, 153, 1, 'DRAFT', '蚀刻PTFE内衬管', 'PTFE LinerFEP', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '在PTFE内衬管的外表面做蚀刻处理，经处理后的内衬管表面变的更加的亲水，利于和Pebax等聚合物紧密包覆在一起，不容易分层。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1154, 154, 1, 'DRAFT', '导丝芯轴', 'Guide Wire Rod', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可定制各种尺寸，各种渐变结构的导丝磨削芯轴，可加工细到0.08mm直径的变径芯轴。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1155, 155, 1, 'DRAFT', '扁丝', 'Stanless Steel Flat Wire', 15, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '可定制尺寸，加工精度0.005mm。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1156, 156, 1, 'DRAFT', '热收缩管', 'Heat shrink tube', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '采用FEP等材料制备的热收缩管，收缩比可达2:1，常用于多层管体的包覆。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1157, 157, 1, 'DRAFT', 'PTFE涂层钢丝', 'PTFE Coated Wires', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '在不锈钢等金属表面涂覆一层很薄的PTFE涂层，是金属丝变得超滑，用于制备造影导丝等产品。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1158, 158, 1, 'DRAFT', '磨削芯丝', 'Guided core wire', 15, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '不锈钢、镍钛材料，可定制化尺寸磨削，最小加工直径0.01mm，精度0.003mm。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1159, 159, 1, 'DRAFT', '冲洗阀', 'Flush Device', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '最大程度减少导管头部出现凝血。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1160, 160, 1, 'DRAFT', '单向阀', 'Check Valve', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '重力输液型单向阀具有十分接近零开启压的特性，为重力输液治疗方案设计。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1161, 161, 1, 'DRAFT', '正压接头', 'Positive pressure connector', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：输液接头及附件
注册证号：国械标准20183141510
生效时间：2023.1.4', '无磁设计，正压冲洗，硅胶材质，多通道设计。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1162, 162, 1, 'DRAFT', '精量调节器', 'Flow Regulator', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '流量稳定性高，连续输液前后误差小，扭矩小。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1163, 163, 1, 'DRAFT', '接头类', 'Connectors', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '标准化，产品多样性。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1164, 164, 1, 'DRAFT', '三通旋塞', 'Stopcock', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：一次性使用三通旋塞
注册证号：沪械注准20242030122
生效时间：2024.5.30', '用于介入手术中压力监测管路中的连接、输液和通路切换。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1165, 165, 1, 'DRAFT', '无针加药接头', 'Needle Free Connectors', 1, 'YINGTAI', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '标准鲁尔接口，低预灌冲量，高流速。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (1166, 166, 1, 'DRAFT', '抗脂三通', 'Lipid-resistant Stopcock', 10, 'SUBSIDIARY', 'REGISTERED', NULL, NULL, '注册证名称：/
注册证号：/
生效时间：/', '适用于与输液管路或压力监测管路连接以达成液体传输及液路控制。', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0);

INSERT INTO tmp_showroom_seed_hall_product (id, hall_id, product_id, display_order, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  (2001, 1, 1, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2002, 1, 2, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2003, 1, 3, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2004, 1, 4, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2005, 1, 5, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2006, 1, 6, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2007, 1, 7, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2008, 1, 8, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2009, 1, 9, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2010, 1, 10, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2011, 1, 11, 11, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2012, 1, 12, 12, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2013, 1, 13, 13, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2014, 1, 14, 14, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2015, 1, 15, 15, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2016, 1, 16, 16, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2017, 1, 17, 17, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2018, 1, 18, 18, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2019, 1, 19, 19, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2020, 1, 20, 20, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2021, 1, 21, 21, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2022, 1, 22, 22, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2023, 1, 23, 23, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2024, 1, 24, 24, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2025, 1, 25, 25, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2026, 1, 26, 26, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2027, 2, 27, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2028, 2, 28, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2029, 2, 29, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2030, 2, 30, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2031, 2, 31, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2032, 2, 32, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2033, 2, 33, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2034, 2, 34, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2035, 2, 35, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2036, 2, 36, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2037, 2, 37, 11, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2038, 2, 38, 12, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2039, 2, 39, 13, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2040, 2, 40, 14, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2041, 2, 41, 15, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2042, 2, 42, 16, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2043, 2, 43, 17, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2044, 2, 44, 18, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2045, 2, 45, 19, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2046, 2, 46, 20, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2047, 2, 47, 21, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2048, 2, 48, 22, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2049, 2, 49, 23, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2050, 2, 50, 24, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2051, 2, 51, 25, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2052, 2, 52, 26, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2053, 2, 53, 27, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2054, 2, 54, 28, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2055, 3, 55, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2056, 3, 56, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2057, 3, 57, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2058, 3, 58, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2059, 3, 59, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2060, 3, 60, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2061, 3, 61, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2062, 3, 62, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2063, 3, 63, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2064, 3, 64, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2065, 3, 65, 11, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2066, 3, 66, 12, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2067, 3, 67, 13, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2068, 3, 68, 14, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2069, 3, 69, 15, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2070, 3, 70, 16, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2071, 3, 71, 17, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2072, 3, 72, 18, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2073, 3, 73, 19, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2074, 3, 74, 20, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2075, 3, 75, 21, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2076, 3, 76, 22, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2077, 3, 77, 23, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2078, 3, 78, 24, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2079, 3, 79, 25, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2080, 3, 80, 26, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2081, 3, 81, 27, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2082, 4, 82, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2083, 4, 83, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2084, 4, 84, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2085, 4, 85, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2086, 4, 86, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2087, 4, 87, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2088, 4, 88, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2089, 4, 89, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2090, 4, 90, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2091, 4, 91, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2092, 4, 92, 11, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2093, 4, 93, 12, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2094, 4, 94, 13, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2095, 4, 95, 14, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2096, 4, 96, 15, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2097, 4, 97, 16, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2098, 4, 98, 17, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2099, 5, 99, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2100, 5, 100, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2101, 5, 101, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2102, 5, 102, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2103, 5, 103, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2104, 5, 104, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2105, 5, 105, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2106, 5, 106, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2107, 5, 107, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2108, 5, 108, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2109, 6, 109, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2110, 6, 110, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2111, 6, 111, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2112, 6, 112, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2113, 6, 113, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2114, 6, 114, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2115, 6, 115, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2116, 6, 116, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2117, 6, 117, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2118, 6, 118, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2119, 6, 119, 11, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2120, 6, 120, 12, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2121, 6, 121, 13, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2122, 6, 122, 14, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2123, 6, 123, 15, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2124, 6, 124, 16, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2125, 6, 125, 17, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2126, 6, 126, 18, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2127, 6, 127, 19, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2128, 6, 128, 20, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2129, 7, 129, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2130, 7, 130, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2131, 7, 131, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2132, 7, 132, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2133, 7, 133, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2134, 7, 134, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2135, 7, 135, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2136, 7, 136, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2137, 7, 137, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2138, 7, 138, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2139, 7, 139, 11, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2140, 8, 140, 1, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2141, 8, 141, 2, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2142, 8, 142, 3, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2143, 8, 143, 4, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2144, 8, 144, 5, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2145, 8, 145, 6, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2146, 8, 146, 7, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2147, 8, 147, 8, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2148, 8, 148, 9, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2149, 8, 149, 10, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2150, 8, 150, 11, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2151, 8, 151, 12, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2152, 8, 152, 13, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2153, 8, 153, 14, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2154, 8, 154, 15, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2155, 8, 155, 16, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2156, 8, 156, 17, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2157, 8, 157, 18, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2158, 8, 158, 19, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2159, 8, 159, 20, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2160, 8, 160, 21, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2161, 8, 161, 22, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2162, 8, 162, 23, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2163, 8, 163, 24, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2164, 8, 164, 25, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2165, 8, 165, 26, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0),
  (2166, 8, 166, 27, 'showroom-seed', '2026-05-19 00:00:00', 'showroom-seed', '2026-05-19 00:00:00', b'0', 0);

  UPDATE tmp_showroom_seed_company SET tenant_id = v_seed_tenant_id;
  UPDATE tmp_showroom_seed_product SET tenant_id = v_seed_tenant_id;
  UPDATE tmp_showroom_seed_product_revision SET tenant_id = v_seed_tenant_id;
  UPDATE tmp_showroom_seed_hall SET tenant_id = v_seed_tenant_id;
  UPDATE tmp_showroom_seed_hall_product SET tenant_id = v_seed_tenant_id;

  SELECT COUNT(*) INTO v_live_products
  FROM showroom_product
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0' AND current_revision_id IS NOT NULL;
  IF v_live_products > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: live product revisions already exist';
  END IF;

  SELECT COUNT(*) INTO v_revision_advanced
  FROM showroom_product
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0' AND current_revision_no > 1;
  IF v_revision_advanced > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: product revisions already advanced beyond initialization';
  END IF;

  SELECT COUNT(*) INTO v_company_revision_rows FROM showroom_company_revision
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_company_revision_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: company revisions already exist';
  END IF;

  SELECT COUNT(*) INTO v_relation_rows FROM showroom_product_revision_relation
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_relation_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: product relations already exist';
  END IF;

  SELECT COUNT(*) INTO v_change_request_rows FROM showroom_change_request
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_change_request_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: change requests already exist';
  END IF;

  SELECT COUNT(*) INTO v_change_request_item_rows FROM showroom_change_request_item
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_change_request_item_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: change request items already exist';
  END IF;

  SELECT COUNT(*) INTO v_assignment_rows FROM showroom_field_assignment
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_assignment_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: assignments already exist';
  END IF;

  SELECT COUNT(*) INTO v_comment_rows FROM showroom_product_comment
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_comment_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: product comments already exist';
  END IF;

  SELECT COUNT(*) INTO v_narration_rows FROM showroom_narration_version
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_narration_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: narration versions already exist';
  END IF;

  SELECT COUNT(*) INTO v_preview_rows FROM showroom_preview_asset_version
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_preview_rows > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: preview asset versions already exist';
  END IF;

  SELECT COUNT(*) INTO v_company_rows FROM showroom_company
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_company_rows > 0 THEN
    SELECT COUNT(*) INTO v_company_diff FROM (
    SELECT c.id
    FROM showroom_company c
    LEFT JOIN tmp_showroom_seed_company s
      ON c.id = s.id
     AND c.company_code = s.company_code
     AND c.display_name = s.display_name
     AND c.company_type = s.company_type
     AND IFNULL(c.current_revision_id, 0) = IFNULL(s.current_revision_id, 0)
     AND IFNULL(c.current_revision_no, 0) = IFNULL(s.current_revision_no, 0)
     AND c.incomplete_flag = s.incomplete_flag
     AND c.status = s.status
     AND c.tenant_id = s.tenant_id
    WHERE c.tenant_id = v_seed_tenant_id AND c.deleted = b'0' AND s.id IS NULL
    UNION ALL
    SELECT s.id
    FROM tmp_showroom_seed_company s
    LEFT JOIN showroom_company c
      ON c.id = s.id
     AND c.deleted = b'0'
     AND c.tenant_id = s.tenant_id
    WHERE c.id IS NULL
    ) diff_company;
    IF v_company_diff > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: company master data no longer matches the initialization seed';
    END IF;
  END IF;

  SELECT COUNT(*) INTO v_product_rows FROM showroom_product
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_product_rows > 0 THEN
    SELECT COUNT(*) INTO v_product_diff FROM (
    SELECT p.id
    FROM showroom_product p
    LEFT JOIN tmp_showroom_seed_product s
      ON p.id = s.id
     AND p.product_code = s.product_code
     AND IFNULL(p.current_revision_id, 0) = IFNULL(s.current_revision_id, 0)
     AND IFNULL(p.current_revision_no, 0) = IFNULL(s.current_revision_no, 0)
     AND p.incomplete_flag = s.incomplete_flag
     AND p.status = s.status
     AND p.tenant_id = s.tenant_id
    WHERE p.tenant_id = v_seed_tenant_id AND p.deleted = b'0' AND s.id IS NULL
    UNION ALL
    SELECT s.id
    FROM tmp_showroom_seed_product s
    LEFT JOIN showroom_product p
      ON p.id = s.id
     AND p.deleted = b'0'
     AND p.tenant_id = s.tenant_id
    WHERE p.id IS NULL
    ) diff_product;
    IF v_product_diff > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: product master data no longer matches the initialization seed';
    END IF;
  END IF;

  SELECT COUNT(*) INTO v_product_revision_rows FROM showroom_product_revision
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_product_revision_rows > 0 THEN
    SELECT COUNT(*) INTO v_product_revision_diff FROM (
    SELECT r.id
    FROM showroom_product_revision r
    LEFT JOIN tmp_showroom_seed_product_revision s
      ON r.id = s.id
     AND r.product_id = s.product_id
     AND r.revision_no = s.revision_no
     AND r.status = s.status
     AND IFNULL(r.name_cn, '') = IFNULL(s.name_cn, '')
     AND IFNULL(r.name_en, '') = IFNULL(s.name_en, '')
     AND IFNULL(r.owner_company_id, 0) = IFNULL(s.owner_company_id, 0)
     AND IFNULL(r.product_owner_type, '') = IFNULL(s.product_owner_type, '')
     AND IFNULL(r.lifecycle_stage, '') = IFNULL(s.lifecycle_stage, '')
     AND IFNULL(r.registration_certificate, '') = IFNULL(s.registration_certificate, '')
     AND IFNULL(r.indication_content, '') = IFNULL(s.indication_content, '')
     AND r.tenant_id = s.tenant_id
    WHERE r.tenant_id = v_seed_tenant_id AND r.deleted = b'0' AND s.id IS NULL
    UNION ALL
    SELECT s.id
    FROM tmp_showroom_seed_product_revision s
    LEFT JOIN showroom_product_revision r
      ON r.id = s.id
     AND r.deleted = b'0'
     AND r.tenant_id = s.tenant_id
    WHERE r.id IS NULL
    ) diff_product_revision;
    IF v_product_revision_diff > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: product revisions no longer match the initialization seed';
    END IF;
  END IF;

  SELECT COUNT(*) INTO v_hall_rows FROM showroom_hall
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_hall_rows > 0 THEN
    SELECT COUNT(*) INTO v_hall_diff FROM (
    SELECT h.id
    FROM showroom_hall h
    LEFT JOIN tmp_showroom_seed_hall s
      ON h.id = s.id
     AND h.hall_code = s.hall_code
     AND h.name = s.name
     AND IFNULL(h.name_en, '') = IFNULL(s.name_en, '')
     AND IFNULL(h.description, '') = IFNULL(s.description, '')
     AND IFNULL(h.description_en, '') = IFNULL(s.description_en, '')
     AND h.display_order = s.display_order
     AND h.status = s.status
     AND h.tenant_id = s.tenant_id
    WHERE h.tenant_id = v_seed_tenant_id AND h.deleted = b'0' AND s.id IS NULL
    UNION ALL
    SELECT s.id
    FROM tmp_showroom_seed_hall s
    LEFT JOIN showroom_hall h
      ON h.id = s.id
     AND h.deleted = b'0'
     AND h.tenant_id = s.tenant_id
    WHERE h.id IS NULL
    ) diff_hall;
    IF v_hall_diff > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: hall metadata no longer matches the initialization seed';
    END IF;
  END IF;

  SELECT COUNT(*) INTO v_hall_product_rows FROM showroom_hall_product
  WHERE tenant_id = v_seed_tenant_id AND deleted = b'0';
  IF v_hall_product_rows > 0 THEN
    SELECT COUNT(*) INTO v_hall_product_diff FROM (
    SELECT hp.id
    FROM showroom_hall_product hp
    LEFT JOIN tmp_showroom_seed_hall_product s
      ON hp.id = s.id
     AND hp.hall_id = s.hall_id
     AND hp.product_id = s.product_id
     AND hp.display_order = s.display_order
     AND hp.tenant_id = s.tenant_id
    WHERE hp.tenant_id = v_seed_tenant_id AND hp.deleted = b'0' AND s.id IS NULL
    UNION ALL
    SELECT s.id
    FROM tmp_showroom_seed_hall_product s
    LEFT JOIN showroom_hall_product hp
      ON hp.id = s.id
     AND hp.deleted = b'0'
     AND hp.tenant_id = s.tenant_id
    WHERE hp.id IS NULL
    ) diff_hall_product;
    IF v_hall_product_diff > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SHOWROOM_SEED_BLOCKED: hall-product mappings no longer match the initialization seed';
    END IF;
  END IF;

  START TRANSACTION;
    DELETE FROM showroom_hall_product WHERE tenant_id = v_seed_tenant_id;
    DELETE FROM showroom_product_revision WHERE tenant_id = v_seed_tenant_id;
    DELETE FROM showroom_hall WHERE tenant_id = v_seed_tenant_id;
    DELETE FROM showroom_product WHERE tenant_id = v_seed_tenant_id;
    DELETE FROM showroom_company WHERE tenant_id = v_seed_tenant_id;

    INSERT INTO showroom_company (id, company_code, display_name, company_type, current_revision_id,
        current_revision_no, incomplete_flag, status, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT id, company_code, display_name, company_type, current_revision_id, current_revision_no,
        incomplete_flag, status, creator, create_time, updater, update_time, deleted, tenant_id
    FROM tmp_showroom_seed_company ORDER BY id;

    INSERT INTO showroom_hall (id, hall_code, name, name_en, description, description_en, display_order, status, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT id, hall_code, name, name_en, description, description_en, display_order, status, creator, create_time, updater, update_time, deleted, tenant_id
    FROM tmp_showroom_seed_hall ORDER BY id;

    INSERT INTO showroom_product (id, product_code, current_revision_id, current_revision_no, incomplete_flag, status, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT id, product_code, current_revision_id, current_revision_no, incomplete_flag, status, creator, create_time, updater, update_time, deleted, tenant_id
    FROM tmp_showroom_seed_product ORDER BY id;

    INSERT INTO showroom_product_revision (id, product_id, revision_no, status, name_cn, name_en, owner_company_id, product_owner_type, lifecycle_stage, target_market, pipeline_layout, registration_certificate, indication_content, core_selling_points, model_specification, clinical_effect, fim_status, submitted_by, approved_by, published_at, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT id, product_id, revision_no, status, name_cn, name_en, owner_company_id, product_owner_type, lifecycle_stage, target_market, pipeline_layout, registration_certificate, indication_content, core_selling_points, model_specification, clinical_effect, fim_status, submitted_by, approved_by, published_at, creator, create_time, updater, update_time, deleted, tenant_id
    FROM tmp_showroom_seed_product_revision ORDER BY id;

    INSERT INTO showroom_hall_product (id, hall_id, product_id, display_order, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT id, hall_id, product_id, display_order, creator, create_time, updater, update_time, deleted, tenant_id
    FROM tmp_showroom_seed_hall_product ORDER BY id;
  COMMIT;
END //
DELIMITER ;

CALL init_showroom_excel_seed();
DROP PROCEDURE IF EXISTS init_showroom_excel_seed;
