/**
 * 积分商店 API：商品列表、兑换、背包、使用
 */

async function get(path) {
  try {
    const res = await fetch('/api/shop' + path)
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

async function post(path, body) {
  try {
    const res = await fetch('/api/shop' + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

/** 商品列表 */
export function apiShopItems() {
  return get('/items')
}

/** 兑换商品 */
export function apiShopBuy(userId, itemId) {
  return post('/buy', { userId, itemId })
}

/** 我的背包 */
export function apiShopBag(userId) {
  return get('/bag?userId=' + userId)
}

/**
 * 使用物品。
 * opts 可选：
 *   petName  —— 孵化宠物蛋时给宠物起的名
 *   newName  —— 改名卡的新名字
 *   toId     —— 积分红包送给谁
 */
export function apiShopUse(userId, itemId, opts = {}) {
  return post('/use', Object.assign({ userId, itemId }, opts))
}
