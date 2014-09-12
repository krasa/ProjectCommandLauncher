package krasa.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestInIntelliJWithDialog extends RunAutotestInIntelliJ {

	public void actionPerformed(final AnActionEvent e) {
		runWithDialog(e);
	}
}
