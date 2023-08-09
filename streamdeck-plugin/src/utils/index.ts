import {
  GlobalSettingsInterface,
  ActionSettingsInterface,
} from './interface'

export function isGlobalSettingsSet(
  settings: GlobalSettingsInterface | unknown
): settings is GlobalSettingsInterface {
  return (settings as GlobalSettingsInterface).showTitle !== undefined
}

interface FetchAPI {
  body?: BodyInit
  endpoint: string
  method: string
  accessToken: string
    host:string
    port:string
    password:string
}

export async function fetchApi<T>({ body, endpoint, method, accessToken }: FetchAPI): Promise<T> {
  return await (
    await fetch(`http://localhost:21420${endpoint}`, {
      method,
      // headers: {
      //   Authorization: `Bearer ${accessToken}`,
      // },
      body,
    })
  ).json()
}

/**
 * Try call IDE built-in HTTP Service
 * @param body
 * @param endpoint
 * @param method
 * @param accessToken
 * @param host
 * @param port
 */
export async function fetchJetBrainsIDE<T>({ body, endpoint, method, password, host, port }: FetchAPI) {
  if(port !== undefined && port !== null) {
      try {
          await fetch(`http://${host}:${port}${endpoint}`, {
              method,
              headers: {
                  Authorization: `${password}`,
              },
              body,
          })
      } catch (e) {
          console.log(e)
      }
  } else {
      for (let i = 63342; i <= 63352; i++) {
          try {
              await fetch(`http://${host}:${i}${endpoint}`, {
                  method,
                  headers: {
                      Authorization: `${password}`,
                  },
                  body,
              })
          } catch (e) {
              console.log(e)
          }
      }
  }

}

export interface SelectElement {
  id?: string
  name?: string
}
interface AddSelectOption {
  select: HTMLSelectElement
  element: SelectElement
}
