package com.jetbrains.ide.streamdeck.customization;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.ide.ui.customization.CustomizationUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ex.QuickList;
import com.intellij.openapi.keymap.impl.ui.Group;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Patched, only show Action ID in Tree view, ignore action group id.
 */
public class CustomizationUtilPatched {
    /**
     * Retrieve text and icon from the object and pass them to {@code consumer}.
     * <p>This types of object can be processed:
     *   <ul>
     *   <li>{@link Group}</li>
     *   <li>{@link String} (action ID)</li>
     *   <li>{@link Pair}&lt;String actionId, Icon customIcon&gt;</li>
     *   <li>{@link Pair}&lt;Group group, Icon customIcon&gt;</li>
     *   <li>{@link Separator}</li>
     *   <li>{@link QuickList}</li>
     *   </ul>
     * </p>
     *
     * @throws IllegalArgumentException if {@code obj} has wrong type
     */
    public static void acceptObjectIconAndText(@Nullable Object obj, @NotNull CustomizationUtil.CustomPresentationConsumer consumer) {
        @NotNull String text;
        @Nullable String description = null;
        Icon icon = null;
        if (obj instanceof Group) {
            Group group = (Group)obj;
            String name = group.getName();
            @NlsSafe String id = group.getId();
            text = name != null ? name : ObjectUtils.notNull(id, IdeBundle.message("action.group.name.unnamed.group"));
            icon = group.getIcon();
            if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
                description = id;
            }
        }
        else if (obj instanceof String) {
            String actionId = (String)obj;
            AnAction action = ActionManager.getInstance().getAction(actionId);
            String name = action != null ? action.getTemplatePresentation().getText() : null;
            text = !StringUtil.isEmptyOrSpaces(name) ? name : actionId;
            if (action != null) {
                Icon actionIcon = action.getTemplatePresentation().getIcon();
                if (actionIcon != null) {
                    icon = actionIcon;
                }
            }
//            if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
                description = actionId;
//            }
        }
        else if (obj instanceof Pair<?, ?> pair) {
            Object actionIdOrGroup = pair.first;
            String actionId = actionIdOrGroup instanceof Group group ? group.getId() : (String)actionIdOrGroup;
            AnAction action = actionId == null ? null : ActionManager.getInstance().getAction(actionId);
            var t = action != null ? action.getTemplatePresentation().getText() : null;
            text = StringUtil.isNotEmpty(t) ? t : ObjectUtils.notNull(actionId, IdeBundle.message("action.group.name.unnamed.group"));
            Icon actionIcon = (Icon)pair.second;
            if (actionIcon == null && action != null) {
                actionIcon = action.getTemplatePresentation().getClientProperty(CustomActionsSchema.PROP_ORIGINAL_ICON);
            }
            icon = actionIcon;
//            if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
                description = actionId;
//            }
        }
        else if (obj instanceof Separator) {
            text = "-------------";
        }
        else if (obj instanceof QuickList quickList) {
            text = quickList.getDisplayName();
//            if (UISettings.getInstance().getShowInplaceCommentsInternal()) {
                description = quickList.getActionId();
//            }
        }
        else if (obj == null) {
            //noinspection HardCodedStringLiteral
            text = "null";
        }
        else {
            throw new IllegalArgumentException("unknown obj: " + obj);
        }
        consumer.accept(text, description, icon);
    }

}
