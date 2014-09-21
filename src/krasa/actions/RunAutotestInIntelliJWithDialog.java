package krasa.actions;

import krasa.model.*;

import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestInIntelliJWithDialog extends RunAutotestInIntelliJ {

	public void actionPerformed(final AnActionEvent e) {
		runWithDialog(e);
	}

	protected void runWithDialog(AnActionEvent e) {
		String environment = DialogUtils.chooseEnvironment();
		if (environment != null) {
			TestFile element = getTestFile(e);
			AutotestState.getInstance().addTestFile(element);
			runInIDEA(e.getProject(), element);
		}
	}
}
