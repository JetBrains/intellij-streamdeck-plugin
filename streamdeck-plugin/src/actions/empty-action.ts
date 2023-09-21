import {DefaultAction} from './default-action';

export class EmptyAction extends DefaultAction<EmptyAction> {
    actionId(): string {
        return "";
    }

    // actionTitle():string {
    //     return "Please\nset\nAction Id";
    // }
}