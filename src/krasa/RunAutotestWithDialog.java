package krasa;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.NonEmptyInputValidator;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.UIBundle;
import com.intellij.ui.popup.PopupFactoryImpl;

import java.awt.*;

/**
 * @author Vojtech Krasa
 */
public class RunAutotestWithDialog extends RunAutotest {
	public void actionPerformed(final AnActionEvent e) {
        String enviroment;
          enviroment = Messages.showInputDialog("Enviroment",
                  "Enviroment", Messages.getQuestionIcon(),TMDEV_LOCALHOST_DE,new NonEmptyInputValidator());
        if (enviroment != null) {
            runInIDEA(e, enviroment);
        }
	}
}
