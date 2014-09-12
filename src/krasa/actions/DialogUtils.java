package krasa.actions;

import krasa.model.AutotestState;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;

public class DialogUtils {
	public static String chooseEnvironment() {
		String[] choices = AutotestState.getInstance().getChoices();
		String last = AutotestState.getInstance().getLast();
		String s = Messages.showEditableChooseDialog("Environment", "Environment", Messages.getQuestionIcon(), choices,
				last, new NonEmptyInputValidator());
		if (s != null) {
			AutotestState.getInstance().addEnvironment(s);
		}
		return s;
	}

}
