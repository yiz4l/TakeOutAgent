# API 设计文档

## 1. 基本约定

- 基础 URL：`/api`
- 除上传文件接口外，请求体均为 `application/json; charset=utf-8`。
- 所有 ID 在 JSON 中以字符串返回和传递，避免前端 JavaScript 精度丢失，例如 `"id": "10001"`。
- 时间格式：`yyyy-MM-dd HH:mm:ss`；日期格式：`yyyy-MM-dd`。
- 分页参数：`page` 从 1 开始，`size` 默认 10，最大 100。
- 除登录、注册接口外，均需在请求头携带 JWT：`Authorization: Bearer <accessToken>`。
- 商家接口仅允许 `MERCHANT` 角色调用；普通用户接口仅允许 `USER` 角色调用。商家只能访问归属自己的数据。
- `/internal/agent/**` 是 Java 业务服务与独立 Python Agent 之间的内部接口，不对浏览器或普通用户开放。生产环境应使用内网隔离，并通过 mTLS 或独立的服务凭证鉴权；不得复用普通用户 JWT。
- 创建异步任务、完成任务等写接口均必须支持幂等。客户端生成的 `requestId`、任务 ID 和数据库唯一索引共同作为幂等依据。

### 1.1 通用响应体

所有接口统一返回 HTTP 200，并用 `code` 表示业务结果；网关或服务器不可用等情形可返回对应 HTTP 状态码。

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

`code` 约定：

| code | 含义 |
| --- | --- |
| 0 | 成功 |
| 40001 | 参数格式错误或缺少必填参数 |
| 40101 | 未登录、JWT 无效或已过期 |
| 40301 | 无权限访问该资源 |
| 40401 | 资源不存在 |
| 40901 | 状态冲突，例如商品已售罄、重复菜品名 |
| 50000 | 服务内部错误 |

以下“返回值”均指通用响应体中的 `data` 字段。

### 1.2 通用对象

**分页结果 `PageResult<T>`**

```json
{
  "records": [],
  "page": 1,
  "size": 10,
  "total": 0,
  "pages": 0
}
```

**商品类型和状态**

- `productType`：`DISH`（单品）、`SETMEAL`（套餐）。
- `productStatus`：`ON_SALE`、`OFF_SALE`、`SOLD_OUT`、`DELETED`。
- `orderStatus`：`PENDING_PAYMENT`、`PENDING_ACCEPT`、`PREPARING`、`DELIVERING`、`COMPLETED`、`CANCELLED`。
- `healthAnalysisTaskStatus`：`PENDING`（待领取）、`RUNNING`（处理中）、`SUCCEEDED`（成功）、`FAILED`（达到重试上限后失败）、`STALE`（输入版本已过期，结果未发布）。

## 2. 认证与账号

### 2.1 用户注册

- 请求类型：`POST`
- 请求路径：`/user/register`
- 鉴权：否
- 请求体：

```json
{
  "phone": "13800138000",
  "password": "Example123!",
  "name": "张三",
  "gender": "MALE",
  "avatarPath": "/uploads/avatar_path/user-1.png"
}
```

`phone`、`password`、`name` 必填；注册账号角色固定为 `USER`。密码长度建议为 8 至 64 位，服务端保存密码哈希。

- 返回值：`UserProfileResponse`

```json
{
  "id": "10001",
  "name": "张三",
  "gender": "MALE",
  "phone": "13800138000",
  "avatarPath": "/uploads/avatar_path/user-1.png",
  "role": "USER"
}
```

商家账号由系统预置或后台创建，本期不提供公开商家注册接口。

### 2.2 登录

- 请求类型：`POST`
- 请求路径：`/user/login`
- 鉴权：否
- 请求体：

```json
{
  "phone": "13800138000",
  "password": "Example123!"
}
```

- 返回值：

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": {
    "id": "10001",
    "name": "张三",
    "phone": "13800138000",
    "gender": "MALE",
    "avatarPath": "/uploads/avatar_path/user-1.png",
    "role": "USER"
  }
}
```

`accessToken` 是 JWT，至少包含用户 ID 和角色。客户端退出登录时删除本地令牌；服务端不保存 JWT。

### 2.3 获取当前账号

- 请求类型：`GET`
- 请求路径：`/user/me`
- 鉴权：是
- URL 参数：无
- 返回值：`UserProfileResponse`。商家账号额外返回 `merchantId`。

### 2.4 更新当前用户资料

- 请求类型：`PUT`
- 请求路径：`/users/update`
- 鉴权：是，`USER`
- 请求体：

```json
{
  "name": "张三",
  "gender": "MALE",
  "avatarPath": "/uploads/avatar_path/user-1-new.png"
}
```

至少传入一个字段；不能通过此接口修改手机号、角色和密码。

- 返回值：`UserProfileResponse`

## 3. 用户端：商家、分类与商品

用户登录后首先进入商家列表。商家列表只展示可供用户浏览的商家；商家的详细联系方式和营业时间在进入商家详情后返回。用户可以使用同一个 `keyword` 搜索商家名称，以及商家在售商品的名称、描述、分类名称和营养标签。

### 3.1 商家列表与跨商家搜索

- 请求类型：`GET`
- 请求路径：`/merchants`
- 鉴权：是，`USER`
- URL 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| keyword | string | 否 | 搜索商家名称、商家描述，以及该商家在售商品的名称、描述、分类名称和营养标签 |
| categoryId | string | 否 | 仅返回有该分类在售单品的商家 |
| productType | string | 否 | `DISH` 或 `SETMEAL`；限制匹配的在售商品类型 |
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页数量 |

只返回 `businessStatus=1` 的商家。搜索条件为空时返回全部营业商家；搜索商品字段时，同一个商家只返回一次。商家停业、已删除的商品不参与匹配。

- 返回值：`PageResult<MerchantSummary>`

```json
{
  "records": [
    {
      "id": "20001",
      "name": "轻食厨房",
      "avatarPath": "/uploads/merchant/20001.png",
      "description": "健康轻食",
      "address": "XX 路 1 号",
      "businessStatus": 1
    }
  ],
  "page": 1,
  "size": 10,
  "total": 1,
  "pages": 1
}
```

`MerchantSummary` 不包含 `businessHours` 和商家电话，避免列表页面携带不必要的详情字段。

### 3.2 商家详情与初始商品集合

- 请求类型：`GET`
- 请求路径：`/merchants/{merchantId}`
- 鉴权：是，`USER`
- 路径参数：`merchantId` 为商家 ID。
- 返回值：`MerchantDetail`

商家不存在时返回 `40401`。商家停业时仍可查看商家详情，但 `products` 返回空数组；已下架、售罄或删除的商品不返回。

```json
{
  "id": "20001",
  "name": "轻食厨房",
  "avatarPath": "/uploads/merchant/20001.png",
  "description": "健康轻食",
  "address": "XX 路 1 号",
  "phone": "010-12345678",
  "businessHours": "09:00-21:00",
  "businessStatus": 1,
  "products": [
    {
      "id": "40001",
      "productType": "DISH",
      "merchantId": "20001",
      "categoryId": "30001",
      "categoryName": "轻食",
      "name": "香煎鸡胸肉",
      "price": 28.00,
      "imagePath": "/uploads/dish/40001.png",
      "description": "低脂高蛋白",
      "salesCount": 120,
      "nutritionTags": ["高蛋白", "低脂"]
    },
    {
      "id": "50001",
      "productType": "SETMEAL",
      "merchantId": "20001",
      "name": "增肌套餐",
      "price": 42.00,
      "imagePath": "/uploads/setmeal/50001.png",
      "description": "鸡胸肉与蔬菜组合",
      "salesCount": 80
    }
  ]
}
```

`MerchantDetail.products` 是进入详情页时展示的初始在售商品集合。商品数量较多或需要搜索时，使用 3.3 的分页接口。

### 3.3 商家内商品列表与搜索

该接口用于商家详情页中的商品分页、搜索和筛选。进入 `/merchant/{merchantId}` 前端路由后，直接使用路由中的 `merchantId` 请求本接口；请求之间不依赖服务端保存“当前商家”。

- 请求类型：`GET`
- 请求路径：`/merchants/{merchantId}/products`
- 鉴权：是，`USER`
- 路径参数：`merchantId` 为当前详情页商家 ID。
- URL 查询参数：

| 参数 | 类型    | 必填 | 说明 |
| --- |---------| --- | --- |
| keyword | string  | 否 | 仅在该商家内按商品名称、描述和分类营养标签搜索 |
| categoryId | string  | 否 | 菜品分类 ID；仅筛选单品 |
| productType | string  | 否 | `DISH`、`SETMEAL`；不传时同时返回单品和套餐 |
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页数量 |

`keyword` 在当前商家内搜索商品名称、描述、分类名称和营养标签。商家不存在时返回 `40401`；商家停业时正常返回空商品列表。接口只返回该商家状态为 `ON_SALE` 的单品和套餐。

- 返回值：`PageResult<ProductSummary>`，格式见 3.5。

示例：

```http
GET /api/merchants/20001/products?keyword=鸡&productType=DISH&page=1&size=10
Authorization: Bearer <accessToken>
```

### 3.4 菜品分类列表

- 请求类型：`GET`
- 请求路径：`/dish-categories`
- 鉴权：是，`USER`
- URL 参数：无
- 返回值：

```json
[
  {
    "id": "30001",
    "name": "轻食",
    "initialSort": 10,
    "nutritionTags": ["高蛋白", "低脂"]
  }
]
```

### 3.5 全局商品列表与搜索

- 请求类型：`GET`
- 请求路径：`/products`
- 鉴权：是，`USER`
- URL 查询参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| merchantId | string | 否 | 限定到某个商家的可选筛选条件；商家详情页优先使用 3.3 |
| categoryId | string | 否 | 菜品分类 ID，仅筛选单品 |
| keyword | string | 否 | 搜索商品名称、描述及分类营养标签 |
| productType | string | 否 | `DISH` 或 `SETMEAL` |
| page | integer | 否 | 页码 |
| size | integer | 否 | 每页数量 |

只返回营业商家的 `ON_SALE` 商品。此接口用于商品搜索、健康推荐分类页或跨商家筛选；如果用户搜索的是商家名称，应使用 3.1；如果用户已经进入某个商家，应使用 3.3。

- 返回值：`PageResult<ProductSummary>`

```json
{
  "records": [
    {
      "id": "40001",
      "productType": "DISH",
      "merchantId": "20001",
      "merchantName": "轻食厨房",
      "categoryId": "30001",
      "categoryName": "轻食",
      "name": "香煎鸡胸肉",
      "price": 28.00,
      "imagePath": "/uploads/dish/40001.png",
      "description": "...",
      "salesCount": 120,
      "nutritionTags": ["高蛋白", "低脂"]
    }
  ],
  "page": 1,
  "size": 10,
  "total": 1,
  "pages": 1
}
```

### 3.6 单品详情

- 请求类型：`GET`
- 请求路径：`/dishes/{dishId}`
- 鉴权：是，`USER`
- 路径参数：`dishId` 为菜品 ID。
- 返回值：`DishDetail`

```json
{
  "id": "40001",
  "productType": "DISH",
  "merchantId": "20001",
  "merchantName": "轻食厨房",
  "categoryId": "30001",
  "categoryName": "轻食",
  "name": "香煎鸡胸肉",
  "price": 28.00,
  "imagePath": "/uploads/dish/40001.png",
  "description": "低脂高蛋白",
  "salesCount": 120,
  "nutritionTags": ["高蛋白", "低脂"],
  "nutritionDetail": {
    "calories": 320,
    "protein_g": 22,
    "fat_g": 8,
    "carbohydrate_g": 30
  }
}
```

`nutritionDetail` 可为空，例如：

```json
{
  "calories": 320,
  "protein_g": 22,
  "fat_g": 8,
  "carbohydrate_g": 30
}
```

### 3.7 套餐详情

- 请求类型：`GET`
- 请求路径：`/setmeals/{setmealId}`
- 鉴权：是，`USER`
- 路径参数：`setmealId` 为套餐 ID。
- 返回值：`SetMealDetail`

```json
{
  "id": "50001",
  "productType": "SETMEAL",
  "merchantId": "20001",
  "merchantName": "轻食厨房",
  "name": "增肌套餐",
  "price": 42.00,
  "imagePath": "/uploads/setmeal/50001.png",
  "description": "鸡胸肉与蔬菜组合",
  "salesCount": 80,
  "dishes": [
    {
      "dishId": "40001",
      "name": "香煎鸡胸肉",
      "imagePath": "/uploads/dish/40001.png",
      "copies": 1
    }
  ]
}
```

## 4. 用户端：地址与购物车

### 4.1 地址列表

- 请求类型：`GET`
- 请求路径：`/addresses`
- 鉴权：是，`USER`
- URL 参数：无
- 返回值：`Address[]`

```json
[
  {
    "id": "60001",
    "address": "XX 市 XX 区 XX 路 1 号 101",
    "contactPhone": "13800138000",
    "enabled": true,
    "remark": "家"
  }
]
```

### 4.2 新增地址

- 请求类型：`POST`
- 请求路径：`/addresses`
- 鉴权：是，`USER`
- 请求体：

```json
{
  "address": "XX 市 XX 区 XX 路 1 号 101",
  "contactPhone": "13800138000",
  "enabled": true,
  "remark": "家"
}
```

`address`、`contactPhone`、`enabled` 必填。

- 返回值：`Address`

### 4.3 修改地址

- 请求类型：`PUT`
- 请求路径：`/addresses/{addressId}`
- 鉴权：是，`USER`
- 路径参数：`addressId` 为地址 ID。
- 请求体：同“新增地址”；允许只传需修改字段。
- 返回值：`Address`

### 4.4 删除地址

- 请求类型：`DELETE`
- 请求路径：`/addresses/{addressId}`
- 鉴权：是，`USER`
- 返回值：`null`

### 4.5 获取购物车

- 请求类型：`GET`
- 请求路径：`/cart-items`
- 鉴权：是，`USER`
- URL 查询参数：`merchantId`（可选；传入时只返回该商家的购物车）。
- 返回值：`CartMerchantGroup[]`

```json
[
  {
    "merchantId": "20001",
    "merchantName": "轻食厨房",
    "items": [
      {
        "id": "70001",
        "productType": "DISH",
        "productId": "40001",
        "name": "香煎鸡胸肉",
        "imagePath": "/uploads/dish/40001.png",
        "unitPrice": 28.00,
        "quantity": 2,
        "selected": true
      }
    ]
  }
]
```

### 4.6 加入购物车

- 请求类型：`POST`
- 请求路径：`/cart-items`
- 鉴权：是，`USER`
- 请求体：

```json
{
  "merchantId": "20001",
  "productType": "DISH",
  "productId": "40001",
  "quantity": 1,
  "selected": true
}
```

商品已存在时服务端累加数量。商品必须是该商家在售商品。

- 返回值：`CartItem`

### 4.7 修改购物车条目

- 请求类型：`PUT`
- 请求路径：`/cart-items/{cartItemId}`
- 鉴权：是，`USER`
- 请求体：

```json
{
  "quantity": 2,
  "selected": true
}
```

至少传入 `quantity` 或 `selected`；`quantity` 必须大于 0。

- 返回值：`CartItem`

### 4.8 删除购物车条目

- 请求类型：`DELETE`
- 请求路径：`/cart-items/{cartItemId}`
- 鉴权：是，`USER`
- 返回值：`null`

## 5. 用户端：订单与模拟支付

### 5.1 从购物车创建订单

- 请求类型：`POST`
- 请求路径：`/orders`
- 鉴权：是，`USER`
- 请求体：

```json
{
  "merchantId": "20001",
  "addressId": "60001",
  "cartItemIds": ["70001", "70002"]
}
```

三个字段均必填。所有购物车条目必须属于当前用户、同一商家且已勾选。服务端以当前商品价格创建订单与明细快照，并将订单状态设为 `PENDING_PAYMENT`。

- 返回值：`OrderDetail`

### 5.2 模拟支付

- 请求类型：`POST`
- 请求路径：`/orders/{orderId}/pay`
- 鉴权：是，`USER`
- 路径参数：`orderId` 为订单 ID。
- 请求体：无。
- 返回值：

```json
{
  "orderId": "80001",
  "status": "PENDING_ACCEPT",
  "paymentTime": "2026-07-28 10:00:00",
  "paidAmount": 56.00
}
```

仅订单本人可对 `PENDING_PAYMENT` 订单支付。支付成功后，清理本次结算的购物车条目。

### 5.3 取消未支付订单

- 请求类型：`POST`
- 请求路径：`/orders/{orderId}/cancel`
- 鉴权：是，`USER`
- 请求体：无。
- 返回值：`OrderDetail`

仅允许取消 `PENDING_PAYMENT` 状态订单。本期不支持已支付订单退款。

### 5.4 用户订单列表

- 请求类型：`GET`
- 请求路径：`/orders`
- 鉴权：是，`USER`
- URL 查询参数：`status`（可选）、`page`（可选）、`size`（可选）。
- 返回值：`PageResult<OrderSummary>`

### 5.5 用户订单详情

- 请求类型：`GET`
- 请求路径：`/orders/{orderId}`
- 鉴权：是，`USER`
- 返回值：`OrderDetail`

```json
{
  "id": "80001",
  "orderNo": "202607280001",
  "merchantId": "20001",
  "merchantName": "轻食厨房",
  "status": "PREPARING",
  "orderTime": "2026-07-28 10:00:00",
  "paymentTime": "2026-07-28 10:01:00",
  "totalAmount": 56.00,
  "paidAmount": 56.00,
  "deliveryAddress": "XX 市 XX 区 XX 路 1 号 101",
  "recipientPhone": "13800138000",
  "reminderCount": 0,
  "items": [
    {
      "productId": "40001",
      "productType": "DISH",
      "productName": "香煎鸡胸肉",
      "productPrice": 28.00,
      "quantity": 2
    }
  ]
}
```

### 5.6 催单

- 请求类型：`POST`
- 请求路径：`/orders/{orderId}/reminders`
- 鉴权：是，`USER`
- 请求体：无。
- 返回值：

```json
{
  "orderId": "80001",
  "reminderCount": 1,
  "lastReminderTime": "2026-07-28 10:05:00"
}
```

仅订单本人且订单为 `PENDING_ACCEPT` 或 `PREPARING` 时可调用。建议服务端限制同一订单两次催单间隔至少 5 分钟。

## 6. 用户端：饮食记录、分析和推荐

### 6.1 新增饮食记录

- 请求类型：`POST`
- 请求路径：`/diet-records`
- 鉴权：是，`USER`
- 请求体：平台菜品与手工食品二选一。

平台菜品：

```json
{
  "dishId": "40001",
  "mealTime": "2026-07-28 12:10:00",
  "quantity": 1
}
```

手工食品：

```json
{
  "foodName": "水煮蛋",
  "foodNutritionTags": ["高蛋白"],
  "mealTime": "2026-07-28 08:00:00",
  "quantity": 1
}
```

`mealTime`、`quantity` 必填；手工食品的 `foodName` 必填，`foodNutritionTags` 可为空。

- 返回值：`DietRecord`

```json
{
  "id": "90001",
  "dishId": "40001",
  "foodName": null,
  "foodNutritionTags": ["高蛋白", "低脂"],
  "mealTime": "2026-07-28 12:10:00",
  "quantity": 1
}
```

服务端创建记录后同步更新对应日期的 `nutrition_record` 聚合数据。

### 6.2 饮食记录列表

- 请求类型：`GET`
- 请求路径：`/diet-records`
- 鉴权：是，`USER`
- URL 查询参数：`date`（可选，`yyyy-MM-dd`）；未传时默认当天。
- 返回值：`DietRecord[]`

### 6.3 删除饮食记录

- 请求类型：`DELETE`
- 请求路径：`/diet-records/{dietRecordId}`
- 鉴权：是，`USER`
- 返回值：`null`

删除后服务端重新计算对应日期的营养聚合数据。

### 6.4 创建健康分析任务

- 请求类型：`POST`
- 请求路径：`/health-analysis-tasks`
- 鉴权：是，`USER`
- 请求头：`Idempotency-Key`，必填，由客户端生成 UUID；同一用户对同一个 key 重试时必须返回同一任务，不得重复调用 Agent。
- 请求体：

```json
{
  "analysisDate": "2026-07-28"
}
```

`analysisDate` 可选，未传时分析当天的营养记录。该接口只创建异步任务，不同步等待 Python Agent、向量检索或大模型返回。

- 返回值：`HealthAnalysisTask`

```json
{
  "id": "110001",
  "requestId": "225b1d68-3858-4b38-8a45-cb93ecb73270",
  "analysisDate": "2026-07-28",
  "inputRevision": "17",
  "modelVersion": "diet-agent-v1",
  "status": "PENDING",
  "attemptCount": 0,
  "analysisId": null,
  "errorMessage": null,
  "createTime": "2026-07-28 13:00:00",
  "updateTime": "2026-07-28 13:00:00"
}
```

Java 服务创建任务时必须在短事务内完成以下操作：

1. 读取当前用户和日期对应的营养输入版本 `inputRevision`。
2. 选择服务端配置的 `modelVersion`，客户端不能指定模型版本。
3. 使用请求头中的 `Idempotency-Key` 作为 `requestId` 插入 `health_analysis_task`。
4. `uk_analysis_request(request_id)` 保证请求重试幂等；`uk_analysis_input(user_id, analysis_date, input_revision, model_version)` 保证相同输入快照与模型版本只创建一个任务。命中唯一键且已有任务属于当前用户时返回已有任务；若 `requestId` 意外命中其他用户的任务，只返回 `40901`，不得返回或泄露其他用户任务。

`inputRevision` 表示指定用户、指定日期的饮食与营养聚合快照版本。新增或删除该日期的 `diet_record` 时，Java 服务必须在更新 `nutrition_record` 的同一数据库事务中原子递增该版本。Agent 执行期间不得长时间锁定饮食记录；通过版本比较识别过期结果。

当前表结构下，Python Agent 使用第 10.1 节的领取接口轮询任务。若后续接入消息队列，不能直接采用“提交数据库事务后再发送消息”的不可靠双写；应增加事务 Outbox，在创建任务的同一事务中写入待投递事件，再由投递器发送消息并重试。消息只用于通知，`health_analysis_task` 仍是任务状态的事实来源。

### 6.5 查询健康分析任务

- 请求类型：`GET`
- 请求路径：`/health-analysis-tasks/{taskId}`
- 鉴权：是，`USER`
- 返回值：`HealthAnalysisTask`

仅任务所属用户可以查询。`SUCCEEDED` 时 `analysisId` 非空；客户端随后查询分析详情。`FAILED` 时返回可展示的通用失败信息，不向用户暴露模型提示词、调用栈、内部地址或供应商响应原文。

### 6.6 查询健康分析任务列表

- 请求类型：`GET`
- 请求路径：`/health-analysis-tasks`
- 鉴权：是，`USER`
- URL 查询参数：`status`、`page`、`size`，均可选。
- 返回值：`PageResult<HealthAnalysisTask>`

### 6.7 查询健康分析历史

- 请求类型：`GET`
- 请求路径：`/health-analyses`
- 鉴权：是，`USER`
- URL 查询参数：`page`（可选）、`size`（可选）。
- 返回值：`PageResult<HealthAnalysisSummary>`

历史列表只返回已成功落库的 `health_analysis`，不包含执行中或失败的任务。

### 6.8 查询分析详情与分类推荐

- 请求类型：`GET`
- 请求路径：`/health-analyses/{analysisId}`
- 鉴权：是，`USER`
- 返回值：`HealthAnalysisDetail`

```json
{
  "id": "100001",
  "analysisDate": "2026-07-28",
  "inputRevision": "17",
  "healthScore": 82.50,
  "riskSummary": "蔬菜摄入不足",
  "optimizationSuggestion": "建议下一餐增加蔬菜和优质蛋白质。",
  "analysisModel": "diet-agent-v1",
  "recommendations": [
    {
      "categoryId": "30001",
      "categoryName": "轻食",
      "recommendationScore": 95.00,
      "reason": "有助于补充优质蛋白质"
    }
  ],
  "createTime": "2026-07-28 13:00:12"
}
```

`HealthAnalysisDetail` 中的事实数据必须来自任务输入快照和业务数据库；RAG 只用于补充营养知识、依据和解释，不能让模型猜测用户饮食记录、商品价格或菜品营养数值。

## 7. 商家端：商家资料与数据概览

### 7.1 获取我的商家资料

- 请求类型：`GET`
- 请求路径：`/merchant/me`
- 鉴权：是，`MERCHANT`
- URL 参数：无。
- 返回值：`MerchantDetail`

### 7.2 更新我的商家资料

- 请求类型：`PUT`
- 请求路径：`/merchant/me`
- 鉴权：是，`MERCHANT`
- 请求体：

```json
{
  "name": "轻食厨房",
  "avatarPath": "/uploads/merchant/20001.png",
  "description": "健康轻食",
  "address": "XX 路 1 号",
  "phone": "021-12345678",
  "businessHours": "09:00-21:00",
  "businessStatus": 1
}
```

至少传一个字段。`businessStatus` 为 1（营业）或 0（停业）。

- 返回值：`MerchantDetail`

### 7.3 商家数据概览

- 请求类型：`GET`
- 请求路径：`/merchant/dashboard`
- 鉴权：是，`MERCHANT`
- URL 查询参数：`startDate`、`endDate`，均可选，格式为 `yyyy-MM-dd`；未传时统计当天。
- 返回值：

```json
{
  "orderCount": 12,
  "paidOrderCount": 10,
  "completedOrderCount": 8,
  "salesAmount": 356.00,
  "pendingAcceptCount": 2,
  "remindedOrderCount": 1
}
```

## 8. 商家端：菜品、套餐和营养信息

### 8.1 获取可用分类

- 请求类型：`GET`
- 请求路径：`/merchant/dish-categories`
- 鉴权：是，`MERCHANT`
- URL 参数：无。
- 返回值：`DishCategory[]`，格式同 3.3。

分类与分类营养标签为平台统一配置，本期商家只读。

### 8.2 商家菜品列表

- 请求类型：`GET`
- 请求路径：`/merchant/dishes`
- 鉴权：是，`MERCHANT`
- URL 查询参数：`keyword`、`categoryId`、`status`、`page`、`size`，均可选。
- 返回值：`PageResult<MerchantDish>`

`MerchantDish` 在 `ProductSummary` 基础上额外返回 `status`、`nutritionDetail`、`createTime`、`updateTime`。

### 8.3 新增菜品

- 请求类型：`POST`
- 请求路径：`/merchant/dishes`
- 鉴权：是，`MERCHANT`
- 请求体：

```json
{
  "categoryId": "30001",
  "name": "香煎鸡胸肉",
  "price": 28.00,
  "description": "低油烹饪",
  "imagePath": "/uploads/dish/40001.png",
  "status": "ON_SALE",
  "nutritionDetail": {
    "calories": 320,
    "protein_g": 22,
    "fat_g": 8,
    "carbohydrate_g": 30
  }
}
```

`categoryId`、`name`、`price`、`status` 必填。具体营养成分可不传，且仅保存到该菜品的 `nutritionDetail` 字段；标签由分类关联数据提供。

- 返回值：`MerchantDish`

### 8.4 更新菜品

- 请求类型：`PUT`
- 请求路径：`/merchant/dishes/{dishId}`
- 鉴权：是，`MERCHANT`
- 请求体：字段与 8.3 相同，均可选；至少传一个字段。
- 返回值：`MerchantDish`

### 8.5 删除菜品

- 请求类型：`DELETE`
- 请求路径：`/merchant/dishes/{dishId}`
- 鉴权：是，`MERCHANT`
- 返回值：`null`

服务端软删除：将状态更新为 `DELETED`。已存在的订单和饮食记录不受影响。

### 8.6 商家套餐列表

- 请求类型：`GET`
- 请求路径：`/merchant/setmeals`
- 鉴权：是，`MERCHANT`
- URL 查询参数：`keyword`、`status`、`page`、`size`，均可选。
- 返回值：`PageResult<MerchantSetmeal>`

### 8.7 新增套餐

- 请求类型：`POST`
- 请求路径：`/merchant/setmeals`
- 鉴权：是，`MERCHANT`
- 请求体：

```json
{
  "name": "增肌套餐",
  "price": 42.00,
  "description": "鸡胸肉配糙米饭",
  "imagePath": "/uploads/setmeal/50001.png",
  "status": "ON_SALE",
  "dishes": [
    { "dishId": "40001", "copies": 1 },
    { "dishId": "40002", "copies": 1 }
  ]
}
```

`name`、`price`、`status`、`dishes` 必填；`dishes` 至少包含一项，所有菜品必须属于当前商家。

- 返回值：`MerchantSetmeal`，包含套餐菜品列表。

### 8.8 更新套餐

- 请求类型：`PUT`
- 请求路径：`/merchant/setmeals/{setmealId}`
- 鉴权：是，`MERCHANT`
- 请求体：字段与 8.7 相同；传入 `dishes` 时以该数组整体替换套餐组成。
- 返回值：`MerchantSetmeal`

### 8.9 删除套餐

- 请求类型：`DELETE`
- 请求路径：`/merchant/setmeals/{setmealId}`
- 鉴权：是，`MERCHANT`
- 返回值：`null`。服务端将状态更新为 `DELETED`。

## 9. 商家端：订单管理与提醒

### 9.1 商家订单列表

- 请求类型：`GET`
- 请求路径：`/merchant/orders`
- 鉴权：是，`MERCHANT`
- URL 查询参数：`status`、`hasReminder`、`page`、`size`，均可选。`hasReminder=true` 只返回存在未处理催单提示的订单。
- 返回值：`PageResult<MerchantOrderSummary>`

`MerchantOrderSummary` 包含订单号、用户手机号、订单状态、金额、下单时间、催单次数和最后催单时间。

### 9.2 商家订单详情

- 请求类型：`GET`
- 请求路径：`/merchant/orders/{orderId}`
- 鉴权：是，`MERCHANT`
- 返回值：`OrderDetail`，包含收货地址快照、商品明细和催单信息。

### 9.3 更新订单状态

- 请求类型：`PATCH`
- 请求路径：`/merchant/orders/{orderId}/status`
- 鉴权：是，`MERCHANT`
- 请求体：

```json
{
  "status": "PREPARING"
}
```

允许的状态流转：

| 当前状态 | 可更新为 |
| --- | --- |
| `PENDING_ACCEPT` | `PREPARING` |
| `PREPARING` | `DELIVERING` |
| `DELIVERING` | `COMPLETED` |

不得通过该接口更新未支付或已取消订单。

- 返回值：`OrderDetail`

### 9.4 查询待处理催单提示

- 请求类型：`GET`
- 请求路径：`/merchant/order-reminders`
- 鉴权：是，`MERCHANT`
- URL 查询参数：`page`、`size`，均可选。
- 返回值：`PageResult<OrderReminder>`

```json
{
  "records": [
    {
      "orderId": "80001",
      "orderNo": "202607280001",
      "status": "PREPARING",
      "reminderCount": 1,
      "lastReminderTime": "2026-07-28 10:05:00"
    }
  ],
  "page": 1,
  "size": 10,
  "total": 1,
  "pages": 1
}
```

本期不建立独立提醒表。商家端基于订单的 `reminderCount`、`lastReminderTime` 轮询此接口展示提示。

## 10. 内部接口：Python 健康饮食 Agent

本节接口仅供独立 Python Agent 服务调用。Java 服务是用户身份、饮食事实、任务状态以及最终分析结果的唯一写入方；Python Agent 不直接修改 `health_analysis_task`、`health_analysis`、`diet_recommendation` 等业务表。

Agent 的标准处理链路为：领取任务、读取上下文、RAG 检索与模型推理、校验结构化输出、提交结果。所有内部写接口都必须校验任务状态、租约所有者和输入版本。

### 10.1 领取健康分析任务

- 请求类型：`POST`
- 请求路径：`/internal/agent/health-analysis-tasks/claim`
- 鉴权：是，`AGENT_SERVICE`
- 请求体：

```json
{
  "workerId": "diet-agent-worker-03",
  "leaseSeconds": 120
}
```

`workerId` 必填且应在一次 Worker 进程生命周期内保持唯一；`leaseSeconds` 默认 120，允许范围 30 至 600。

- 返回值：有任务时返回 `AgentTaskLease`，无可领取任务时返回 `null`。

```json
{
  "taskId": "110001",
  "requestId": "225b1d68-3858-4b38-8a45-cb93ecb73270",
  "userId": "10001",
  "analysisDate": "2026-07-28",
  "inputRevision": "17",
  "modelVersion": "diet-agent-v1",
  "attemptCount": 1,
  "leaseOwner": "diet-agent-worker-03",
  "leaseUntil": "2026-07-28 13:02:00"
}
```

Java 服务应使用短事务和 `SELECT ... FOR UPDATE SKIP LOCKED` 领取一条 `PENDING` 任务，然后将其更新为 `RUNNING`、增加 `attempt_count` 并写入租约。多个 Python Worker 同时领取时不能获得同一任务。租约过期的 `RUNNING` 任务可以重新入队；达到最大重试次数后更新为 `FAILED`。

如果已接入消息队列，消息只携带 `taskId`。Worker 收到消息后仍需通过带条件的领取操作取得租约，消息的至少一次投递不能导致同一任务被重复发布结果。

### 10.2 获取任务输入上下文

- 请求类型：`GET`
- 请求路径：`/internal/agent/health-analysis-tasks/{taskId}/context`
- 鉴权：是，`AGENT_SERVICE`
- 请求头：`X-Agent-Worker-Id`，必填，必须等于任务当前 `leaseOwner`。
- 返回值：`HealthAnalysisContext`

```json
{
  "taskId": "110001",
  "userId": "10001",
  "analysisDate": "2026-07-28",
  "inputRevision": "17",
  "modelVersion": "diet-agent-v1",
  "nutritionSummary": [
    {
      "categoryId": "30001",
      "nutritionTag": "高蛋白",
      "count": 2
    }
  ],
  "dietRecords": [
    {
      "id": "90001",
      "dishId": "40001",
      "foodName": null,
      "foodNutritionTags": ["高蛋白", "低脂"],
      "mealTime": "2026-07-28 12:10:00",
      "quantity": 1,
      "dishNutritionDetail": {
        "calories": 320,
        "protein_g": 22,
        "fat_g": 8,
        "carbohydrate_g": 30
      }
    }
  ],
  "availableRecommendationCategories": [
    {
      "categoryId": "30001",
      "categoryName": "轻食",
      "nutritionTags": ["高蛋白", "低脂"]
    }
  ]
}
```

Java 服务负责一次性组装经过权限过滤的结构化事实。Python Agent 不使用用户 JWT，也不自行拼接任意用户 ID 查询业务接口。接口仅允许读取状态为 `RUNNING`、租约未过期且属于当前 Worker 的任务。

### 10.3 续租任务

- 请求类型：`POST`
- 请求路径：`/internal/agent/health-analysis-tasks/{taskId}/heartbeat`
- 鉴权：是，`AGENT_SERVICE`
- 请求体：

```json
{
  "workerId": "diet-agent-worker-03",
  "leaseSeconds": 120
}
```

- 返回值：更新后的 `leaseUntil`。

仅当任务为 `RUNNING`、`leaseOwner` 匹配且当前租约尚未过期时续租。模型调用可能超过初始租约时，Worker 应定期续租；续租失败后必须停止提交该任务结果。

### 10.4 提交健康分析结果

- 请求类型：`POST`
- 请求路径：`/internal/agent/health-analysis-tasks/{taskId}/complete`
- 鉴权：是，`AGENT_SERVICE`
- 请求头：`Idempotency-Key`，必填，建议使用任务 `requestId`。
- 请求体：

```json
{
  "workerId": "diet-agent-worker-03",
  "inputRevision": "17",
  "modelVersion": "diet-agent-v1",
  "healthScore": 82.50,
  "riskSummary": "蔬菜摄入不足",
  "optimizationSuggestion": "建议下一餐增加蔬菜和优质蛋白质。",
  "recommendations": [
    {
      "categoryId": "30001",
      "recommendationScore": 95.00,
      "reason": "有助于补充优质蛋白质"
    }
  ]
}
```

Java 服务必须校验：

1. 任务属于该 Worker，状态为 `RUNNING` 且租约未过期。
2. 请求中的 `inputRevision`、`modelVersion` 与任务一致。
3. `healthScore` 位于 0 至 100；推荐分类存在，分数位于 0 至 100；同一分类不得重复。
4. 当前用户该日期的营养输入版本仍等于任务 `inputRevision`。

完成接口必须开启一个短事务，先使用 `SELECT ... FOR UPDATE` 锁定任务行。若任务已经是 `SUCCEEDED`，直接返回其 `health_analysis_id`；否则校验任务仍为 `RUNNING`、租约所有者匹配且租约未过期，再写入 `health_analysis` 和全部 `diet_recommendation`，最后将任务更新为 `SUCCEEDED` 并关联 `health_analysis_id`。分析、推荐和任务状态必须一起提交或一起回滚。

不能先插入分析结果、提交后再更新任务状态，也不能在调用大模型期间持有该事务或任务行锁。状态更新仍应包含 `WHERE id = ? AND status = 'RUNNING' AND lease_owner = ?` 条件，影响行数必须为 1。

如果当前输入版本已经变化，服务端不得发布旧结果，应将任务更新为 `STALE`；可根据最新版本自动创建新任务。相同完成请求重复到达时，若任务已为 `SUCCEEDED`，返回已保存的同一 `analysisId`，不得重复插入分析和推荐。

- 返回值：

```json
{
  "taskId": "110001",
  "status": "SUCCEEDED",
  "analysisId": "100001"
}
```

### 10.5 上报任务失败

- 请求类型：`POST`
- 请求路径：`/internal/agent/health-analysis-tasks/{taskId}/fail`
- 鉴权：是，`AGENT_SERVICE`
- 请求体：

```json
{
  "workerId": "diet-agent-worker-03",
  "retryable": true,
  "errorCode": "MODEL_TIMEOUT",
  "errorMessage": "模型调用超时"
}
```

可重试且未达到最大次数时，将任务重新置为 `PENDING` 并清空租约；不可重试或达到最大次数时置为 `FAILED`。`errorMessage` 只保存截断、脱敏后的诊断摘要，不保存完整提示词、用户隐私、模型密钥或供应商原始响应。

### 10.6 任务状态机与并发约束

允许的状态流转：

| 当前状态 | 可更新为 | 触发原因 |
| --- | --- | --- |
| `PENDING` | `RUNNING` | Worker 成功领取并获得租约 |
| `RUNNING` | `SUCCEEDED` | 结果校验和事务写入成功 |
| `RUNNING` | `PENDING` | 可重试失败或租约过期回收 |
| `RUNNING` | `FAILED` | 不可重试或达到最大重试次数 |
| `RUNNING` | `STALE` | 用户输入版本已经变化 |
| `FAILED` | `PENDING` | 用户手动重试同一输入版本，清空错误和租约 |

`SUCCEEDED`、`STALE` 为终态；`FAILED` 默认停止自动重试，但允许用户手动将原任务恢复为 `PENDING`。由于 `uk_analysis_input` 限制相同输入版本和模型只能有一条任务，手动重试必须复用原任务，不能插入新任务。所有状态更新必须把期望的旧状态写入 SQL 条件，不能使用“先查询状态，再无条件更新”。Python Worker、多个 Java 实例和多个设备之间的一致性由数据库条件更新、唯一索引和租约保证，不依赖 Java 本地锁。

## 11. 本期不提供的接口

下列功能已在需求中明确为后续范围，当前不设计 API：优惠活动配置、优惠券领取与核销、订单优惠结算、第三方支付回调、退款、配送员调度、图片上传。
