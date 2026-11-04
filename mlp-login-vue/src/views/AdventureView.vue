<template>
  <div class="adventure">
    <!-- 顶部 -->
    <div class="adv-head">
      <router-link to="/home" class="adv-back" title="回首页">«</router-link>
      <div class="adv-title">
        <b>🗺️ 探险营地</b>
        <p>九位小马各有一款拿手游戏，闯关赢积分！</p>
      </div>
    </div>

    <!-- 游戏卡片 -->
    <div class="adv-grid">
      <button
        v-for="g in GAMES"
        :key="g.id"
        class="adv-card"
        :style="{ '--pc': PONIES[g.pony].color }"
        @click="openGame(g)"
      >
        <span class="adv-face">
          <CutieMark :mark="PONIES[g.pony].mark" />
        </span>
        <span class="adv-name">{{ g.name }}</span>
        <span class="adv-teacher">{{ PONIES[g.pony].emoji }} {{ PONIES[g.pony].name }}出品</span>
        <span class="adv-rule">{{ g.rule }}</span>
        <span class="adv-best" v-if="best(g.id) > 0">🏆 最佳 {{ best(g.id) }} 分</span>
        <span class="adv-go">去探险 →</span>
      </button>
    </div>
  </div>
</template>

<script>
import CutieMark from '../components/CutieMark.vue'
import { GAMES, PONIES } from '../courses'
import { gameBest } from '../learn'

export default {
  name: 'AdventureView',
  components: { CutieMark },
  computed: {
    GAMES: () => GAMES,
    PONIES: () => PONIES
  },
  methods: {
    openGame(g) {
      this.$router.push({ name: 'game', params: { id: g.id } })
    },
    best(id) {
      return gameBest(id)
    }
  }
}
</script>

<style scoped>
.adv-head {
  display: flex; align-items: center; gap: 12px; margin-bottom: 16px;
}
.adv-back {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--card); border: 1.5px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: var(--muted); text-decoration: none;
  transition: all .15s;
}
.adv-back:hover { color: var(--primary); border-color: var(--primary); }
.adv-title b { font-size: 17px; }
.adv-title p { margin: 2px 0 0; font-size: 12px; color: var(--muted); }

.adv-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.adv-card {
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  padding: 18px 14px 14px; border-radius: 16px;
  background: var(--card); border: 1.5px solid var(--border);
  cursor: pointer; text-align: center;
  transition: all .15s; position: relative;
}
.adv-card:hover {
  border-color: var(--pc, var(--primary));
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(160,108,213,.14);
}
.adv-face {
  width: 52px; height: 52px; border-radius: 50%;
  background: color-mix(in srgb, var(--pc, var(--primary)) 18%, #fff);
  display: flex; align-items: center; justify-content: center;
}
.adv-name { font-size: 14.5px; font-weight: 800; color: var(--text); }
.adv-teacher { font-size: 11.5px; color: var(--pc, var(--primary)); font-weight: 700; }
.adv-rule {
  font-size: 11px; color: var(--muted); line-height: 1.5;
  min-height: 33px;
}
.adv-best { font-size: 11px; color: #d99a26; font-weight: 700; }
.adv-go {
  margin-top: 2px; font-size: 11.5px; font-weight: 800; color: #fff;
  padding: 4px 14px; border-radius: 999px;
  background: linear-gradient(135deg, var(--pc, var(--pink)), var(--purple));
}
</style>
