import {DefaultAction} from "./default-action";

export class PauseAction extends DefaultAction<PauseAction> {

  actionId(): string {
    return "Pause";
  }
}
