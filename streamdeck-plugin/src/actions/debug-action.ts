import {DefaultAction} from './default-action';

export class DebugAction extends DefaultAction<DebugAction> {

    // constructor(public plugin: IdeaPlugin, actionName: string) {
    //     super(plugin, actionName)
    // }

    actionId(): string {
        return "Debug";
    }
}