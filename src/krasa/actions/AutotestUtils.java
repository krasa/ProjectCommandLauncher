package krasa.actions;

import com.intellij.openapi.vfs.VirtualFile;

public class AutotestUtils {

	public static String getTestFileRelativePath(String path) {
		int i = path.indexOf("/testscripts/");
		return path.substring(i + "/testscripts/".length(), path.length());
	}

	public static String getTestFileRelativePath(VirtualFile virtualFile) {
		return getTestFileRelativePath(virtualFile.getPath());
	}
}
