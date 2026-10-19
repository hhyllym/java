<template>
  <div class="game-fluttershy">
    <!-- 得分 + 倒计时 -->
    <div class="forest-hud">
      <span>已喂食 {{ fed }} 只</span>
      <span class="forest-timer" :class="{ urgent: left <= 10 }">⏱ {{ left }}s</span>
    </div>

    <!-- 小动物登场 -->
    <div class="forest-stage">
      <div v-if="animal" class="forest-animal" :class="{ happy: happy, sad: sad }">
        <div class="forest-want">
          <span class="forest-bubble">想吃 {{ animal.food }}</span>
        </div>
        <div class="forest-body">{{ animal.emoji }}</div>
      </div>
      <div v-else class="forest-empty">小动物们吃饱啦，正在换下一只…</div>
    </div>

    <!-- 食物按钮 -->
    <div class="forest-foods">
      <button
        v-for="f in foods"
        :key="f.emoji"
        class="forest-food"
        :disabled="!animal"
        @click="feed(f)"
      >{{ f.emoji }}<em>{{ f.name }}</em></button>
    </div>

    <div class="forest-tip">💡 柔柔说：点对了小动物会很开心，点错了它会难过的</div>
  </div>
</template>

<script>
const DURATION = 60

const ANIMALS = [
  { emoji: '🐰', food: '🥕' },
  { emoji: '🦊', food: '🌰' },
  { emoji: '🐿️', food: '🍄' },
  { emoji: '🐦', food: '🐟' },
  { emoji: '🦔', food: '🫐' },
  { emoji: '🐭', food: '🧀' }
]

const FOODS = [
  { emoji: '🥕', name: '胡萝卜' },
  { emoji: '🌰', name: '坚果' },
  { emoji: '🍄', name: '蘑菇' },
  { emoji: '🐟', name: '小鱼' },
  { emoji: '🫐', name: '蓝莓' },
  { emoji: '🧀', name: '奶酪' }
]

export default {
  name: 'GameFluttershy',
  emits: ['score', 'over'],
  data() {
    return {
      animal: null,
      foods: FOODS,
      score: 0,
      fed: 0,
      left: DURATION,
      happy: false,
      sad: false,
      timer: null,
      queue: [],
      finished: false
    }
  },
  created() {
    this.queue = this.shuffle(ANIMALS.slice())
    this.nextAnimal()
    this.timer = setInterval(() => {
      this.left -= 1
      if (this.left <= 0) this.timeUp()
    }, 1000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    shuffle(arr) {
      for (let i = arr.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        ;[arr[i], arr[j]] = [arr[j], arr[i]]
      }
      return arr
    },
    nextAnimal() {
      if (!this.queue.length) {
        this.queue = this.shuffle(ANIMALS.slice())
      }
      this.animal = this.queue.pop()
    },
    feed(f) {
      if (!this.animal || this.finished) return
      if (f.emoji === this.animal.food) {
        this.score += 12
        this.fed += 1
        this.happy = true
        this.$emit('score', this.score)
        setTimeout(() => {
          this.happy = false
          this.nextAnimal()
        }, 450)
      } else {
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
      this.animal = null
      this.$emit('over', this.score)
    }
  }
}
</script>

<style scoped>
.forest-hud {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--muted); margin-bottom: 10px;
}
.forest-timer { font-weight: 700; color: var(--primary); }
.forest-timer.urgent { color: var(--danger); }

.forest-stage {
  position: relative; height: 180px;
  background: linear-gradient(180deg, #fff, var(--panel));
  border: 1.5px solid var(--border); border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; margin-bottom: 12px;
}
.forest-empty { font-size: 12.5px; color: var(--muted); }

.forest-animal {
  text-align: center; transition: transform .2s;
}
.forest-animal.happy { animation: animalHappy .45s ease; }
.forest-animal.sad { animation: animalSad .45s ease; }
@keyframes animalHappy {
  50% { transform: translateY(-16px) scale(1.15); }
}
@keyframes animalSad {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  75% { transform: translateX(8px); }
}

.forest-body { font-size: 62px; line-height: 1.2; }
.forest-want { margin-bottom: 4px; }
.forest-bubble {
  position: relative; display: inline-block;
  background: var(--card); border: 1.5px solid var(--border);
  border-radius: 999px; padding: 4px 12px;
  font-size: 12px; font-weight: 700; color: var(--primary);
}
.forest-bubble::after {
  content: ""; position: absolute; left: 50%; bottom: -5px;
  transform: translateX(-50%);
  border: 5px solid transparent;
  border-top-color: var(--border);
}

.forest-foods {
  display: grid; grid-template-columns: repeat(6, 1fr); gap: 6px;
}
.forest-food {
  border: 1.5px solid var(--border); background: var(--card);
  border-radius: 12px; padding: 8px 2px 6px; cursor: pointer;
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  font-size: 22px; transition: all .15s;
}
.forest-food:hover:not(:disabled) {
  border-color: var(--primary); transform: translateY(-3px);
}
.forest-food:disabled { opacity: .5; cursor: default; }
.forest-food em {
  font-style: normal; font-size: 9px; color: var(--muted);
}

.forest-tip {
  margin-top: 10px; font-size: 11px; color: var(--muted); text-align: center;
}
</style>
