import {DefaultAction} from "./default-action";

//
export class RunAction extends DefaultAction<RunAction> {
  // constructor(public plugin: IdeaPlugin, private actionName: string) {
  //   super(plugin, actionName)
  // }

  actionId(): string {
    return "Run";
  }
}
