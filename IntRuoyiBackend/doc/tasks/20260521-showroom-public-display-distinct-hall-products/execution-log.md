BDD: each public hall can expose a distinct live product -> Given the local public display verification dataset should better resemble a real showroom arrangement / When anonymous `GET /showroom/display/app-config` is requested / Then the 8 halls should map to 8 distinct products that each have current live preview and ZH/EN narration bundles.

RED: current local public display verification dataset -> FAIL, all 8 halls currently map to product `1`.

GREEN: local distinct-hall repair -> PASS, halls now map to products `240, 241, 242, 243, 245, 246, 248, 251`.

GREEN: anonymous `GET /showroom/display/app-config` -> PASS

GREEN: real browser probe -> PASS, the root kiosk now shows distinct hall titles and distinct product titles from IntRuoyi.
