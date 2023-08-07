package com.jetbrains.ide.streamdeck.settings;

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.newui.UpdateButton;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.panel.PanelGridBuilder;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UI;
import com.jetbrains.ide.streamdeck.ActionServer;
import com.jetbrains.ide.streamdeck.ActionServerListener;
import com.jetbrains.ide.streamdeck.service.StreamDeckHttpService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.ide.BuiltInServerManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

/**
 * Preference UI.
 */
public class StreamDeckPreferenceComponent extends JPanel implements Disposable {
    private final ActionServerSettings myActionServerSettings = ActionServerSettings.getInstance();

    @NotNull
    private final IntegerField myPortIntField = new IntegerField(null,
            10,
            65535);
    /**
     * Ser token
     */
    private final JBTextField myPasswordField = new JBTextField();

    private final JBCheckBox myEnableCheckBox =
            new JBCheckBox("Enable Stream Deck service");

    private final JBCheckBox myFocusOnly =
            new JBCheckBox("Perform actions only when IDE window is focused");

    private final JBLabel statusLabel = new JBLabel();
    private final JBTextArea serverLogTextArea = new JBTextArea(10, 80);
    private final JBLabel builtInPortLabel = new JBLabel();
    private final UpdateButton serverControlButton;

    public StreamDeckPreferenceComponent() {
        setLayout(new BorderLayout());
        serverControlButton = createServerControlButton();

        updateFromSettings();
        updateServerStatus();

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new VerticalLayout(4));
        contentPane.add(createMiscSetting());
        contentPane.add(createServerStatusSetting());
        add(contentPane, BorderLayout.CENTER);

        ActionServerListener.subscribe(this, new ActionServerListener() {
            @Override
            public void statusChanged() {
                updateServerStatus();
            }
        });
    }

    protected JComponent createServerStatusSetting() {
        PanelGridBuilder builder = UI.PanelFactory.grid();
        builder.add(UI.PanelFactory.panel(builtInPortLabel).withLabel("IDE built-in server port:"));

        builder.add(UI.PanelFactory.panel(statusLabel).withLabel("Started:"));
        // builder.add(UI.PanelFactory.panel(serverControlButton).withLabel("Control:"));
        serverLogTextArea.setBorder(IdeBorderFactory.createBorder());
        builder.add(UI.PanelFactory.panel(serverLogTextArea).withLabel("Server log:"));


        JPanel pane = builder.createPanel();
        pane.setBorder(IdeBorderFactory.createTitledBorder("Stream Deck Server Status"));
        return pane;
    }

    protected JComponent createMiscSetting() {
        PanelGridBuilder builder = UI.PanelFactory.grid();
        // builder.add(UI.PanelFactory.panel(myPortIntField).withLabel("Http remote action server listener port:").
        //         withComment("Default value is 21420"));
        builder.add(UI.PanelFactory.panel(myPasswordField).withLabel("Password for client connection:").
                withComment("Optional, could be empty"));
        builder.add(UI.PanelFactory.panel(myEnableCheckBox));
        builder.add(UI.PanelFactory.panel(myFocusOnly).withComment("Once enabled, StreamDeck actions are performed only when the IDE window is focused"));

        JPanel pane = builder.createPanel();
        pane.setBorder(IdeBorderFactory.createTitledBorder("Stream Deck Server Options"));
        return pane;
    }

     UpdateButton createServerControlButton() {
        UpdateButton installButton = new UpdateButton();
//            installButton.setButtonColors(false);

        installButton.addActionListener(e -> {
            // boolean status = ActionServer.getInstance().isStarted();
            // if(status) {
            //     ActionServer.getInstance().close();
            // } else {
            //     try {
            //         ActionServer.getInstance().start();
            //     } catch (IOException ex) {
            //
            //     }
            // }
        });

        return installButton;
    }

    public boolean isModified() {
        return !Objects.equals(myPortIntField.getValue(), myActionServerSettings.getDefaultPort())
                || !Objects.equals(myPasswordField.getText(), myActionServerSettings.getPassword())
                || !Objects.equals(myEnableCheckBox.isSelected(), myActionServerSettings.getEnable() )
                || !Objects.equals(myFocusOnly.isSelected(), myActionServerSettings.getFocusOnly());
    }


    public void apply() {

        myActionServerSettings.setDefaultPort(myPortIntField.getValue());
        myActionServerSettings.setPassword(myPasswordField.getText());
        myActionServerSettings.setEnable(myEnableCheckBox.isSelected());
        myActionServerSettings.setFocusOnly(myFocusOnly.isSelected());
    }

    public void reset() {
        updateFromSettings();
    }

    private void updateFromSettings() {
        myPasswordField.setText(myActionServerSettings.getPassword());
        myPortIntField.setText(String.valueOf(myActionServerSettings.getDefaultPort()));

        myEnableCheckBox.setSelected(myActionServerSettings.getEnable());
        myFocusOnly.setSelected(myActionServerSettings.getFocusOnly());
    }

    private void updateServerStatus() {
        // status
        // boolean status = ActionServer.getInstance().isStarted();
        statusLabel.setText(myActionServerSettings.getEnable() ? "Yes" : "No");

        // serverControlButton.setEnabled(true);
        // serverControlButton.setText(myActionServerSettings.getEnable() ? "Stop Server":"Start Server");
        // serverControlButton.setIcon(myActionServerSettings.getEnable() ? AllIcons.Actions.Suspend : AllIcons.Debugger.ThreadRunning);

        builtInPortLabel.setText(String.valueOf(BuiltInServerManager.getInstance().getPort()));
        serverLogTextArea.setText(StreamDeckHttpService.serverLog.toString());

        // serverLogTextArea.setText(ActionServer.getInstance().getServerLog());
    }

    @Override
    public void dispose() {

    }
}
