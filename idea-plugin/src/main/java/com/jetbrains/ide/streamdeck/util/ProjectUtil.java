package com.jetbrains.ide.streamdeck.util;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.ui.ComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;

public class ProjectUtil {
    public static Project tryGuessFocusedProject() {
        System.out.println("--- tryGuessFocusedProject ---");
        Component focusOwner =
                KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if(focusOwner != null) {
            IdeFrame frame = ComponentUtil.getParentOfType((Class<? extends IdeFrame>)IdeFrame.class, focusOwner);
            if(frame != null) {
                // System.out.println(frame.getProject());
                return frame.getProject();
            }
        }

        return null;
        // return ProjectManager.getInstance().getDefaultProject();
    }

    public static @NotNull AnActionEvent createFromAnAction(@NotNull AnAction action,
                                                            @Nullable InputEvent event,
                                                            @NotNull String place,
                                                            @NotNull DataContext dataContext) {
        int modifiers = event == null ? 0 : event.getModifiers();
        // Presentation presentation = action.getTemplatePresentation().clone();
        AnActionEvent anActionEvent = new AnActionEvent(event, dataContext, place, new Presentation(), ActionManager.getInstance(), modifiers);
        anActionEvent.setInjectedContext(action.isInInjectedContext());
        return anActionEvent;
    }

    @Nullable
    public static Component getFocusedComponent(@NotNull Project project) {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
            IdeFocusManager focusManager = IdeFocusManager.getInstance(project);
            Window frame = focusManager.getLastFocusedIdeWindow();
            if (frame != null) {
                focusOwner = focusManager.getLastFocusedFor(frame);
            }
        }
        return focusOwner;
    }

    @Nullable
    public static Editor getFocusedEditor(@NotNull Project project) {
        Component component = getFocusedComponent(project);
        Editor editor = component instanceof EditorComponentImpl ? ((EditorComponentImpl)component).getEditor() : null;
        return editor != null && !editor.isDisposed() ? editor : null;
    }
}
