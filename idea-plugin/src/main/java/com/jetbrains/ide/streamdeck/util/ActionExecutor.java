package com.jetbrains.ide.streamdeck.util;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.FocusBasedCurrentEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ActionExecutor {
    public static void performAction(@NotNull AnAction action, @Nullable Component c, boolean allowCallInBackground) {
        if (action != null) {
            try {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if(allowCallInBackground) {
                        performAction(action, null);
                    } else {
                        performActionFocusedProject(action);
                    }
                }, ModalityState.defaultModalityState());


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * Invoke a particular component's action.
     * TODO Make DataContext for common editor actions
     *
     * @param c If specified, this will be used as the context for the action. If unspecified, the
     *          most recently focused window will be used instead.
     */
    public static void performAction(@NotNull AnAction action, @Nullable Component c) {
        if (c == null) {
            c = ApplicationManager.getApplication().getService(IdeFocusManager.class).getLastFocusedIdeWindow();
        }

        FileEditor fileEditor = new FocusBasedCurrentEditorProvider().getCurrentEditor();
        VirtualFile virtualFile = null;
        if(fileEditor != null) {
            virtualFile = fileEditor.getFile();
        }
        IdeFrame ideFrame = ApplicationManager.getApplication().getService(IdeFocusManager.class)
                .getLastFocusedFrame();
        if(ideFrame != null) {
            Project project = ideFrame.getProject();
            if(project != null) {
                Editor editor = ProjectUtil.getFocusedEditor(project);
                if(virtualFile != null) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                }
            }
        }

        DataContext context = DataManager.getInstance().getDataContext(c);
        AnActionEvent event = ProjectUtil.createFromAnAction(action, null, ActionPlaces.MAIN_TOOLBAR, context);
        action.actionPerformed(event);
    }

    public static void performActionFocusedProject(@NotNull AnAction action) {
        action.actionPerformed(
                AnActionEvent.createFromDataContext(ActionPlaces.MAIN_TOOLBAR, null,
                        dataId -> {
                            // System.out.println("Request dataId =" + dataId);
                            Project project = ProjectUtil.tryGuessFocusedProject();
                            // System.out.println("Request project =" + project);

                            if (CommonDataKeys.PROJECT.is(dataId)) return project;
                            return null;
                        }));
    }
}
