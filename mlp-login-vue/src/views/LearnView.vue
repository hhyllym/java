<template>
  <div class="home learn">
    <!-- 顶部：标题 + 返回 -->
    <div class="home-top">
      <div class="logo">🎓</div>
      <div class="home-greet">
        <h2>小马学院</h2>
        <p>跟六位主角学本领 · 玩转友谊魔法</p>
      </div>
      <button class="logout" @click="goHome">返回</button>
    </div>

    <!-- 学习档案：积分 / 连击 / 徽章 -->
    <div class="stat-bar">
      <div class="stat"><b>💎 {{ state.points }}</b><span>学习积分</span></div>
      <div class="stat"><b>🔥 {{ state.checkin.streak }}</b><span>连击天数</span></div>
      <div class="stat"><b>🏅 {{ state.badges.length }}/{{ BADGES.length }}</b><span>徽章</span></div>
    </div>

    <!-- 每日打卡 -->
    <div class="checkin-row">
      <div class="checkin-txt">
        <b>每日打卡</b>
        <span>{{ checkedToday ? '今天已打卡，保持连击！' : '今天还没打卡，来签个到吧' }}</span>
      </div>
      <button class="checkin-btn" :class="{ done: checkedToday }" :disabled="checkedToday" @click="doCheckin">
        {{ checkedToday ? '✓ 已打卡' : '+5 积分' }}
      </button>
    </div>

    <!-- 小游戏入口 -->
    <div class="filter-label">🎮 小马游乐场 · 六位主角各有一款拿手游戏</div>
    <div class="game-grid">
      <button v-for="g in GAMES" :key="g.id" class="game-card" :style="{ '--pc': PONIES[g.pony].color }" @click="openGame(g)">
        <span class="game-face">
          <CutieMark :mark="PONIES[g.pony].mark" />
        </span>
        <span class="game-name">{{ g.name }}</span>
        <span class="game-teacher">{{ PONIES[g.pony].name }}出品</span>
        <span class="game-best" v-if="gameBest(g.id) > 0">最佳 {{ gameBest(g.id) }} 分</span>
      </button>
    </div>

    <!-- 课程入口 -->
    <div class="filter-label">📖 六大元素课程 · 学完一节 +10 积分，小测答对再 +5</div>
    <div class="course-list">
      <button v-for="c in COURSES" :key="c.id" class="course-card" :style="{ '--pc': PONIES[c.pony].color }" @click="openCourse(c)">
        <span class="course-face">{{ c.icon }}</span>
        <span class="course-main">
          <b>{{ c.name }}</b>
          <em>{{ c.desc }}</em>
          <span class="course-progress">
            <i :style="{ width: progressOf(c) + '%' }"></i>
          </span>
          <span class="course-num">{{ doneLessons(c) }}/{{ c.lessons.length }} 节</span>
        </span>
        <span class="course-badge" v-if="courseDone(c)">🏅</span>
      </button>
    </div>

    <!-- 徽章墙 -->
    <div class="filter-label">🏅 徽章墙 · 完成整门课即可点亮</div>
    <div class="badge-wall">
      <div v-for="b in BADGES" :key="b.id" class="badge" :class="{ on: hasBadge(b.id) }" :title="hasBadge(b.id) ? b.name : '尚未获得'">
        <span class="badge-icon">{{ b.icon }}</span>
        <span class="badge-name">{{ b.name }}</span>
      </div>
    </div>

    <div class="learn-tip">
      💡 小提示：所有学习进度都保存在本机浏览器里，换设备不会同步哦
    </div>

    <!-- 课程详情弹窗 -->
    <Teleport to="body">
      <div v-if="course" class="sheet-mask" @click.self="course = null">
        <div class="sheet">
          <div class="sheet-top">
            <span class="sheet-face" :style="{ '--pc': PONIES[course.pony].color }">{{ course.icon }}</span>
            <div class="sheet-title">
              <h3>{{ course.name }}</h3>
              <p>{{ PONIES[course.pony].name }} · {{ PONIES[course.pony].element }}元素</p>
            </div>
            <button class="sheet-close" @click="course = null">✕</button>
          </div>

          <p class="sheet-intro">{{ course.desc }}</p>

          <!-- 小节 -->
          <div class="sheet-sec">
            <h4>课程小节</h4>
            <div v-for="(l, i) in course.lessons" :key="l.id" class="lesson">
              <div class="lesson-head" @click="toggleLesson(i)">
                <span class="lesson-idx" :class="{ done: lessonDone(l) }">{{ lessonDone(l) ? '✓' : i + 1 }}</span>
                <b>{{ l.title }}</b>
                <span class="lesson-arrow" :class="{ open: openIdx === i }">›</span>
              </div>
              <div v-if="openIdx === i" class="lesson-body">
                <p v-for="(p, pi) in l.paras" :key="pi">{{ p }}</p>

                <!-- B 站跟学视频 -->
                <a
                  v-if="l.link"
                  class="lesson-link"
                  :href="l.link.url"
                  target="_blank"
                  rel="noopener"
                  @click="sound.hover()"
                >📺 {{ l.link.label }} ↗</a>

                <!-- 小测 -->
                <div class="quiz">
                  <div class="quiz-q">📝 {{ l.quiz.q }}</div>
                  <div class="quiz-opts">
                    <button
                      v-for="(o, oi) in l.quiz.options"
                      :key="oi"
                      class="quiz-opt"
                      :class="quizState(l.id, oi)"
                      :disabled="quizAnswered(l.id)"
                      @click="answerQuiz(l, oi)"
                    >{{ o }}</button>
                  </div>
                  <div v-if="quizResult[l.id]" class="quiz-explain" :class="{ right: quizResult[l.id].ok }">
                    {{ quizResult[l.id].ok ? '✅ 答对了，+' + RULES.QUIZ_POINTS + ' 积分！' : '💡 ' + l.quiz.explain }}
                  </div>
                </div>

                <button v-if="!lessonDone(l)" class="lesson-done-btn" @click="markLesson(l)">
                  我学完这一节了（+{{ RULES.LESSON_POINTS }} 积分）
                </button>
                <div v-else class="lesson-done-tip">✓ 本节课已完成，积分已到账</div>
              </div>
            </div>
          </div>

          <!-- 全部完成提示 -->
          <div v-if="courseDone(course)" class="course-all-done">
            🎉 整门课已完成！徽章「{{ badgeOf(course).name }}」已收入囊中，+{{ RULES.COURSE_POINTS }} 积分
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script>
import { getSession } from '../auth'
import CutieMark from '../components/CutieMark.vue'
import { COURSES, BADGES, GAMES, PONIES } from '../courses'
import {
  loadLearn, completeLesson, quizRight, finishCourse, gameBest, checkinToday
} from '../learn'
import sound from '../sound'

export default {
  name: 'LearnView',
  components: { CutieMark },
  inject: ['toast'],
  data() {
    return {
      session: getSession() || {},
      state: loadLearn(),
      course: null,        // 当前打开的课程
      openIdx: -1,         // 展开的小节下标
      quizResult: {},      // { lessonId: { ok, answer } } 小测作答记录
      checkedToday: false
    }
  },
  computed: {
    COURSES: () => COURSES,
    BADGES: () => BADGES,
    GAMES: () => GAMES,
    PONIES: () => PONIES,
    RULES: () => ({ LESSON_POINTS: 10, QUIZ_POINTS: 5, COURSE_POINTS: 50 })
  },
  created() {
    // 今天是否已打卡
    const today = new Date()
    const t = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
    this.checkedToday = this.state.checkin.last === t
    // 从背景可爱标志跳进来：/learn?pony=xxx 自动打开对应小马的课程
    const pony = this.$route.query.pony
    if (pony) {
      const c = COURSES.find(c => c.pony === pony)
      if (c) this.openCourse(c)
    }
  },
  methods: {
    goHome() {
      this.$router.push({ name: 'home' })
    },
    playHover() {
      sound.hover()
    },
    openGame(g) {
      this.$router.push({ name: 'game', params: { id: g.id } })
    },
    openCourse(c) {
      this.course = c
      this.openIdx = -1
      // 自动展开第一节没学过的小节
      const first = c.lessons.findIndex(l => !this.lessonDone(l))
      this.openIdx = first >= 0 ? first : 0
    },
    toggleLesson(i) {
      this.openIdx = this.openIdx === i ? -1 : i
    },
    lessonDone(l) {
      const c = this.state.courses[this.course ? this.course.id : ''] || {}
      return (c.lessons || []).includes(l.id)
    },
    doneLessons(c) {
      const cc = this.state.courses[c.id]
      return cc ? (cc.lessons || []).length : 0
    },
    progressOf(c) {
      return Math.round(this.doneLessons(c) / c.lessons.length * 100)
    },
    courseDone(c) {
      return this.doneLessons(c) >= c.lessons.length
    },
    badgeOf(c) {
      return BADGES.find(b => b.from === c.id) || { name: '' }
    },
    hasBadge(id) {
      return this.state.badges.includes(id)
    },
    gameBest(gid) {
      return gameBest(gid)
    },
    markLesson(l) {
      if (!this.course) return
      completeLesson(this.course.id, l.id)
      this.state = loadLearn()
      this.toast('学完「' + l.title + '」，+10 积分！', 'ok')
      // 整门课完成 → 发徽章
      if (this.courseDone(this.course)) {
        const badge = this.badgeOf(this.course)
        const first = finishCourse(this.course.id, badge.id)
        this.state = loadLearn()
        if (first) this.toast('🎉 获得徽章「' + badge.name + '」+50 积分！', 'ok')
      }
    },
    quizAnswered(l) {
      return !!this.quizResult[l.id]
    },
    quizState(l, oi) {
      const r = this.quizResult[l.id]
      if (!r) return ''
      if (oi === r.answer) return 'right'
      if (oi === r.picked) return 'wrong'
      return 'dim'
    },
    answerQuiz(l, oi) {
      const ok = oi === l.quiz.answer
      this.quizResult = Object.assign({}, this.quizResult, {
        [l.id]: { ok, picked: oi, answer: l.quiz.answer }
      })
      if (ok) {
        sound.success()
        if (this.course) {
          quizRight(this.course.id, l.id)
          this.state = loadLearn()
        }
      } else {
        sound.fail()
      }
    },
    doCheckin() {
      const r = checkinToday()
      this.state = loadLearn()
      this.checkedToday = true
      if (r.gained) {
        sound.success()
        this.toast(`打卡成功，+${r.gained} 积分，连击 ${r.streak} 天 🔥`, 'ok')
      }
    }
  }
}
</script>

<style scoped>
/* ========== 小马学院 ========== */
.checkin-row {
  display: flex; align-items: center; gap: 12px;
  margin: 14px 0 4px; padding: 12px 14px;
  background: var(--panel); border-radius: 14px;
}
.checkin-txt { flex: 1; min-width: 0; }
.checkin-txt b { display: block; font-size: 13.5px; }
.checkin-txt span { font-size: 11.5px; color: var(--muted); }
.checkin-btn {
  border: none; cursor: pointer; flex-shrink: 0;
  padding: 8px 16px; border-radius: 999px; font-size: 12.5px; font-weight: 700;
  color: #fff; background: linear-gradient(135deg, var(--pink), var(--purple));
  box-shadow: 0 4px 10px rgba(160,108,213,.25); transition: filter .15s;
}
.checkin-btn:hover { filter: brightness(1.06); }
.checkin-btn.done { background: var(--mint); color: #2b6b4e; cursor: default; box-shadow: none; }

/* 游戏入口 */
.game-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.game-card {
  border: none; cursor: pointer; text-align: center;
  background: var(--panel); border-radius: 14px; padding: 14px 6px 12px;
  display: flex; flex-direction: column; align-items: center; gap: 5px;
  transition: transform .15s, box-shadow .15s;
}
.game-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 16px rgba(160,108,213,.16);
}
.game-face {
  width: 42px; height: 42px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--pc), #ffffff66);
  box-shadow: 0 3px 8px rgba(160,108,213,.2);
}
.game-face svg { width: 26px; height: 26px; }
.game-name { font-size: 12.5px; font-weight: 700; color: var(--text); }
.game-teacher { font-size: 10px; color: var(--muted); }
.game-best {
  font-size: 9.5px; color: var(--primary);
  background: var(--card); border-radius: 999px; padding: 1px 7px;
}

/* 课程列表 */
.course-list { display: flex; flex-direction: column; gap: 10px; }
.course-card {
  display: flex; align-items: center; gap: 12px; text-align: left;
  border: 1.5px solid var(--border); background: var(--card);
  border-radius: 14px; padding: 12px 14px; cursor: pointer;
  transition: border-color .15s, transform .15s;
}
.course-card:hover { border-color: var(--pc); transform: translateY(-2px); }
.course-face {
  width: 42px; height: 42px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 21px;
  background: linear-gradient(135deg, var(--pc), #ffffff66);
}
.course-main { flex: 1; min-width: 0; }
.course-main b { font-size: 13.5px; display: block; }
.course-main em {
  font-style: normal; font-size: 11px; color: var(--muted);
  display: block; margin: 2px 0 6px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.course-progress {
  display: block; height: 5px; border-radius: 4px;
  background: var(--track); overflow: hidden;
}
.course-progress i {
  display: block; height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, var(--pc), var(--purple));
  transition: width .3s ease;
}
.course-num { font-size: 10px; color: var(--muted); margin-top: 4px; display: block; }
.course-badge { font-size: 18px; flex-shrink: 0; }

/* 徽章墙 */
.badge-wall { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.badge {
  text-align: center; padding: 12px 4px 10px;
  border-radius: 14px; background: var(--panel);
  border: 1.5px dashed var(--border);
  opacity: .45; filter: grayscale(1); transition: all .2s;
}
.badge.on {
  opacity: 1; filter: none;
  background: linear-gradient(160deg, #fff, var(--panel));
  border-style: solid; border-color: var(--purple);
  box-shadow: 0 4px 12px rgba(160,108,213,.16);
}
.badge-icon { font-size: 24px; display: block; }
.badge-name { font-size: 10.5px; color: var(--text); margin-top: 4px; display: block; }

.learn-tip {
  margin-top: 18px; font-size: 11px; color: var(--muted);
  text-align: center; line-height: 1.6;
}

/* ========== 课程详情弹窗内 ========== */
.lesson {
  border: 1.5px solid var(--border); border-radius: 12px;
  margin-bottom: 8px; overflow: hidden; background: var(--card);
}
.lesson-head {
  display: flex; align-items: center; gap: 9px;
  padding: 11px 12px; cursor: pointer; user-select: none;
}
.lesson-idx {
  width: 21px; height: 21px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}
.lesson-idx.done { background: var(--success); }
.lesson-head b { flex: 1; font-size: 13px; text-align: left; }
.lesson-arrow {
  color: var(--muted); font-size: 16px; transition: transform .2s;
  transform: rotate(90deg);
}
.lesson-arrow.open { transform: rotate(-90deg); }
.lesson-body { padding: 0 12px 12px; }
.lesson-body p {
  font-size: 12.5px; line-height: 1.8; color: var(--text);
  margin: 8px 0;
}
/* B 站跟学链接 */
.lesson-link {
  display: inline-flex; align-items: center; gap: 4px;
  margin: 2px 0 4px; padding: 6px 12px;
  border-radius: 999px; text-decoration: none;
  font-size: 12px; font-weight: 700;
  color: var(--primary); background: var(--card);
  border: 1.5px solid var(--border);
  transition: all .15s;
}
.lesson-link:hover {
  border-color: var(--primary);
  background: linear-gradient(135deg, var(--pink), var(--purple));
  color: #fff;
}

/* 小测 */
.quiz {
  margin-top: 10px; padding: 11px 12px;
  background: var(--panel); border-radius: 11px;
}
.quiz-q { font-size: 12.5px; font-weight: 700; margin-bottom: 8px; }
.quiz-opts { display: flex; flex-direction: column; gap: 6px; }
.quiz-opt {
  text-align: left; font-size: 12px; padding: 8px 11px;
  border: 1.5px solid var(--border); border-radius: 9px;
  background: var(--card); color: var(--text); cursor: pointer;
  transition: all .15s;
}
.quiz-opt:hover:not(:disabled) { border-color: var(--primary); color: var(--primary); }
.quiz-opt:disabled { cursor: default; }
.quiz-opt.right {
  border-color: var(--success); color: var(--success); font-weight: 700;
  background: rgba(76,175,125,.08);
}
.quiz-opt.wrong {
  border-color: var(--danger); color: var(--danger);
  background: rgba(224,102,102,.08);
}
.quiz-opt.dim { opacity: .55; }
.quiz-explain {
  font-size: 11.5px; line-height: 1.6; margin-top: 8px;
  color: var(--danger);
}
.quiz-explain.right { color: var(--success); font-weight: 700; }

.lesson-done-btn {
  width: 100%; margin-top: 10px; padding: 9px;
  border: none; border-radius: 10px; cursor: pointer;
  color: #fff; font-size: 12.5px; font-weight: 700;
  background: linear-gradient(135deg, var(--pink), var(--purple));
}
.lesson-done-tip {
  margin-top: 10px; font-size: 11.5px; color: var(--success);
  text-align: center; font-weight: 700;
}

.course-all-done {
  margin-top: 14px; padding: 12px;
  border-radius: 12px; text-align: center;
  font-size: 12.5px; font-weight: 700; line-height: 1.6;
  color: #8a5a00; background: linear-gradient(135deg, #fff7e0, #ffeec2);
  border: 1.5px solid #f2d488;
}
</style>
