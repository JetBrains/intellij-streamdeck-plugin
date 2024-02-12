import {
    DidReceiveGlobalSettingsEvent,
    DidReceiveSettingsEvent,
    KeyUpEvent,
    SDOnActionEvent,
    SendToPluginEvent,
    StreamDeckAction,
    WillAppearEvent,
    WillDisappearEvent
} from 'streamdeck-typescript';

import {IdeaPlugin} from '../idea-plugin'
import {ActionSettingsInterface, GlobalSettingsInterface} from "../utils/interface";
import {fetchJetBrainsIDE, isGlobalSettingsSet} from "../utils";
import {Status} from "../types";

export abstract class DefaultAction<Instance> extends StreamDeckAction<
    IdeaPlugin,
    Instance
> {

    protected context: string;
    protected customTitle: string;
    protected showTitle: string;

    public constructor(public plugin: IdeaPlugin, actionName: string) {
        super(plugin, actionName);
        console.log(`Initialized ${actionName}`);
    }

    /**
     * IDEA action ID should be returned, eg: Run, Debug
     */
    abstract actionId(): string;

    /**
     * Allow customize button title, default to action id.
     */
    actionTitle(): string {
        return this.actionId();
    }

    @SDOnActionEvent('keyUp')
    public async onKeyUp({payload}: KeyUpEvent<ActionSettingsInterface>): Promise<void> {
        console.log('onKeyUp() actionId=' + this.actionId())
        let action = payload.settings.action // current button's customized action ID
        let runConfig = payload.settings.runConfig
        console.log('onKeyUp() customAction=' + action)

        if (action == null || action === '') {
            action = this.actionId()
        }

        if (action == null || action === '') {
            if(this.context != null) {
                this.plugin.showAlert(this.context);
            }

            return
        }

        const globalSettings = this.plugin.settingsManager.getGlobalSettings<GlobalSettingsInterface>()
        let host: string = '127.0.0.1'
        let password: string = ''
        let port: string = ''

        if (isGlobalSettingsSet(globalSettings)) {
            host = globalSettings.host
        }

        if(host === undefined || host === '') {
            host = '127.0.0.1'
        }

        if (globalSettings !== undefined) {
            const settings: GlobalSettingsInterface = globalSettings as GlobalSettingsInterface
            password = settings.password
            port = settings.port
        }

        // Handle customized run/debug configuration
        let endpoint = `/api/action/${action}`;

        if (runConfig == null || runConfig === undefined) {
            runConfig = ''
        }

        console.log('runConfig=' + runConfig)
        if(runConfig !== '') {
            endpoint += '?name=' + encodeURIComponent(runConfig)
        }

        await fetchJetBrainsIDE({
            endpoint: endpoint,
            port: port,
            password: password,
            accessToken: '',
            host: host,
            method: 'GET',
        })

    }

    @SDOnActionEvent('willAppear')
    onContextAppear({context, payload}: WillAppearEvent) {
        console.log('onContextAppear() actionId=' + this.actionId() + " context=" + context)
        this.context = context // Save for later update title
        this.readCustomActionTitle(payload.settings)
        this.toggleTitleVisible()
    }

    private readCustomActionTitle(settings: ActionSettingsInterface): void {
        let actionTitle = settings.action
        if (actionTitle == null || actionTitle === '') {
            actionTitle = this.actionTitle()
        }

        if (actionTitle == null || actionTitle === '') {
            actionTitle = this.actionId()
        }

        this.customTitle = actionTitle;
    }

    toggleTitleVisible(): void {
        if (this.showTitle !== "on" && this.context != undefined) {
            this.plugin.setTitle("", this.context);
        } else if (this.context != undefined) {
            if (this.customTitle == null || this.customTitle === '') {
                this.plugin.setTitle("", this.context);
                return
            } else {
                this.plugin.setTitle(this.customTitle, this.context);
            }
        }
    }

    @SDOnActionEvent('willDisappear')
    onContextDisappear(event: WillDisappearEvent): void {
    }

    // TODO Not work?!
    @SDOnActionEvent('sendToPlugin')
    onSendToPluginEvent({context, payload}: SendToPluginEvent): void {
        console.log('onSendToPluginEvent() payload.showTitle=' + payload.showTitle)
    }

    /**
     * Update current button's title based on the customized action id (if any)
     * @param context
     * @param settings
     * @private
     */
    @SDOnActionEvent('didReceiveSettings')
    private onSettings({context, payload: {settings}}: DidReceiveSettingsEvent<ActionSettingsInterface>) {
        console.log('onSettings() settings.action=' + settings.action)
        console.log('onSettings() settings.showTitle=' + settings.showTitle)
        this.showTitle = settings.showTitle;
        this.readCustomActionTitle(settings)
        this.toggleTitleVisible()
    }

    /**
     * Once triggered the title visible checkbox in the Property Inspection page, this event will be fired again but
     * no context provided.
     * @param settings
     * @private
     */
    @SDOnActionEvent('didReceiveGlobalSettings')
    private onReceiveGlobalSettings({payload: {settings}}: DidReceiveGlobalSettingsEvent<GlobalSettingsInterface>) {
        // this.plugin.setTitle(settings.count.toString() ?? 0, context);
        // console.log('onReceiveGlobalSettings() payload.showTitle=' + settings.showTitle)
        // this.showTitle = settings.showTitle;

        // console.log('onReceiveGlobalSettings() this.context=' + this.context)
        // this.toggleTitleVisible();
    }
}
