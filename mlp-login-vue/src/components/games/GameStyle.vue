<template>
  <div class="game-style">
    <!-- HUD -->
    <div class="style-hud">
      <span>第 {{ idx + 1 }} / {{ total }} 场</span>
      <span>已搭 {{ pickedOk }} / {{ needCount }} 件</span>
      <span class="style-timer" :class="{ urgent: left <= 15 }">⏱ {{ left }}s</span>
    </div>

    <!-- 场合 + 评委 -->
    <div class="style-scene">
      <div class="style-occ">{{ occasion.name }}</div>
      <div class="style-hint">💡 {{ occasion.hint }}</div>
      <div class="style-judge">
        <span class="style-judge-face">{{ judge.emoji }}</span>
        <span class="style-judge-name">{{ judge.name }}</span>
        <span class="style-judge-say">{{ judgeSays }}</span>
      </div>
    </div>

    <!-- 衣柜 -->
    <div class="style-wardrobe">
      <button
        v-for="(w, i) in wardrobe"
        :key="i"
        class="style-item"
        :class="{ picked: picked.includes(i), right: revealed.includes(i) && w.ok, wrong: shake === i }"
        :disabled="revealed.includes(i)"
        @click="pick(i)"
      >
        <span class="style-emoji">{{ w.emoji }}</span>
        <em>{{ w.name }}</em>
      </button>
    </div>

    <!-- 回合反馈 -->
    <div v-if="fb" class="style-fb" :class="fb.type">
      {{ fb.type === 'good' ? '✨ 搭配成功！' : '💡 这件不适合这个场合' }}
      {{ fb.extra }}
    </div>
  </div>
</template>

<script>
import { FASHION_PONIES } from '../../courses'

const TOTAL = 5
const DURATION = 90
const NEED = 3        // 每场需要搭对的件数

/** 场合：need 是衣柜单品的适用标签 */
const OCCASIONS = [
  { name: '皇家晚宴', hint: '庄重华丽，裙摆要长，配饰要闪', need: '晚宴' },
  { name: '阳光运动会', hint: '透气有弹性，怎么跑都不怕', need: '运动' },
  { name: '森林野餐', hint: '棉麻质地，记得防晒遮阳', need: '野餐' },
  { name: '加冕典礼', hint: '全场最隆重的装扮，气场拉满', need: '典礼' },
  { name: '海边度假', hint: '轻薄凉爽，墨镜必备', need: '度假' }
]

/** 衣柜：tags 说明这件单品适合哪些场合 */
const WARDROBE = [
  { emoji: '👗', name: '晚礼服', tags: ['晚宴', '典礼'] },
  { emoji: '👠', name: '高跟鞋', tags: ['晚宴', '典礼'] },
  { emoji: '👜', name: '手包', tags: ['晚宴', '典礼'] },
  { emoji: '💎', name: '宝石胸针', tags: ['晚宴', '典礼'] },
  { emoji: '👑', name: '小皇冠', tags: ['典礼'] },
  { emoji: '🧣', name: '丝巾', tags: ['典礼', '野餐'] },
  { emoji: '🎽', name: '运动衫', tags: ['运动'] },
  { emoji: '🩳', name: '运动短裤', tags: ['运动'] },
  { emoji: '🧢', name: '棒球帽', tags: '运动' },
  { emoji: '👒', name: '遮阳帽', tags: ['野餐', '度假'] },
  { emoji: '🌂', name: '蕾丝阳伞', tags: ['野餐', '度假'] },
  { emoji: '🕶️', name: '太阳镜', tags: ['度假'] },
  { emoji: '👘', name: '度假长裙', tags: ['度假'] },
  { emoji: '🥿', name: '平底鞋', tags: ['野餐', '运动'] }
]

const PRAISES = [
  '这件选得妙，亲爱的！',
  '品味不错嘛～',
  '嗯，还算入得了眼。',
  '完美！出场就是焦点！',
  '看来你很有天赋。'
]
const BOOS = [
  '亲爱的，这场合穿这个？',
  '太寻常了…再想想。',
  '唔，品味有待提升。'
]

export default {
  name: 'GameStyle',
  emits: ['score', 'over'],
  data() {
    return {
      idx: 0,
      total: TOTAL,
      needCount: NEED,
      left: DURATION,
      score: 0,
      occasion: OCCASIONS[0],
      judge: FASHION_PONIES[0],
      judgeSays: '按场合搭配，选够 3 件就过关～',
      wardrobe: [],
      picked: [],
      pickedOk: 0,
      revealed: [],
      shake: -1,
      fb: null,
      timer: null,
      finished: false
    }
  },
  created() {
    this.buildRound()
    this.timer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.timeUp()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    /** 组一场：3 件合适的 + 6 件不合适的，打乱 */
    buildRound() {
      const occ = OCCASIONS[this.idx % OCCASIONS.length]
      this.occasion = occ
      // 评委轮换（从时尚伙伴里挑，含珍奇本人）
      this.judge = FASHION_PONIES[Math.floor(Math.random() * FASHION_PONIES.length)]
      this.judgeSays = '按场合搭配，选够 3 件就过关～'

      const ok = WARDROBE.filter(w => w.tags.includes(occ.need))
      const no = WARDROBE.filter(w => !w.tags.includes(occ.need))
      const picked = []
      while (picked.length < NEED && ok.length) {
        picked.push({ ...ok.splice(Math.floor(Math.random() * ok.length), 1)[0], ok: true })
      }
      while (picked.length < 9 && no.length) {
        picked.push({ ...no.splice(Math.floor(Math.random() * no.length), 1)[0], ok: false })
      }
      for (let i = picked.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        ;[picked[i], picked[j]] = [picked[j], picked[i]]
      }
      this.wardrobe = picked
      this.picked = []
      this.pickedOk = 0
      this.revealed = []
      this.shake = -1
      this.fb = null
    },
    pick(i) {
      if (this.finished || this.revealed.includes(i)) return
      const w = this.wardrobe[i]
      if (w.ok) {
        this.score += 15
        this.pickedOk += 1
        this.revealed.push(i)
        this.fb = { type: 'good', extra: '+15 分' }
        this.$emit('score', this.score)
        if (this.pickedOk >= this.needCount) {
          this.judgeSays = PRAISES[Math.floor(Math.random() * PRAISES.length)]
          setTimeout(() => {
            if (this.idx + 1 >= this.total) {
              this.finish()
            } else {
              this.idx += 1
              this.buildRound()
            }
          }, 1000)
        }
      } else {
        this.score = Math.max(0, this.score - 8)
        this.shake = i
        this.fb = { type: 'bad', extra: '-8 分' }
        this.$emit('score', this.score)
        setTimeout(() => { this.shake = -1 }, 450)
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
.style-hud {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: var(--muted); margin-bottom: 12px;
}
.style-timer { font-weight: 700; color: var(--primary); }
.style-timer.urgent { color: var(--danger); }

.style-scene {
  text-align: center; padding: 14px;
  background: var(--panel); border-radius: 14px; margin-bottom: 12px;
}
.style-occ {
  font-size: 18px; font-weight: 800; color: var(--primary);
  margin-bottom: 4px;
}
.style-hint { font-size: 11.5px; color: var(--muted); margin-bottom: 10px; }

.style-judge {
  display: inline-flex; align-items: center; gap: 8px;
  background: var(--card); border: 1.5px solid var(--border);
  border-radius: 999px; padding: 5px 14px 5px 6px;
}
.style-judge-face { font-size: 24px; }
.style-judge-name { font-size: 12px; font-weight: 700; color: var(--text); }
.style-judge-say { font-size: 11px; color: var(--muted); }

.style-wardrobe {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
}
.style-item {
  border: 1.5px solid var(--border); background: var(--card);
  border-radius: 12px; padding: 10px 2px 7px; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  transition: all .15s;
}
.style-item:hover:not(:disabled) {
  border-color: var(--primary); transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(160,108,213,.15);
}
.style-item:disabled { cursor: default; }
.style-emoji { font-size: 30px; line-height: 1.1; }
.style-item em { font-style: normal; font-size: 10px; color: var(--muted); }

.style-item.right {
  border-color: var(--success);
  background: rgba(76,175,125,.1);
  animation: matchPop .3s ease;
}
.style-item.right em { color: var(--success); font-weight: 700; }
.style-item.wrong { animation: animalSad .45s ease; border-color: var(--danger); }
@keyframes matchPop { 50% { transform: scale(1.12); } }

.style-fb {
  margin-top: 12px; font-size: 12.5px; font-weight: 700;
  text-align: center; min-height: 20px; color: var(--muted);
}
.style-fb.good { color: var(--success); }
.style-fb.bad { color: var(--danger); }
</style>
