import { SelectElement } from './index'

export interface GlobalSettingsInterface {
  accessToken: string
  host: string
  port: string
  password: string
  showTitle: string
}

export interface ActionSettingsInterface {
  sceneId: string
  action: string
}
