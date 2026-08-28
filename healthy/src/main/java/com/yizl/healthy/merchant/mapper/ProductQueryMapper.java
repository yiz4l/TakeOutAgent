package com.yizl.healthy.merchant.mapper;

import com.yizl.healthy.merchant.mapper.param.ProductQueryCondition;
import com.yizl.healthy.merchant.mapper.row.ProductSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductQueryMapper {

    long countByCondition(ProductQueryCondition condition);

    List<ProductSummaryRow> selectPageByCondition(ProductQueryCondition condition);

    List<ProductSummaryRow> selectAllByMerchantId(@Param("merchantId") Long merchantId);
}
