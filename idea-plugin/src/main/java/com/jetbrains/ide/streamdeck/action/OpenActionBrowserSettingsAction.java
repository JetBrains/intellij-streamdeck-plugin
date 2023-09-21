package com.jetbrains.ide.streamdeck.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.jetbrains.ide.streamdeck.keymap.KeymapPanel;

public class OpenActionBrowserSettingsAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), KeymapPanel.class);
    }

    @Override
    public boolean isDumbAware() {
        return true;
    }
}
