import {DefaultAction} from "./default-action";

export class ActionIdBrowserAction extends DefaultAction<ActionIdBrowserAction> {

  actionId(): string {
    return "streamdeck.show.action.browser";
  }

  actionTitle():string {
    return "Action\n Browser";
  }
}
