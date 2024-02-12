package com.jetbrains.ide.streamdeck.util;

import com.intellij.execution.ExecutorRegistryImpl;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.ide.DataManager;
import com.intellij.ide.KeyboardAwareFocusOwner;
import com.intellij.ide.actions.GotoActionAction;
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
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.jetbrains.ide.streamdeck.settings.ActionServerSettings;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.reflect.Method;

public class ActionExecutor {

    public static void performActionUrl(@NotNull String uri,
                                        boolean allowCallInBackground) {
        var actionStr = uri.substring("/api/action/".length());
        if(StringUtil.isEmpty(actionStr)) return;
        QueryStringDecoder decoder = new QueryStringDecoder(actionStr);
        var actionId = decoder.path();
        String name;

        if (decoder.parameters().get("name") != null) {
            name = decoder.parameters().get("name").get(0);
        } else {
            name = null;
        }


        runInEdt(() -> {
            if (actionId.equalsIgnoreCase("Run") || actionId.equalsIgnoreCase("Debug")) {
                if (StringUtil.isNotEmpty(name)) {
                    runOrDebug(name, actionId.equalsIgnoreCase("Run"), allowCallInBackground);
                    return;
                }
            }

            performAction(actionId, allowCallInBackground);
        });

    }

    @RequiresEdt
    public static void performAction(@NotNull String actionId, boolean allowCallInBackground) {
        // WelcomeScreen.CreateNewProject Run
        // JetBrains Gateway/JetBrains Client, the markdown action will be NULL!
        AnAction action = ActionManager.getInstance().getAction(actionId);
        if(action == null) return;
        performActionFocusedProject(action, allowCallInBackground);
    }

    public static void runInEdt(Runnable runnable) {
        try {
            // Execute in EDT
            ApplicationManager.getApplication().invokeLater(runnable, ModalityState.defaultModalityState());
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
        if (fileEditor != null) {
            virtualFile = fileEditor.getFile();
        }
        IdeFrame ideFrame = ApplicationManager.getApplication().getService(IdeFocusManager.class)
                .getLastFocusedFrame();
        if (ideFrame != null) {
            Project project = ideFrame.getProject();
            if (project != null) {
                Editor editor = ProjectUtil.getFocusedEditor(project);
                if (virtualFile != null) {
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
     * @param action                AnAction
     * @param allowCallInBackground allow to perform action in non focused IDE window
     * @see GotoActionAction#openOptionOrPerformAction(Object, String, Project, Component, int)
     * @see IdeKeyEventDispatcher#processAction(InputEvent, ActionProcessor)
     */
    public static void performActionFocusedProject(@NotNull AnAction action, boolean allowCallInBackground) {
        System.out.println("ActionExecutor.performActionFocusedProject(allowCallInBackground = " + allowCallInBackground + ")");

        Result result = getResult(allowCallInBackground);
        if (result == null) return;

        var focusOwner = result.focusOwner;// FocusManager.getCurrentManager().getFocusOwner();

        var ideFocusManager = result.ideFocusManager;


        System.out.println("FocusManager.getCurrentManager() = " + FocusManager.getCurrentManager());
        System.out.println("focusManager = " + ideFocusManager);
        System.out.println("keyboard focusOwner = " + FocusManager.getCurrentManager().getFocusOwner());
        System.out.println("ideFocusManager focusOwner = " + ideFocusManager.getFocusOwner());

//        if(focusOwner == null) {
//            IdeFrame ideFrame = ideFocusManager.getLastFocusedFrame();
//            if(ideFrame != null) {
//                focusOwner = ideFrame.getComponent();
//            }
//        }


        // Keymap shortcuts (i.e. not local shortcuts) should work only in:
        // - main frame
        // - floating focusedWindow
        // - when there's an editor in contexts
        // boolean isModalContext = focusedWindow != null && isModalContext(focusedWindow);

        DataContext context = getDataContext(focusOwner);

//        if(fileEditor instanceof TextEditor && fileEditor.isValid() && dataManager != null) {
//            context = dataManager.getDataContext(fileEditor.getComponent());
//        }

        DataContext wrappedContext = Utils.wrapDataContext(context);
        Project project = CommonDataKeys.PROJECT.getData(wrappedContext);
        System.out.println("project=" + project);
        if (project != null && project.isDisposed()) return;

//        CurrentEditorProvider currentEditorProvider = ApplicationManager.getApplication().getService(CurrentEditorProvider.class);
//        FileEditor fileEditor = null;
//        try {
//            // Works for 2023.3 which has deprecated old API for `new FocusBasedCurrentEditorProvider().getCurrentEditor()`
//            Method currentEditor = CurrentEditorProvider.class.getDeclaredMethod("getCurrentEditor", Project.class);
//            fileEditor = (FileEditor) currentEditor.invoke(currentEditorProvider, project);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            fileEditor = new FocusBasedCurrentEditorProvider().getCurrentEditor();
//        }
//
//        System.out.println("fileEditor=" + fileEditor);
//
//        if (fileEditor != null) {
////            ActionToolbar actionToolbar = ActionToolbar.findToolbarBy(fileEditor.getComponent());
////            try {
////                System.out.println(actionToolbar.getActions());
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
//        }


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
                if (focusOwner != null) {
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

    @NotNull
    public static DataContext getDataContext(@NotNull Component focusOwner) {
        Application app = ApplicationManager.getApplication();
        DataManager dataManager = app == null ? null : app.getServiceIfCreated(DataManager.class);
        System.out.println("dataManager=" + dataManager);

        return dataManager != null ? dataManager.getDataContext(focusOwner) : DataContext.EMPTY_CONTEXT;
    }

    @RequiresEdt
    public static void runOrDebug(@NotNull String runConfigurationName,
                                  boolean runMode, boolean allowCallInBackground) {
        Result result = getResult(allowCallInBackground);
        if (result == null) return;

        DataContext context = getDataContext(result.focusOwner());

        Project project = CommonDataKeys.PROJECT.getData(context);
        System.out.println("project=" + project);
        if (project != null && project.isDisposed()) return;

        runOrDebug(runConfigurationName, project, context, runMode);
    }

    @Nullable
    private static Result getResult(boolean allowCallInBackground) {
        var focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        IdeFocusManager ideFocusManager = ApplicationManager.getApplication().getService(IdeFocusManager.class);
//        var focusOwner = focusManager.getFocusOwner();
        // When there is no editor, the focusOwner is always null --> FocusManager.getCurrentManager().getFocusOwner()
        var focusOwner = ideFocusManager.getFocusOwner();// FocusManager.getCurrentManager().getFocusOwner();

        var focusedWindow = ideFocusManager.getLastFocusedIdeWindow();

        // No focused component
        if (focusedWindow != null && !focusedWindow.isFocused() && !allowCallInBackground) return null;

        return new Result(focusManager, ideFocusManager, focusOwner);
    }

    private record Result(KeyboardFocusManager focusManager, IdeFocusManager ideFocusManager, Component focusOwner) {
    }

    public static void runOrDebug(@NotNull String runConfigurationName, @NotNull Project project,
                                  @NotNull DataContext dataContext, boolean runMode) {
        if (project.isDisposed()) return;
        RunManager runManager = RunManager.getInstanceIfCreated(project);
        if(runManager == null) return;
        RunnerAndConfigurationSettings settings = runManager.findConfigurationByName(runConfigurationName);
        if (settings == null) {
            return;
        }

        if (runMode) {
            ExecutorRegistryImpl.RunnerHelper.run(project, settings.getConfiguration(), settings, dataContext,
                    DefaultRunExecutor.getRunExecutorInstance());
        } else {
            ExecutorRegistryImpl.RunnerHelper.run(project, settings.getConfiguration(), settings, dataContext,
                    DefaultDebugExecutor.getDebugExecutorInstance());
        }


    }

}
