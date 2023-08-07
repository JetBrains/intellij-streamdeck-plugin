import {
  SDOnPiEvent,
  StreamDeckPropertyInspectorHandler,
  DidReceiveSettingsEvent,
} from 'streamdeck-typescript'
import {
  isGlobalSettingsSet,
  fetchApi,
  SelectElement,
  isDeviceSetting,
  isSceneSetting,
} from './utils/index'
import {
  GlobalSettingsInterface,
  SceneSettingsInterface,
  DeviceSettingsInterface,
} from './utils/interface'

const pluginName = 'com.jetbrains.ide'

/**
 * Load and save settings.
 */
class IdeaPI extends StreamDeckPropertyInspectorHandler {
  private selectOptions?: SelectElement[]
  private selectedBehaviour = 'toggle'
  private selectedOptionId: string

  private hostElement: HTMLInputElement;
  private portElement: HTMLInputElement;
  private passwordElement: HTMLInputElement;
  private actionElement: HTMLInputElement;
  private saveElement: HTMLButtonElement;

  constructor() {
    super()
  }

  @SDOnPiEvent('documentLoaded')
  onDocumentLoaded(): void {
    console.log('onDocumentLoaded()')
    const validateButton = document.getElementById('validate_button') as HTMLButtonElement
    const selectLabel = document.getElementById('select_label') as HTMLSelectElement
    const behaviour = document.getElementById('behaviour') as HTMLDivElement
    // this.mainElement = document.getElementById(
    //     'mainSettings'
    // ) as HTMLElement;
    // this.mainElement.style.display = 'initial';

    this.hostElement = document.getElementById('host') as HTMLInputElement;
    this.portElement = document.getElementById('port') as HTMLInputElement;
    this.passwordElement = document.getElementById(
        'password'
    ) as HTMLInputElement;
    this.actionElement = document.getElementById('action') as HTMLInputElement;
    this.saveElement = document.getElementById('save') as HTMLButtonElement;

    this.saveElement?.addEventListener('click', this.onValidateButtonPressed.bind(this))

    validateButton?.addEventListener('click', this.onValidateButtonPressed.bind(this))


    switch (this.actionInfo.action) {
      case pluginName + '.device': {
        selectLabel.textContent = 'Devices'
        validateButton.textContent = 'Fetch devices list'
        behaviour.className = 'sdpi-item' // Remove hidden class and display radio selection
        break
      }
      case pluginName + '.scene': {
        validateButton.textContent = 'Fetch scenes list'
        selectLabel.textContent = 'Scenes'
        break
      }
    }
  }

  @SDOnPiEvent('setupReady')
  private documentLoaded() {

  }

  // Save settings
  private async onValidateButtonPressed() {
    console.log('onValidateButtonPressed()')

    const accessToken = (<HTMLInputElement>document.getElementById('accesstoken'))?.value
    const password = (<HTMLInputElement>document.getElementById('password'))?.value
    const host = this.hostElement?.value
    const action = this.actionElement.value
    console.log('password =  ' + password + ", action=" + action)
    this.settingsManager.setGlobalSettings({ accessToken, password, host })

    let elements: SelectElement[] = []

    switch (this.actionInfo.action) {
      case pluginName + '.scene': {
        break
      }
      case pluginName + '.device': {
        break
      }
    }

    this.setSettings({
      selectOptions: elements,
      behaviour: this.selectedBehaviour,
      action: action
    })
    this.requestSettings() // requestSettings will add the options to the select element
  }

  // Prefill PI elements from cache
  @SDOnPiEvent('globalSettingsAvailable')
  propertyInspectorDidAppear(): void {
    console.log('propertyInspectorDidAppear()')
    this.requestSettings()
    const globalSettings = this.settingsManager.getGlobalSettings<GlobalSettingsInterface>()

    if (isGlobalSettingsSet(globalSettings)) {
      const accessToken = globalSettings.accessToken
      if (accessToken) {
        (<HTMLInputElement>document.getElementById('accesstoken')).value = accessToken
      }
      const password = globalSettings.password;
      if(password) {
        console.log(`password=${password}`)
        this.passwordElement.value = password;
      }
      const host = globalSettings.host;
      if(host) {
        this.hostElement.value = host;
      }
    }
  }

  // Get the devices list from cache
  @SDOnPiEvent('didReceiveSettings')
  onReceiveSettings({
    payload,
  }: DidReceiveSettingsEvent<SceneSettingsInterface>): void {
    const select = document.getElementById('select_value') as HTMLSelectElement
    console.log("onReceiveSettings()")
    console.debug(payload.settings)

    this.actionElement.value = payload.settings.action ?? "";

    let activeIndex: number | undefined
    if (isDeviceSetting(payload.settings)) {
      const deviceId = payload.settings.deviceId
      this.selectedOptionId = deviceId

      this.selectedBehaviour = payload.settings.behaviour;

      (document.getElementById(this.selectedBehaviour) as HTMLInputElement).checked = true

      activeIndex = this.selectOptions?.findIndex((element) => element.id === deviceId) || 0
    }
    if (isSceneSetting(payload.settings)) {
      const sceneId = payload.settings.sceneId
      activeIndex = this.selectOptions?.findIndex((element) => element.id === sceneId) || 0

      this.selectedOptionId = sceneId
    }

  }
}

new IdeaPI()
