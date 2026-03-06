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
  // Global settings
  private hostElement: HTMLInputElement;
  private portElement: HTMLInputElement;
  private passwordElement: HTMLInputElement;
  // Action settings
  private actionElement: HTMLInputElement;
  private showTitleElement: HTMLInputElement;
  private runConfigurationNameElement: HTMLInputElement;
  private actionPortElement: HTMLInputElement;

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
    this.showTitleElement = document.getElementById('singlechk') as HTMLInputElement;
    this.runConfigurationNameElement = document.getElementById('run_config_name') as HTMLInputElement;
    this.actionPortElement = document.getElementById('action_port') as HTMLInputElement;
  }

  private saveAllSettings() {
    const password = this.passwordElement?.value
    const host = this.hostElement?.value
    const port = this.portElement?.value
    this.settingsManager.setGlobalSettings({ password, host, port })

    this.setSettings({
      action: this.actionElement?.value ?? "",
      showTitle: this.showTitleElement?.checked ? "on" : "off",
      runConfig: this.runConfigurationNameElement?.value ?? "",
      port: this.actionPortElement?.value ?? ""
    })
  }

  private registerAutoSave() {
    [
      // Global
      this.hostElement,
      this.portElement,
      this.passwordElement,
      // Action
      this.actionElement,
      this.runConfigurationNameElement,
      this.actionPortElement
    ].forEach(el => el?.addEventListener('input', () => this.saveAllSettings()))
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
    this.registerAutoSave();
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

    if(this.actionPortElement) {
      this.actionPortElement.value = payload.settings.port ?? "";
    }
  }
}

new IdeaPI()
