/**
 * 小马学院 · 学习状态管理（localStorage 持久化）
 * 积分、徽章、课程进度、游戏最佳分、连续打卡
 */

const KEY = 'mlp_learn_v1'

const LESSON_POINTS = 10   // 学完一节课
const QUIZ_POINTS = 5      // 小测答对
const COURSE_POINTS = 50   // 完成整门课
const CHECKIN_POINTS = 5   // 每日打卡

function defaultState() {
  return {
    points: 0,
    badges: [],            // 已获得徽章 id
    courses: {},           // { courseId: { lessons: [lessonId], quiz: { lessonId: true } } }
    games: {},             // { gameId: { best: 分数, plays: 次数 } }
    checkin: { last: '', streak: 0 }
  }
}

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export function loadLearn() {
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return defaultState()
    const st = JSON.parse(raw)
    return Object.assign(defaultState(), st)
  } catch (e) {
    return defaultState()
  }
}

function save(st) {
  try { localStorage.setItem(KEY, JSON.stringify(st)) } catch (e) {}
}

export function addPoints(n) {
  const st = loadLearn()
  st.points = Math.max(0, (st.points || 0) + n)
  save(st)
  return st.points
}

/** 学完一节课：+10 分；返回本课已完成小节数 */
export function completeLesson(courseId, lessonId) {
  const st = loadLearn()
  const c = st.courses[courseId] || (st.courses[courseId] = { lessons: [], quiz: {} })
  if (!c.lessons.includes(lessonId)) {
    c.lessons.push(lessonId)
    st.points += LESSON_POINTS
  }
  save(st)
  return c.lessons.length
}

/** 小测答对：+5 分（每节只记一次） */
export function quizRight(courseId, lessonId) {
  const st = loadLearn()
  const c = st.courses[courseId] || (st.courses[courseId] = { lessons: [], quiz: {} })
  if (!c.quiz[lessonId]) {
    c.quiz[lessonId] = true
    st.points += QUIZ_POINTS
  }
  save(st)
  return st.points
}

/** 整门课完成：+50 分 + 徽章；返回是否首次完成 */
export function finishCourse(courseId, badgeId) {
  const st = loadLearn()
  const first = badgeId && !st.badges.includes(badgeId)
  if (first) {
    st.badges.push(badgeId)
    st.points += COURSE_POINTS
  }
  save(st)
  return first
}

/** 游戏结算：刷新最佳分；积分按「本次得分超出历史最佳的部分」奖励 */
export function settleGame(gameId, score) {
  const st = loadLearn()
  const g = st.games[gameId] || (st.games[gameId] = { best: 0, plays: 0 })
  const gain = Math.max(0, score - g.best)
  g.best = Math.max(g.best, score)
  g.plays += 1
  st.points += gain
  save(st)
  return { gain, best: g.best, plays: g.plays }
}

export function gameBest(gameId) {
  const st = loadLearn()
  return (st.games[gameId] || {}).best || 0
}

/** 每日打卡：+5 分，连击 +1；断签归 1。返回 { gained, streak } */
export function checkinToday() {
  const st = loadLearn()
  const today = todayStr()
  if (st.checkin.last === today) return { gained: 0, streak: st.checkin.streak }
  const yesterday = new Date(Date.now() - 86400000)
  const yStr = `${yesterday.getFullYear()}-${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`
  st.checkin.streak = st.checkin.last === yStr ? (st.checkin.streak || 0) + 1 : 1
  st.checkin.last = today
  st.points += CHECKIN_POINTS
  save(st)
  return { gained: CHECKIN_POINTS, streak: st.checkin.streak }
}

export function hasBadge(badgeId) {
  return loadLearn().badges.includes(badgeId)
}

export const RULES = { LESSON_POINTS, QUIZ_POINTS, COURSE_POINTS, CHECKIN_POINTS }
