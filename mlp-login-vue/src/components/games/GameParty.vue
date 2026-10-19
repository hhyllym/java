<template>
  <div class="game-party">
    <!-- HUD -->
    <div class="party-hud">
      <span>已完成 {{ done }} / {{ total }} 场布置</span>
      <span class="party-timer" :class="{ urgent: left <= 15 }">⏱ {{ left }}s</span>
    </div>

    <!-- 订单 -->
    <div class="party-order">
      <div class="party-order-title">📋 本场派对需求</div>
      <div class="party-order-list">
        <span
          v-for="(it, i) in order"
          :key="i"
          class="party-need"
          :class="{ filled: filled.includes(i) }"
        >{{ filled.includes(i) ? '✅' : it.emoji }} {{ it.name }}</span>
      </div>
    </div>

    <!-- 道具栏 -->
    <div class="party-items">
      <button
        v-for="d in decos"
        :key="d.name"
        class="party-deco"
        @click="place(d)"
      >{{ d.emoji }}<em>{{ d.name }}</em></button>
    </div>

    <!-- 反馈 -->
    <div class="party-feedback" :class="fbType">
      <template v-if="fbType === 'good'">🎉 布置完成！本场 +{{ roundScore }} 分</template>
      <template v-else-if="fbType === 'bad'">😅 这个道具不在清单里，-5 分</template>
      <template v-else>💡 点道具栏，把清单上的需求一一摆好</template>
    </div>
  </div>
</template>

<script>
const TOTAL = 5      // 总共 5 场布置
const DURATION = 90  // 总时长

/** 派对道具池 */
const DECOS = [
  { name: '气球拱门', emoji: '🎈' },
  { name: '彩带拉花', emoji: '🎊' },
  { name: '礼物堆', emoji: '🎁' },
  { name: '纸灯笼', emoji: '🏮' },
  { name: '蛋糕台', emoji: '🍰' },
  { name: '蝴蝶结', emoji: '🎀' },
  { name: '烟花棒', emoji: '🎆' },
  { name: '签到墙', emoji: '🖼️' },
  { name: '泡泡机', emoji: '🫧' }
]

export default {
  name: 'GameParty',
  emits: ['score', 'over'],
  data() {
    return {
      decos: DECOS,
      order: [],
      filled: [],
      done: 0,
      total: TOTAL,
      left: DURATION,
      score: 0,
      roundScore: 0,
      fbType: '',   // '' | good | bad
      timer: null,
      finished: false
    }
  },
  created() {
    this.newOrder()
    this.timer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.timeUp()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    /** 生成一张订单：从道具池随机抽 3-4 个 */
    newOrder() {
      const pool = DECOS.slice()
      const count = 3 + Math.floor(Math.random() * 2)
      const order = []
      for (let i = 0; i < count && pool.length; i++) {
        order.push(pool.splice(Math.floor(Math.random() * pool.length), 1)[0])
      }
      this.order = order
      this.filled = []
      this.roundScore = 0
      this.fbType = ''
    },
    place(d) {
      if (this.finished || this.fbType === 'good') return
      const idx = this.order.findIndex((it, i) => it.name === d.name && !this.filled.includes(i))
      if (idx >= 0) {
        this.filled.push(idx)
        // 清单完成 → 结算本场
        if (this.filled.length >= this.order.length) {
          this.roundScore = 20 + this.left * 1   // 剩余时间就是奖励
          this.score += this.roundScore
          this.done += 1
          this.fbType = 'good'
          this.$emit('score', this.score)
          setTimeout(() => {
            if (this.done >= this.total) {
              this.finish()
            } else {
              this.newOrder()
            }
          }, 1100)
        }
      } else {
        this.score = Math.max(0, this.score - 5)
        this.fbType = 'bad'
        this.$emit('score', this.score)
        setTimeout(() => {
          if (this.fbType === 'bad') this.fbType = ''
        }, 900)
      }
    },
    finish() {
      if (this.finished) return
      this.finished = true
      clearInterval(this.timer)
      this.$emit('over', this.score)
    },
    timeUp() {
      if (this.finished) return
      this.finished = true
      clearInterval(this.timer)
      this.$emit('over', this.score)
    }
  }
}
</script>

<style scoped>
.party-hud {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--muted); margin-bottom: 12px;
}
.party-timer { font-weight: 700; color: var(--primary); }
.party-timer.urgent { color: var(--danger); }

.party-order {
  background: var(--panel); border-radius: 14px;
  padding: 13px 14px; margin-bottom: 12px;
}
.party-order-title { font-size: 11.5px; color: var(--muted); margin-bottom: 8px; }
.party-order-list { display: flex; flex-wrap: wrap; gap: 6px; }
.party-need {
  font-size: 12px; font-weight: 700; color: var(--text);
  background: var(--card); border: 1.5px dashed var(--border);
  border-radius: 999px; padding: 5px 12px;
  transition: all .2s;
}
.party-need.filled {
  border-style: solid; border-color: var(--success);
  color: var(--success); background: rgba(76,175,125,.1);
}

.party-items {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
}
.party-deco {
  border: 1.5px solid var(--border); background: var(--card);
  border-radius: 12px; padding: 10px 2px 7px; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  font-size: 26px; transition: all .15s;
}
.party-deco:hover {
  border-color: var(--primary); transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(160,108,213,.15);
}
.party-deco em { font-style: normal; font-size: 9.5px; color: var(--muted); }

.party-feedback {
  margin-top: 12px; font-size: 12.5px; font-weight: 700;
  text-align: center; min-height: 20px; color: var(--muted);
}
.party-feedback.good { color: var(--success); }
.party-feedback.bad { color: var(--danger); }
</style>
