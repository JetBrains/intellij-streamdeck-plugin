import { SelectElement } from './index'

/**
 * Global settings for all buttons
 */
export interface GlobalSettingsInterface {
  accessToken: string
  host: string
  port: string
  password: string
}

/**
 * Per action button's config
 */
export interface ActionSettingsInterface {
  action: string // Custom action ID
  showTitle: string // Whether show action tip
  runConfig: string // Customized Run/Debug Configuration Name(Optional)
}
