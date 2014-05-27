package krasa;

import com.intellij.openapi.diagnostic.Logger;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
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
		try {
			GeneralCommandLine generalCommandLine = new GeneralCommandLine(cygwinExecutablePath(e), "--login", "-i");
			final Process process = generalCommandLine.createProcess();

			final OSProcessHandler osProcessHandler = new OSProcessHandler(process, "");
			osProcessHandler.addProcessListener(new ProcessAdapter() {
				public void onTextAvailable(ProcessEvent event, Key outputType) {
					if (event.getText().contains("Shutdown process finished")) {
						process.destroy();
					}
				}
			});
			AutotestContentExecutor executor = new AutotestContentExecutor(e.getProject(), osProcessHandler);
			executor.withRerun(new Runnable() {

				@Override
				public void run() {
					osProcessHandler.destroyProcess();
					osProcessHandler.waitFor(2000L);
					runInIDEA(e, enviroment);
				}
			});
			executor.run();

			writeCommands(e, process, enviroment);

		} catch (ExecutionException e1) {
			log.error(e1);
		}
	}

	private String cygwinExecutablePath(AnActionEvent e) {
		VirtualFile baseDir = e.getProject().getBaseDir();
		return baseDir.getPath().substring(0, 1) + ":" + "/cygwin/bin/bash.exe";
	}

	private void writeCommands(AnActionEvent e, Process process, String enviroment) {
		PrintWriter printWriter = new PrintWriter(process.getOutputStream());
		VirtualFile baseDir = e.getProject().getBaseDir();
		String path = baseDir.getPath();

		List<String> strings = Arrays.asList(goToDrive(path), goToBaseFolder(path), runAutotest(enviroment, e));
		for (String string : strings) {
			printWriter.println(string);

		}
		printWriter.flush();
	}

	private String getTestFileRelativePath(VirtualFile file) {
		String path1 = file.getPath();
		int i = path1.indexOf("/testscripts/");
		return path1.substring(i + "/testscripts/".length(), path1.length());
	}

	private String runAutotest(String enviroment, AnActionEvent e) {
		VirtualFile virtualFile = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		String testFileRelativePath = getTestFileRelativePath(virtualFile);
		return "./bin/TD.sh -e " + enviroment + " -m off -t verbose -f " + testFileRelativePath;
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
