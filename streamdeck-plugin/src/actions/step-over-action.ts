import {DefaultAction} from "./default-action";

export class StepOverAction extends DefaultAction<StepOverAction> {
  actionId(): string {
    return "StepOver";
  }

  actionTitle():string {
    return "Step\nOver";
  }
}
