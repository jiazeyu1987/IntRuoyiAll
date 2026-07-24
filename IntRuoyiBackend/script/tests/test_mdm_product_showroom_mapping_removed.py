import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]


class MdmProductShowroomMappingRemovalTest(unittest.TestCase):

    def test_mapping_endpoint_service_api_and_permission_are_removed(self) -> None:
        controller = (
            REPO_ROOT
            / "yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/"
            "controller/admin/ShowroomAdminController.java"
        ).read_text(encoding="utf-8")
        mdm_api = (
            REPO_ROOT
            / "yudao-module-mdm/src/main/java/cn/iocoder/yudao/module/mdm/api/product/"
            "MdmProductApi.java"
        ).read_text(encoding="utf-8")
        migration = (REPO_ROOT / "sql/mysql/20260607_product_master_data.sql").read_text(
            encoding="utf-8"
        )

        self.assertNotIn("mdm-mapping-preview", controller)
        self.assertNotIn("mdm-mapping-confirm", controller)
        self.assertNotIn("mdm:product:map-showroom", controller)
        self.assertNotIn("getProductByProductCode", mdm_api)
        self.assertNotIn("saveProductFromShowroom", mdm_api)
        self.assertNotIn("MdmProductShowroomSyncReqDTO", mdm_api)
        self.assertNotIn("mdm:product:map-showroom", migration)
        self.assertFalse(
            (
                REPO_ROOT
                / "yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/"
                "content/service/ShowroomMdmProductMappingService.java"
            ).exists()
        )
        self.assertFalse(
            (
                REPO_ROOT
                / "yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/"
                "content/model/ShowroomMdmProductMappingPreview.java"
            ).exists()
        )


if __name__ == "__main__":
    unittest.main()
