// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.ide.streamdeck.customization;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.ui.customization.*;
import com.intellij.ide.ui.customization.CustomActionsSchemaKt;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.QuickList;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.KeyMapBundle;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.impl.ui.ActionsTree;
import com.intellij.openapi.keymap.impl.ui.Group;
import com.intellij.openapi.keymap.impl.ui.Hyperlink;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.Strings;
import com.intellij.packageDependencies.ui.TreeExpansionMonitor;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.jetbrains.ide.streamdeck.customization.CustomActionsSchemaKt.loadCustomIcon;

/**
 * Browse and copy action id, readonly view, disable drag and drop, paiting action id in highlight.
 * @see com.intellij.ide.ui.customization.CustomizableActionsPanel
 * @see  com.intellij.openapi.keymap.impl.ui.ActionsTree#paintRowData(Tree, Object, Rectangle, Graphics2D)
 */
public class CustomizableActionsPanel {
  private final JPanel myPanel = new BorderLayoutPanel(5, 5);
  protected JTree myActionsTree;
  private final JPanel myTopPanel = new BorderLayoutPanel();
  protected CustomActionsSchema mySelectedSchema;

  public CustomizableActionsPanel() {
    //noinspection HardCodedStringLiteral
    @SuppressWarnings("DialogTitleCapitalization")
    Group rootGroup = new Group("root");
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootGroup);
    DefaultTreeModel model = new DefaultTreeModel(root);
    myActionsTree = new Tree(model){
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        Rectangle visibleRect = getVisibleRect();
        Insets insets = getInsets();
        if (insets != null && insets.right > 0) {
          visibleRect.width -= JBUIScale.scale(9);
        }
        Rectangle clip = g.getClipBounds();
        for (int row = 0; row < getRowCount(); row++) {
          Rectangle rowBounds = getRowBounds(row);
          rowBounds.x = 0;
          rowBounds.width = Integer.MAX_VALUE;

          if (rowBounds.intersects(clip)) {
            Object node = getPathForRow(row).getLastPathComponent();
            if (node instanceof DefaultMutableTreeNode) {
              Object data = ((DefaultMutableTreeNode)node).getUserObject();
              if (!(data instanceof Hyperlink)) {
                Rectangle fullRowRect = new Rectangle(visibleRect.x, rowBounds.y, visibleRect.width, rowBounds.height);

                paintRowData(this, data, fullRowRect, (Graphics2D)g);
              }
            }
          }
        }
      }

//      @Override
//      public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
//        if (value instanceof DefaultMutableTreeNode) {
//          String path = ActionsTree.this.getPath((DefaultMutableTreeNode)value, true);
//          return StringUtil.notNullize(path);
//        }
//        return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
//      }
    };

    myActionsTree.setRootVisible(false);
    myActionsTree.setShowsRootHandles(true);
    myActionsTree.setCellRenderer(createDefaultRenderer());
//    RowsDnDSupport.install(myActionsTree, model);

    patchActionsTreeCorrespondingToSchema(root);

    TreeExpansionMonitor.install(myActionsTree);
    myTopPanel.add(setupFilterComponent(myActionsTree), BorderLayout.CENTER);
    myTopPanel.add(createToolbar(), BorderLayout.WEST);

    myPanel.add(myTopPanel, BorderLayout.NORTH);
    myPanel.add(ScrollPaneFactory.createScrollPane(myActionsTree), BorderLayout.CENTER);

    JLabel hint = new JLabel("Use UP, DOWN, HOME, and END keys to quickly navigate between the search results.");
    hint.setForeground(JBColor.gray);
    hint.setIcon(AllIcons.Actions.IntentionBulb);

    myPanel.add(ScrollPaneFactory.createScrollPane(hint, true), BorderLayout.SOUTH);
  }

  private void paintRowData(Tree tree, Object data, Rectangle bounds, Graphics2D g) {
    CustomizationUtilPatched.acceptObjectIconAndText(data, (text, description, icon) -> {
      if (description != null) {
        final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);

        int totalWidth = 0;
        final FontMetrics metrics = tree.getFontMetrics(tree.getFont());
        totalWidth += metrics.stringWidth(description);
        totalWidth += 10;
//        totalWidth -= 5;

        int x = bounds.x + bounds.width - totalWidth;
        int fontHeight = (int)metrics.getMaxCharBounds(g).getHeight();

        Color c1 = new Color(206, 234, 176);
        Color c2 = new Color(126, 208, 82);

        g.translate(0, bounds.y - 1);

        int width = metrics.stringWidth(description);
        UIUtil.drawSearchMatch(g, x, x + width, bounds.height, c1, c2);
        g.setColor(Gray._50);
        g.drawString(description, x, fontHeight);

        x += width;
//        x += 10;
        g.translate(0, -bounds.y + 1);
        config.restore();
      }
    });




  }

  private ActionToolbarImpl createToolbar() {
    ActionToolbarImpl toolbar = (ActionToolbarImpl)ActionManager.getInstance()
      .createActionToolbar(ActionPlaces.TOOLBAR, new DefaultActionGroup(new CopyActionIdAction()), true);
    toolbar.setForceMinimumSize(true);
//    toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
    toolbar.setTargetComponent(myTopPanel);
    return toolbar;
  }

  private void showHint(@NotNull String message) {
    HintManager.getInstance().hideAllHints();
    final JLabel label = new JLabel(message);
    label.setBorder(HintUtil.createHintBorder());
    label.setBackground(HintUtil.getInformationColor());
    label.setOpaque(true);
    HintManager.getInstance().showHint(label, RelativePoint.getCenterOf(myActionsTree),
            HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE, -1);
  }

  static FilterComponent setupFilterComponent(JTree tree) {
    final TreeSpeedSearch mySpeedSearch = new TreeSpeedSearch(tree, true, null, new TreePathStringFunction()) {
      @Override
      public boolean isPopupActive() {
        return /*super.isPopupActive()*/true;
      }

      @Override
      public void showPopup(String searchText) {
        //super.showPopup(searchText);
      }

      @Override
      protected boolean isSpeedSearchEnabled() {
        return /*super.isSpeedSearchEnabled()*/false;
      }

      @Override
      public void showPopup() {
        //super.showPopup();
      }
    };
    mySpeedSearch.setupListeners();
    final FilterComponent filterComponent = new FilterComponent("FIND_ACTIONS", 5) {
      @Override
      public void filter() {
        mySpeedSearch.findAndSelectElement(getFilter());
        mySpeedSearch.getComponent().repaint();
      }
    };
    JTextField textField = filterComponent.getTextEditor();
    int[] keyCodes = {KeyEvent.VK_HOME, KeyEvent.VK_END, KeyEvent.VK_UP, KeyEvent.VK_DOWN};
    for (int keyCode : keyCodes) {
      new DumbAwareAction(){
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          String filter = filterComponent.getFilter();
          if (!StringUtil.isEmpty(filter)) {
            mySpeedSearch.adjustSelection(keyCode, filter);
          }
        }
      }.registerCustomShortcutSet(keyCode, 0, textField);

    }
    return filterComponent;
  }


  public JPanel getPanel() {
    return myPanel;
  }

  public void apply() throws ConfigurationException {
    // do nothing
  }

  protected void updateGlobalSchema() {
    CustomActionsSchema.getInstance().copyFrom(mySelectedSchema);
  }

  protected void updateLocalSchema(CustomActionsSchema localSchema) {
  }

  public void reset() {
    // First call reset and setup tree content
    reset(true);
  }

  public void resetToDefaults() {
    reset(false);
  }

  private void reset(boolean restoreLastState) {
    List<String> expandedIds = toActionIDs(TreeUtil.collectExpandedPaths(myActionsTree));
    List<String> selectedIds = toActionIDs(TreeUtil.collectSelectedPaths(myActionsTree));
    DefaultMutableTreeNode root = (DefaultMutableTreeNode)myActionsTree.getModel().getRoot();
    TreeUtil.treeNodeTraverser(root).traverse()
      .filter(node -> node instanceof DefaultMutableTreeNode && ((DefaultMutableTreeNode)node).getUserObject() instanceof Pair)
      .forEach(node -> doSetIcon(mySelectedSchema, (DefaultMutableTreeNode)node, null));
    CustomActionsSchema source = restoreLastState ? CustomActionsSchema.getInstance() : new CustomActionsSchema(null);
    if (mySelectedSchema == null) mySelectedSchema = new CustomActionsSchema(null);
    mySelectedSchema.copyFrom(source);
    updateLocalSchema(mySelectedSchema);
    mySelectedSchema.initActionIcons();
    patchActionsTreeCorrespondingToSchema(root);
    if (needExpandAll()) {
      new DefaultTreeExpander(myActionsTree).expandAll();
    } else {
      TreeUtil.restoreExpandedPaths(myActionsTree, toTreePaths(root, expandedIds));
    }
    TreeUtil.selectPaths(myActionsTree, toTreePaths(root, selectedIds));
    TreeUtil.ensureSelection(myActionsTree);
  }

  private static List<String> toActionIDs(List<? extends TreePath> paths) {
    return ContainerUtil.map(paths, path -> getActionId((DefaultMutableTreeNode)path.getLastPathComponent()));
  }

  private static List<TreePath> toTreePaths(DefaultMutableTreeNode root, List<String> actionIDs) {
    List<TreePath> result = new ArrayList<>();
    for (String actionId : actionIDs) {
      DefaultMutableTreeNode treeNode = TreeUtil.findNode(root, node -> Objects.equals(actionId, getActionId(node)));
      if (treeNode != null) result.add(TreeUtil.getPath(root, treeNode));
    }
    return result;
  }

  protected boolean needExpandAll() {
    return false;
  }

  public boolean isModified() {
    return false;
  }

  protected void patchActionsTreeCorrespondingToSchema(DefaultMutableTreeNode root) {
    root.removeAllChildren();
    if (mySelectedSchema != null) {
      mySelectedSchema.fillCorrectedActionGroups(root);
    }
    ((DefaultTreeModel)myActionsTree.getModel()).reload();
  }

  private static final class TreePathStringFunction implements Function<TreePath, String> {
    @Override
    public String apply(TreePath o) {
      Object node = o.getLastPathComponent();
      if (node instanceof DefaultMutableTreeNode) {
        Object object = ((DefaultMutableTreeNode)node).getUserObject();
        if (object instanceof Group) return ((Group)object).getName();
        if (object instanceof QuickList) return ((QuickList)object).getName();
        String actionId;
        if (object instanceof String) {
          actionId = (String)object;
        }
        else if (object instanceof Pair) {
          Object obj = ((Pair<?, ?>)object).first;
          if (obj instanceof Group group) return group.getName();
          actionId = (String)obj;
        }
        else {
          return "";
        }
        if (Strings.isEmpty(actionId)) return "";
        AnAction action = ActionManager.getInstance().getAction(actionId);
        if (action != null) {
          return action.getTemplatePresentation().getText();
        }
      }
      return "";
    }
  }

  static TreeCellRenderer createDefaultRenderer() {
    return new MyTreeCellRenderer();
  }

  private static class MyTreeCellRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      if (value instanceof DefaultMutableTreeNode) {
        Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
        CustomizationUtil.acceptObjectIconAndText(userObject, (text, description, icon) -> {
          append(text);
          if (description != null) {
            append("   ", SimpleTextAttributes.REGULAR_ATTRIBUTES, false);
//            append(description, SimpleTextAttributes.GRAY_ATTRIBUTES);// Always hide this description
          }
          // do not show the icon for the top groups
          if (((DefaultMutableTreeNode)value).getLevel() > 1) {
            setIcon(icon);
          }
        });
        setForeground(UIUtil.getTreeForeground(selected, hasFocus));
      }
    }
  }

  static @Nullable String getActionId(DefaultMutableTreeNode node) {
    Object obj = node.getUserObject();
    if (obj instanceof String actionId) return actionId;
    if (obj instanceof Group group) return group.getId();
    if (obj instanceof Pair<?, ?> pair) {
      Object first = pair.first;
      return first instanceof Group group ? group.getId() : (String)first;
    }
    return null;
  }

  static @NotNull Pair<@Nullable String, @Nullable Icon> getActionIdAndIcon(@NotNull DefaultMutableTreeNode node) {
    Object userObj = node.getUserObject();
    if (userObj instanceof String actionId) {
      AnAction action = ActionManager.getInstance().getAction(actionId);
      if (action != null) {
        return Pair.create(actionId, action.getTemplatePresentation().getIcon());
      }
    }
    else if (userObj instanceof Group group) {
      return Pair.create(group.getId(), group.getIcon());
    }
    else if (userObj instanceof Pair<?, ?> pair) {
      Object first = pair.first;
      String actionId = first instanceof Group group ? group.getId() : (String)first;
      return Pair.create(actionId, (Icon)pair.second);
    }
    return Pair.empty();
  }

  private static boolean doSetIcon(@NotNull CustomActionsSchema schema,
                                   @NotNull DefaultMutableTreeNode node,
                                   @Nullable String path) {
    Object userObj = node.getUserObject();
    Object value = userObj instanceof Pair<?, ?> pair ? pair.first : userObj;
    String actionId = value instanceof Group group ? group.getId()
                                                   : value instanceof String ? (String)value : null;
    if (actionId == null) return false;
    if (StringUtil.isEmpty(path)) {
      node.setUserObject(Pair.create(value, null));
      schema.removeIconCustomization(actionId);
      return true;
    }
    ActionManager actionManager = ActionManager.getInstance();
    AnAction action = actionManager.getAction(actionId);
    if (action == null) return false;

    AnAction reuseFrom = actionManager.getAction(path);
    if (reuseFrom != null) {
      Icon toSet = CustomActionsSchemaKt.getOriginalIconFrom(reuseFrom);
      Icon defaultIcon = CustomActionsSchemaKt.getOriginalIconFrom(action);
      node.setUserObject(Pair.create(value, toSet));
      schema.addIconCustomization(actionId, toSet != defaultIcon ? path : null);
    }
    else {
      Icon icon;
      try {
        icon = loadCustomIcon(path);
      }
      catch (Throwable t) {
        Logger.getInstance(CustomizableActionsPanel.class)
          .warn(String.format("Failed to load icon with path '%s' and set it to action '%s'", path, actionId), t);
        return false;
      }
      node.setUserObject(Pair.create(value, icon));
      schema.addIconCustomization(actionId, path);
    }
    return true;
  }




  private abstract class TreeSelectionAction extends DumbAwareAction {
    private TreeSelectionAction(@NotNull Supplier<String> text) {
      super(text);
    }

    private TreeSelectionAction(@NotNull Supplier<String> text, @NotNull Supplier<String> description, @Nullable Icon icon) {
      super(text, description, icon);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(true);
      TreePath[] selectionPaths = myActionsTree.getSelectionPaths();
      if (selectionPaths == null) {
        e.getPresentation().setEnabled(false);
        return;
      }
      for (TreePath path : selectionPaths) {
        if (path.getPath().length <= minSelectionPathLength()) {
          e.getPresentation().setEnabled(false);
          return;
        }
      }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.EDT;
    }

    protected int minSelectionPathLength() {
      return 2;
    }

    protected final boolean isSingleSelection() {
      final TreePath[] selectionPaths = myActionsTree.getSelectionPaths();
      return selectionPaths != null && selectionPaths.length == 1;
    }
  }


  private final class CopyActionIdAction extends TreeSelectionAction {
    private CopyActionIdAction() {
      super(() -> "Copy Action ID", Presentation.NULL_STRING, AllIcons.Actions.Copy);
      registerCustomShortcutSet(CommonShortcuts.getCopy(), myPanel);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) myActionsTree.getLeadSelectionPath().getLastPathComponent();
      String actionId = getActionId(node);
      if (StringUtil.isNotEmpty(actionId)) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(actionId), null);
        showHint("Action id `" + actionId + "` copied to clipboard");
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      super.update(e);
      if (e.getPresentation().isEnabled()) {
        final ActionManager actionManager = ActionManager.getInstance();
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)myActionsTree.getLeadSelectionPath().getLastPathComponent();
        String actionId = getActionId(node);
        if (actionId != null) {
          final AnAction action = actionManager.getAction(actionId);
          e.getPresentation().setEnabled(action != null);
        }
        else {
          e.getPresentation().setEnabled(false);
        }

      }
    }
  }

  private class MyModel extends DefaultTreeModel implements TreeTableModel {
    protected MyModel(DefaultMutableTreeNode root) {
      super(root);
    }

    @Override
    public void setTree(JTree tree) {
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int column) {
      return switch (column) {
        case 0 -> KeyMapBundle.message("action.column.name");
        case 1 -> KeyMapBundle.message("shortcuts.column.name");
        default -> "";
      };
    }

    @Override
    public Object getValueAt(Object value, int column) {
      if (!(value instanceof DefaultMutableTreeNode)) {
        return "???";
      }

      if (column == 0) {
        return value;
      }
      else if (column == 1) {
        Object userObject = ((DefaultMutableTreeNode)value).getUserObject();
        if (userObject instanceof QuickList) {
          userObject = ((QuickList)userObject).getActionId();
        }
        return userObject instanceof String ? (String)userObject : "";
      }
      else {
        return "???";
      }
    }

    @Override
    public Object getChild(Object parent, int index) {
      return ((TreeNode)parent).getChildAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
      return ((TreeNode)parent).getChildCount();
    }

    @Override
    public Class getColumnClass(int column) {
      if (column == 0) {
        return TreeTableModel.class;
      }
      else {
        return Object.class;
      }
    }

    @Override
    public boolean isCellEditable(Object node, int column) {
      return column == 0;
    }

    @Override
    public void setValueAt(Object aValue, Object node, int column) {
    }
  }

}
