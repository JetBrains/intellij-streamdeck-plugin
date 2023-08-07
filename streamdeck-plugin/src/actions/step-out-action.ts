import {DefaultAction} from "./default-action";

export class StepOutAction extends DefaultAction<StepOutAction> {
  actionId(): string {
    return "StepOut";
  }

  actionTitle():string {
    return "Step\nOut";
  }
}
