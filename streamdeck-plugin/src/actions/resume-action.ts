import {DefaultAction} from "./default-action";

export class ResumeAction extends DefaultAction<ResumeAction> {

  actionId(): string {
    return "Resume";
  }
}
