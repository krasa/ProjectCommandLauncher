package krasa;

import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestWithDialog extends RunAutotest {
	public void actionPerformed(final AnActionEvent e) {
		runWithDialog(e);
	}

}
