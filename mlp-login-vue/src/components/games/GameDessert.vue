<template>
  <div class="game-dessert">
    <!-- HUD -->
    <div class="shop-hud">
      <span>已出餐 {{ served }} 单</span>
      <span v-if="combo >= 2" class="shop-combo">🔥 连对 {{ combo }}</span>
      <span class="shop-timer" :class="{ urgent: left <= 10 }">⏱ {{ left }}s</span>
    </div>

    <!-- 柜台：点单的小马 -->
    <div class="shop-counter">
      <template v-if="customer">
        <div class="shop-customer" :class="{ happy: happy, sad: sad }">
          <div class="shop-order">
            <span class="shop-bubble">我要一份 {{ order.emoji }}<em>{{ order.name }}</em></span>
          </div>
          <div class="shop-pony">{{ customer.emoji }}</div>
          <div class="shop-pname">{{ customer.name }}</div>
        </div>
      </template>
      <div v-else class="shop-empty">小马们都在品尝美味，下一单马上来～</div>
    </div>

    <!-- 甜品台 -->
    <div class="shop-tray">
      <button
        v-for="d in desserts"
        :key="d.emoji"
        class="shop-item"
        :disabled="!customer"
        @click="serve(d)"
      >{{ d.emoji }}<em>{{ d.name }}</em></button>
    </div>

    <div class="shop-tip">💡 碧琪的店规：看清楚订单再动手，连对越多小费越高</div>
  </div>
</template>

<script>
import { DESSERT_PONIES } from '../../courses'

const DURATION = 60
const COMBO_BONUS = 3   // 连对每单额外加分

/** 甜品菜单 */
const DESSERTS = [
  { emoji: '🧁', name: '纸杯蛋糕' },
  { emoji: '🎂', name: '庆典蛋糕' },
  { emoji: '🥧', name: '苹果派' },
  { emoji: '🍩', name: '甜甜圈' },
  { emoji: '🍪', name: '曲奇饼' },
  { emoji: '🍭', name: '棒棒糖' }
]

export default {
  name: 'GameDessert',
  emits: ['score', 'over'],
  data() {
    return {
      desserts: DESSERTS,
      customers: DESSERT_PONIES,
      customer: null,
      order: null,
      score: 0,
      served: 0,
      combo: 0,
      left: DURATION,
      happy: false,
      sad: false,
      timer: null,
      finished: false
    }
  },
  created() {
    this.nextCustomer()
    this.timer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.timeUp()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    nextCustomer() {
      const c = this.customers[Math.floor(Math.random() * this.customers.length)]
      this.customer = c
      this.order = DESSERTS[Math.floor(Math.random() * DESSERTS.length)]
    },
    serve(d) {
      if (!this.customer || this.finished) return
      if (d.emoji === this.order.emoji) {
        this.combo += 1
        this.score += 12 + (this.combo >= 2 ? COMBO_BONUS : 0)
        this.served += 1
        this.happy = true
        this.$emit('score', this.score)
        setTimeout(() => {
          this.happy = false
          this.nextCustomer()
        }, 450)
      } else {
        this.combo = 0
        this.score = Math.max(0, this.score - 5)
        this.sad = true
        this.$emit('score', this.score)
        setTimeout(() => { this.sad = false }, 450)
      }
    },
    timeUp() {
      if (this.finished) return
      this.finished = true
      clearInterval(this.timer)
      this.customer = null
      this.$emit('over', this.score)
    }
  }
}
</script>

<style scoped>
.shop-hud {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.shop-combo { color: var(--primary); font-weight: 700; }
.shop-timer { font-weight: 700; color: var(--primary); }
.shop-timer.urgent { color: var(--danger); }

.shop-counter {
  position: relative; height: 190px;
  background: linear-gradient(180deg, #fff, var(--panel));
  border: 1.5px solid var(--border); border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; margin-bottom: 12px;
}
.shop-empty { font-size: 12.5px; color: var(--muted); }

.shop-customer { text-align: center; }
.shop-customer.happy { animation: animalHappy .45s ease; }
.shop-customer.sad { animation: animalSad .45s ease; }
@keyframes animalHappy { 50% { transform: translateY(-14px) scale(1.12); } }
@keyframes animalSad {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  75% { transform: translateX(8px); }
}

.shop-pony { font-size: 58px; line-height: 1.2; }
.shop-pname {
  font-size: 11px; font-weight: 700; color: var(--text);
  margin-top: 2px;
}
.shop-order { margin-bottom: 4px; }
.shop-bubble {
  position: relative; display: inline-block;
  background: var(--card); border: 1.5px solid var(--border);
  border-radius: 999px; padding: 4px 12px;
  font-size: 12px; font-weight: 700; color: var(--primary);
}
.shop-bubble em {
  font-style: normal; font-weight: 400; color: var(--muted);
  margin-left: 5px; font-size: 11px;
}
.shop-bubble::after {
  content: ""; position: absolute; left: 50%; bottom: -5px;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: var(--border);
}

.shop-tray {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
}
.shop-item {
  border: 1.5px solid var(--border); background: var(--card);
  border-radius: 12px; padding: 10px 2px 7px; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  font-size: 30px; transition: all .15s;
}
.shop-item:hover:not(:disabled) {
  border-color: var(--primary); transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(160,108,213,.15);
}
.shop-item:disabled { opacity: .5; cursor: default; }
.shop-item em { font-style: normal; font-size: 10px; color: var(--muted); }

.shop-tip {
  margin-top: 10px; font-size: 11px; color: var(--muted); text-align: center;
}
</style>
