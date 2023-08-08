import {
  SDOnPiEvent,
  StreamDeckPropertyInspectorHandler,
  DidReceiveSettingsEvent,
} from 'streamdeck-typescript'
import {
  isGlobalSettingsSet,
  fetchApi,
  SelectElement,
} from './utils/index'
import {
  GlobalSettingsInterface,
  ActionSettingsInterface,
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
  private showTitleElement: HTMLInputElement;

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
    this.showTitleElement = document.getElementById(
        'singlechk'
    ) as HTMLInputElement;

    this.saveElement?.addEventListener('click', this.onSaveButtonPressed.bind(this))
    this.showTitleElement?.addEventListener('click', this.onUpdateTitleButtonPressed.bind(this))

    switch (this.actionInfo.action) {
      case pluginName + '.custom': {
        selectLabel.textContent = 'Devices'
        validateButton.textContent = 'Fetch devices list'
        behaviour.className = 'sdpi-item' // Remove hidden class and display radio selection
        break
      }
    }
  }

  @SDOnPiEvent('setupReady')
  private documentLoaded() {

  }

  /**
   * Save global settings and customized action ID settings
   * @private
   */
  private async onSaveButtonPressed() {
    console.log('onValidateButtonPressed()')

    const password = (<HTMLInputElement>document.getElementById('password'))?.value
    const host = this.hostElement?.value
    const action = this.actionElement.value
    const showTitle = this.showTitleElement.checked ? "on" : "off"
    console.log('password =  ' + password + ", action=" + action + ", showTitle=" + showTitle)
    this.settingsManager.setGlobalSettings({ password, host, showTitle })

    let elements: SelectElement[] = []

    switch (this.actionInfo.action) {
      case pluginName + '.custom': {
        break
      }
    }

    this.setSettings({
      selectOptions: elements,
      behaviour: this.selectedBehaviour,
      action: action
    })
    this.requestSettings() // requestSettings will add the options to the select element

    // this.sendToPlugin( { showTitle }, "updateTitle")
  }

  /**
   * Only update the title global visibility status.
   * @private
   */
  private async onUpdateTitleButtonPressed() {
    console.log('onUpdateTitleButtonPressed()')

    const showTitle = this.showTitleElement.checked ? "on" : "off"
    this.settingsManager.setGlobalSettings({ showTitle })

    // this.sendToPlugin( { showTitle }, "updateTitle")
  }

  // Prefill PI elements from cache
  @SDOnPiEvent('globalSettingsAvailable')
  propertyInspectorDidAppear(): void {
    console.log('propertyInspectorDidAppear()')
    this.requestSettings()
    const globalSettings = this.settingsManager.getGlobalSettings<GlobalSettingsInterface>()

    // this.showTitleElement.checked = true

    if (isGlobalSettingsSet(globalSettings)) {
      const showTitle = globalSettings.showTitle
      this.showTitleElement.checked = showTitle === "on";

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
  @SDOnPiEvent('didReceiveSettings')
  onReceiveSettings({
    payload,
  }: DidReceiveSettingsEvent<ActionSettingsInterface>): void {
    const select = document.getElementById('select_value') as HTMLSelectElement
    console.log("onReceiveSettings()")
    console.debug(payload.settings)

    this.actionElement.value = payload.settings.action ?? "";
  }
}

new IdeaPI()
