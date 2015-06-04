package krasa.toolwindow;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
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

	@Nullable
	@Override
	public Object getData(String dataId) {
		if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
			return autotestPanel.getCopyProvider();
		}
		return super.getData(dataId);
	}

	@Override
	public void dispose() {
		autotestPanel = null;
	}
}
