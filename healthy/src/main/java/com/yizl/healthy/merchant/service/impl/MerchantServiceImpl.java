package com.yizl.healthy.merchant.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizl.healthy.common.api.PageResponse;
import com.yizl.healthy.common.exception.BusinessException;
import com.yizl.healthy.common.exception.ErrorCode;
import com.yizl.healthy.merchant.dto.request.MerchantListQueryRequest;
import com.yizl.healthy.merchant.dto.request.MerchantProductQueryRequest;
import com.yizl.healthy.merchant.dto.request.ProductQueryRequest;
import com.yizl.healthy.merchant.dto.response.DishCategories;
import com.yizl.healthy.merchant.dto.response.DishDetail;
import com.yizl.healthy.merchant.dto.response.MerchantDetail;
import com.yizl.healthy.merchant.dto.response.MerchantSummary;
import com.yizl.healthy.merchant.dto.response.NutritionDetail;
import com.yizl.healthy.merchant.dto.response.ProductSummary;
import com.yizl.healthy.merchant.dto.response.SetMealDetail;
import com.yizl.healthy.merchant.dto.response.SetMealDish;
import com.yizl.healthy.merchant.entity.DishCategoryEntity;
import com.yizl.healthy.merchant.entity.DishEntity;
import com.yizl.healthy.merchant.entity.MerchantEntity;
import com.yizl.healthy.merchant.entity.SetMealEntity;
import com.yizl.healthy.merchant.mapper.DishCategoryMapper;
import com.yizl.healthy.merchant.mapper.DishMapper;
import com.yizl.healthy.merchant.mapper.MerchantMapper;
import com.yizl.healthy.merchant.mapper.ProductQueryMapper;
import com.yizl.healthy.merchant.mapper.SetMealDishMapper;
import com.yizl.healthy.merchant.mapper.SetMealMapper;
import com.yizl.healthy.merchant.mapper.param.MerchantQueryCondition;
import com.yizl.healthy.merchant.mapper.param.ProductQueryCondition;
import com.yizl.healthy.merchant.mapper.row.DishCategoryRow;
import com.yizl.healthy.merchant.mapper.row.MerchantSummaryRow;
import com.yizl.healthy.merchant.mapper.row.ProductSummaryRow;
import com.yizl.healthy.merchant.mapper.row.SetMealDishRow;
import com.yizl.healthy.merchant.service.MerchantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MerchantServiceImpl implements MerchantService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final MerchantMapper merchantMapper;
    private final ProductQueryMapper productQueryMapper;
    private final DishMapper dishMapper;
    private final SetMealMapper setMealMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final SetMealDishMapper setMealDishMapper;
    private final ObjectMapper objectMapper;

    public MerchantServiceImpl(
            MerchantMapper merchantMapper,
            ProductQueryMapper productQueryMapper,
            DishMapper dishMapper,
            SetMealMapper setMealMapper,
            DishCategoryMapper dishCategoryMapper,
            SetMealDishMapper setMealDishMapper,
            ObjectMapper objectMapper
    ) {
        this.merchantMapper = merchantMapper;
        this.productQueryMapper = productQueryMapper;
        this.dishMapper = dishMapper;
        this.setMealMapper = setMealMapper;
        this.dishCategoryMapper = dishCategoryMapper;
        this.setMealDishMapper = setMealDishMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResponse<MerchantSummary> getMerchants(MerchantListQueryRequest request) {
        long offset = (long) (request.page() - 1) * request.size();
        MerchantQueryCondition condition = new MerchantQueryCondition(
                normalizeKeyword(request.keyword()),
                request.categoryId(),
                request.productType(),
                offset,
                request.size()
        );

        long total = merchantMapper.countByCondition(condition);
        if (total == 0) {
            return new PageResponse<>(0, request.page(), request.size(), List.of());
        }

        List<MerchantSummary> records = merchantMapper.selectPageByCondition(condition)
                .stream()
                .map(this::toMerchantSummary)
                .toList();
        return new PageResponse<>(total, request.page(), request.size(), records);
    }

    @Override
    public MerchantDetail getMerchantDetail(Long merchantId) {
        MerchantEntity merchant = requireMerchant(merchantId);
        List<ProductSummary> products = productQueryMapper.selectAllByMerchantId(merchantId)
                .stream()
                .map(this::toProductSummary)
                .toList();

        return new MerchantDetail(
                merchant.getId(),
                merchant.getName(),
                merchant.getAvatarPath(),
                merchant.getDescription(),
                merchant.getAddress(),
                merchant.getPhone(),
                merchant.getBusinessHours(),
                merchant.getBusinessStatus(),
                products
        );
    }

    @Override
    public PageResponse<ProductSummary> getMerchantProducts(
            Long merchantId,
            MerchantProductQueryRequest request
    ) {
        requireMerchant(merchantId);
        ProductQueryCondition condition = new ProductQueryCondition(
                merchantId,
                request.categoryId(),
                normalizeKeyword(request.keyword()),
                request.productType(),
                offset(request.page(), request.size()),
                request.size()
        );
        return getProductPage(condition, request.page(), request.size());
    }

    @Override
    public List<DishCategories> getDishCategories() {
        return dishCategoryMapper.selectAllWithNutritionTags().stream()
                .map(this::toDishCategories)
                .toList();
    }

    @Override
    public PageResponse<ProductSummary> getProducts(ProductQueryRequest request) {
        ProductQueryCondition condition = new ProductQueryCondition(
                request.merchantId(),
                request.categoryId(),
                normalizeKeyword(request.keyword()),
                request.productType(),
                offset(request.page(), request.size()),
                request.size()
        );
        return getProductPage(condition, request.page(), request.size());
    }

    @Override
    public DishDetail getDishDetail(Long dishId) {
        DishEntity dish = dishMapper.selectById(dishId);
        if (dish == null || !"ON_SALE".equals(dish.getStatus())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "菜品不存在或不可用");
        }

        MerchantEntity merchant = requireOpenMerchant(dish.getMerchantId());
        DishCategoryEntity category = dishCategoryMapper.selectById(dish.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "菜品分类不存在");
        }

        return new DishDetail(
                dish.getId(),
                "DISH",
                merchant.getId(),
                merchant.getName(),
                category.getId(),
                category.getName(),
                dish.getName(),
                dish.getPrice(),
                dish.getImagePath(),
                dish.getDescription(),
                valueOrZero(dish.getSalesCount()),
                List.copyOf(dishCategoryMapper.selectNutritionTags(category.getId())),
                parseNutritionDetail(dish.getNutritionDetail())
        );
    }

    @Override
    public SetMealDetail getSetMealDetail(Long setMealId) {
        SetMealEntity setMeal = setMealMapper.selectById(setMealId);
        if (setMeal == null || !"ON_SALE".equals(setMeal.getStatus())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "套餐不存在或不可用");
        }

        MerchantEntity merchant = requireOpenMerchant(setMeal.getMerchantId());
        List<SetMealDish> dishes = setMealDishMapper.selectDetailsBySetMealId(setMealId)
                .stream()
                .map(this::toSetMealDish)
                .toList();

        return new SetMealDetail(
                setMeal.getId(),
                "SETMEAL",
                merchant.getId(),
                merchant.getName(),
                setMeal.getName(),
                setMeal.getPrice(),
                setMeal.getImagePath(),
                setMeal.getDescription(),
                valueOrZero(setMeal.getSalesCount()),
                dishes
        );
    }

    private PageResponse<ProductSummary> getProductPage(
            ProductQueryCondition condition,
            int page,
            int size
    ) {
        long total = productQueryMapper.countByCondition(condition);
        if (total == 0) {
            return new PageResponse<>(0, page, size, List.of());
        }

        List<ProductSummary> records = productQueryMapper.selectPageByCondition(condition)
                .stream()
                .map(this::toProductSummary)
                .toList();
        return new PageResponse<>(total, page, size, records);
    }

    private MerchantEntity requireMerchant(Long merchantId) {
        MerchantEntity merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商家不存在");
        }
        return merchant;
    }

    private MerchantEntity requireOpenMerchant(Long merchantId) {
        MerchantEntity merchant = requireMerchant(merchantId);
        if (!Integer.valueOf(1).equals(merchant.getBusinessStatus())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "商家当前未营业");
        }
        return merchant;
    }

    private MerchantSummary toMerchantSummary(MerchantSummaryRow row) {
        return new MerchantSummary(
                row.getId(),
                row.getName(),
                row.getAvatarPath(),
                row.getDescription(),
                row.getAddress(),
                valueOrZero(row.getBusinessStatus())
        );
    }

    private ProductSummary toProductSummary(ProductSummaryRow row) {
        return new ProductSummary(
                row.getId(),
                row.getProductType(),
                row.getMerchantId(),
                row.getMerchantName(),
                row.getCategoryId(),
                row.getCategoryName(),
                row.getName(),
                row.getPrice(),
                row.getImagePath(),
                row.getDescription(),
                valueOrZero(row.getSalesCount()),
                parseStringList(row.getNutritionTagsJson())
        );
    }

    private DishCategories toDishCategories(DishCategoryRow row) {
        return new DishCategories(
                row.getId(),
                row.getName(),
                valueOrZero(row.getInitialSort()),
                parseStringList(row.getNutritionTagsJson())
        );
    }

    private SetMealDish toSetMealDish(SetMealDishRow row) {
        return new SetMealDish(
                row.getDishId(),
                row.getName(),
                row.getImagePath(),
                row.getCopies()
        );
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.copyOf(objectMapper.readValue(json, STRING_LIST_TYPE));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "营养标签数据格式错误");
        }
    }

    private NutritionDetail parseNutritionDetail(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, NutritionDetail.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "营养详情数据格式错误");
        }
    }

    private static long offset(int page, int size) {
        return (long) (page - 1) * size;
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
