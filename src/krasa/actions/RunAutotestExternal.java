package krasa.actions;

import java.io.*;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestExternal extends RunAutotest {

	public static final String XMLBASEDTESTS = "testscripts/";

	@Override
	protected void runInIDEA(AnActionEvent e, String enviroment) {
		VirtualFile data = PlatformDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
		try {
			FileDocumentManager documentManager = FileDocumentManager.getInstance();
			documentManager.saveDocument(documentManager.getDocument(data));
			String path1 = data.getPath();
			int i = path1.lastIndexOf(XMLBASEDTESTS);

			String testFile = "./" + path1.substring(i + XMLBASEDTESTS.length());

			VirtualFile baseDir = e.getProject().getBaseDir();
			String path = baseDir.getPath();
			String xmltestsPath = path + "/tool/xmlbasedtests/";
			xmltestsPath = xmltestsPath.replaceAll("/", "\\\\");

			Runtime.getRuntime().exec(
					new String[] { "cmd", "/c", "start", "runAutotest.bat", xmltestsPath, testFile, enviroment }, null,
					new File("\\workspace\\"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
