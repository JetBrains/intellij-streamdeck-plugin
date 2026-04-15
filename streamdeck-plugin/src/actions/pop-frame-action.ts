/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import {DefaultAction} from "./default-action";

export class PopFrameAction extends DefaultAction<PopFrameAction> {
  actionId(): string {
    return "Debugger.PopFrame";
  }

  actionTitle(): string {
    return "Reset\nFrame";
  }
}