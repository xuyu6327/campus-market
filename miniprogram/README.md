# 校园二手交易平台 - 微信小程序（miniprogram/）

> 原生小程序（WXML/WXSS/JS，无 uni-app），复用 Web 版后端全部接口，接口契约以项目根 CLAUDE.md 第 8 节为准。
> 设计规范：主色 #FF6B35、辅色 #2EC4B6、背景 #FFF9F5（rpx 适配，见 app.wxss）。

## 运行方式

1. 确保后端运行在 `http://localhost:8080`（`cd backend && mvn spring-boot:run`）
2. 微信开发者工具 → 导入项目 → 选择本 `miniprogram/` 目录（appid 用测试号/touristappid）
3. 详情 → 本地设置 → 勾选 **「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」**
4. 后端地址在 `utils/request.js` 的 `BASE` 常量中修改

## 一、页面清单（16 页，全部完成）

| 页面 | 路径 | 功能 |
|------|------|------|
| 首页（tab） | pages/index | 搜索/热门词、分类筛选、排序（最新/价格↑↓/热门 popular）、成色筛选、价格区间、分页加载 |
| 商品详情 | pages/detail | 轮播、收藏、私聊、预订弹窗（买家至少填一种联系方式）、举报、卖家主页入口、评价预览、卖家自运营（下架/重新上架） |
| 发布（tab） | pages/publish | 全字段表单 + 多图上传（wx.uploadFile 复用 /file/upload）；编辑模式复用（强制下架商品"编辑并申请上架" PUT /goods/{id}） |
| 登录注册 | pages/auth | 学号/手机号登录、注册；401 清登录态跳此页 |
| 我的（tab） | pages/profile | 资料/头像上传、信用分卡片、菜单（发布/收藏/记录/订单/消息/评价/设置/信用分记录）、未读角标、退出 |
| 收藏（tab） | pages/favorites | 收藏列表 + 取消收藏 |
| 浏览记录 | pages/history | 浏览历史列表 |
| 我的发布 | pages/my-goods | 状态筛选（全部/在售/预订/售出/待审核/下架）、下架/重新上架/编辑并申请上架（强制下架原因展示） |
| 订单列表 | pages/orders | 买到的/卖出的 tab、状态筛选、商品名搜索、分页 |
| 订单详情 | pages/order-detail | 状态/商品/联系方式（交易双方可见）、取消/确认完成/联系不上卖家（幂等）、评价（星级+匿名）与已评价灰显 |
| 消息（tab） | pages/notifications | 通知/私信双 tab + 未读角标；通知点击按 relatedId 语义跳转（type1-6 订单详情、type7 商品详情）；全部已读；会话列表 |
| 聊天窗口 | pages/chat-detail | 5 秒轮询增量拉取（afterId）、发送、onHide/onUnload 清理定时器、会话信息导航标题 |
| 用户主页 | pages/user-profile | 公开信息（信用分/在售数/好评率）、在售商品、收到的评价、举报用户 |
| 我的评价 | pages/reviews | 我发出的/我收到的 |
| 信用分记录 | pages/credit-logs | 当前分 + 变更记录（before→after、changeValue、reason） |
| 提交举报 | pages/report | 用户/商品举报（6 类理由 + 描述） |

tabBar（5 格，无图标纯文字）：首页 / 消息 / 发布 / 我的 / 收藏。

## 二、接口对照表（小程序 → 后端）

| 功能 | 方法+路径 | 页面 |
|------|-----------|------|
| 分类列表 | GET /goods/category | index/publish |
| 商品列表（分页/筛选/排序） | GET /goods/list?pageNum&pageSize&categoryId&keyword&minPrice&maxPrice&goodsCondition&sortBy | index |
| 商品详情 | GET /goods/detail/{id} | detail/publish(编辑) |
| 发布商品 | POST /goods | publish |
| 编辑并申请上架 | PUT /goods/{id} | publish(编辑模式) |
| 收藏/取消收藏 | POST|DELETE /goods/{id}/favorite | detail/favorites |
| 我的收藏/浏览历史 | GET /goods/favorites、/goods/history | favorites/history |
| 下架/重新上架 | PUT /goods/{id}/takedown、/relist | detail/my-goods |
| 我的发布 | GET /goods/my（含 takedownBy/takedownReason） | my-goods |
| 用户在售 | GET /goods/user/{id} | user-profile |
| 登录/注册 | POST /user/login、/user/register | auth |
| 个人信息/修改 | GET|PUT /user/info | profile |
| 公开主页 | GET /user/{id}/profile | user-profile |
| 信用分/记录 | GET /user/credit、/user/credit/logs | credit-logs |
| 预订 | POST /order（buyerPhone/buyerQq/buyerWechat 至少一种） | detail |
| 买家取消/卖家取消/确认完成 | PUT /order/{id}/buyer-cancel、/seller-cancel、/confirm | order-detail |
| 联系不上卖家 | POST /order/{id}/contact-fail | order-detail |
| 买到的/卖出的 | GET /order/buy、/order/sell（status/keyword 筛选） | orders |
| 订单详情 | GET /order/{id} | order-detail |
| 创建评价 | POST /review | order-detail |
| 评价列表（商品/发出/收到/用户） | GET /review/goods/{goodsId}、/sent、/received、/user/{id} | detail/reviews/user-profile |
| 通知列表/已读/全部已读/未读 | GET /notification/list、PUT /{id}/read、/read-all、GET /unread-count | notifications/profile |
| 提交举报/我的举报 | POST /report | report |
| 发起会话/会话列表/信息/消息/发送/未读 | POST /chat/conversation、GET /chat/conversations、/chat/{id}/info、/chat/{id}/messages?afterId=、POST /chat/{id}/messages、GET /chat/unread-count | detail/notifications/chat-detail |
| 图片上传 | POST /file/upload（wx.uploadFile，multipart） | publish/profile |

## 三、技术要点与已知限制

### 已实现的技术要点
- **请求封装** `utils/request.js`：仿 Web 版 api.js（get/post/put/del + 统一 {code,msg,data} + 401 清登录态 `wx.reLaunch` 跳登录页），token 存 `wx.setStorageSync`，请求头 `Authorization: Bearer`；上传用 `wx.uploadFile`（name=file）
- **安全**：WXML `{{}}` 天然转义，无 innerHTML 注入面；所有图片 URL 经 `util.resolveUrl` 相对转绝对（后端存 /uploads/ 相对路径）
- **手机号展示规则**：列表/详情脱敏（后端处理）；订单详情仅交易双方可见对方联系方式（买家填了才有，卖家侧有"未填写"提示）
- **状态映射**：商品 0下架/1在售/2预订中/3已售出/4待审核；订单 0待交易/1已完成/2买家取消/3卖家取消/4超时取消；成色 1-6（与 CONDITION_MAP 一致）
- **成色图片规则**（与后端一致）：成色 3 至少 2 图，4-6 至少 3 图，其余至少 1 图
- **通知跳转**：按 CLAUDE.md 3.7 relatedId 语义（type 1-6 → 订单详情；type 7 → 商品详情；relatedId 为空不跳）
- **私聊轮询**：5 秒 setInterval 增量拉取（afterId），onHide/onUnload 清理
- **编辑并申请上架**：my-goods → 发布页编辑模式（publish 是 tabBar 页，switchTab 无法传参，用 `globalData.editGoodsId` 传递）

### 已知限制
1. **请求基础地址硬编码** `http://localhost:8080`（utils/request.js `BASE`），真机调试需改为局域网 IP/公网域名，且后端需支持 CORS（CorsConfig 已允许）
2. **tabBar 无图标**（纯文字 5 格），如需图标需补充 81x81px PNG 到 `images/` 并配置 iconPath
3. **管理端已开发（2026-08-22）**：`pages/admin/` 原生小程序管理端（浅色卡片现代风），入口在"我的"页（仅 role=1 可见）。页面：`admin`（仪表盘+宫格）、`users`（搜索/封禁/启用/重置密码/调整信用分/详情）、`goods`（搜索/详情/强制下架带原因/审核上架）、`orders`（列表/筛选/详情）、`reports`（处理：警告/下架/封禁/驳回+恶意标记）、`categories`（增改/启停）。注意分类启停接口 status 是 **query 参数**（`?status=0/1`）
4. **登录态安全**与 Web 版一致：JWT 无撤销（重置密码后旧 token 7 天内有效）、无验证码注册、无找回密码
5. **图片上传需后端运行**；上传接口禁 SVG、≤5MB、白名单扩展名+魔数校验（后端已实现）
6. **聊天为轮询方案**（5 秒），非实时推送；后续可升级 WebSocket（表结构不变）
7. **wx.chooseMedia 需基础库 2.10.0+**；开发者工具需勾选"不校验合法域名"（project.config.json 已设 urlCheck:false）
8. 商品详情"举报"入口在页面底部（默认导航栏无自定义按钮），与 Web 版头部入口位置不同

## 四、测试记录

- 小程序关键接口 curl 实测（8080 现网实例）：36/38 通过（2 个 FAIL 为测试脚本选错账号导致，换普通用户后全部通过）
- 后端回归：`BASE=http://localhost:8081 bash scripts/api_test/run_all.sh` → **66 项全部 PASS**（冒烟 30 + 回归 22 + 安全 14）
