package com.jetbrains.ide.streamdeck.util;

import com.intellij.ide.DataManager;
import com.intellij.ide.KeyboardAwareFocusOwner;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.Utils;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.FocusBasedCurrentEditorProvider;
import com.intellij.openapi.keymap.impl.ActionProcessor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class ActionExecutor {
    public static void performAction(@NotNull AnAction action, @Nullable Component c, boolean allowCallInBackground) {
        if (action != null) {
            try {
                // Execute in EDT
                 ApplicationManager.getApplication().invokeLater(() -> {
                     performActionFocusedProject(action, allowCallInBackground);
//                     if(allowCallInBackground) {
//                         performAction(action, null);
//                     } else {
//
//                     }
                 }, ModalityState.defaultModalityState());
//                performActionFocusedProject(action);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Invoke a particular component's action.
     * TODO Make DataContext for common editor actions
     * NOTE: This method has bug, can't perform editor action
     *
     * @param c If specified, this will be used as the context for the action. If unspecified, the
     *          most recently focused window will be used instead.
     */
    @Deprecated
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

    /**
     * Perform action in the IDE focused Editor.
     *
     * @see com.intellij.ide.actions.GotoActionAction#openOptionOrPerformAction(Object, String, Project, Component, int)
     * @see com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher#processAction(InputEvent, ActionProcessor)
     * @param action AnAction
     * @param allowCallInBackground allow to perform action in non focused IDE window
     */
    public static void performActionFocusedProject(@NotNull AnAction action, boolean allowCallInBackground) {
        var focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
//        var focusOwner = focusManager.getFocusOwner();
        var focusOwner = FocusManager.getCurrentManager().getFocusOwner();

        // No keyboard focused component
        if(focusOwner == null && !allowCallInBackground) return;

        // Keymap shortcuts (i.e. not local shortcuts) should work only in:
        // - main frame
        // - floating focusedWindow
        // - when there's an editor in contexts
        Window focusedWindow = focusManager.getFocusedWindow();
        // boolean isModalContext = focusedWindow != null && isModalContext(focusedWindow);

        Application app = ApplicationManager.getApplication();
        DataManager dataManager = app == null ? null : app.getServiceIfCreated(DataManager.class);
        System.out.println("dataManager=" + dataManager);

        FileEditor fileEditor = new FocusBasedCurrentEditorProvider().getCurrentEditor();

        DataContext context = dataManager != null ? dataManager.getDataContext(focusOwner) : DataContext.EMPTY_CONTEXT;

        if(fileEditor instanceof TextEditor && fileEditor.isValid() && dataManager != null) {
            context = dataManager.getDataContext(fileEditor.getComponent());
        }

        DataContext wrappedContext = Utils.wrapDataContext(context);
        Project project = CommonDataKeys.PROJECT.getData(wrappedContext);
        if (project != null && project.isDisposed()) return;

        boolean dumb = project != null && DumbService.getInstance(project).isDumb();

        // Note: some action may produce exceptions if executed from the wrong focused component, eg:
        // GET http://localhost:63344/api/action/Vcs.ShowTabbedFileHistory requires a vcs file
        // TODO: Enable enable check?
        AnActionEvent event = AnActionEvent.createFromDataContext(ActionPlaces.MAIN_TOOLBAR, action.getTemplatePresentation().clone(),
                wrappedContext
                // dataId -> {
                //     // System.out.println("Request dataId =" + dataId);
                //     Project project = ProjectUtil.tryGuessFocusedProject();
                //     // System.out.println("Request project =" + project);
                //
                //     if (CommonDataKeys.PROJECT.is(dataId)) return project;
                //     return null;
                // }

        );
        event.setInjectedContext(action.isInInjectedContext());
        // Executed in EDT, so catch it
        try {
            if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
                Window window;
                if(focusOwner != null) {
                    window = SwingUtilities.getWindowAncestor(focusOwner);
                } else {
                    window = null;
                }
                ActionUtil.performDumbAwareWithCallbacks(action, event, () ->
                        ActionUtil.doPerformActionOrShowPopup(action, event, popup -> {
                            if (window != null) {
                                popup.showInCenterOf(window);
                            }
                            else {
                                popup.showInFocusCenter();
                            }
                        }));
            }
//            action.actionPerformed(event);
        } catch (Exception e) {
            e.printStackTrace();
//            throw new RuntimeException(e);
        }


    }
}
