import { StreamDeckPluginHandler } from 'streamdeck-typescript'
import { GitPullAction } from './actions/git-pull-action'
import { NewProjectAction } from './actions/newProject'
import { RunAction } from './actions/run-action'
import { StepOverAction } from './actions/step-over-action'
import {DebugAction} from "./actions/debug-action";
import {ResumeAction} from "./actions/resume-action";
import {SearchEverywhereAction} from "./actions/search-everywhere-action";
import {StopWithDropDownAction} from "./actions/stop-action";
import {PauseAction} from "./actions/pause-action";
import {StepOutAction} from "./actions/step-out-action";
import {ShowProjectStructureAction} from "./actions/project-structure-action";

export class IdeaPlugin extends StreamDeckPluginHandler {
  constructor() {
    super()
    new GitPullAction(this, 'com.jetbrains.idea.git.pull')
    new NewProjectAction(this, 'com.jetbrains.idea.new')
    new RunAction(this, 'com.jetbrains.idea.run')
    new DebugAction(this, "com.jetbrains.idea.debug")
    new StepOverAction(this, 'com.jetbrains.idea.step.over')
    new StepOutAction(this, 'com.jetbrains.idea.action.step.out')
    new ResumeAction(this, 'com.jetbrains.idea.resume')
    new PauseAction(this, 'com.jetbrains.idea.action.pause')
    new StopWithDropDownAction(this, 'com.jetbrains.idea.action.stop')
    new SearchEverywhereAction(this, 'com.jetbrains.idea.search.everywhere')
    new ShowProjectStructureAction(this, 'com.jetbrains.idea.action.show.project.structure')

  }
}

new IdeaPlugin()
