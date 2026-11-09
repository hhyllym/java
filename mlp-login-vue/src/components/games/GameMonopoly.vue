<template>
  <div class="mp-shell">
    <div class="mp-tabs">
      <button :class="{ on: tab === 'online' }" @click="switchTab('online')">🌐 四人联机（各用各的电脑）</button>
      <button :class="{ on: tab === 'local' }" @click="switchTab('local')">🤖 本地练习（同屏 / AI）</button>
    </div>
    <p class="mp-tab-tip">
      {{ tab === 'online'
        ? '四个同学各用自己的电脑或手机，连同一个 WiFi，进同一个房间号；每人需 45 可用积分上桌，终局按名次结算，赢的积分直接进账号、马上能花。'
        : '一台电脑上四人同桌轮流掷骰，空位可交给电脑托管；练习模式不消耗积分。想用积分玩真的，切到「四人联机」。' }}
    </p>
    <MonopolyOnline v-if="tab === 'online'" :key="'online-' + onlineKey" @score="onScore" @over="onOver" />
    <MonopolyLocal v-else :key="'local-' + localKey" @score="onScore" @over="onOver" />
  </div>
</template>

<script>
import MonopolyOnline from './MonopolyOnline.vue'
import MonopolyLocal from './MonopolyLocal.vue'
import sound from '../../sound'

/** 🎲 糖霜镇大富翁入口：联机四人 / 本地同桌二选一，统一向上抛 score / over */
export default {
  name: 'GameMonopoly',
  components: { MonopolyOnline, MonopolyLocal },
  emits: ['score', 'over'],
  data() {
    return {
      tab: 'online',
      onlineKey: 0,
      localKey: 0
    }
  },
  methods: {
    switchTab(t) {
      if (this.tab === t) return
      sound.click()
      this.tab = t
      if (t === 'online') this.onlineKey += 1
      else this.localKey += 1
      this.$emit('score', 0)
    },
    onScore(v) { this.$emit('score', v) },
    onOver(v) { this.$emit('over', v) }
  }
}
</script>

<style scoped>
.mp-shell { display: flex; flex-direction: column; gap: 10px; }
.mp-tabs { display: flex; gap: 8px; }
.mp-tabs button {
  flex: 1; padding: 9px 8px; border-radius: 12px; cursor: pointer;
  font-size: 11.5px; font-weight: 800; color: var(--muted);
  background: var(--card); border: 1.5px solid var(--border);
  transition: filter .12s;
}
.mp-tabs button:hover { filter: brightness(.97); }
.mp-tabs button.on {
  color: #fff; border-color: transparent;
  background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160, 108, 213, .24);
}
.mp-tab-tip { margin: 0; font-size: 11px; color: var(--muted); line-height: 1.6; }
</style>
