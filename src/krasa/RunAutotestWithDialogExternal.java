package krasa;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestWithDialogExternal extends RunAutotestExternal {
    public void actionPerformed(final AnActionEvent e) {
        String enviroment;
        enviroment = Messages.showInputDialog("Enviroment",
                "Enviroment", Messages.getQuestionIcon(), TMDEV_LOCALHOST_DE, new NonEmptyInputValidator());
        if (enviroment != null) {
            runInIDEA(e, enviroment);
        }
    }
}
