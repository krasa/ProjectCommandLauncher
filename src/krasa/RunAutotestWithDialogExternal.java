package krasa;

import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestWithDialogExternal extends RunAutotestExternal {
	public void actionPerformed(final AnActionEvent e) {
		runWithDialog(e);
	}
}
