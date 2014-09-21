package krasa.actions;

import krasa.model.*;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.*;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.*;
import com.intellij.openapi.project.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestInIntelliJ extends DumbAwareAction {

	private static final Logger log = Logger.getInstance(RunAutotestInIntelliJ.class);

	public void actionPerformed(AnActionEvent e) {
		TestFile element = getTestFile(e);
		AutotestState.getInstance().addTestFile(element);
		runInIDEA(e.getProject(), element);
	}

	private String getEnvironment() {
		return AutotestState.getInstance().getLast();
	}

	protected TestFile getTestFile(AnActionEvent e) {
		String name = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName();
		return new TestFile(getEnvironment(), name, getTestFilePath(e));
	}

	public void runInIDEA(Project project, TestFile element) {
		ApplicationConfiguration applicationConfiguration = getApplicationConfiguration(project, element);
		try {
			ExecutionEnvironmentBuilder executionEnvironmentBuilder = ExecutionEnvironmentBuilder.create(project,
					DefaultRunExecutor.getRunExecutorInstance(), applicationConfiguration);
			ExecutionEnvironment build = executionEnvironmentBuilder.build();
			build.getRunner().execute(build);
			// RunManagerEx.getInstanceEx(project).addConfiguration((RunnerAndConfigurationSettings)
			// build.getConfigurationSettings(), true);
		} catch (ExecutionException ex) {
			Messages.showMessageDialog(project, "error", "error", Messages.getErrorIcon());
		}
	}

	protected ApplicationConfiguration getApplicationConfiguration(Project project, TestFile element) {
		VirtualFile baseDir = project.getBaseDir();
		ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration(
				String.valueOf(element.getName()), project, ApplicationConfigurationType.getInstance());
		applicationConfiguration.setMainClassName("com.tmobile.utils.autotesting.control.MainIntellij");
		applicationConfiguration.setWorkingDirectory(baseDir.getPath() + "/tool/xmlbasedtests");
		applicationConfiguration.setVMParameters("-Dsystem.code=" + element.getEnviroment() + " -Dfile="
				+ element.getPath());
		Module module = ModuleManager.getInstance(project).getModules()[0];
		applicationConfiguration.setModule(module);
		return applicationConfiguration;
	}

	private String getTestFilePath(AnActionEvent e) {
		VirtualFile virtualFile = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		return AutotestUtils.getTestFileRelativePath(virtualFile);
	}

	@Override
	public void update(AnActionEvent e) {
		super.update(e);
		determineVisibility(e);
	}

	private void determineVisibility(AnActionEvent e) {
		DataContext dataContext = e.getDataContext();
		VirtualFile data1 = PlatformDataKeys.VIRTUAL_FILE.getData(dataContext);
		FileType fileType = null;
		if (data1 != null) {
			fileType = data1.getFileType();
		}
		if (fileType != null) {
			e.getPresentation().setVisible(fileType.equals(XmlFileType.INSTANCE));
		}
	}

}
