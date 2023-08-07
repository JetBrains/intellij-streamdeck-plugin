import {DefaultAction} from "./default-action";

export class NewProjectAction extends DefaultAction<NewProjectAction> {
  // constructor(public plugin: IdeaPlugin, private actionName: string) {
  //   super(plugin, actionName)
  // }

  actionId(): string {
    return "NewProject";
  }

  actionTitle():string {
    return "New\nProject";
  }
}

