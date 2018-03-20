import config  from '../utils/config'
import { query } from '../services/app'
import * as menuService from '../services/menus'
import { routerRedux } from 'dva/router'
import queryString from 'query-string'
import enumRoleType from '../utils/enums'
export default {
  namespace: 'app',
  state: {
    user: {},
    permissions:{
      visit:[],
    },
    menu:[],
    locationPathname: '',
  },
  subscriptions: {

    setup ({ dispatch}) {
      dispatch({ type: 'query'})
    },

    setupHistory ({ dispatch, history }) {
      history.listen((location) => {
        dispatch({
          type: 'updateState',
          payload: {
            locationPathname: location.pathname,
            locationQuery: queryString.parse(location.search),
          },
        })
      })
    },
  },
  effects: {
    * query ({ payload}, {call, put, select}) {
      const { success, user } = yield call(query, payload)
      const { locationPathname } = yield select( _ => _.app)
      if ( success && user) {
        const { list } = yield call(menuService.query)
        let menu = []
        const { permissions } = user
        if (permissions.role === enumRoleType.ADMIN) {
          permissions.visit = list.map(item => item.id)
          menu = list
        } else {

        }
        yield put({
          type: 'updateState',
          payload:{
            user,
            permissions,
            menu,
          }
        })
        if (location.pathname === '/login') {
          yield put(routerRedux.push({
            pathname: '/dashboard',
          }))
        }
      } else if (config.openPages && config.openPages.indexOf(locationPathname) < 0) {
        yield put(routerRedux.push({
          pathname: '/login',
          search: queryString.stringify({
            from: locationPathname,
          })
        }))
      }
    }
  },
  reducers: {
    updateState (state, { payload }) {
      return {
        ...state,
        ...payload,
      }
    }
  }
}
