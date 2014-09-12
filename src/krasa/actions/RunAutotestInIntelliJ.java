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
import com.intellij.openapi.ui.*;
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

	protected void runWithDialog(AnActionEvent e) {
		String[] choices = AutotestState.getInstance().getChoices();
		String last = AutotestState.getInstance().getLast();
		String enviroment = Messages.showEditableChooseDialog("Enviroment", "Enviroment", Messages.getQuestionIcon(),
				choices,
				last, new NonEmptyInputValidator());
		if (enviroment != null) {
			AutotestState.getInstance().addEnvironment(enviroment);
			TestFile element = getTestFile(e);
			AutotestState.getInstance().addTestFile(element);
			runInIDEA(e.getProject(), element);
		}
	}

	private String getEnvironment() {
		return AutotestState.getInstance().getLast();
	}

	private TestFile getTestFile(AnActionEvent e) {
		String name = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext()).getName();
		return new TestFile(getEnvironment(), name, getTestFilePath(e));
	}

	public void runInIDEA(Project project, TestFile element) {
		VirtualFile baseDir = project.getBaseDir();

		ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration(
				String.valueOf(element.getName()), project,
				ApplicationConfigurationType.getInstance());
		applicationConfiguration.setMainClassName("com.tmobile.utils.autotesting.control.MainIntellij");
		applicationConfiguration.setWorkingDirectory(baseDir.getPath() + "/tool/xmlbasedtests");
		applicationConfiguration.setVMParameters("-Dsystem.code=" + element.getEnviroment() + " -Dfile="
				+ element.getPath());
		Module module = ModuleManager.getInstance(project).getModules()[0];
		applicationConfiguration.setModule(module);
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

	private String getTestFileRelativePath(String path) {
		int i = path.indexOf("/testscripts/");
		return path.substring(i + "/testscripts/".length(), path.length());
	}

	private String getTestFilePath(AnActionEvent e) {
		VirtualFile virtualFile = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		return getTestFileRelativePath(virtualFile.getPath());
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
