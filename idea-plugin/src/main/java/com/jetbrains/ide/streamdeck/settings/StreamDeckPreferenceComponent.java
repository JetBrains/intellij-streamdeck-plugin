package com.jetbrains.ide.streamdeck.settings;

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.newui.UpdateButton;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.panel.PanelGridBuilder;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.*;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.UI;
import com.jetbrains.ide.streamdeck.RemoteActionServer;
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
    private final JBPasswordField myPasswordField = new JBPasswordField();

    private final JBCheckBox myEnableStreamDeckCheckBox =
            new JBCheckBox("Enable Stream Deck service");

    private final JBCheckBox myEnableRemoteStreamDeckCheckBox =
            new JBCheckBox("Enable Stream Deck remote action server");

    private final JBCheckBox myFocusOnly =
            new JBCheckBox("Perform action only when IDE window is focused");

    private final JBLabel statusLabel = new JBLabel();
    private final JBTextArea serverLogTextArea = new JBTextArea(10, 60);
    private final JBLabel builtInPortLabel = new JBLabel();
    private final JButton serverControlButton;

    public StreamDeckPreferenceComponent() {
        setLayout(new BorderLayout());
        serverControlButton = createServerControlButton();
        serverLogTextArea.setWrapStyleWord(true);
        serverLogTextArea.setLineWrap(true);
//        updateFromSettings();
        updateServerStatus();

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new VerticalLayout(4));
        contentPane.add(createMiscSetting());
        contentPane.add(createRemoteHttpSetting());
        contentPane.add(createRemoteServerStatusSetting());
        contentPane.add(serverControlButton);
        contentPane.add(new JBLabel("Server Log:"));
        contentPane.add(new JBScrollPane(serverLogTextArea));
        add(contentPane, BorderLayout.CENTER);

        ActionServerListener.subscribe(this, new ActionServerListener() {
            @Override
            public void statusChanged() {
                updateServerStatus();
            }
        });
    }

    protected JComponent createRemoteServerStatusSetting() {
        PanelGridBuilder builder = UI.PanelFactory.grid();
        builder.add(UI.PanelFactory.panel(builtInPortLabel).withLabel("IDE built-in server port:"));

        builder.add(UI.PanelFactory.panel(statusLabel).withLabel("Remote server started:"));
//        builder.add(UI.PanelFactory.panel(serverControlButton).withLabel(""));
//        serverLogTextArea.setBorder(IdeBorderFactory.createBorder());
//        builder.add(UI.PanelFactory.panel(new JBScrollPane(serverLogTextArea)).withLabel("Server log:"));

        JPanel pane = builder.createPanel();
        pane.setBorder(IdeBorderFactory.createTitledBorder("Action Server Status"));
        return pane;
    }

    protected JComponent createMiscSetting() {
        PanelGridBuilder builder = UI.PanelFactory.grid();
        builder.add(UI.PanelFactory.panel(myPasswordField).withLabel("Password:").
                withComment("Optional, could be empty, recommended when you enable remote HTTP action server"));
        builder.add(UI.PanelFactory.panel(myEnableStreamDeckCheckBox));
        builder.add(UI.PanelFactory.panel(myFocusOnly).withComment("Once enabled, StreamDeck actions are performed only when the IDE window is focused"));

        JPanel pane = builder.createPanel();
        pane.setBorder(IdeBorderFactory.createTitledBorder("General Action Options"));
        return pane;
    }

    protected JComponent createRemoteHttpSetting() {
        PanelGridBuilder builder = UI.PanelFactory.grid();
        builder.add(UI.PanelFactory.panel(myPortIntField).withLabel("Remote Port:").
                withComment("Default value is 21420"));
        builder.add(UI.PanelFactory.panel(myEnableRemoteStreamDeckCheckBox).withComment(
                "Once enabled, will start remote action server automatically once IDE started, thus IDE actions can be execute remotely and listen to the remote port."));

        JPanel pane = builder.createPanel();
        pane.setBorder(IdeBorderFactory.createTitledBorder("Remote Action Server Options"));
        return pane;
    }

    private JButton createServerControlButton() {
        JButton installButton = new UpdateButton();
        UpdateButton.setWidth( installButton, 32);
//            installButton.setButtonColors(false);

        installButton.addActionListener(e -> {
            boolean status = RemoteActionServer.getInstance().isStarted();
            if (status) {
                RemoteActionServer.getInstance().close();
            } else {
                try {
                    RemoteActionServer.getInstance().start();
                } catch (IOException ex) {

                }
            }
        });

        return installButton;
    }

    public boolean isModified() {
        return !Objects.equals(myPortIntField.getValue(), myActionServerSettings.getDefaultPort())
                || !Objects.equals(new String(myPasswordField.getPassword()), myActionServerSettings.getPassword())
                || !Objects.equals(myEnableStreamDeckCheckBox.isSelected(), myActionServerSettings.getEnable())
                || !Objects.equals(myEnableRemoteStreamDeckCheckBox.isSelected(), myActionServerSettings.getEnableRemote())
                || !Objects.equals(myFocusOnly.isSelected(), myActionServerSettings.getFocusOnly());
    }


    public void apply() {
        myActionServerSettings.setDefaultPort(myPortIntField.getValue());
        myActionServerSettings.setPassword(new String(myPasswordField.getPassword()));
        myActionServerSettings.setEnable(myEnableStreamDeckCheckBox.isSelected());
        myActionServerSettings.setEnableRemote(myEnableRemoteStreamDeckCheckBox.isSelected());
        myActionServerSettings.setFocusOnly(myFocusOnly.isSelected());
    }

    public void reset() {
        updateFromSettings();
    }

    private void updateFromSettings() {
        myPasswordField.setText(myActionServerSettings.getPassword());
        myPortIntField.setText(String.valueOf(myActionServerSettings.getDefaultPort()));

        myEnableStreamDeckCheckBox.setSelected(myActionServerSettings.getEnable());
        myEnableRemoteStreamDeckCheckBox.setSelected(myActionServerSettings.getEnableRemote());
        myFocusOnly.setSelected(myActionServerSettings.getFocusOnly());
    }

    private void updateServerStatus() {
        boolean status = RemoteActionServer.getInstance().isStarted();
        statusLabel.setText(status ? "Yes" : "No");

        serverControlButton.setEnabled(true);
        serverControlButton.setText(status ? "Stop Remote Server" : "Start Remote Server");
        serverControlButton.setIcon(status ? AllIcons.Actions.Suspend : AllIcons.Debugger.ThreadRunning);

        builtInPortLabel.setText(String.valueOf(BuiltInServerManager.getInstance().getPort()));
        serverLogTextArea.setText(StreamDeckHttpService.serverLog + RemoteActionServer.getInstance().getServerLog());
    }

    @Override
    public void dispose() {

    }
}
