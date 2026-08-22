#!/bin/bash
# ============================================================
# 03_security.sh - 安全与输入校验专项（自动创建/清理测试数据）
# 覆盖：权限越界 / XSS 存储原样 / 超长输入 / 非法价格 / 敏感词 / SQL 注入
# ============================================================
source "$(dirname "$0")/common.sh"
health_check
echo "==== 03 SECURITY TEST ===="

TS=$(date +%s)
ATOKEN=$(login_or_die "admin")
TSID=$(register_user "3${TS}")   # 普通用户（权限/越权测试用）
assert "S0 register temp user" '^[0-9]+$' "$TSID"
[ -n "$TSID" ] || { echo "ABORT: cannot register user"; exit 1; }
TUID=$(uid_of "$TSID")
UTOKEN=$(login_or_die "$TSID")

# ---------- 权限 ----------
echo "-- authz --"
assert "S1 unauthenticated -> 401" '"code":401' "$(api GET "/user/info")"
assert "S2 non-admin on admin api -> 403" '"code":403' "$(api GET "/admin/dashboard" "$UTOKEN")"

# ---------- 输入校验（发布校验由 @Validated 拦截）----------
echo "-- validation --"
LONG101=$(printf 'a%.0s' $(seq 1 101))
assert "S3 title 101 chars rejected" '"code":400' \
  "$(api POST /goods "$ATOKEN" "{\"title\":\"$LONG101\",\"description\":\"x\",\"categoryId\":3,\"price\":1,\"goodsCondition\":1,\"images\":[\"/uploads/p.jpg\"],\"tradeLocation\":\"t\",\"contactMethod\":2,\"contactQq\":\"1\"}")"
assert "S4 price=0 rejected" '"code":400' \
  "$(api POST /goods "$ATOKEN" '{"title":"t","description":"x","categoryId":3,"price":0,"goodsCondition":1,"images":["/uploads/p.jpg"],"tradeLocation":"t","contactMethod":2,"contactQq":"1"}')"
assert "S5 no images rejected" '"code":400' \
  "$(api POST /goods "$ATOKEN" '{"title":"t","description":"x","categoryId":3,"price":1,"goodsCondition":1,"images":[],"tradeLocation":"t","contactMethod":2,"contactQq":"1"}')"

# 敏感词（从词库取第一个词拼进标题）
SW=$(mysqlq "SELECT word FROM sensitive_word WHERE status=1 LIMIT 1;")
if [ -n "$SW" ]; then
  assert "S6 sensitive word blocked" '"code":400' \
    "$(api POST /goods "$ATOKEN" "{\"title\":\"xx${SW}xx\",\"description\":\"x\",\"categoryId\":3,\"price\":1,\"goodsCondition\":1,\"images\":[\"/uploads/p.jpg\"],\"tradeLocation\":\"t\",\"contactMethod\":2,\"contactQq\":\"1\"}")"
else
  echo "SKIP S6 (no sensitive words in table)"
fi

# ---------- XSS：存储原样 + 前端展示由 escapeHtml 负责 ----------
echo "-- XSS --"
XSS_TITLE='<script>alert(1)</script>'
XGID=$(api POST /goods "$ATOKEN" \
  "{\"title\":\"$XSS_TITLE\",\"description\":\"x\",\"categoryId\":3,\"price\":1.5,\"goodsCondition\":1,\"images\":[\"/uploads/p.jpg\"],\"tradeLocation\":\"t\",\"contactMethod\":2,\"contactQq\":\"1\"}" | sed -E 's/.*"data":([0-9]+).*/\1/')
assert "S7 xss title accepted (stored raw)" '^[0-9]+$' "$XGID"
STORED=$(mysqlq "SELECT title FROM goods_info WHERE id=$XGID;")
assert "S8 stored raw, not encoded" "$XSS_TITLE" "$STORED"
assert "S9 admin api returns raw" "$XSS_TITLE" "$(api GET "/admin/goods/$XGID" "$ATOKEN")"
cleanup_goods "$XGID"

# ---------- SQL 注入字符串（MyBatis 参数化应安全返回）----------
assert "S10 sql injection in keyword" '"code":0' \
  "$(api GET "/goods/list?keyword=%27%20OR%201%3D1%20--")"

# ---------- 越权：他人商品不可编辑/下架 ----------
echo "-- ownership --"
OGID=$(publish_goods "$ATOKEN" "9${TS}")   # admin 发布的商品
assert "S11 admin published goods" '^[0-9]+$' "$OGID"
assert "S12 other user edit -> 403" '"code":403' \
  "$(api PUT "/goods/$OGID" "$UTOKEN" \
    "{\"title\":\"x\",\"description\":\"x\",\"categoryId\":3,\"price\":1,\"goodsCondition\":1,\"images\":[\"/uploads/p.jpg\"],\"tradeLocation\":\"t\",\"contactMethod\":2,\"contactQq\":\"1\"}")"
assert "S13 other user relist -> 403" '"code":403' \
  "$(api PUT "/goods/$OGID/relist" "$UTOKEN")"
cleanup_goods "$OGID"

# ================== 清理 ==================
echo "-- cleanup --"
cleanup_user "$TUID"
cleanup_credit_logs
echo "cleanup done"

summary
