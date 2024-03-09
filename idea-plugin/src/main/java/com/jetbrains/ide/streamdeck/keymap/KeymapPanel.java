/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package com.jetbrains.ide.streamdeck.keymap;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.diagnostic.VMOptions;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.QuickList;
import com.intellij.openapi.actionSystem.ex.QuickListsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.*;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.keymap.impl.SystemShortcuts;
import com.intellij.openapi.keymap.impl.ui.KeymapListener;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.OptionsBundle;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.packageDependencies.ui.TreeExpansionMonitor;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.mac.foundation.NSDefaults;
import com.intellij.ui.mac.touchbar.Helpers;
import com.intellij.ui.mac.touchbar.TouchbarSupport;
import com.intellij.util.Alarm;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.IoErrorText;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.function.Supplier;

import static com.intellij.openapi.actionSystem.impl.ActionToolbarImpl.updateAllToolbarsImmediately;

public final class KeymapPanel extends JPanel implements SearchableConfigurable, Configurable.NoScroll, KeymapListener, Disposable {
  private JCheckBox nationalKeyboardsSupport;

  private final KeymapSelector myKeymapSelector = new KeymapSelector(this::currentKeymapChanged);
  private final KeymapSchemeManager myManager = myKeymapSelector.getManager();
  private final ActionsTree myActionsTree = new ActionsTree();
  private FilterComponent myFilterComponent;
  private TreeExpansionMonitor myTreeExpansionMonitor;
  private final @NotNull ShortcutFilteringPanel myFilteringPanel = new ShortcutFilteringPanel();

  private boolean myQuickListsModified = false;
  private QuickList[] myQuickLists = QuickListsManager.getInstance().getAllQuickLists();

  private ShowFNKeysSettingWrapper myShowFN;

  private boolean myShowOnlyConflicts;

  public KeymapPanel() { this(false); }

  public KeymapPanel(boolean showOnlyConflicts) {
    myShowOnlyConflicts = showOnlyConflicts;
    setLayout(new BorderLayout());
    JPanel keymapPanel = new JPanel(new BorderLayout());
    keymapPanel.add(myManager.getSchemesPanel(), BorderLayout.NORTH);
    keymapPanel.add(createKeymapSettingsPanel(), BorderLayout.CENTER);

    IdeFrame ideFrame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
    if (ideFrame != null && NationalKeyboardSupport.isSupportedKeyboardLayout(ideFrame.getComponent())) {
      nationalKeyboardsSupport = new JCheckBox(
        new AbstractAction(KeyMapBundle.message(NationalKeyboardSupport.getKeymapBundleKey())) {
          @Override
          public void actionPerformed(ActionEvent e) {
            NationalKeyboardSupport.getInstance().setEnabled(nationalKeyboardsSupport.isSelected());
            try {
              VMOptions.setProperty(NationalKeyboardSupport.getVMOption(), Boolean.toString(NationalKeyboardSupport.getInstance().getEnabled()));
              ApplicationManager.getApplication().invokeLater(
                () -> ApplicationManager.getApplication().restart(),
                ModalityState.nonModal()
              );
            }
            catch (IOException x) {
              Messages.showErrorDialog(keymapPanel, IoErrorText.message(x), OptionsBundle.message("cannot.save.settings.default.dialog.title"));
            }
          }
        });
      nationalKeyboardsSupport.setSelected(NationalKeyboardSupport.getInstance().getEnabled());
      nationalKeyboardsSupport.setBorder(JBUI.Borders.empty());
      keymapPanel.add(nationalKeyboardsSupport, BorderLayout.SOUTH);
    }

    add(keymapPanel, BorderLayout.CENTER);
    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(final @NotNull PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("ancestor") && evt.getNewValue() != null && evt.getOldValue() == null && myQuickListsModified) {
          currentKeymapChanged();
          myQuickListsModified = false;
        }
      }
    });
    myFilteringPanel.addPropertyChangeListener("shortcut", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        filterTreeByShortcut(myFilteringPanel.getShortcut());
      }
    });
  }

  @Override
  public void updateUI() {
    super.updateUI();
    //noinspection ConstantValue -- can be called during superclass initialization
    if (myFilteringPanel != null) {
      SwingUtilities.updateComponentTreeUI(myFilteringPanel);
    }
  }

  @Override
  public void quickListRenamed(final @NotNull QuickList oldQuickList, final @NotNull QuickList newQuickList) {
    myManager.visitMutableKeymaps(keymap -> {
      String actionId = oldQuickList.getActionId();
      Shortcut[] shortcuts = keymap.getShortcuts(actionId);
      if (shortcuts.length != 0) {
        String newActionId = newQuickList.getActionId();
        for (Shortcut shortcut : shortcuts) {
          removeShortcut(keymap, actionId, shortcut);
          addShortcut(keymap, newActionId, shortcut);
        }
      }
    });
    myQuickListsModified = true;
  }

  private static void addShortcut(Keymap keymap, String actionId, Shortcut shortcut) {
    if (keymap instanceof KeymapImpl) {
      ((KeymapImpl)keymap).addShortcut(actionId, shortcut);
    }
    else {
      keymap.addShortcut(actionId, shortcut);
    }
  }

  private static void removeShortcut(Keymap keymap, String actionId, Shortcut shortcut) {
    if (keymap instanceof KeymapImpl) {
      ((KeymapImpl)keymap).removeShortcut(actionId, shortcut);
    }
    else {
      keymap.removeShortcut(actionId, shortcut);
    }
  }

  @Override
  public Runnable enableSearch(final String option) {
    return () -> showOption(option);
  }

  @Override
  public void processCurrentKeymapChanged() {
    currentKeymapChanged();
  }

  @Override
  public void processCurrentKeymapChanged(QuickList @NotNull [] ids) {
    myQuickLists = ids;
    currentKeymapChanged();
  }

  private void currentKeymapChanged() {
    currentKeymapChanged(myManager.getSelectedKeymap());
  }

  private void currentKeymapChanged(Keymap selectedKeymap) {
    if (selectedKeymap == null) selectedKeymap = new KeymapImpl();
    SystemShortcuts systemShortcuts = SystemShortcuts.getInstance();
    systemShortcuts.updateKeymapConflicts(selectedKeymap);
    myActionsTree.setBaseFilter(myShowOnlyConflicts ? systemShortcuts.createKeymapConflictsActionFilter() : null);
    myActionsTree.reset(selectedKeymap, myQuickLists, myFilteringPanel.getShortcut());
  }

  private JPanel createKeymapSettingsPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(createToolbarPanel(), BorderLayout.NORTH);
    panel.add(myActionsTree.getComponent(), BorderLayout.CENTER);

    myTreeExpansionMonitor = TreeExpansionMonitor.install(myActionsTree.getTree());

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(@NotNull MouseEvent e) {
        editSelection(e, true);
        return true;
      }
    }.installOn(myActionsTree.getTree());


    myActionsTree.getTree().addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(@NotNull MouseEvent e) {
        if (e.isPopupTrigger()) {
          editSelection(e, false);
          e.consume();
        }
      }

      @Override
      public void mouseReleased(@NotNull MouseEvent e) {
        if (e.isPopupTrigger()) {
          editSelection(e, false);
          e.consume();
        }
      }
    });

    if (TouchbarSupport.isAvailable()) {
      myShowFN = new ShowFNKeysSettingWrapper();
      if (myShowFN.getCheckbox() != null) {
        panel.add(myShowFN.getCheckbox(), BorderLayout.SOUTH);
      }
    }

    return panel;
  }

  private JPanel createToolbarPanel() {
    DefaultActionGroup group = new DefaultActionGroup();
    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("KeymapEdit", group, true);
    toolbar.setTargetComponent(myActionsTree.getTree());
    final CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
    final TreeExpander treeExpander = createTreeExpander(myActionsTree);
    group.add(commonActionsManager.createExpandAllAction(treeExpander, myActionsTree.getTree()));
    group.add(commonActionsManager.createCollapseAllAction(treeExpander, myActionsTree.getTree()));
    group.add(new CopyActionIdAction());
    group.add(new CopyActionNameAction());
    group.add(new CopyActionClassNameAction());
    group.add(new CopyAllActionInfoAction());


    group = new DefaultActionGroup();
    ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Keymap", group, true);
    actionToolbar.setTargetComponent(myActionsTree.getTree());
    actionToolbar.setReservePlaceAutoPopupIcon(false);
    final JComponent searchToolbar = actionToolbar.getComponent();
    final Alarm alarm = new Alarm();
    myFilterComponent = new FilterComponent("KEYMAP", 5) {
      @Override
      public void filter() {
        alarm.cancelAllRequests();
        alarm.addRequest(() -> {
          if (!myFilterComponent.isShowing()) return;
          myTreeExpansionMonitor.freeze();
          myFilteringPanel.setShortcut(null);
          final String filter = getFilter();
          myActionsTree.filter(filter, myQuickLists);
          final JTree tree = myActionsTree.getTree();
          TreeUtil.expandAll(tree);
          if (filter == null || filter.length() == 0) {
            TreeUtil.collapseAll(tree, 0);
            myTreeExpansionMonitor.restore();
          }
          else {
            myTreeExpansionMonitor.unfreeze();
          }
        }, 300);
      }
    };
    myFilterComponent.reset();

    group.add(new FindByShortcutAction(searchToolbar));

    group.add(new ClearFilteringAction());

    JPanel panel = new JPanel(new GridLayout(1, 2));
    panel.add(toolbar.getComponent());
    panel.add(new BorderLayoutPanel().addToCenter(myFilterComponent).addToRight(searchToolbar));
    return panel;
  }

  public static @NotNull TreeExpander createTreeExpander(@NotNull ActionsTree actionsTree) {
    return new DefaultTreeExpander(actionsTree::getTree);
  }

  private void filterTreeByShortcut(Shortcut shortcut) {
    boolean wasFreezed = myTreeExpansionMonitor.isFreeze();
    if (!wasFreezed) myTreeExpansionMonitor.freeze();
    myActionsTree.filterTree(shortcut, myQuickLists);
    final JTree tree = myActionsTree.getTree();
    TreeUtil.expandAll(tree);
    if (!wasFreezed) myTreeExpansionMonitor.restore();
  }

  public void showOption(String option) {
    currentKeymapChanged();
    myFilterComponent.setFilter(option);
    myFilteringPanel.setShortcut(null);
    myActionsTree.filter(option, myQuickLists);
  }

  @Override
  public @NotNull String getId() {
    return "preferences.keymap";
  }

  @Override
  public void reset() {
    if (nationalKeyboardsSupport != null) {
      nationalKeyboardsSupport.setSelected(NationalKeyboardSupport.getInstance().getEnabled());
    }
    myManager.reset();
  }

  @Override
  public void apply() throws ConfigurationException {
    String error = myManager.apply();
    if (error != null) throw new ConfigurationException(error);
    updateAllToolbarsImmediately();

    if (myShowFN != null)
      myShowFN.applyChanges();
  }

  @Override
  public boolean isModified() {
    return myManager.isModified() || (myShowFN != null && myShowFN.isModified());
  }

  @Override
  public @Nls String getDisplayName() {
    return KeyMapBundle.message("keymap.display.name");
  }

  @Override
  public String getHelpTopic() {
    return "preferences.keymap";
  }

  @Override
  public JComponent createComponent() {
    if (myShowFN != null) {
      Disposer.register(this, myShowFN);
    }
    KeymapExtension.EXTENSION_POINT_NAME.addChangeListener(this::currentKeymapChanged, this);
    myKeymapSelector.attachKeymapListener(this);
    ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(CHANGE_TOPIC, this);
    return this;
  }

  @Override
  public void disposeUIResources() {
    myFilteringPanel.hidePopup();
    if (myFilterComponent != null) {
      myFilterComponent.dispose();
    }
    Disposer.dispose(this);
  }

  @Override
  public void dispose() {
  }

  public Shortcut @Nullable [] getCurrentShortcuts(@NotNull String actionId) {
    Keymap keymap = myManager.getSelectedKeymap();
    return keymap == null ? null : keymap.getShortcuts(actionId);
  }

  private void editSelection(InputEvent e, boolean isDoubleClick) {
    String actionId = myActionsTree.getSelectedActionId();
    if (actionId == null) return;

    Keymap selectedKeymap = myManager.getSelectedKeymap();
    if (selectedKeymap == null) return;

    DefaultActionGroup group = createEditActionGroup(actionId, selectedKeymap);
    if (e instanceof MouseEvent && ((MouseEvent)e).isPopupTrigger()) {
      ActionManager.getInstance()
        .createActionPopupMenu("popup@Keymap.ActionsTree.Menu", group)
        .getComponent()
        .show(e.getComponent(), ((MouseEvent)e).getX(), ((MouseEvent)e).getY());
    }
    else if (!isDoubleClick || !ActionManager.getInstance().isGroup(actionId)) {
      DataContext dataContext = DataManager.getInstance().getDataContext(this);
      ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(IdeBundle.message("popup.title.edit.shortcuts"),
                                                                            group,
                                                                            dataContext,
                                                                            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                                                            true);

      if (e instanceof MouseEvent) {
        popup.show(new RelativePoint((MouseEvent)e));
      }
      else {
        popup.showInBestPositionFor(dataContext);
      }
    }
  }

  private @NotNull DefaultActionGroup createEditActionGroup(@NotNull String actionId, Keymap selectedKeymap) {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new CopyActionIdAction());
    group.add(new CopyActionNameAction());
    group.add(new CopyActionClassNameAction());
    group.add(new CopyAllActionInfoAction());
    return group;
  }

  private void showHint(@NotNull String message) {
    HintManager.getInstance().hideAllHints();
    final JLabel label = new JLabel(message);
    label.setBorder(HintUtil.createHintBorder());
    label.setBackground(HintUtil.getInformationColor());
    label.setOpaque(true);
    HintManager.getInstance().showHint(label, RelativePoint.getCenterOf(myActionsTree.getComponent()),
            HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE, -1);
  }

  private static final class ShowFNKeysSettingWrapper implements Disposable {
    private boolean myShowFnInitial = false;
    private JCheckBox myCheckbox = null;
    private volatile boolean myDisposed;

    ShowFNKeysSettingWrapper() {
      if (TouchbarSupport.isAvailable()) {
        final String appId = Helpers.getAppId();
        if (appId != null && !appId.isEmpty()) {
          myShowFnInitial = NSDefaults.isShowFnKeysEnabled(appId);
          myCheckbox = new JCheckBox(KeyMapBundle.message("keymap.show.f.on.touch.bar"), myShowFnInitial);
        } else
          Logger.getInstance(KeymapPanel.class).error("can't obtain application id from NSBundle");
      }
    }

    JCheckBox getCheckbox() { return myCheckbox; }

    boolean isModified() { return myCheckbox != null && myShowFnInitial != myCheckbox.isSelected(); }

    void applyChanges() {
      if (!TouchbarSupport.isAvailable() || myCheckbox == null || !isModified())
        return;

      final String appId = Helpers.getAppId();
      if (appId == null || appId.isEmpty()) {
        Logger.getInstance(KeymapPanel.class).error("can't obtain application id from NSBundle");
        return;
      }

      final boolean prevVal = myShowFnInitial;
      myShowFnInitial = myCheckbox.isSelected();
      NSDefaults.setShowFnKeysEnabled(appId, myShowFnInitial);
      TouchbarSupport.enable(!myShowFnInitial);

      if (myShowFnInitial != NSDefaults.isShowFnKeysEnabled(appId)) {
        NSDefaults.setShowFnKeysEnabled(appId, myShowFnInitial, true); // try again with extra checks
        if (myShowFnInitial != NSDefaults.isShowFnKeysEnabled(appId))
          return;
      }

      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        final boolean result = Helpers.restartTouchBarServer();
        if (!result) {
          // System.out.println("can't restart touchbar-server, roll back settings");
          myShowFnInitial = prevVal;
          NSDefaults.setShowFnKeysEnabled(appId, myShowFnInitial);
          TouchbarSupport.enable(!myShowFnInitial);

          if (!myDisposed) {
            // System.out.println("ui wasn't disposed, invoke roll back of checkbox state");
            ApplicationManager.getApplication().invokeLater(() -> {
              if (!myDisposed)
                myCheckbox.setSelected(prevVal);
            }, ModalityState.stateForComponent(myCheckbox));
          }
        }
      });
    }

    @Override
    public void dispose() {
      if (!myDisposed) {
        myDisposed = true;
        myCheckbox = null;
      }
    }
  }

  private static final class SafeKeymapAccessor {
    private final Component parent;
    private final Keymap selected;
    private KeymapSchemeManager manager;
    private Keymap mutable;

    SafeKeymapAccessor(@NotNull Component parent, @NotNull Keymap selected) {
      this.parent = parent;
      this.selected = selected;
    }

    Keymap keymap() {
      if (mutable == null) {
        if (parent instanceof KeymapPanel panel) {
          mutable = panel.myManager.getMutableKeymap(selected);
        }
        else {
          if (manager == null) {
            manager = new KeymapSelector(selectedKeymap -> { }).getManager();
            manager.reset();
          }
          mutable = manager.getMutableKeymap(selected);
        }
      }
      return mutable;
    }

    void add(@NotNull String actionId, @NotNull Shortcut newShortcut) {
      Keymap keymap = keymap();
      Shortcut[] shortcuts = keymap.getShortcuts(actionId);
      for (Shortcut shortcut : shortcuts) {
        if (shortcut.equals(newShortcut)) {
          // if shortcut is already registered to this action, just select it
          if (manager != null) manager.apply();
          return;
        }
      }
      addShortcut(keymap, actionId, newShortcut);
      if (StringUtil.startsWithChar(actionId, '$')) {
        addShortcut(keymap, KeyMapBundle.message("editor.shortcut", actionId.substring(1)), newShortcut);
      }
      if (manager != null) manager.apply();
    }
  }

  private static final BadgeIconSupplier SHORTCUT_FILTER_ICON = new BadgeIconSupplier(AllIcons.Actions.ShortcutFilter);

  private final class FindByShortcutAction extends DumbAwareAction {
    private final JComponent mySearchToolbar;

    private FindByShortcutAction(JComponent searchToolbar) {
      super(KeyMapBundle.message("filter.shortcut.action.text"), KeyMapBundle.message("filter.shortcut.action.description"),
            AllIcons.Actions.ShortcutFilter);
      mySearchToolbar = searchToolbar;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setIcon(SHORTCUT_FILTER_ICON.getSuccessIcon(myFilteringPanel.getShortcut() != null));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myFilterComponent.reset();
      currentKeymapChanged();
      myFilteringPanel.showPopup(mySearchToolbar, e.getInputEvent().getComponent());
    }
  }

  private final class ClearFilteringAction extends DumbAwareAction {
    private ClearFilteringAction() {
      super(KeyMapBundle.message("filter.clear.action.text"), KeyMapBundle.message("filter.clear.action.description"), AllIcons.Actions.GC);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      boolean enabled = null != myFilteringPanel.getShortcut();
      Presentation presentation = event.getPresentation();
      presentation.setEnabled(enabled);
      presentation.setIcon(enabled ? AllIcons.Actions.Cancel : EmptyIcon.ICON_16);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myTreeExpansionMonitor.freeze();
      myFilteringPanel.setShortcut(null);
      myActionsTree.filter(null, myQuickLists); //clear filtering
      TreeUtil.collapseAll(myActionsTree.getTree(), 0);
      myTreeExpansionMonitor.restore();
    }
  }

  private abstract class BaseCopyAction extends DumbAwareAction {

    protected BaseCopyAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText,
                              @NotNull Supplier<@NlsActions.ActionDescription String> dynamicDescription,
                              @Nullable Icon icon) {
      super(dynamicText, dynamicDescription, icon);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    public String getSelectedActionId() {
      return myActionsTree.getSelectedActionId();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(getSelectedActionId() != null);
    }
  }

  private final class CopyActionIdAction extends BaseCopyAction {

    private CopyActionIdAction() {
      super(() -> "Copy Action ID", Presentation.NULL_STRING, AllIcons.Actions.Copy);
      registerCustomShortcutSet(CommonShortcuts.getCopy(), KeymapPanel.this);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      String actionId = getSelectedActionId();
      if (StringUtil.isNotEmpty(actionId)) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(actionId), null);
        showHint("Action id `" + actionId + "` copied to clipboard.");
      }
    }

  }

  private final class CopyActionNameAction extends BaseCopyAction {

    private CopyActionNameAction() {
      super(() -> "Copy Action Name", Presentation.NULL_STRING, AllIcons.Actions.Copy);
      registerCustomShortcutSet(CommonShortcuts.ALT_ENTER, KeymapPanel.this);
    }

      @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      String actionId =getSelectedActionId();
      if (StringUtil.isNotEmpty(actionId)) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        if (action != null && action.getTemplateText() != null) {
          String text = action.getTemplateText();
          Toolkit.getDefaultToolkit()
                  .getSystemClipboard()
                  .setContents(new StringSelection(text), null);
          showHint("Action name `" + text + "` copied to clipboard.");
        }

      }
    }
  }

  private final class CopyActionClassNameAction extends BaseCopyAction {
    private CopyActionClassNameAction() {
      super(() -> "Copy Action Class Name", Presentation.NULL_STRING, AllIcons.Nodes.Class);
    }

      @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      String actionId = getSelectedActionId();
      if (StringUtil.isNotEmpty(actionId)) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        if (action != null) {
          String text = action.getClass().getCanonicalName();
          Toolkit.getDefaultToolkit()
                  .getSystemClipboard()
                  .setContents(new StringSelection(text), null);
          showHint("Action class name `" + text + "` copied to clipboard.");
        }
      }
    }
  }

  private final class CopyAllActionInfoAction extends BaseCopyAction {
    private CopyAllActionInfoAction() {
      super(() -> "Copy All Action Info, Includes Name, Class Name and Shortcut", Presentation.NULL_STRING, AllIcons.Actions.Preview);
      registerCustomShortcutSet(CommonShortcuts.ENTER, KeymapPanel.this.myActionsTree.getComponent());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Keymap selectedKeymap = myManager.getSelectedKeymap();
      if (selectedKeymap == null) return;

      String actionId = getSelectedActionId();
      if (StringUtil.isNotEmpty(actionId)) {
        AnAction action = ActionManager.getInstance().getAction(actionId);
        if (action != null) {
          /*
          | ID | Action | Class | Shortcut |
          |----|-------|------|------|
          | A  | B      | C     | D        |
           */
          String text =  " | " + actionId + " | " + action.getTemplateText() + " | " + action.getClass().getCanonicalName() + " | "
                  + KeymapUtil.getShortcutText(actionId) + " | ";
          Toolkit.getDefaultToolkit()
                  .getSystemClipboard()
                  .setContents(new StringSelection(text), null);
          showHint("Action info `" + text + "` copied to clipboard.");
        }
      }
    }
  }


}
