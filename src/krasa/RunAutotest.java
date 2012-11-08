package krasa;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class RunAutotest extends AnAction {

    public static final String TMDEV_LOCALHOST_DE = "tmdev-localhost-de";

    public void actionPerformed(AnActionEvent e) {
		runInIDEA(e, TMDEV_LOCALHOST_DE);
	}

	protected void runInIDEA(AnActionEvent e, String enviroment) {
		try {
            GeneralCommandLine generalCommandLine = new GeneralCommandLine(cygwinExecutablePath(e), "--login", "-i");
			final Process process = generalCommandLine.createProcess();

			OSProcessHandler osProcessHandler = new OSProcessHandler(process, "");
			osProcessHandler.addProcessListener(new ProcessAdapter() {
				public void onTextAvailable(ProcessEvent event, Key outputType) {
					if (event.getText().contains("Shutdown process finished")) {
						process.destroy();
					}
				}
			});
			RunContentExecutor executor = new RunContentExecutor(e.getProject(), osProcessHandler);
			executor.run();

			writeCommands(e, process, enviroment);

		} catch (ExecutionException e1) {
			e1.printStackTrace();
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

        List<String> strings = Arrays.asList(goToDrive(path), goToBaseFolder(path),
                runAutotest(enviroment,e )
		);
		for (String string : strings) {
			printWriter.println(string);

		}
		printWriter.flush();
	}

    private String getTestFileRelativePath(VirtualFile file) {
        String path1 = file.getPath();
        int i = path1.indexOf("/portal/");
        return path1.substring(i + 1, path1.length());
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
        return "cd "+path.substring(0, 1)+":";
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
