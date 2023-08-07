import { Subject } from 'rxjs';
import {
    KeyDownEvent,
    KeyUpEvent, SDOnActionEvent,
    StreamDeckAction,
    WillAppearEvent,
    WillDisappearEvent,
} from 'streamdeck-typescript';

import { IdeaPlugin } from '../idea-plugin'
import {GlobalSettingsInterface, SceneSettingsInterface} from "../utils/interface";
import {fetchApi, fetchJetBrainsIDE, isGlobalSettingsSet} from "../utils";
import {Status} from "../types";

export abstract class DefaultAction<Instance> extends StreamDeckAction<
    IdeaPlugin,
    Instance
> {
    public constructor(public plugin: IdeaPlugin, actionName: string) {
        super(plugin, actionName);
        console.log(`Initialized ${actionName}`);
    }

    /**
     * IDEA action ID should be returned, eg: Run, Debug
     */
    abstract actionId(): string;

    actionTitle():string {
        return this.actionId();
    }

    @SDOnActionEvent('keyUp')
    public async onKeyUp({ payload }: KeyUpEvent<SceneSettingsInterface>): Promise<void> {
        console.log('onKeyUp() actionId=' + this.actionId())
        let action = payload.settings.action
        console.log('onKeyUp() customAction=' + action)

        if(action == null || action === '') {
            action = this.actionId()
        }

        if(action == null || action === '') {
            return
        }

        const globalSettings = this.plugin.settingsManager.getGlobalSettings<GlobalSettingsInterface>()
        let host:string = '127.0.0.1'
        let password:string = ''
        let port:string = ''

        if (isGlobalSettingsSet(globalSettings)) {
            host = globalSettings.host
        }

        if(globalSettings !== undefined) {
            const settings:GlobalSettingsInterface = globalSettings as GlobalSettingsInterface
            password = settings.password
            port = settings.port
        }

        // await fetchApi<Status>({
        //     endpoint: `/api/action/${this.actionId()}`,
        //     accessToken: '',
        //     method: 'GET',
        // })

        await fetchJetBrainsIDE<Status>({
            endpoint: `/api/action/${action}`,
            port:port,
            password: password,
            accessToken: '',
            host: host,
            method: 'GET',
        })

    }

    @SDOnActionEvent('willAppear')
    onContextAppear({ context , payload }: WillAppearEvent) {
        console.log('onContextAppear() actionId=' + this.actionId())
        let actionTitle = payload.settings.action

        console.log('onContextAppear() customAction=' + actionTitle)

        if(actionTitle == null || actionTitle === '') {
            actionTitle =  this.actionTitle()
        }

        if(actionTitle == null || actionTitle === '') {
            actionTitle = this.actionId()
        }

        if(actionTitle == null || actionTitle === '') {
            return
        }

        this.plugin.setTitle(
            actionTitle,
            context
        );
    }

    @SDOnActionEvent('willDisappear')
    onContextDisappear(event: WillDisappearEvent): void {
    }
}
