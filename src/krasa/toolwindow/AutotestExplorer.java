package krasa.toolwindow;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

public class AutotestExplorer extends SimpleToolWindowPanel implements DataProvider, Disposable {

	private AutotestPanel autotestPanel;

	public AutotestExplorer(Project vertical) {
		super(true, true);
		if (autotestPanel == null) {
			autotestPanel = new AutotestPanel(vertical);
		}
		add(autotestPanel.getRoot());
	}

	@Override
	public void dispose() {
		autotestPanel = null;
	}
}
