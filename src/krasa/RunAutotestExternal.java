package krasa;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;

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

            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "runAutotest.bat", xmltestsPath, testFile, enviroment}, null, new File("\\workspace\\"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
