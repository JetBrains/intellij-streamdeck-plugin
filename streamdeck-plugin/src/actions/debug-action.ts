import {KeyUpEvent, SDOnActionEvent, StateType, WillAppearEvent, WillDisappearEvent} from 'streamdeck-typescript';
import { IdeaPlugin } from '../idea-plugin'
import {DefaultAction} from './default-action';
import {distinctUntilChanged, map, takeUntil} from "rxjs/operators";

export class DebugAction extends DefaultAction<DebugAction> {

    // constructor(public plugin: IdeaPlugin, actionName: string) {
    //     super(plugin, actionName)
    // }

    actionId(): string {
        return "Debug";
    }
}