package krasa.actions;

import krasa.model.AutotestState;
import krasa.model.TestFile;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerRegistry;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestInIntelliJ extends DumbAwareAction {

	private static final Logger log = Logger.getInstance(RunAutotestInIntelliJ.class);

	public void actionPerformed(AnActionEvent e) {
		TestFile element = getTestFile(e);
		AutotestState.getInstance().addTestFile(element);
		execute(e, element);
	}

	protected void execute(AnActionEvent e, TestFile element) {
		runInIDEA(e.getProject(), element);
	}

	private String getEnvironment() {
		return AutotestState.getInstance().getLast();
	}

	protected TestFile getTestFile(AnActionEvent e) {
		String name = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName();
		return new TestFile(getEnvironment(), name, getTestFilePath(e), getTestFileFullPath(e));
	}

	public void runInIDEA(Project project, TestFile element) {
		ApplicationConfiguration applicationConfiguration = getApplicationConfiguration(project, element);
		try {

			Executor runExecutorInstance = DefaultRunExecutor.getRunExecutorInstance();
			final ProgramRunner runner = RunnerRegistry.getInstance().getRunner(DefaultRunExecutor.EXECUTOR_ID,
					applicationConfiguration);
			ExecutionEnvironmentBuilder executionEnvironmentBuilder = new ExecutionEnvironmentBuilder(project,
					runExecutorInstance).setRunnerId(runExecutorInstance.getId()).setRunProfile(
					applicationConfiguration);
			ExecutionEnvironment build = executionEnvironmentBuilder.build();
			runner.execute(build);
			// RunManagerEx.getInstanceEx(project).addConfiguration((RunnerAndConfigurationSettings)
			// build.getConfigurationSettings(), true);
		} catch (ExecutionException ex) {
			Messages.showMessageDialog(project, "error", "error", Messages.getErrorIcon());
		}
	}

	protected ApplicationConfiguration getApplicationConfiguration(Project project, TestFile testFile) {
		ApplicationConfiguration ac = new ApplicationConfiguration(String.valueOf(testFile.getName()), project,
				ApplicationConfigurationType.getInstance());
		ac.setMainClassName("com.tmobile.utils.autotesting.control.MainIntellij");
		ac.setWorkingDirectory(getWorkingDirectory(project, testFile));
		ac.setVMParameters("-Dsystem.code=" + testFile.getEnviroment() + " -Dfile=" + testFile.getPath());
		ac.setModule(getModule(project, testFile));

		return ac;
	}

	protected String getWorkingDirectory(Project project, TestFile testFile) {
		final String testFileFullPath = testFile.getFullPath();
		if (testFileFullPath == null) {
			// backward compatibility
			VirtualFile baseDir = project.getBaseDir();
			return baseDir.getPath() + "/tool/xmlbasedtests";
		}

		String workingDirectory = null;
		if (testFileFullPath.contains("tool/xmlbasedtests")) {
			workingDirectory = testFileFullPath.substring(0, testFileFullPath.indexOf("tool/xmlbasedtests")
					+ "tool/xmlbasedtests".length());
		}
		return workingDirectory;
	}

	private Module getModule(Project project, TestFile element) {
		final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
		String fullPath = element.getFullPath();
		if (fullPath == null) {
			// backward compatibility
			return ModuleManager.getInstance(project).getModules()[0];
		}
		VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl("file://" + fullPath);
		return projectFileIndex.getModuleForFile(fileByUrl);
	}

	private String getTestFilePath(AnActionEvent e) {
		VirtualFile virtualFile = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		return AutotestUtils.getTestFileRelativePath(virtualFile);
	}

	private String getTestFileFullPath(AnActionEvent e) {
		VirtualFile virtualFile = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		return AutotestUtils.getTestFileFullPath(virtualFile);
	}

	@Override
	public void update(AnActionEvent e) {
		super.update(e);
		determineVisibility(e);
	}

	private void determineVisibility(AnActionEvent e) {
		DataContext dataContext = e.getDataContext();
		VirtualFile virtualFile = PlatformDataKeys.VIRTUAL_FILE.getData(dataContext);
		boolean visible = false;
		if (isTestScript(virtualFile)) {
			visible = true;
		}
		e.getPresentation().setVisible(visible);
	}

	private boolean isTestScript(VirtualFile file) {
		return isXml(file) && file.getPath().contains("tool/xmlbasedtests/testscripts");
	}

	private boolean isXml(VirtualFile file) {
		if (file == null) {
			return false;
		}
		return XmlFileType.INSTANCE.equals(file.getFileType());
	}

}
