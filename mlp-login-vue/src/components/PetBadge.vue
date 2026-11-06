<template>
  <div class="pet-badge" :class="{ compact }" v-if="ready">
    <!-- 已有宠物：点一下去宠物页照顾它 -->
    <div v-if="pet" class="pb-card" @click="goPet" title="点我去照顾宠物">
      <div class="pb-face" :class="{ hungry: pet.hunger < 30 }">
        <span>{{ (pet.kindInfo && pet.kindInfo.emoji) || '🐾' }}</span>
        <span v-if="pet.cosmetic" class="pb-cosmetic" :title="pet.cosmetic.name">{{ pet.cosmetic.icon }}</span>
      </div>
      <div class="pb-main">
        <p class="pb-name"><b>{{ pet.name }}</b><span class="pb-lv">Lv.{{ pet.level }}</span></p>
        <div v-if="!compact" class="pb-stats">
          <span>🍖 饱食 {{ pet.hunger }}</span>
          <span>💖 心情 {{ pet.mood }}</span>
        </div>
        <p v-else class="pb-sub">点我去照顾它 🐾</p>
      </div>
    </div>

    <!-- 还没有宠物：引导去领养 -->
    <div v-else class="pb-empty" @click="goPet">
      <span class="pb-egg">🥚</span>
      <div class="pb-main">
        <p class="pb-name"><b>还没有宠物</b></p>
        <p v-if="!compact" class="pb-sub">领养一只小家伙，陪你一起冒险</p>
        <p v-else class="pb-sub">去领养 🐾</p>
      </div>
      <button class="pb-adopt">领养</button>
    </div>
  </div>
</template>

<script>
import { getSession } from '../auth'
import { apiPetMine } from '../pet'

/**
 * 我的宠物小部件：领养之后会出现在 主页 / 个人主页 / 聊天界面 三处。
 * 自己拉自己的宠物数据，调用方无需传数据。
 *  - compact=false（默认）：完整卡片，带 饱食/心情 状态，用于主页与个人主页
 *  - compact=true：紧凑小条，用于聊天界面顶部
 */
export default {
  name: 'PetBadge',
  props: {
    compact: { type: Boolean, default: false }
  },
  data() {
    return {
      pet: null,
      ready: false
    }
  },
  created() {
    const s = getSession()
    if (!s || !s.id) { this.ready = true; return }
    apiPetMine(s.id)
      .then(r => { if (r.code === 200) this.pet = r.data })
      .catch(() => {})
      .finally(() => { this.ready = true })
  },
  methods: {
    goPet() {
      this.$router.push({ name: 'pet' })
    }
  }
}
</script>

<style scoped>
.pet-badge { margin: 12px 0; }
.pb-card, .pb-empty {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 16px;
  background: var(--panel, #f6eefb);
  border: 1.5px solid var(--border, #eadcff);
  cursor: pointer; transition: transform .15s, box-shadow .15s;
  text-align: left;
}
.pb-card:hover, .pb-empty:hover { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(160, 108, 213, .18); }
.pb-face {
  position: relative; width: 60px; height: 60px; flex-shrink: 0;
  border-radius: 16px; background: var(--card, #fff);
  display: flex; align-items: center; justify-content: center;
  font-size: 34px; animation: pbBounce 2.6s ease-in-out infinite;
}
.pb-face.hungry { filter: grayscale(.55); }
@keyframes pbBounce { 0%, 100% { transform: translateY(0) rotate(-2deg); } 50% { transform: translateY(-5px) rotate(2deg); } }
.pb-cosmetic {
  position: absolute; top: -8px; right: -6px; font-size: 18px;
  filter: drop-shadow(0 2px 3px rgba(160, 108, 213, .4));
  animation: pbBounce 2s ease-in-out infinite;
}
.pb-main { flex: 1; min-width: 0; }
.pb-name { margin: 0 0 4px; font-size: 15px; }
.pb-lv {
  font-size: 10.5px; color: #fff; margin-left: 6px;
  background: var(--primary, #a06cd5); border-radius: 999px; padding: 1px 8px;
}
.pb-stats { display: flex; gap: 10px; font-size: 11.5px; color: var(--muted, #8a7ca8); }
.pb-sub { margin: 0; font-size: 11.5px; color: var(--muted, #8a7ca8); }
.pb-egg { font-size: 34px; flex-shrink: 0; }
.pb-adopt {
  flex-shrink: 0; border: none; cursor: pointer; border-radius: 999px;
  padding: 8px 16px; font-size: 12.5px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, var(--pink, #ff9ec6), var(--purple, #b58bff));
}

/* 紧凑模式：聊天界面顶部一条 */
.pet-badge.compact { margin: 8px 12px; }
.compact .pb-card, .compact .pb-empty { padding: 6px 12px; border-radius: 999px; gap: 8px; }
.compact .pb-face { width: 34px; height: 34px; font-size: 20px; animation: none; }
.compact .pb-name { font-size: 13px; margin: 0; }
</style>
