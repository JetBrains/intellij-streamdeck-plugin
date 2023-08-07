import { SelectElement } from './index'

export interface GlobalSettingsInterface {
  accessToken: string
  host: string
  port: string
  password: string
}

export interface SceneSettingsInterface {
  sceneId: string
  action: string
}

export interface DeviceSettingsInterface {
  deviceId: string
  behaviour: string
}