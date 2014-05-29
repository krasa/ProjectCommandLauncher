package krasa;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Vojtech Krasa
 */
public class RunAutotest extends DumbAwareAction {

	private static final Logger log = Logger.getInstance(RunAutotest.class);

	public void actionPerformed(AnActionEvent e) {
		runInIDEA(e, getEnvironment());
	}

	private String getEnvironment() {
		return AutotestState.getInstance().getLast();
	}

	protected void runWithDialog(AnActionEvent e) {
		String enviroment;
		String[] choices = AutotestState.getInstance().getChoices();
		String last = AutotestState.getInstance().getLast();
		enviroment = Messages.showEditableChooseDialog("Enviroment", "Enviroment", Messages.getQuestionIcon(), choices,
				last, new NonEmptyInputValidator());
		if (enviroment != null) {
			AutotestState.getInstance().addEnvironment(enviroment);
			runInIDEA(e, enviroment);
		}
	}

	protected void runInIDEA(final AnActionEvent e, final String enviroment) {
		VirtualFile baseDir = e.getProject().getBaseDir();
		String path = baseDir.getPath();
		String testFilePath = getTestFilePath(e);
		String cygwinPath = cygwinExecutablePath(e);
		runInIDEA(e.getProject(), enviroment, path, testFilePath, cygwinPath);

	}

	private void runInIDEA(final Project project, final String enviroment, final String baseDir,
			final String testFilePath, final String cygwinPath) {
		try {
			GeneralCommandLine generalCommandLine = new GeneralCommandLine(cygwinPath, "--login", "-i");
			final Process process = generalCommandLine.createProcess();

			final OSProcessHandler osProcessHandler = new OSProcessHandler(process, "");
			osProcessHandler.addProcessListener(new ProcessAdapter() {

				public void onTextAvailable(ProcessEvent event, Key outputType) {
					if (event.getText().contains("Shutdown process finished")) {
						process.destroy();
					}
				}
			});
			AutotestContentExecutor executor = new AutotestContentExecutor(project, osProcessHandler);
			executor.withRerun(new Runnable() {

				@Override
				public void run() {
					osProcessHandler.destroyProcess();
					osProcessHandler.waitFor(2000L);
					runInIDEA(project, enviroment, baseDir, testFilePath, cygwinPath);
				}
			});
			executor.withStop(new Runnable() {

				@Override
				public void run() {
					osProcessHandler.destroyProcess();
				}
			}, new Computable<Boolean>() {

				@Override
				public Boolean compute() {
					return !osProcessHandler.isProcessTerminated();
				}
			});
			executor.run();

			PrintWriter printWriter = new PrintWriter(process.getOutputStream());
			List<String> strings = Arrays.asList(goToDrive(baseDir), goToBaseFolder(baseDir),
					runAutotest(enviroment, testFilePath));
			for (String string : strings) {
				printWriter.println(string);

			}
			printWriter.flush();

		} catch (ExecutionException e1) {
			throw new RuntimeException(e1);
		}
	}

	private String cygwinExecutablePath(AnActionEvent e) {
		VirtualFile baseDir = e.getProject().getBaseDir();
		return baseDir.getPath().substring(0, 1) + ":" + "/cygwin/bin/bash.exe";
	}

	private String getTestFileRelativePath(String path) {
		int i = path.indexOf("/testscripts/");
		return path.substring(i + "/testscripts/".length(), path.length());
	}

	private String runAutotest(String enviroment, String testFilePath) {
		return "./bin/TD.sh -e " + enviroment + " -m off -t verbose -f " + testFilePath;
	}

	private String getTestFilePath(AnActionEvent e) {
		VirtualFile virtualFile = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		return getTestFileRelativePath(virtualFile.getPath());
	}

	private String goToBaseFolder(String path) {
		return "cd /cygdrive/" + path.replaceAll(":", "") + "/tool/xmlbasedtests";
	}

	private String goToDrive(String path) {
		return "cd " + path.substring(0, 1) + ":";
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
