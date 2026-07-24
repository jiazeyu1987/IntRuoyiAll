package cn.iocoder.yudao.module.mdm.dal.mysql.product;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MdmProductReferenceMapper {

    @Select("SELECT COUNT(1) FROM dcc_controlled_file WHERE deleted = b'0' AND product_master_id = #{productId}")
    Long countDccReferences(Long productId);

    @Select("SELECT COUNT(1) FROM showroom_product WHERE deleted = b'0' AND product_master_id = #{productId}")
    Long countShowroomReferences(Long productId);

}
