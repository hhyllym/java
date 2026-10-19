/**
 * 宠物 API：查看、领养、喂食、玩耍、改名
 */

async function get(path) {
  try {
    const res = await fetch('/api/pet' + path)
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

async function post(path, body) {
  try {
    const res = await fetch('/api/pet' + path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
    return await res.json()
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务', data: null }
  }
}

/** 我的宠物（没有则 data 为 null） */
export function apiPetMine(userId) {
  return get('/mine?userId=' + userId)
}

/** 领养 */
export function apiPetAdopt(userId, kind, name) {
  return post('/adopt', { userId, kind, name })
}

/** 喂食 */
export function apiPetFeed(userId) {
  return post('/feed', { userId })
}

/** 玩耍 */
export function apiPetPlay(userId) {
  return post('/play', { userId })
}

/** 改名 */
export function apiPetRename(userId, name) {
  return post('/rename', { userId, name })
}

/** 我的衣橱：拥有的装扮，data: [{ id, name, icon, count, equipped }] */
export function apiPetCosmetics(userId) {
  return get('/cosmetics?userId=' + userId)
}

/** 装备装扮（会卸下其它，只能戴一件） */
export function apiPetCosmeticEquip(userId, cosmeticId) {
  return post('/cosmetic/equip', { userId, cosmeticId })
}

/** 卸下当前装扮 */
export function apiPetCosmeticUnequip(userId) {
  return post('/cosmetic/unequip', { userId })
}
