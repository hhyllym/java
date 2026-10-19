<template>
  <div class="game-twilight">
    <!-- 进度 + 倒计时 -->
    <div class="quiz-hud">
      <span>第 {{ idx + 1 }} / {{ total }} 题</span>
      <span v-if="combo >= 2" class="quiz-combo">🔥 连对 {{ combo }}</span>
      <span class="quiz-timer" :class="{ urgent: left <= 5 }">⏱ {{ left }}s</span>
    </div>
    <div class="quiz-progress"><i :style="{ width: ((idx) / total) * 100 + '%' }"></i></div>

    <!-- 题目 -->
    <div v-if="q" class="quiz-stage" :class="{ right: flash === 'right', wrong: flash === 'wrong' }">
      <div class="quiz-q-text">{{ q.q }}</div>
      <div class="quiz-opts">
        <button
          v-for="(o, i) in q.options"
          :key="i"
          class="quiz-opt-btn"
          :class="optClass(i)"
          :disabled="locked"
          @click="pick(i)"
        >
          <em>{{ ['A', 'B', 'C', 'D'][i] }}</em>
          <span>{{ o }}</span>
        </button>
      </div>
      <div v-if="locked" class="quiz-feedback">
        <template v-if="flash === 'right'">✨ 答对了！+20 分{{ combo >= 2 ? '（连对 +5）' : '' }}</template>
        <template v-else>💡 正确答案：{{ q.options[q.answer] }}</template>
      </div>
      <div v-if="flash === 'right'" class="quiz-stars">
        <span v-for="s in 6" :key="s" :style="{ left: 12 + s * 13 + '%', animationDelay: s * 0.06 + 's' }">✨</span>
      </div>
    </div>
  </div>
</template>

<script>
import { QUIZ_BANK } from '../../courses'

const PER_QUESTION = 15   // 每题秒数
const TOTAL = 5           // 每局题数
const BASE = 20           // 答对基础分
const COMBO = 5           // 连对奖励

export default {
  name: 'GameTwilight',
  emits: ['score', 'over'],
  data() {
    return {
      idx: 0,
      total: TOTAL,
      combo: 0,
      left: PER_QUESTION,
      score: 0,
      locked: false,
      flash: '',
      questions: this.pickQuestions(),
      timer: null
    }
  },
  computed: {
    q() {
      return this.questions[this.idx] || null
    }
  },
  created() {
    this.startTimer()
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    /** 从题库随机抽 TOTAL 题 */
    pickQuestions() {
      const arr = QUIZ_BANK.slice()
      const out = []
      while (out.length < TOTAL && arr.length) {
        out.push(arr.splice(Math.floor(Math.random() * arr.length), 1)[0])
      }
      return out
    },
    startTimer() {
      clearInterval(this.timer)
      this.left = PER_QUESTION
      this.timer = setInterval(() => {
        this.left -= 1
        if (this.left <= 0) {
          this.timeUp()
        }
      }, 1000)
    },
    timeUp() {
      if (this.locked) return
      this.locked = true
      this.flash = 'wrong'
      this.combo = 0
      clearInterval(this.timer)
      this.$emit('score', this.score)
      setTimeout(this.next, 1400)
    },
    optClass(i) {
      if (!this.locked) return ''
      if (i === this.q.answer) return 'right'
      return 'dim'
    },
    pick(i) {
      if (this.locked) return
      this.locked = true
      clearInterval(this.timer)
      if (i === this.q.answer) {
        this.combo += 1
        this.score += BASE + (this.combo >= 2 ? COMBO : 0)
        this.flash = 'right'
      } else {
        this.combo = 0
        this.flash = 'wrong'
      }
      this.$emit('score', this.score)
      setTimeout(this.next, 1300)
    },
    next() {
      this.locked = false
      this.flash = ''
      if (this.idx + 1 >= this.total) {
        this.$emit('over', this.score)
        return
      }
      this.idx += 1
      this.startTimer()
    }
  }
}
</script>

<style scoped>
.quiz-hud {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 8px;
}
.quiz-combo { color: var(--primary); font-weight: 700; }
.quiz-timer { font-weight: 700; color: var(--primary); }
.quiz-timer.urgent { color: var(--danger); animation: pulse .8s infinite; }
@keyframes pulse { 50% { opacity: .5; } }

.quiz-progress {
  height: 5px; border-radius: 4px; background: var(--track);
  overflow: hidden; margin-bottom: 16px;
}
.quiz-progress i {
  display: block; height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, var(--pink), var(--purple));
  transition: width .3s ease;
}

.quiz-stage {
  position: relative;
  background: var(--panel); border-radius: 14px;
  padding: 16px 14px; min-height: 260px;
  transition: background .2s;
}
.quiz-stage.right { background: rgba(76,175,125,.12); }
.quiz-stage.wrong { background: rgba(224,102,102,.1); }

.quiz-q-text {
  font-size: 14.5px; font-weight: 700; line-height: 1.6;
  margin-bottom: 14px;
}

.quiz-opts { display: flex; flex-direction: column; gap: 8px; }
.quiz-opt-btn {
  display: flex; align-items: center; gap: 10px; text-align: left;
  border: 1.5px solid var(--border); border-radius: 11px;
  background: var(--card); color: var(--text);
  font-size: 13px; padding: 10px 12px; cursor: pointer;
  transition: all .15s;
}
.quiz-opt-btn:hover:not(:disabled) {
  border-color: var(--primary); color: var(--primary);
  transform: translateX(3px);
}
.quiz-opt-btn:disabled { cursor: default; }
.quiz-opt-btn em {
  font-style: normal; width: 20px; height: 20px; flex-shrink: 0;
  border-radius: 50%; background: var(--panel);
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; color: var(--primary);
}
.quiz-opt-btn.right {
  border-color: var(--success); color: var(--success); font-weight: 700;
  background: rgba(76,175,125,.08);
}
.quiz-opt-btn.right em { background: var(--success); color: #fff; }
.quiz-opt-btn.dim { opacity: .5; }

.quiz-feedback {
  margin-top: 12px; font-size: 12.5px; font-weight: 700;
  text-align: center; color: var(--danger);
}
.quiz-stage.right .quiz-feedback { color: var(--success); }

/* 答对的星星粒子 */
.quiz-stars { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.quiz-stars span {
  position: absolute; top: 30%; font-size: 18px;
  animation: starFly 1s ease-out forwards;
}
@keyframes starFly {
  to { transform: translateY(-46px) scale(1.4) rotate(30deg); opacity: 0; }
}
</style>
