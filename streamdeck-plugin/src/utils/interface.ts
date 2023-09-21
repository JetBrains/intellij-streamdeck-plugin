import { SelectElement } from './index'

export interface GlobalSettingsInterface {
  accessToken: string
  host: string
  port: string
  password: string
  showTitle: string
}

export interface ActionSettingsInterface {
  action: string// Custom action ID
  showTitle: string// Whether show action tip
}
