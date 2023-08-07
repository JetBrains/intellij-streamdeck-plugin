package com.jetbrains.ide.streamdeck.settings;

import com.intellij.openapi.options.SearchableConfigurable;

import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActionServerConfigurable implements SearchableConfigurable {
    private StreamDeckPreferenceComponent configDialog;


    public String getDisplayName() {
        return "Stream Deck";
    }

    @Nullable
    public String getHelpTopic() {
        return "streamdeck.settings";
    }

    @NotNull
    public String getId() {
        return "streamdecke.settings";
    }

    public JComponent createComponent() {
        this.configDialog = new StreamDeckPreferenceComponent();
        return this.configDialog;
    }

    public synchronized boolean isModified() {
        return this.configDialog.isModified();
    }

    // 点OK时也会调用
    public synchronized void apply() {
        this.configDialog.apply();
    }

    public synchronized void reset() {
        this.configDialog.reset();
    }

    public synchronized void disposeUIResources() {
        this.configDialog = null;
    }
}
