import {DefaultAction} from "./default-action";

export class GitPullAction extends DefaultAction<GitPullAction> {
  // constructor(public plugin: IdeaPlugin, private actionName: string) {
  //   super(plugin, actionName)
  // }

  actionId(): string {
    return "Vcs.UpdateProject";
  }

  actionTitle():string {
    return "VCS\nUpdate";
  }
}
