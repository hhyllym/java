/**
 * 首页图鉴接口封装（Spring Boot）
 *   GET /api/home/overview   首页展示：统计 + 分类树 + 热搜词 + 热门小马
 *   GET /api/home/search     分级分类 + 关键词搜索（分页）
 *   GET /api/category/tree   分类树
 *   GET /api/pony/{id}       条目详情
 * 返回格式跟 auth.js 一样：{ code, msg, data }
 */

const HOME_BASE = '/api/home'
const CATEGORY_BASE = '/api/category'
const PONY_BASE = '/api/pony'

async function get(url) {
  try {
    const res = await fetch(url)
    const text = await res.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      return {
        code: -1,
        msg: `后端返回了非 JSON 内容（HTTP ${res.status}），去后端窗口看报错日志`,
        data: null
      }
    }
  } catch (e) {
    return { code: -1, msg: '无法连接后端服务，请确认 Spring Boot 已启动（端口 8081）', data: null }
  }
}

/** 首页展示数据（一次请求拿齐） */
export function apiOverview() {
  return get(HOME_BASE + '/overview')
}

/**
 * 分级分类 + 关键词搜索
 * @param {string} keyword    关键词，可空
 * @param {number} categoryId 分类 id，一级/二级都行；不传 = 全部分类
 * @param {number} page       从 1 开始
 * @param {number} size       每页条数
 */
export function apiSearch({ keyword = '', categoryId = null, page = 1, size = 12 } = {}) {
  const q = new URLSearchParams()
  if (keyword) q.set('keyword', keyword)
  if (categoryId) q.set('categoryId', categoryId)
  q.set('page', page)
  q.set('size', size)
  return get(HOME_BASE + '/search?' + q.toString())
}

/** 分类树（带每类数量） */
export function apiCategoryTree() {
  return get(CATEGORY_BASE + '/tree')
}

/** 条目详情（含所属分类、标签、同类推荐） */
export function apiPonyDetail(id) {
  return get(PONY_BASE + '/' + id)
}
