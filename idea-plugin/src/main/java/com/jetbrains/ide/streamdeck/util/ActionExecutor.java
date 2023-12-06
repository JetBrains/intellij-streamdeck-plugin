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
import com.intellij.openapi.fileEditor.impl.CurrentEditorProvider;
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
import java.lang.reflect.Method;

public class ActionExecutor {
    public static void performAction(@NotNull AnAction action, @Nullable Component c, boolean allowCallInBackground) {
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
     * TODO Support JetBrains Gateway / Client
     *
     * @see com.intellij.ide.actions.GotoActionAction#openOptionOrPerformAction(Object, String, Project, Component, int)
     * @see com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher#processAction(InputEvent, ActionProcessor)
     * @param action AnAction
     * @param allowCallInBackground allow to perform action in non focused IDE window
     */
    public static void performActionFocusedProject(@NotNull AnAction action, boolean allowCallInBackground) {
        System.out.println("ActionExecutor.performActionFocusedProject(allowCallInBackground = " + allowCallInBackground + ")");
        var focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        IdeFocusManager ideFocusManager = ApplicationManager.getApplication().getService(IdeFocusManager.class);
//        var focusOwner = focusManager.getFocusOwner();
        // When there is no editor, the focusOwner is always null --> FocusManager.getCurrentManager().getFocusOwner()
        var focusOwner = ideFocusManager.getFocusOwner();// FocusManager.getCurrentManager().getFocusOwner();

        var focusedWindow = ideFocusManager.getLastFocusedIdeWindow();

        if(focusedWindow != null && !focusedWindow.isFocused() && !allowCallInBackground) return;

        System.out.println("FocusManager.getCurrentManager() = " + FocusManager.getCurrentManager());
        System.out.println("focusManager = " + focusManager);
        System.out.println("keyboard focusOwner = " + FocusManager.getCurrentManager().getFocusOwner());
        System.out.println("ideFocusManager focusOwner = " + ideFocusManager.getFocusOwner());

//        if(focusOwner == null) {
//            IdeFrame ideFrame = ideFocusManager.getLastFocusedFrame();
//            if(ideFrame != null) {
//                focusOwner = ideFrame.getComponent();
//            }
//        }

        // No focused component
        if(focusOwner == null && !allowCallInBackground) return;

        // Keymap shortcuts (i.e. not local shortcuts) should work only in:
        // - main frame
        // - floating focusedWindow
        // - when there's an editor in contexts
        // boolean isModalContext = focusedWindow != null && isModalContext(focusedWindow);

        Application app = ApplicationManager.getApplication();
        DataManager dataManager = app == null ? null : app.getServiceIfCreated(DataManager.class);
        System.out.println("dataManager=" + dataManager);

        DataContext context = dataManager != null ? dataManager.getDataContext(focusOwner) : DataContext.EMPTY_CONTEXT;

//        if(fileEditor instanceof TextEditor && fileEditor.isValid() && dataManager != null) {
//            context = dataManager.getDataContext(fileEditor.getComponent());
//        }

        DataContext wrappedContext = Utils.wrapDataContext(context);
        Project project = CommonDataKeys.PROJECT.getData(wrappedContext);
        System.out.println("project=" + project);
        if (project != null && project.isDisposed()) return;

        CurrentEditorProvider currentEditorProvider = ApplicationManager.getApplication().getService(CurrentEditorProvider.class);
        FileEditor fileEditor = null;
        try {
            // Works for 2023.3 which has deprecated old API for `new FocusBasedCurrentEditorProvider().getCurrentEditor()`
            Method currentEditor = CurrentEditorProvider.class.getDeclaredMethod("getCurrentEditor", Project.class);
            fileEditor = (FileEditor) currentEditor.invoke(currentEditorProvider, project);
        } catch (Exception ex) {
            ex.printStackTrace();
            fileEditor = new FocusBasedCurrentEditorProvider().getCurrentEditor();
        }

        System.out.println("fileEditor=" + fileEditor);

        if(fileEditor != null) {
//            ActionToolbar actionToolbar = ActionToolbar.findToolbarBy(fileEditor.getComponent());
//            try {
//                System.out.println(actionToolbar.getActions());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }


//        boolean dumb = project != null && DumbService.getInstance(project).isDumb();

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

        System.out.println("event=" + event);
        // Executed in EDT, so catch it
        try {
            System.out.println("ActionUtil.lastUpdateAndCheckDumb(action, event, false)=" + ActionUtil.lastUpdateAndCheckDumb(action, event, false));

            System.out.println("performActionDumbAwareWithCallbacks action =" + action);
            ActionUtil.performActionDumbAwareWithCallbacks(action, event);

            if (ActionUtil.lastUpdateAndCheckDumb(action, event, false)) {
                Window window;
                if(focusOwner != null) {
                    window = SwingUtilities.getWindowAncestor(focusOwner);
                } else {
                    window = null;
                }

//                System.out.println("performActionDumbAwareWithCallbacks action =" + action);
//                ActionUtil.performActionDumbAwareWithCallbacks(action, event);
//                action.actionPerformed(event);
//                ActionUtil.performDumbAwareWithCallbacks(action, event, () ->
//                        ActionUtil.doPerformActionOrShowPopup(action, event, popup -> {
//                            if (window != null) {
//                                popup.showInCenterOf(window);
//                            }
//                            else {
//                                popup.showInFocusCenter();
//                            }
//                        }));
            }
//            action.actionPerformed(event);
        } catch (Exception e) {
            e.printStackTrace();
//            throw new RuntimeException(e);
        }


    }
}
