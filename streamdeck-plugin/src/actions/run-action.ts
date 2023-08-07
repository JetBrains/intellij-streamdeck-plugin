import { GlobalSettingsInterface, SceneSettingsInterface } from '../utils/interface'
import { KeyUpEvent, SDOnActionEvent, StreamDeckAction } from 'streamdeck-typescript'
import { fetchApi, isGlobalSettingsSet } from '../utils/index'

import { IdeaPlugin } from '../idea-plugin'
import { Status } from '../types'
import {DefaultAction} from "./default-action";

//
export class RunAction extends DefaultAction<RunAction> {
  // constructor(public plugin: IdeaPlugin, private actionName: string) {
  //   super(plugin, actionName)
  // }

  actionId(): string {
    return "Run\n";
  }
}
