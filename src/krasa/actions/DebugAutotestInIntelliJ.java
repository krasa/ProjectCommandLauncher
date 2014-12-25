package krasa.actions;

import krasa.model.TestFile;

import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class DebugAutotestInIntelliJ extends RunAutotestInIntelliJ {

	@Override
	protected void execute(AnActionEvent e, TestFile element) {
		debugInIDEA(e.getProject(), element);
	}

	public void debugInIDEA(Project project, TestFile element) {
		ApplicationConfiguration applicationConfiguration = getApplicationConfiguration(project, element);
		try {
			// ExecutionEnvironmentBuilder executionEnvironmentBuilder = ExecutionEnvironmentBuilder.create(project,
			// DefaultDebugExecutor.getDebugExecutorInstance(), applicationConfiguration);
			// ExecutionEnvironment build = executionEnvironmentBuilder.build();
			// build.getRunner().execute(build);

			Executor runExecutorInstance = DefaultDebugExecutor.getDebugExecutorInstance();
			final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(DefaultDebugExecutor.EXECUTOR_ID,
					applicationConfiguration);
			ExecutionEnvironmentBuilder executionEnvironmentBuilder = new ExecutionEnvironmentBuilder(project,
					runExecutorInstance)
				.setRunnerId(runExecutorInstance.getId())
				.setRunProfile(applicationConfiguration);
			ExecutionEnvironment build = executionEnvironmentBuilder.build();
			runner.execute(build);

			// RunManagerEx.getInstanceEx(project).addConfiguration((RunnerAndConfigurationSettings)
			// build.getConfigurationSettings(), true);
		} catch (ExecutionException ex) {
			Messages.showMessageDialog(project, "error", "error", Messages.getErrorIcon());
		}
	}
}
