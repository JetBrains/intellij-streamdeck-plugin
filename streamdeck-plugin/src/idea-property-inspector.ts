/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import {
  SDOnPiEvent,
  StreamDeckPropertyInspectorHandler,
  DidReceiveSettingsEvent,
} from 'streamdeck-typescript'
import {
  isGlobalSettingsSet
} from './utils'
import {
  GlobalSettingsInterface,
  ActionSettingsInterface,
} from './utils/interface'

const pluginName = 'com.jetbrains.idea'

/**
 * Load and save settings.
 */
class IdeaPI extends StreamDeckPropertyInspectorHandler {
  private hostElement: HTMLInputElement;
  private portElement: HTMLInputElement;
  private passwordElement: HTMLInputElement;
  private actionElement: HTMLInputElement;
  private saveElement: HTMLButtonElement;
  private showTitleElement: HTMLInputElement;
  private runConfigurationNameElement: HTMLInputElement;

  constructor() {
    super()
  }

  @SDOnPiEvent('documentLoaded')
  onDocumentLoaded(): void {
    this.logMessage('onDocumentLoaded() ' + this.actionInfo.action)

    const runConfig = document.getElementById('run_config') as HTMLDivElement
    // this.mainElement = document.getElementById(
    //     'mainSettings'
    // ) as HTMLElement;
    // this.mainElement.style.display = 'initial';

    this.saveElement?.addEventListener('click', this.onSaveButtonPressed.bind(this))
    this.showTitleElement?.addEventListener('click', this.onUpdateTitleButtonPressed.bind(this))

    switch (this.actionInfo.action) {
      case pluginName + '.run':
      case pluginName + '.debug':{
        runConfig.className = 'sdpi-item' // Remove hidden class and display run configuration name input box
        break
      }
    }

    // Open all URL in HTML like this: <a data-open-url="https://github.com/JetBrains/intellij-streamdeck-plugin/issues">Bugtracker</a>
    document.querySelectorAll('[data-open-url]').forEach(e => {
      const value = e.getAttribute('data-open-url');
      if(value) {
        e?.addEventListener('click', () => {
          this.openUrl(value)
        })

      } else {
        this.logMessage(`${value} is not a supported url`);
      }
    });

  }

  @SDOnPiEvent('setupReady')
  private documentLoaded() {

  }

  private initElements() {
    this.hostElement = document.getElementById('host') as HTMLInputElement;
    this.portElement = document.getElementById('port') as HTMLInputElement;
    this.passwordElement = document.getElementById(
        'password'
    ) as HTMLInputElement;
    this.actionElement = document.getElementById('action') as HTMLInputElement;
    this.saveElement = document.getElementById('save') as HTMLButtonElement;
    this.showTitleElement = document.getElementById('singlechk') as HTMLInputElement;
    this.runConfigurationNameElement = document.getElementById('run_config_name') as HTMLInputElement;
  }

  /**
   * Save global settings and customized action ID settings
   * @private
   */
  private async onSaveButtonPressed() {
    this.logMessage('onValidateButtonPressed()')

    const password = (<HTMLInputElement>document.getElementById('password'))?.value
    const host = this.hostElement?.value
    const port = this.portElement?.value
    const action = this.actionElement.value
    const runConfig = this.runConfigurationNameElement.value
    const showTitle = this.showTitleElement.checked ? "on" : "off"
    this.logMessage("action=" + action + ", showTitle=" + showTitle)
    this.settingsManager.setGlobalSettings({ password, host, port })

    switch (this.actionInfo.action) {
      case pluginName + '.custom': {
        break
      }
    }

    this.setSettings({
      action: action,
      showTitle,
      runConfig
    })
    this.requestSettings() // requestSettings will add the options to the select element

    // this.sendToPlugin( { showTitle }, "updateTitle")
  }

  /**
   * Only update the title global visibility status.
   * @private
   */
  private async onUpdateTitleButtonPressed() {
    this.logMessage('onUpdateTitleButtonPressed()')

    const showTitle = this.showTitleElement.checked ? "on" : "off"
    // this.settingsManager.setGlobalSettings({ showTitle })
    this.setSettings({
      showTitle
    })

    // this.sendToPlugin( { showTitle }, "updateTitle")
  }

  // Prefill PI elements from cache
  @SDOnPiEvent('globalSettingsAvailable')
  propertyInspectorDidAppear(): void {
    this.logMessage('propertyInspectorDidAppear()')
    this.initElements();
    this.requestSettings()
    const globalSettings = this.settingsManager.getGlobalSettings<GlobalSettingsInterface>()
    // this.showTitleElement.checked = true

    if (isGlobalSettingsSet(globalSettings)) {
      // const showTitle = globalSettings.showTitle
      // this.showTitleElement.checked = (showTitle === "on");

      const password = globalSettings.password;
      if(password) {
        this.passwordElement.value = password;
      }
      const host = globalSettings.host;
      if(host) {
        this.hostElement.value = host;
      }

      const port = globalSettings.port;
      if(port) {
        this.portElement.value = port;
      }
    }
  }

  // Update per button settings
  @SDOnPiEvent('didReceiveSettings')
  onReceiveSettings({
                      payload,
                    }: DidReceiveSettingsEvent<ActionSettingsInterface>): void {
    this.logMessage("onReceiveSettings()")
    this.logMessage("payload.settings=" + JSON.stringify(payload.settings))
    this.logMessage("this.actionElement=" + this.actionElement)

    // This method will be called two times, the first time actionElement is undefined
    if(this.actionElement) {
      this.actionElement.value = payload.settings.action ?? "";
    }

    if(this.runConfigurationNameElement) {
      this.runConfigurationNameElement.value = payload.settings.runConfig ?? "";
    }
  }
}

new IdeaPI()
