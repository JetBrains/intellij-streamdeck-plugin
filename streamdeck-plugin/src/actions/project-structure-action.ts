import {DefaultAction} from "./default-action";

export class ShowProjectStructureAction extends DefaultAction<ShowProjectStructureAction> {

  actionId(): string {
    return "ShowProjectStructureSettings";
  }

  actionTitle():string {
    return "Project\nStructure";
  }
}
