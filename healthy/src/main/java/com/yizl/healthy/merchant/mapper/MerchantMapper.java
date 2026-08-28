package com.yizl.healthy.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yizl.healthy.merchant.entity.MerchantEntity;
import com.yizl.healthy.merchant.mapper.param.MerchantQueryCondition;
import com.yizl.healthy.merchant.mapper.row.MerchantSummaryRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MerchantMapper extends BaseMapper<MerchantEntity> {

    long countByCondition(MerchantQueryCondition condition);

    List<MerchantSummaryRow> selectPageByCondition(MerchantQueryCondition condition);

    default Long selectIdByUserId(Long userId) {
        MerchantEntity merchant = selectOne(
                Wrappers.<MerchantEntity>lambdaQuery()
                        .select(MerchantEntity::getId)
                        .eq(MerchantEntity::getUserId, userId)
        );
        return merchant == null ? null : merchant.getId();
    }
}
