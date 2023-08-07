import {DefaultAction} from "./default-action";

export class SearchEverywhereAction extends DefaultAction<SearchEverywhereAction> {

  actionId(): string {
    return "SearchEverywhere";
  }

  actionTitle():string {
    return "Search\nEverywhere";
  }
}
