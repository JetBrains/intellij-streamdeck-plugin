import {DefaultAction} from "./default-action";

export class StopWithDropDownAction extends DefaultAction<StopWithDropDownAction> {

  actionId(): string {
    return "Stop";
  }

  actionTitle():string {
    return "Stop";
  }
}
