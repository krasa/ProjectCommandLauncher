package krasa.actions;

import krasa.model.TestFile;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public class DebugAutotestInIntelliJ extends RunAutotestInIntelliJ {

	public void debugInIDEA(Project project, TestFile element) {
		ApplicationConfiguration applicationConfiguration = getApplicationConfiguration(project, element);
		try {
			ExecutionEnvironmentBuilder executionEnvironmentBuilder = ExecutionEnvironmentBuilder.create(project,
					DefaultDebugExecutor.getDebugExecutorInstance(), applicationConfiguration);
			ExecutionEnvironment build = executionEnvironmentBuilder.build();
			build.getRunner().execute(build);
			// RunManagerEx.getInstanceEx(project).addConfiguration((RunnerAndConfigurationSettings)
			// build.getConfigurationSettings(), true);
		} catch (ExecutionException ex) {
			Messages.showMessageDialog(project, "error", "error", Messages.getErrorIcon());
		}
	}
}
