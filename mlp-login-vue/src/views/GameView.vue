<template>
  <div class="home game-page">
    <!-- 游戏不存在 -->
    <div v-if="!game" class="empty">
      没有这个游戏 🥺 <router-link :to="fromHub ? '/games' : '/learn'" class="back-link">回{{ fromHub ? '游戏中心' : '学院' }}看看</router-link>
    </div>

    <template v-else>
      <!-- 顶部 HUD -->
      <div class="game-top">
        <button class="chat-back" title="返回学院" @click="goBack">‹</button>
        <span class="game-hud-face" :style="{ '--pc': PONIES[game.pony].color }">
          <CutieMark :mark="PONIES[game.pony].mark" />
        </span>
        <div class="home-greet">
          <h2>{{ game.icon }} {{ game.name }}</h2>
          <p>{{ PONIES[game.pony].name }}出品 · {{ game.rule }}</p>
        </div>
        <div class="game-score">
          <b>{{ score }}</b>
          <span>得分</span>
        </div>
      </div>

      <!-- 历史最佳 + 积分说明 -->
      <div class="game-best-bar" v-if="best > 0">🏆 历史最佳 {{ best }} 分 · 破纪录超出的分数会变成积分存入账户 💎</div>
      <div class="game-best-bar" v-else>💎 破纪录超出的分数会直接变成积分，存入你的账户</div>

      <!-- 游戏本体 -->
      <component :is="gameComp" :key="round" @score="onScore" @over="onOver" />

      <!-- 结算弹窗 -->
      <Teleport to="body">
        <div v-if="result" class="sheet-mask" @click.self="result = null">
          <div class="sheet result-sheet">
            <div class="result-emoji">{{ result.accountReward > 0 ? '🎉' : '💪' }}</div>
            <h3 class="result-title">{{ resultTitle }}</h3>
            <div class="result-score">{{ result.score }} 分</div>
            <p class="result-detail">
              <template v-if="result.accountReward > 0">🏆 新纪录！积分 <b>+{{ result.accountReward }}</b> 已存入账户 💎</template>
              <template v-else>这次没破纪录，去冲个更高的分数就能领积分啦～</template>
            </p>
            <p class="result-best">历史最佳：{{ result.best }} 分</p>
            <div class="result-btns">
              <button class="result-btn main" @click="replay">🔁 再玩一次</button>
              <button class="result-btn ghost" @click="openRank">🏆 排行榜</button>
              <button class="result-btn ghost" @click="openVs">⚔️ 好友对战</button>
              <button class="result-btn ghost" @click="goBack">{{ fromHub ? '🎮 返回游戏中心' : '🎓 返回学院' }}</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 排行榜弹窗 -->
      <Teleport to="body">
        <div v-if="rankOpen" class="sheet-mask" @click.self="rankOpen = false">
          <div class="sheet rank-sheet">
            <h3>🏆 {{ game.name }} · 排行榜</h3>
            <p class="sheet-tip">服务器前 20 名，看看谁是最强大脑</p>
            <div class="rank-list">
              <div v-for="(r, i) in rankList" :key="i" class="rank-row" :class="{ me: r.userId === session.id }">
                <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
                <span class="rank-face">{{ r.avatar || '🦄' }}</span>
                <b>{{ r.name }}</b>
                <em>{{ r.score }}</em>
              </div>
              <div v-if="!rankList.length" class="sheet-tip">还没有人上榜，快去抢第一！</div>
            </div>
            <div class="sheet-btns">
              <button class="ghost" @click="rankOpen = false">关闭</button>
            </div>
          </div>
        </div>
      </Teleport>

      <!-- 好友对战弹窗 -->
      <Teleport to="body">
        <div v-if="vsOpen" class="sheet-mask" @click.self="vsOpen = false">
          <div class="sheet vs-sheet">
            <template v-if="!vsResult">
              <h3>⚔️ 好友对战 · {{ game.name }}</h3>
              <p class="sheet-tip">选一位好友，比比谁的最高分高</p>
              <div class="vs-friends">
                <button
                  v-for="f in vsFriends"
                  :key="f.id"
                  @click="doVs(f)"
                >{{ f.avatar || '🦄' }} {{ f.name }}</button>
                <div v-if="!vsFriends.length" class="sheet-tip">还没有好友，去加一个再来 PK 吧</div>
              </div>
              <div class="sheet-btns">
                <button class="ghost" @click="vsOpen = false">取消</button>
              </div>
            </template>
            <template v-else>
              <h3>⚔️ 对战结果</h3>
              <div class="vs-board">
                <div class="vs-side" :class="{ win: vsResult.winner === 'me' }">
                  <span class="vs-face">{{ vsResult.me.avatar || '🦄' }}</span>
                  <b>{{ vsResult.me.name }}</b>
                  <em>{{ vsResult.me.best }} 分</em>
                </div>
                <div class="vs-mid">{{ vsResult.winner === 'me' ? '我赢！' : vsResult.winner === 'friend' ? '好友赢' : '平局' }}</div>
                <div class="vs-side" :class="{ win: vsResult.winner === 'friend' }">
                  <span class="vs-face">{{ vsResult.friend.avatar || '🦄' }}</span>
                  <b>{{ vsResult.friend.name }}</b>
                  <em>{{ vsResult.friend.best }} 分</em>
                </div>
              </div>
              <div class="sheet-btns">
                <button class="ghost" @click="vsResult = null">换个人</button>
                <button class="ghost" @click="vsOpen = false">关闭</button>
              </div>
            </template>
          </div>
        </div>
      </Teleport>
    </template>
  </div>
</template>

<script>
import CutieMark from '../components/CutieMark.vue'
import { GAMES, PONIES } from '../courses'
import { gameBest, settleGame } from '../learn'
import { apiGameScore, apiGameRank, apiGameVs } from '../gameScore'
import { apiFriendList } from '../friend'
import { getSession } from '../auth'
import { bump } from '../quest'
import GameStarQuiz from '../components/games/GameStarQuiz.vue'
import GameOrchard from '../components/games/GameOrchard.vue'
import GameReflex from '../components/games/GameReflex.vue'
import GameFlipCard from '../components/games/GameFlipCard.vue'
import GameForestFeed from '../components/games/GameForestFeed.vue'
import GameGemStudio from '../components/games/GameGemStudio.vue'
import GameDessert from '../components/games/GameDessert.vue'
import GameParty from '../components/games/GameParty.vue'
import GameStyle from '../components/games/GameStyle.vue'
import GameMatch3 from '../components/games/GameMatch3.vue'
import GameMemory from '../components/games/GameMemory.vue'
import GameRainbow from '../components/games/GameRainbow.vue'
import GameWhack from '../components/games/GameWhack.vue'
import GameGem from '../components/games/GameGem.vue'
import GameSnake from '../components/games/GameSnake.vue'
import GameMonopoly from '../components/games/GameMonopoly.vue'
import GamePetBattle from '../components/games/GamePetBattle.vue'
import GameRps from '../components/games/GameRps.vue'
import sound from '../sound'

/** 游戏 id → 组件 */
const GAME_COMPS = {
  twilight: GameStarQuiz,
  applejack: GameOrchard,
  dash: GameReflex,
  pinkie: GameFlipCard,
  fluttershy: GameForestFeed,
  rarity: GameGemStudio,
  dessert: GameDessert,
  party: GameParty,
  style: GameStyle,
  sports: GameMatch3,
  petbattle: GamePetBattle,
  rps: GameRps,
  memory: GameMemory,
  rainbow: GameRainbow,
  whack: GameWhack,
  gem: GameGem,
  snake: GameSnake,
  monopoly: GameMonopoly
}

export default {
  name: 'GameView',
  components: { CutieMark },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      score: 0,
      best: 0,
      result: null,
      round: 0,   // 变一下 key 就能重开一局
      // 排行榜 / 好友对战
      rankOpen: false,
      rankList: [],
      vsOpen: false,
      vsFriends: [],
      vsResult: null
    }
  },
  computed: {
    /** 从游戏中心进来的人，返回时回中心；从学院进来的回学院 */
    fromHub() {
      return this.$route.query.from === 'games'
    },
    game() {
      return GAMES.find(g => g.id === this.$route.params.id) || null
    },
    gameComp() {
      return this.game ? GAME_COMPS[this.game.id] : null
    },
    PONIES: () => PONIES,
    resultTitle() {
      const s = this.result ? this.result.score : 0
      if (s >= 200) return '太厉害啦！友谊之星非你莫属 ✨'
      if (s >= 120) return '干得漂亮，继续冲！💪'
      if (s >= 60) return '不错哦，再来一局会更好～'
      return '别灰心，多练几次就熟啦！'
    }
  },
  created() {
    if (this.game) this.best = gameBest(this.game.id)
  },
  methods: {
    goBack() {
      this.$router.push({ name: this.fromHub ? 'games' : 'learn' })
    },
    onScore(n) {
      this.score = n
    },
    async onOver(score) {
      // 本地学习积分（首页学习中心装饰用，保留）
      const r = settleGame(this.game.id, score)
      // 提交到后端并领取「真实到账积分」（破纪录增量）
      let accountReward = 0
      let serverBest = r.best
      try {
        const res = await apiGameScore(this.game.id, this.session.id, score, this.game.name)
        if (res && res.code === 200 && res.data) {
          accountReward = res.data.reward || 0
          if (typeof res.data.best === 'number') serverBest = res.data.best
        }
      } catch (e) { /* 网络异常不影响本地结算 */ }
      this.best = serverBest
      this.result = {
        score,
        gain: r.gain,
        accountReward,
        best: serverBest,
        newBest: accountReward > 0
      }
      // 结算音效 + 提示：明确告知「已存入账户」
      if (accountReward > 0) {
        this.toast(`🎉 破纪录！积分 +${accountReward} 已存入账户 💎`, 'ok')
        sound.magic()
      } else if (score > 0) {
        this.toast(`本局得分 ${score}，未破纪录，下次冲更高就能领积分！`, 'ok')
        sound.success()
      } else {
        this.toast('再试一次吧～', 'err')
        sound.fail()
      }
      bump('game')
    },
    replay() {
      this.result = null
      this.score = 0
      this.round += 1
    },
    /** 服务器排行榜 */
    async openRank() {
      this.rankOpen = true
      this.rankList = []
      const res = await apiGameRank(this.game.id)
      if (res.code === 200) this.rankList = res.data || []
      else this.toast(res.msg || '排行榜加载失败', 'err')
    },
    /** 打开好友对战选择 */
    async openVs() {
      this.vsOpen = true
      this.vsResult = null
      this.vsFriends = []
      const res = await apiFriendList(this.session.id)
      if (res.code === 200) this.vsFriends = res.data || []
    },
    /** 和好友比最高分 */
    async doVs(f) {
      const res = await apiGameVs(this.session.id, f.id, this.game.id)
      if (res.code !== 200) {
        this.toast(res.msg || '对比失败', 'err')
        return
      }
      const d = res.data || {}
      const meBest = d.meBest == null ? 0 : d.meBest
      const friendBest = d.friendBest == null ? 0 : d.friendBest
      this.vsResult = {
        me: { name: '我', avatar: this.session.avatar || '', best: meBest },
        friend: { name: f.name, avatar: f.avatar || '', best: friendBest },
        winner: meBest > friendBest ? 'me' : (friendBest > meBest ? 'friend' : 'tie')
      }
      if (this.vsResult.winner === 'me') sound.magic()
      else sound.pop()
    }
  }
}
</script>

<style scoped>
.back-link { color: var(--primary); margin-left: 8px; }

.game-top {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 12px;
}
.game-hud-face {
  width: 44px; height: 44px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--pc), #ffffff66);
  box-shadow: 0 3px 8px rgba(160,108,213,.22);
}
.game-hud-face svg { width: 27px; height: 27px; }
.game-top .home-greet h2 { font-size: 17px; }
.game-top .home-greet p {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.game-score {
  flex-shrink: 0; text-align: center;
  background: var(--panel); border-radius: 12px; padding: 6px 12px;
}
.game-score b { display: block; font-size: 19px; color: var(--primary); line-height: 1.2; }
.game-score span { font-size: 10px; color: var(--muted); }

.game-best-bar {
  font-size: 11.5px; color: var(--muted);
  text-align: center; margin-bottom: 12px;
}

/* 结算弹窗 */
.result-sheet { text-align: center; max-width: var(--app-w); }
.result-emoji { font-size: 46px; }
.result-title { margin: 8px 0 4px; font-size: 17px; }
.result-score {
  font-size: 34px; font-weight: 800; color: var(--primary);
  margin: 6px 0;
}
.result-detail { font-size: 12.5px; color: var(--muted); margin: 0 0 6px; line-height: 1.7; }
.result-detail b { color: var(--primary); font-size: 14px; }
.result-best { font-size: 11.5px; color: var(--muted); opacity: .75; margin: 0 0 16px; }
.result-btns { display: flex; flex-direction: column; gap: 8px; }
.result-btn {
  width: 100%; padding: 11px; border: none; border-radius: 11px;
  font-size: 14px; font-weight: 700; cursor: pointer; transition: filter .15s;
}
.result-btn.main {
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 12px rgba(160,108,213,.3);
}
.result-btn.main:hover { filter: brightness(1.05); }
.result-btn.ghost { background: var(--panel); color: var(--primary); }
</style>
